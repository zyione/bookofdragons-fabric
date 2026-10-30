package net.magister.liberanimalium.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.logging.LogUtils;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import mod.azure.azurelib.model.AzBakedModel;
import mod.azure.azurelib.model.AzBone;
import mod.azure.azurelib.model.AzBoneSnapshot;
import net.magister.liberanimalium.anim.rig.BoneAxis;
import net.magister.liberanimalium.anim.rig.RigFrame;
import net.magister.liberanimalium.anim.rig.RigPass;
import net.magister.liberanimalium.anim.rig.RigRunner;
import net.magister.liberanimalium.anim.rig.RiggedBody;
import net.magister.liberanimalium.anim.v2.LayerSpec;
import net.magister.liberanimalium.body.anim.binding.AnimationBindingLoader;
import net.magister.liberanimalium.body.anim.binding.AnimationBindingResolver;
import net.magister.liberanimalium.body.anim.binding.AnimationBindingTable;
import net.magister.liberanimalium.body.state.FlightAnimationState;
import net.magister.liberanimalium.chassis.FaunaBase;
import net.magister.liberanimalium.util.PerfClock;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import org.joml.Matrix4f;
import org.slf4j.Logger;

public class FaunaProceduralAnimator {
   private static final ThreadLocal<AzBakedModel> CURRENT_MODEL = new ThreadLocal<>();
   private static final ThreadLocal<PoseStack> RENDER_STACK = new ThreadLocal<>();
   private static final ThreadLocal<Matrix4f> ENTITY_TRANSLATIONS = new ThreadLocal<>();
   private static final Map<Integer, long[]> LAST_RENDER_FRAME = new ConcurrentHashMap<>();
   private static boolean replayLogged;
   private static final Set<String> WORLD_ANCHORED = Set.of("body", "chain", "wings", "limbs", "hitbox");
   private static final Logger LOGGER = LogUtils.getLogger();
   private static volatile int whoEntity = Integer.MIN_VALUE;
   private static final Map<String, RigPass> PASSES = new ConcurrentHashMap<>(
      Map.ofEntries(
         Map.entry("size", new SizePass()),
         Map.entry("attitude", new AttitudePass()),
         Map.entry("extract", new ExtractPass()),
         Map.entry("body_bank", new BodyBankPass()),
         Map.entry("swim", new SwimPass()),
         Map.entry("cascade", new CascadePass()),
         Map.entry("head_track", new HeadTrackPass()),
         Map.entry("eyes", new EyesPass()),
         Map.entry("body", new BodyPass()),
         Map.entry("wings", new WingsPass()),
         Map.entry("limbs", new LimbsPass()),
         Map.entry("chain", new ChainPass()),
         Map.entry("hitbox", new HitboxPass())
      )
   );
   private static final Map<String, RigPass> BODY_RELATIVE = new ConcurrentHashMap<>();
   private static final Map<String, RigPass> WORLD_PASSES = new ConcurrentHashMap<>();
   private static final Map<Integer, RigRunner> RUNNERS = new ConcurrentHashMap<>();
   private static final Map<Integer, RigRunner> WORLD_RUNNERS = new ConcurrentHashMap<>();
   private static final ThreadLocal<FlightAnimationState> CURRENT_ANIM_STATE = new ThreadLocal<>();

   public static void setCurrentModel(AzBakedModel model) {
      CURRENT_MODEL.set(model);
   }

   public static void clearCurrentModel() {
      CURRENT_MODEL.remove();
   }

   public static void setRenderTransform(PoseStack stack, Matrix4f entityRenderTranslations) {
      RENDER_STACK.set(stack);
      ENTITY_TRANSLATIONS.set(new Matrix4f(entityRenderTranslations));
   }

   public static void clearRenderTransform() {
      RENDER_STACK.remove();
      ENTITY_TRANSLATIONS.remove();
   }

   static Matrix4f modelToWorld() {
      PoseStack stack = RENDER_STACK.get();
      Matrix4f entity = ENTITY_TRANSLATIONS.get();
      return stack != null && entity != null ? new Matrix4f(entity).invert().mul(stack.last().pose()) : new Matrix4f();
   }

   private static boolean isFirstRenderThisFrame(Entity body, float partialTick) {
      long key = (long)body.tickCount << 32 | Float.floatToRawIntBits(partialTick) & 4294967295L;
      long[] slot = LAST_RENDER_FRAME.computeIfAbsent(body.getId(), k -> new long[]{~key, -2147483648L});
      if (slot[0] == key) {
         int frame = PerfClock.frameId();
         if (!replayLogged && slot[1] != frame) {
            replayLogged = true;
            LogUtils.getLogger()
               .debug(
                  "[PerfClock] rig replayed in a LATER frame: {} tick={} partial={} lastFrame={} thisFrame={}",
                  new Object[]{body.getName().getString(), body.tickCount, partialTick, slot[1], frame}
               );
         }

         return false;
      } else {
         slot[0] = key;
         slot[1] = PerfClock.frameId();
         return true;
      }
   }

   public static void forget(int entityId) {
      LAST_RENDER_FRAME.remove(entityId);
      RUNNERS.remove(entityId);
      WORLD_RUNNERS.remove(entityId);
      ChainPass.forget(entityId);
      SwimPass.forget(entityId);
      EyesPass.forget(entityId);
      BodyWatch.forget(entityId);
   }

   public static void forgetAll() {
      LAST_RENDER_FRAME.clear();
      RUNNERS.clear();
      WORLD_RUNNERS.clear();
      ChainPass.forgetAll();
   }

   public static void apply(FaunaBase body, float partialTick, FlightAnimationState animState) {
      AzBakedModel model = CURRENT_MODEL.get();
      if (model != null) {
         apply(body, model, partialTick, animState);
      }
   }

   public static void apply(FaunaBase body, AzBakedModel model, float partialTick, FlightAnimationState animState) {
      if (body.level() != null) {
         boolean paused = Minecraft.getInstance().isPaused();
         boolean firstFrame = !paused && isFirstRenderThisFrame(body, partialTick);
         CURRENT_ANIM_STATE.set(animState);

         try {
            RigRunner runner = RUNNERS.computeIfAbsent(body.getId(), id -> new RigRunner());
            if (firstFrame && whoEntity == body.getId()) {
               whoEntity = Integer.MIN_VALUE;
               armWho(body, model, partialTick, runner);
            }

            Map<String, RigPass> group = PoseInterpolator.path() == PoseInterpolator.Path.TICK ? BODY_RELATIVE : PASSES;
            runner.run(body, model, partialTick, firstFrame, group, modelToWorld(), body.getPosition(partialTick));
            if (group == PASSES) {
               if (paused) {
                  BonePalettes.update(body.getId(), model, modelToWorld());
               } else {
                  posePaletteWithoutHitbox(body, model);
               }
            }
         } finally {
            CURRENT_ANIM_STATE.remove();
         }
      }
   }

   public static void applyWorldAnchored(FaunaBase body, AzBakedModel model, float partialTick) {
      if (body.level() != null) {
         boolean paused = Minecraft.getInstance().isPaused();
         CURRENT_ANIM_STATE.set(FlightAnimationState.compute(body));

         try {
            RigRunner runner = WORLD_RUNNERS.computeIfAbsent(body.getId(), id -> new RigRunner());
            runner.run(body, model, partialTick, !paused, WORLD_PASSES, modelToWorld(), body.getPosition(partialTick));
            if (paused) {
               BonePalettes.update(body.getId(), model, modelToWorld());
            } else {
               posePaletteWithoutHitbox(body, model);
            }
         } finally {
            CURRENT_ANIM_STATE.remove();
         }
      }
   }

   public static void poseRigOnly(RiggedBody body, AzBakedModel model, float partialTick, Matrix4f modelToWorld) {
      Entity entity = body.animEntity();
      if (entity != null && entity.level() != null) {
         for (AzBone bone : model.getBonesByName().values()) {
            AzBoneSnapshot rest = bone.getInitialAzSnapshot();
            bone.setRotX(rest.getRotX());
            bone.setRotY(rest.getRotY());
            bone.setRotZ(rest.getRotZ());
            bone.setPosX(rest.getOffsetX());
            bone.setPosY(rest.getOffsetY());
            bone.setPosZ(rest.getOffsetZ());
         }

         boolean firstFrame = !Minecraft.getInstance().isPaused() && isFirstRenderThisFrame(entity, partialTick);
         RigRunner runner = RUNNERS.computeIfAbsent(entity.getId(), id -> new RigRunner());
         runner.run(body, model, partialTick, firstFrame, PASSES, modelToWorld, entity.getPosition(partialTick));
      }
   }

   private static void posePaletteWithoutHitbox(FaunaBase body, AzBakedModel model) {
      if (!body.animRig().passes().contains("hitbox")) {
         BonePalettes.update(body.getId(), model, modelToWorld());
      }
   }

   public static void who(int entityId) {
      whoEntity = entityId;
   }

   private static void armWho(FaunaBase body, AzBakedModel model, float partialTick, RigRunner runner) {
      List<String> lines = new ArrayList<>();
      AnimationBindingResolver resolver = body.getStateContext().getBindingResolver();
      StringBuilder playing = new StringBuilder();
      AnimationBindingTable table = AnimationBindingLoader.get(body.animBindingKey());

      for (LayerSpec layer : table.v2.layers()) {
         String clip = resolver == null ? "?" : resolver.getActiveClip(layer.name());
         playing.append(' ').append(layer.name()).append('=').append(clip);
      }

      lines.add(
         String.format(
            Locale.ROOT,
            "[RigWho] %s t=%d pt=%.2f transport=%s onGround=%s y=%.3f feet-over-block=%.3f drawn-y=%.3f speed=%.3f |%s",
            body.getName().getString(),
            body.tickCount,
            partialTick,
            body.getTransportMode(),
            body.onGround(),
            body.getY(),
            body.getY() - Math.floor(body.getY()),
            body.getPosition(partialTick).y,
            body.getDeltaMovement().horizontalDistance(),
            playing
         )
      );
      lines.add("[RigWho] the clips' pose (bones off their rest, model units / degrees), before the rig:");

      for (AzBone bone : model.getBonesByName().values()) {
         AzBoneSnapshot rest = bone.getInitialAzSnapshot();
         float dx = bone.getPosX() - rest.getOffsetX();
         float dy = bone.getPosY() - rest.getOffsetY();
         float dz = bone.getPosZ() - rest.getOffsetZ();
         float rx = (float)Math.toDegrees(bone.getRotX() - rest.getRotX());
         float ry = (float)Math.toDegrees(bone.getRotY() - rest.getRotY());
         float rz = (float)Math.toDegrees(bone.getRotZ() - rest.getRotZ());
         boolean moved = Math.abs(dx) + Math.abs(dy) + Math.abs(dz) > 0.001;
         boolean turned = Math.abs(rx) + Math.abs(ry) + Math.abs(rz) > 0.05;
         if (moved || turned) {
            lines.add(
               String.format(
                  Locale.ROOT,
                  "[RigWho]   %-28s %s%s",
                  bone.getName(),
                  moved ? String.format(Locale.ROOT, "pos %+.3f %+.3f %+.3f  ", dx, dy, dz) : "",
                  turned ? String.format(Locale.ROOT, "rot %+.1f %+.1f %+.1f", rx, ry, rz) : ""
               )
            );
         }
      }

      runner.traceNextFrame(trace -> {
         lines.add("[RigWho] the rig's writes, in pass order (pass  bone.axis  found -> left; radians / model units):");
         if (trace.isEmpty()) {
            lines.add("[RigWho]   (none)");
         }

         for (String line : trace) {
            lines.add("[RigWho]   " + line);
         }

         for (String line : lines) {
            LOGGER.debug(line);
         }
      });
      WORLD_RUNNERS.computeIfAbsent(body.getId(), id -> new RigRunner()).traceNextFrame(trace -> {
         LOGGER.debug("[RigWho] the world-anchored passes' writes:");

         for (String line : trace) {
            LOGGER.debug("[RigWho]   " + line);
         }
      });
   }

   public static void registerPass(String name, RigPass pass, boolean worldAnchored) {
      if (PASSES.putIfAbsent(name, pass) != null) {
         throw new IllegalArgumentException("rig pass '" + name + "' is already registered");
      }

      (worldAnchored ? WORLD_PASSES : BODY_RELATIVE).put(name, pass);
   }

   public static Set<String> passNames() {
      return PASSES.keySet();
   }

   static FlightAnimationState currentAnimState() {
      return CURRENT_ANIM_STATE.get();
   }

   static String first(RigFrame frame, String chain) {
      List<String> bones = frame.spec.chain(chain);
      return bones.isEmpty() ? null : bones.get(0);
   }

   static void capture(RigFrame frame, String bone) {
      if (frame.write.has(bone)) {
         frame.write.set(bone, BoneAxis.ROT_X, frame.write.read(bone, BoneAxis.ROT_X));
         frame.write.set(bone, BoneAxis.ROT_Y, frame.write.read(bone, BoneAxis.ROT_Y));
         frame.write.set(bone, BoneAxis.ROT_Z, frame.write.read(bone, BoneAxis.ROT_Z));
      }
   }

   static {
      PASSES.forEach((name, pass) -> (WORLD_ANCHORED.contains(name) ? WORLD_PASSES : BODY_RELATIVE).put(name, pass));
      registerPass("grip", new GripPass(), true);
   }
}
