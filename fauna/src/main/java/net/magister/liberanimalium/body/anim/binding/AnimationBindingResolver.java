package net.magister.liberanimalium.body.anim.binding;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.Map.Entry;
import org.jetbrains.annotations.Nullable;
import mod.azure.azurelib.animation.AzAnimator;
import mod.azure.azurelib.animation.controller.AzAnimationController;
import mod.azure.azurelib.animation.controller.AzAnimationControllerContainer;
import mod.azure.azurelib.animation.dispatch.command.AzCommand;
import mod.azure.azurelib.animation.dispatch.command.AzControllerCommandBuilder;
import mod.azure.azurelib.animation.easing.AzEasingType;
import net.magister.liberanimalium.anim.ExpressionEventClient;
import net.magister.liberanimalium.anim.mask.BoneMask;
import net.magister.liberanimalium.anim.molang.AnimQueries;
import net.magister.liberanimalium.anim.rig.LimbDef;
import net.magister.liberanimalium.anim.v2.Ambit;
import net.magister.liberanimalium.anim.v2.ControllerMachine;
import net.magister.liberanimalium.anim.v2.ControllerSpec;
import net.magister.liberanimalium.anim.v2.LayerSpec;
import net.magister.liberanimalium.anim.v2.StateSpec;
import net.magister.liberanimalium.body.anim.data.BoneAccessCache;
import net.magister.liberanimalium.body.state.FaunaStateContext;
import net.magister.liberanimalium.body.state.TransportMode;
import net.magister.liberanimalium.chassis.FaunaAnimator;
import net.magister.liberanimalium.chassis.FaunaBase;
import net.magister.liberanimalium.chassis.FaunaDispatcher;
import net.magister.liberanimalium.fauna.api.ExpressionKey;
import net.minecraft.world.phys.Vec3;

public final class AnimationBindingResolver {
   private final Map<String, String> lastKey = new HashMap<>();
   private final Map<String, String> interrupts = new HashMap<>();
   private final Map<String, Integer> feetTaken = new HashMap<>();
   private boolean feetHeld;
   private TransportMode lastTransport = null;
   private static final int WAKE_TICKS = 5;
   private int wake;
   private final Map<String, Float> weights = new HashMap<>();
   private final Map<String, Float> lastWeights = new HashMap<>();
   private final Map<String, StateSpec> lastWeighted = new HashMap<>();
   private boolean wasPlayingAbilityAnim = false;
   private final Map<String, ControllerMachine> machines = new HashMap<>();
   private AnimationBindingTable machineTable;
   private final Set<String> suppressedLayers = new HashSet<>();
   private String machinePrefix;
   private static final float WEIGHT_EPSILON = 0.005F;
   private static final float ANIM_SPEED_SMOOTHING = 0.2F;

   public void resolve(FaunaBase body, FaunaStateContext ctx, FaunaDispatcher dispatcher) {
      if (body.level().isClientSide() && dispatcher != null) {
         if (dispatcher.ready()) {
            AnimationBindingTable table = AnimationBindingLoader.get(body.animBindingKey());
            StateSnapshot snapshot = StateSnapshot.from(body);
            String prefix = body.animClipPrefix();
            TransportMode currentTransport = snapshot.transport();
            boolean transportChanged = currentTransport != this.lastTransport;
            if (transportChanged && this.lastTransport != null) {
               this.cancelTransportGroup(this.lastTransport, currentTransport, table, dispatcher);
            }

            if (transportChanged) {
               this.wake = 5;
            }

            AnimQueries.install(AnimSignals.scalarKeys());
            AnimQueries.bind(snapshot);
            this.driveControllers(body, table, snapshot, dispatcher, prefix);
            this.handleAbility(body, dispatcher);
            if (this.wake > 0) {
               this.wake--;
            }

            this.lastTransport = currentTransport;
         }
      }
   }

   private void cancelTransportGroup(TransportMode mode, TransportMode next, AnimationBindingTable table, FaunaDispatcher dispatcher) {
      for (Entry<String, ControllerSpec> entry : table.v2.controllers().entrySet()) {
         Set<String> choices = entry.getValue().activeWhen().enumChoices("transport");
         if (choices != null && choices.contains(mode.name()) && !choices.contains(next.name())) {
            this.cancelAndForget(entry.getKey(), dispatcher);
            this.weights.remove(entry.getKey());
            this.lastWeights.remove(entry.getKey());
         }
      }
   }

   private void cancelAndForget(String slotName, FaunaDispatcher dispatcher) {
      dispatcher.cancelSlot(slotName);
      this.clearSlotState(slotName);
   }

   private void driveControllers(FaunaBase body, AnimationBindingTable table, StateSnapshot snapshot, FaunaDispatcher dispatcher, String prefix) {
      if (!table.v2.controllers().isEmpty()) {
         if (this.machineTable != table) {
            this.machines.clear();
            this.machineTable = table;
            this.applyLayerProperties(table, body);
         }

         this.machinePrefix = prefix;
         this.suppressedLayers.clear();
         this.feetHeld = false;
         Iterator<Entry<String, Integer>> it = this.feetTaken.entrySet().iterator();

         while (it.hasNext()) {
            Entry<String, Integer> taken = it.next();
            boolean playing = this.interrupts.containsKey(taken.getKey())
               && (body.tickCount - taken.getValue() <= 1 || !this.isControllerSilent(taken.getKey(), body));
            if (playing) {
               this.feetHeld = true;
            } else {
               it.remove();
            }
         }

         LinkedHashMap<String, ControllerMachine.Step> steps = new LinkedHashMap<>();

         for (String layer : table.v2.controllers().keySet()) {
            ControllerMachine machine = this.machines.computeIfAbsent(layer, name -> new ControllerMachine(table.v2.controller(name)));
            Ambit ambit = new Ambit(body.tickCount, body.getRandom()::nextInt, prefix, dispatcher::hasAnimation, this.isControllerDone(layer, body));
            steps.put(layer, machine.tick(snapshot, ambit));
            this.suppressedLayers.addAll(machine.suppressed());
         }

         LinkedHashMap<String, AnimationBindingResolver.Play> plays = new LinkedHashMap<>();
         HashSet<String> starting = new HashSet<>();

         for (Entry<String, ControllerMachine.Step> entry : steps.entrySet()) {
            String layer = entry.getKey();
            ControllerMachine machine = this.machines.get(layer);
            StateSpec state = entry.getValue().state() == null ? null : machine.spec().state(entry.getValue().state());
            String clip = state != null && !this.suppressedLayers.contains(layer) ? machine.currentClip(prefix) : null;
            if (clip != null) {
               PlayBehavior behavior = PlayBehavior.parse(state.behavior(), PlayBehavior.LOOP);
               String key = clip + "\u0000" + behavior.name();
               plays.put(layer, new AnimationBindingResolver.Play(state, clip, behavior, key));
               boolean fresh = !key.equals(this.lastKey.get(layer)) || this.isControllerSilent(layer, body);
               if (fresh && dispatcher.hasAnimation(clip)) {
                  starting.add(layer);
               }
            }
         }

         for (Entry<String, AnimationBindingResolver.Play> play : plays.entrySet()) {
            String layer = play.getKey();
            StateSpec state = play.getValue().state();
            if (state.resyncing()) {
               boolean resyncing = !starting.contains(layer) && starting.contains(state.syncTo()) && dispatcher.hasAnimation(play.getValue().clip());
               if (!resyncing) {
                  this.restoreTransitionLength(layer, body);
               } else {
                  this.matchTransitionLength(layer, state.syncTo(), body);
                  dispatcher.cancelSlot(layer);
                  this.clearSlotState(layer);
                  starting.add(layer);
               }
            }
         }

         HashMap<String, Float> rates = new HashMap<>();

         for (Entry<String, AnimationBindingResolver.Play> play : plays.entrySet()) {
            if (!play.getValue().state().borrowsSpeed()) {
               Float rate = this.applyAnimSpeed(play.getKey(), play.getValue().state(), body);
               if (rate != null) {
                  rates.put(play.getKey(), rate);
               }
            }
         }

         for (Entry<String, AnimationBindingResolver.Play> play : plays.entrySet()) {
            StateSpec state = play.getValue().state();
            if (state.borrowsSpeed()) {
               Float rate = rates.get(state.speedFrom());
               this.pushAnimSpeed(play.getKey(), rate == null ? 1.0F : rate, body);
            }
         }

         for (LayerSpec layerSpec : table.v2.layers()) {
            if (layerSpec.mode() == LayerSpec.Mode.WEIGHTED) {
               String layer = layerSpec.name();
               AnimationBindingResolver.Play play = plays.get(layer);
               StateSpec state = play == null ? null : play.state();
               float current = this.weights.getOrDefault(layer, 0.0F);
               float next;
               if (state != null && state.weighted()) {
                  this.lastWeighted.put(layer, state);
                  float target = (float)Math.max(0.0, Math.min(1.0, state.animWeight().eval(0.0)));
                  next = this.wake > 0 && state.weightOnWake()
                     ? target
                     : (
                        target >= current
                           ? current + (target - current) * (float)state.weightIn()
                           : Math.max(target, current - 1.0F / Math.max(1, this.lastWeighted.get(layer).weightOutTicks()))
                     );
               } else {
                  StateSpec was = this.lastWeighted.get(layer);
                  next = was == null ? 0.0F : Math.max(0.0F, current - 1.0F / Math.max(1, was.weightOutTicks()));
               }

               if (next < 0.005F) {
                  next = 0.0F;
               }

               this.lastWeights.put(layer, current);
               this.weights.put(layer, next);
               this.pushWeight(layer, next, body);
            }
         }

         for (Entry<String, ControllerMachine.Step> entry : steps.entrySet()) {
            String layer = entry.getKey();
            ControllerMachine machine = this.machines.get(layer);
            ControllerMachine.Step step = entry.getValue();
            if (step.exited() != null) {
               this.fireStateEvent(body, table, machine.spec().state(step.exited()), false);
            }

            if (step.entered() != null) {
               this.fireStateEvent(body, table, machine.spec().state(step.entered()), true);
            }

            if (this.interrupts.containsKey(layer)) {
               boolean moved = step.entered() != null;
               if (!moved && !this.isControllerSilent(layer, body)) {
                  continue;
               }

               this.interrupts.remove(layer);
            }

            AnimationBindingResolver.Play play = plays.get(layer);
            if (play == null) {
               if (!(this.weights.getOrDefault(layer, 0.0F) > 0.0F) || !this.lastKey.containsKey(layer)) {
                  if (this.lastKey.containsKey(layer)) {
                     dispatcher.cancelSlot(layer);
                  }

                  this.clearSlotState(layer);
               }
            } else if (starting.contains(layer) && dispatcher.hasAnimation(play.clip())) {
               AzCommand.create(layer, play.clip(), play.behavior().az).sendForEntity(body);
               this.lastKey.put(layer, play.key());
            }
         }
      }
   }

   @Nullable
   private Float applyAnimSpeed(String layer, StateSpec state, FaunaBase body) {
      if (state != null && state.animSpeed() != null && state.animSpeed().present()) {
         float target = (float)state.animSpeed().eval(1.0);
         if (Float.isFinite(target) && target != 0.0F) {
            BoneAccessCache cache = body.getBoneAccessCache();
            float prev = cache.getSmoothedSpeedFactor(layer, target);
            float smoothed = prev + (target - prev) * 0.2F;
            this.pushAnimSpeed(layer, smoothed, body);
            return smoothed;
         } else {
            return null;
         }
      } else {
         return null;
      }
   }

   private void pushWeight(String layer, float weight, FaunaBase body) {
      body.getBoneAccessCache().setSmoothedRotation(layer, new Vec3(weight, 0.0, 0.0));
      AzAnimator<UUID, FaunaBase> animator = body.getAnimatorOrNull();
      if (animator != null) {
         AzAnimationControllerContainer<FaunaBase> container = animator.getAnimationControllerContainer();
         if (container != null) {
            AzAnimationController<FaunaBase> ctrl = container.getOrNull(layer);
            if (ctrl != null) {
               ctrl.setWeight(weight);
            }
         }
      }
   }

   public void frameWeights(FaunaBase body, float partialTicks) {
      if (!this.weights.isEmpty()) {
         AzAnimator<UUID, FaunaBase> animator = body.getAnimatorOrNull();
         if (animator != null) {
            AzAnimationControllerContainer<FaunaBase> container = animator.getAnimationControllerContainer();
            if (container != null) {
               float share = Math.max(0.0F, Math.min(1.0F, partialTicks));

               for (Entry<String, Float> entry : this.weights.entrySet()) {
                  AzAnimationController<FaunaBase> ctrl = container.getOrNull(entry.getKey());
                  if (ctrl != null) {
                     float from = this.lastWeights.getOrDefault(entry.getKey(), entry.getValue());
                     float weight = from + (entry.getValue() - from) * share;
                     ctrl.setWeight(weight);
                     body.getBoneAccessCache().setSmoothedRotation(entry.getKey(), new Vec3(weight, 0.0, 0.0));
                  }
               }
            }
         }
      }
   }

   private void pushAnimSpeed(String layer, float rate, FaunaBase body) {
      body.getBoneAccessCache().setSmoothedSpeedFactor(layer, rate);
      AzCommand.controllerBuilder().setSpeed(layer, rate).build().sendForEntity(body);
   }

   private void fireStateEvent(FaunaBase body, AnimationBindingTable table, StateSpec state, boolean entering) {
      if (state != null) {
         String key = entering ? state.onEnterEvent() : state.onExitEvent();
         if (key != null) {
            ExpressionEventClient.fireLocal(body, body.getId(), key, body.level().getGameTime());
         }
      }
   }

   private void clearSlotState(String slotName) {
      this.lastKey.remove(slotName);
      this.interrupts.remove(slotName);
   }

   private void matchTransitionLength(String follower, String leader, FaunaBase body) {
      AzAnimator<UUID, FaunaBase> animator = body.getAnimatorOrNull();
      if (animator != null) {
         AzAnimationControllerContainer<FaunaBase> container = animator.getAnimationControllerContainer();
         if (container != null) {
            AzAnimationController<FaunaBase> lead = container.getOrNull(leader);
            AzAnimationController<FaunaBase> follow = container.getOrNull(follower);
            if (lead != null && follow != null) {
               float wanted = lead.animationProperties().transitionLength();
               if (follow.animationProperties().transitionLength() != wanted) {
                  AzCommand.controllerBuilder().setTransitionSpeed(follower, wanted).build().sendForEntity(body);
               }
            }
         }
      }
   }

   private void restoreTransitionLength(String layer, FaunaBase body) {
      LayerSpec spec = this.machineTable == null ? null : this.machineTable.v2.layer(layer);
      if (spec != null) {
         AzAnimator<UUID, FaunaBase> animator = body.getAnimatorOrNull();
         if (animator != null) {
            AzAnimationControllerContainer<FaunaBase> container = animator.getAnimationControllerContainer();
            if (container != null) {
               AzAnimationController<FaunaBase> ctrl = container.getOrNull(layer);
               if (ctrl != null) {
                  if (ctrl.animationProperties().transitionLength() != spec.transition()) {
                     AzCommand.controllerBuilder().setTransitionSpeed(layer, spec.transition()).build().sendForEntity(body);
                  }
               }
            }
         }
      }
   }

   private void applyLayerProperties(AnimationBindingTable table, FaunaBase body) {
      AzAnimator<UUID, FaunaBase> animator = body.getAnimatorOrNull();
      if (animator != null) {
         AzAnimationControllerContainer<FaunaBase> container = animator.getAnimationControllerContainer();
         if (container != null) {
            for (LayerSpec layer : table.v2.layers()) {
               AzAnimationController<FaunaBase> ctrl = container.getOrNull(layer.name());
               if (ctrl != null) {
                  AzControllerCommandBuilder builder = AzCommand.controllerBuilder();
                  boolean send = false;
                  if (ctrl.animationProperties().transitionLength() != layer.transition()) {
                     builder.setTransitionSpeed(layer.name(), layer.transition());
                     send = true;
                  }

                  AzEasingType easing = FaunaAnimator.easingOf(layer);
                  if (easing != null && ctrl.animationProperties().easingType() != easing) {
                     builder.setEasingType(layer.name(), easing);
                     send = true;
                  }

                  if (send) {
                     builder.build().sendForEntity(body);
                  }
               }
            }
         }
      }
   }

   private boolean isControllerDone(String controllerName, FaunaBase body) {
      if (this.isControllerSilent(controllerName, body)) {
         return true;
      }

      AzAnimator<UUID, FaunaBase> animator = body.getAnimatorOrNull();
      AzAnimationControllerContainer<FaunaBase> container = animator == null ? null : animator.getAnimationControllerContainer();
      AzAnimationController<FaunaBase> ctrl = container == null ? null : container.getOrNull(controllerName);
      return ctrl != null && ctrl.stateMachine().isStopped();
   }

   private boolean isControllerSilent(String controllerName, FaunaBase body) {
      AzAnimator<UUID, FaunaBase> animator = body.getAnimatorOrNull();
      if (animator == null) {
         return false;
      }

      AzAnimationControllerContainer<FaunaBase> container = animator.getAnimationControllerContainer();
      if (container == null) {
         return false;
      }

      AzAnimationController<FaunaBase> ctrl = container.getOrNull(controllerName);
      return ctrl != null && ctrl.currentAnimation() == null && ctrl.animationQueue().isEmpty();
   }

   private void handleAbility(FaunaBase body, FaunaDispatcher dispatcher) {
      boolean hasAbilityAnim = body.hasActiveAbilityAnimation();
      if (hasAbilityAnim && !this.wasPlayingAbilityAnim) {
         String animName = body.getActiveAbilityAnimation();
         if (animName != null && !animName.isEmpty()) {
            ExpressionEventClient.fireLocal(body, body.getId(), ExpressionKey.body(ExpressionKey.Body.ABILITY).key(), body.level().getGameTime());
         }

         this.wasPlayingAbilityAnim = true;
      } else if (!hasAbilityAnim && this.wasPlayingAbilityAnim) {
         this.wasPlayingAbilityAnim = false;
      }
   }

   public void interrupt(String layer, String fullClip, FaunaBase body) {
      AzCommand.create(layer, fullClip, PlayBehavior.PLAY_ONCE.az).sendForEntity(body);
      this.lastKey.put(layer, fullClip + "\u0000" + PlayBehavior.PLAY_ONCE.name());
      this.interrupts.put(layer, fullClip);
      if (movesSteppingLegs(layer, fullClip, body)) {
         this.feetTaken.put(layer, body.tickCount);
      } else {
         this.feetTaken.remove(layer);
      }
   }

   private static boolean movesSteppingLegs(String layer, String fullClip, FaunaBase body) {
      FaunaDispatcher dispatcher = body.getDispatcher();
      if (dispatcher == null) {
         return false;
      }

      AnimationBindingTable table = AnimationBindingLoader.get(body.animBindingKey());
      if (table.steps.limbs.isEmpty()) {
         return false;
      }

      BoneMask mask = table.maskFor(layer);
      Set<String> legs = new HashSet<>();

      for (LimbDef limb : table.steps.limbs) {
         legs.add(limb.root());
         legs.add(limb.mid());
         legs.add(limb.foot());
         if (limb.knee() != null) {
            legs.add(limb.knee());
         }
      }

      return dispatcher.clipKeys(fullClip, bone -> legs.contains(bone) && mask.allows(bone));
   }

   public String getActiveClip(String layer) {
      ControllerMachine machine = this.machines.get(layer);
      if (machine != null && this.machinePrefix != null) {
         String clip = machine.currentClip(this.machinePrefix);
         if (clip == null) {
            return null;
         } else {
            return clip.startsWith(this.machinePrefix) ? clip.substring(this.machinePrefix.length()) : clip;
         }
      } else {
         return null;
      }
   }

   public boolean stepsAllowed() {
      return this.heldSteps(false);
   }

   public boolean stepsMoving() {
      return this.heldSteps(true);
   }

   public Set<String> limbsOff() {
      Set<String> off = null;

      for (Entry<String, ControllerMachine> entry : this.machines.entrySet()) {
         String current = entry.getValue().current();
         if (current != null && !this.suppressedLayers.contains(entry.getKey())) {
            StateSpec state = entry.getValue().spec().state(current);
            if (state != null && !state.limbsOff().isEmpty()) {
               if (off == null) {
                  off = new HashSet<>();
               }

               off.addAll(state.limbsOff());
            }
         }
      }

      return off == null ? Set.of() : off;
   }

   private boolean heldSteps(boolean moving) {
      for (Entry<String, ControllerMachine> entry : this.machines.entrySet()) {
         ControllerMachine machine = entry.getValue();
         String current = machine.current();
         if (current != null) {
            StateSpec state = machine.spec().state(current);
            if (state != null
               && state.steps()
               && !this.suppressedLayers.contains(entry.getKey())
               && (!moving || state.stepsMoving())
               && !(this.weights.getOrDefault(entry.getKey(), 0.0F) > 0.0F)) {
               return !this.feetHeld;
            }
         }
      }

      return false;
   }

   private record Play(StateSpec state, String clip, PlayBehavior behavior, String key) {
   }
}
