package net.magister.liberanimalium.chassis.component.ranged;

import com.mojang.logging.LogUtils;
import java.util.List;
import net.magister.liberanimalium.anim.ExpressionEventClient;
import net.magister.liberanimalium.attack.AttackManager;
import net.magister.liberanimalium.attack.data.AttackDefinition;
import net.magister.liberanimalium.attack.data.EmitterConfig;
import net.magister.liberanimalium.attack.data.LightConfig;
import net.magister.liberanimalium.attack.data.VisualEmitter;
import net.magister.liberanimalium.attack.data.delivery.ChargedDelivery;
import net.magister.liberanimalium.attack.data.delivery.StreamDelivery;
import net.magister.liberanimalium.attack.light.LightScribe;
import net.magister.liberanimalium.attack.runtime.AttackContext;
import net.magister.liberanimalium.attack.runtime.AttackController;
import net.magister.liberanimalium.attack.runtime.AttackInvoker;
import net.magister.liberanimalium.attack.runtime.AttackPhaseClock;
import net.magister.liberanimalium.attack.runtime.AttackState;
import net.magister.liberanimalium.body.anim.data.PerEntityHeadMatrixExtractor;
import net.magister.liberanimalium.body.data.CrippledFeature;
import net.magister.liberanimalium.chassis.FaunaBase;
import net.magister.liberanimalium.chassis.component.simulation.FaunaStaminaSystem;
import net.magister.liberanimalium.fauna.api.ExpressionKey;
import net.magister.liberanimalium.fauna.debug.FaunaDebug;
import net.magister.liberanimalium.network.BreathEmitMessage;
import net.magister.liberanimalium.network.FaunaNetwork;
import net.magister.liberanimalium.sound.FaunaSoundHelper;
import net.magister.liberanimalium.sound.SoundEventKey;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

public class OmniAttackHandler implements IRangedAttackHandler {
   public static final int FIRING_WINDOW_TICKS = 20;
   private FaunaBase body;
   private ResourceLocation attackId;
   private OmniAttackHandler.OmniAttackState newAttackState;
   public int aimingTicks = 0;
   private final AttackPhaseClock phaseClock = new AttackPhaseClock();
   private final AttackPhaseClock emitClock = new AttackPhaseClock();
   private int lastReleaseTick = Integer.MIN_VALUE;
   private int lastAimLogTick = Integer.MIN_VALUE;

   public void setAttackId(ResourceLocation attackId) {
      this.attackId = attackId;
   }

   public ResourceLocation getAttackId() {
      return this.attackId;
   }

   @Nullable
   public AttackDefinition definition() {
      return this.attackId == null ? null : AttackManager.getInstance().getAttack(this.attackId);
   }

   public boolean hasAttack() {
      return this.definition() != null;
   }

   public boolean isStream() {
      AttackDefinition def = this.definition();
      return def != null && def.delivery() instanceof StreamDelivery;
   }

   @Override
   public void onAttach(FaunaBase host) {
      this.body = host;
      if (this.attackId != null) {
         AttackDefinition newDef = this.definition();
         if (newDef != null && newDef.delivery() instanceof StreamDelivery streamDelivery) {
            this.getOrCreateState().setFuel(streamDelivery.fuel().max());
         }
      }
   }

   @Override
   public void serverTick() {
      AttackDefinition newDef = this.definition();
      if (newDef != null) {
         if (this.isCharging() && this.mouthSubmerged()) {
            this.cancelCharge();
         }

         AttackState state = this.getOrCreateState();
         if (newDef.delivery() instanceof StreamDelivery) {
            AttackController.tick(newDef, this.buildStreamAttackContext(), state, this.isCharging());
            this.mirrorStreamState(state);
         } else {
            this.tickNewPathBookkeeping();
            AttackController.tick(newDef, this.buildAttackContext(), state, this.isCharging());
         }

         this.lightMouth(newDef, state);
         this.sendEmission(newDef);
      }
   }

   private void sendEmission(AttackDefinition def) {
      this.emitClock
         .update(
            new AttackPhaseClock.Inputs(this.isCharging(), this.getFiringTicks(), this.getWindupTicks(), this.isBreathing()), clocksOf(def), this.body.tickCount
         );
      if (this.emitClock.phase() != AttackPhaseClock.Phase.IDLE && hasSourceAnchoredVisual(def)) {
         FaunaNetwork.sendToClientsTracking(
            new BreathEmitMessage(
               this.body.getId(),
               this.body.tickCount,
               this.emitClock.phase(),
               this.emitClock.progress(),
               this.getWorldBreathPosition().subtract(this.body.position()),
               this.getBreathDirection()
            ),
            this.body
         );
      }
   }

   private static boolean hasSourceAnchoredVisual(AttackDefinition def) {
      for (EmitterConfig emitter : def.emitters()) {
         if (emitter instanceof VisualEmitter visual && visual.isSourceAnchored()) {
            return true;
         }
      }

      return false;
   }

   @Override
   public boolean isAttacking() {
      return this.newAttackState != null && this.newAttackState.isActive();
   }

   @Override
   public void handlePlayerAttack(boolean pressed) {
      if (!pressed && this.isCharging()) {
         this.lastReleaseTick = this.body.tickCount;
      }

      this.setCharging(pressed);
   }

   public void cancelCharge() {
      this.setCharging(false);
      AttackDefinition def = this.definition();
      if (this.newAttackState != null && def != null && def.delivery() instanceof ChargedDelivery) {
         AttackController.cancel(this.newAttackState);
      }
   }

   public int ticksSinceBreathReleased() {
      return this.lastReleaseTick != Integer.MIN_VALUE && !this.isCharging() ? this.body.tickCount - this.lastReleaseTick : Integer.MAX_VALUE;
   }

   @Override
   public boolean canAttack() {
      return !this.body.getSyncedIsFeatureCrippled(CrippledFeature.BREATH)
         && (this.newAttackState == null || this.newAttackState.getCooldown() == 0)
         && !this.mouthSubmerged();
   }

   public boolean mouthSubmerged() {
      return this.body.level().getFluidState(BlockPos.containing(this.getWorldBreathPosition())).is(FluidTags.WATER);
   }

   public void stopBreathing() {
      this.setBreathing(false);
      this.setWindupTicks(0);
      this.setBreathCooldownTicks(40);
      if (this.getCurrentFuel() <= 0) {
         this.setFullRechargeCooldownTicks(100);
      }
   }

   public Vec3 getWorldBreathPosition() {
      Vec3 mouth = PerEntityHeadMatrixExtractor.resolveMouthPosition(this.body);
      return this.body.headClear() && this.body.lineOfSight(this.body.getEyePosition(), mouth) ? mouth : this.body.getEyePosition();
   }

   public Vec3 getBreathDirection() {
      return this.aiAimDirection();
   }

   private Vec3 aiAimDirection() {
      if (this.body.getControllingPassenger() instanceof Player player) {
         return player.getViewVector(1.0F);
      } else {
         if (FaunaDebug.anyoneWatching() && !this.body.level().isClientSide && this.body.tickCount - this.lastAimLogTick >= 10) {
            this.lastAimLogTick = this.body.tickCount;
            LogUtils.getLogger().debug(this.body.getHeadTrackingSystem().debugAimLine());
         }

         return this.body.getHeadTrackingSystem().headLookDirection();
      }
   }

   public void resetFireCooldown() {
      this.setTicksSinceLastFire(0);
   }

   public FaunaBase getBody() {
      return this.body;
   }

   public FaunaStaminaSystem getStaminaSystem() {
      return this.body.getStaminaSystem();
   }

   public AttackPhaseClock phaseClock() {
      return this.phaseClock;
   }

   public float serverGlow() {
      AttackDefinition def = this.definition();
      return def != null && this.newAttackState != null ? this.serverGlow(def, this.newAttackState) : 0.0F;
   }

   public int getCurrentFuel() {
      return this.body.<Integer>getData(FaunaBase.BREATH_CURRENT_FUEL);
   }

   public int fuelTicksRemaining() {
      AttackDefinition def = this.definition();
      if (def != null && def.delivery() instanceof StreamDelivery sd) {
         float var4 = Math.max(0.01F, sd.fuel().drainPerTick());
         return (int)(this.getCurrentFuel() / var4);
      } else {
         return Integer.MAX_VALUE;
      }
   }

   public int staminaTicksRemaining() {
      FaunaStaminaSystem stamina = this.body.getStaminaSystem();
      if (stamina == null) {
         return Integer.MAX_VALUE;
      } else {
         float scale = this.body.getStatSheet().breathCost();
         AttackDefinition def = this.definition();
         if (def != null && def.delivery() instanceof StreamDelivery) {
            float perTick = 1.4F * scale;
            return perTick <= 0.0F ? Integer.MAX_VALUE : (int)(stamina.getCurrentStamina() / perTick);
         } else {
            return stamina.hasStamina(10.0F * scale) ? Integer.MAX_VALUE : 0;
         }
      }
   }

   public int chargeTicks() {
      AttackDefinition def = this.definition();
      return def != null && def.delivery() instanceof ChargedDelivery cd ? cd.chargeTimeTicks() : 0;
   }

   public void setCurrentFuel(int fuel) {
      this.body.setData(FaunaBase.BREATH_CURRENT_FUEL, Math.max(0, fuel));
   }

   public boolean isBreathing() {
      return this.body.<Boolean>getData(FaunaBase.BREATH_IS_BREATHING);
   }

   public void setBreathing(boolean breathing) {
      this.body.setData(FaunaBase.BREATH_IS_BREATHING, breathing);
   }

   public int getBreathCooldownTicks() {
      return this.body.<Integer>getData(FaunaBase.BREATH_COOLDOWN);
   }

   public void setBreathCooldownTicks(int ticks) {
      this.body.setData(FaunaBase.BREATH_COOLDOWN, ticks);
   }

   public int getFullRechargeCooldownTicks() {
      return this.body.<Integer>getData(FaunaBase.BREATH_FULL_RECHARGE_COOLDOWN);
   }

   public void setFullRechargeCooldownTicks(int ticks) {
      this.body.setData(FaunaBase.BREATH_FULL_RECHARGE_COOLDOWN, ticks);
   }

   public int getWindupTicks() {
      return this.body.<Integer>getData(FaunaBase.BREATH_WINDUP_TICKS);
   }

   public void setWindupTicks(int ticks) {
      this.body.setData(FaunaBase.BREATH_WINDUP_TICKS, ticks);
   }

   public boolean isWindingUp() {
      return this.getWindupTicks() > 0;
   }

   public int getTicksSinceLastFire() {
      return this.body.<Integer>getData(FaunaBase.PROJ_TICK_SINCE_LAST_FIRE);
   }

   public void setTicksSinceLastFire(int ticks) {
      this.body.setData(FaunaBase.PROJ_TICK_SINCE_LAST_FIRE, ticks);
   }

   public int getFiringTicks() {
      return this.body.<Integer>getData(FaunaBase.PROJ_FIRING_TICKS);
   }

   public void setFiringTicks(int ticks) {
      this.body.setData(FaunaBase.PROJ_FIRING_TICKS, ticks);
   }

   public boolean isCharging() {
      return this.body.<Boolean>getData(FaunaBase.PROJ_IS_CHARGING);
   }

   public void setCharging(boolean charging) {
      this.body.setData(FaunaBase.PROJ_IS_CHARGING, charging);
   }

   public boolean isFiring() {
      return this.getFiringTicks() > 0;
   }

   public int getAimingTicks() {
      return this.aimingTicks;
   }

   public int getChargeBuildupTicks() {
      AttackDefinition def = this.definition();
      return def != null && def.delivery() instanceof ChargedDelivery charged ? charged.chargeTimeTicks() : 40;
   }

   private void tickNewPathBookkeeping() {
      int ticksSinceFire = this.getTicksSinceLastFire();
      AttackDefinition def = this.definition();
      int counted = def == null ? 60 : Math.max(60, def.delivery().cooldownTicks());
      if (ticksSinceFire < counted) {
         this.setTicksSinceLastFire(ticksSinceFire + 1);
      }

      int firingTicks = this.getFiringTicks();
      if (firingTicks > 0) {
         this.setFiringTicks(firingTicks - 1);
      }

      if (this.isCharging()) {
         this.aimingTicks++;
         if (this.aimingTicks % 10 == 1) {
            FaunaSoundHelper.playSound(this.body, SoundEventKey.CHARGE);
         }
      } else {
         this.aimingTicks = 0;
      }
   }

   private OmniAttackHandler.OmniAttackState getOrCreateState() {
      if (this.newAttackState == null) {
         this.newAttackState = new OmniAttackHandler.OmniAttackState();
      }

      return this.newAttackState;
   }

   private AttackContext buildAttackContext() {
      return new AttackContext(
         this.body,
         this.body.level(),
         this.attackId,
         this::getWorldBreathPosition,
         this::aiAimDirection,
         () -> this.body.getStaminaSystem().tryConsume(10.0F * this.body.getStatSheet().breathCost()),
         () -> this.body.getSyncedIsFeatureCrippled(CrippledFeature.BREATH),
         () -> {
            this.setFiringTicks(20);
            this.resetFireCooldown();
            FaunaSoundHelper.playSound(this.body, SoundEventKey.SHOOT);
            if (this.body.getNeedsSystem() != null) {
               this.body.getNeedsSystem().onProjectileAttack();
            }
         }
      );
   }

   private AttackContext buildStreamAttackContext() {
      return new AttackContext(
         this.body,
         this.body.level(),
         this.attackId,
         this::getWorldBreathPosition,
         this::getBreathDirection,
         () -> this.body.getStaminaSystem().drainContinuous(1.4F * this.body.getStatSheet().breathCost()),
         () -> this.body.getSyncedIsFeatureCrippled(CrippledFeature.BREATH),
         () -> {
            if (this.body.getNeedsSystem() != null) {
               this.body.getNeedsSystem().onBreathAttack();
            }
         }
      );
   }

   private void mirrorStreamState(AttackState state) {
      boolean windingUp = state.getPhase() == 3;
      boolean streaming = state.getPhase() == 4;
      this.setWindupTicks(windingUp ? state.getTimer() : 0);
      this.setBreathing(streaming);
      this.setCurrentFuel((int)state.getFuel());
      if (!windingUp && !streaming) {
         this.setFullRechargeCooldownTicks(state.getTimer());
      }
   }

   private float serverGlow(AttackDefinition def, AttackState state) {
      if (def.delivery() instanceof StreamDelivery stream) {
         if (state.getPhase() == 4) {
            return 1.0F;
         } else {
            return state.getPhase() == 3 ? Mth.clamp(1.0F - (float)state.getTimer() / Math.max(1, stream.windupTicks()), 0.0F, 1.0F) : 0.0F;
         }
      } else {
         if (this.isCharging() && state.isActive()) {
            return Mth.clamp(state.getChargeScale(), 0.0F, 1.0F);
         }

         int firing = this.getFiringTicks();
         return firing > 0 ? Mth.clamp(firing / 20.0F, 0.0F, 1.0F) : 0.0F;
      }
   }

   private void lightMouth(AttackDefinition def, AttackState state) {
      if (!def.light().isEmpty() && def.light().get().source()) {
         if (this.body.level() instanceof ServerLevel serverLevel) {
            LightConfig var6 = def.light().get();
            if (this.body.tickCount % var6.everyTicks() == 0) {
               float glow = this.serverGlow(def, state);
               if (!(glow <= 0.0F)) {
                  LightScribe.of(serverLevel).illuminate(serverLevel, this.getWorldBreathPosition(), this.body.getId(), var6, glow);
               }
            }
         }
      }
   }

   @Override
   public void clientTick() {
      AttackDefinition newDef = this.definition();
      if (newDef != null) {
         AttackPhaseClock.Phase before = this.phaseClock.phase();
         this.phaseClock
            .update(
               new AttackPhaseClock.Inputs(this.isCharging(), this.getFiringTicks(), this.getWindupTicks(), this.isBreathing()),
               clocksOf(newDef),
               this.body.tickCount
            );
         this.publishBreathEdges(before, this.phaseClock.phase());
      }
   }

   public void emitFromServer(int tick, AttackPhaseClock.Phase phase, float progress, Vec3 mouth, Vec3 direction) {
      AttackDefinition def = this.definition();
      if (def != null) {
         Vec3 origin = this.body.position().add(mouth);
         Vec3 carried = AttackInvoker.carried(this.body);
         List<EmitterConfig> emitters = def.emitters();

         for (int i = 0; i < emitters.size(); i++) {
            if (emitters.get(i) instanceof VisualEmitter visual && visual.isSourceAnchored() && replaysThisTick(visual, phase, tick)) {
               AttackInvoker.spawnVisualParticles(
                  this.body.level(), origin, direction, this.attackId, visual, i, progress, runSeedOf(this.body), tick, carried
               );
            }
         }
      }
   }

   private void publishBreathEdges(AttackPhaseClock.Phase before, AttackPhaseClock.Phase now) {
      if (before != now) {
         boolean wasDelivering = before == AttackPhaseClock.Phase.RELEASE || before == AttackPhaseClock.Phase.FIRE;
         boolean delivering = now == AttackPhaseClock.Phase.RELEASE || now == AttackPhaseClock.Phase.FIRE;
         if (delivering != wasDelivering) {
            ExpressionKey.Body moment = delivering ? ExpressionKey.Body.BREATH_BEGIN : ExpressionKey.Body.BREATH_END;
            ExpressionEventClient.fireLocal(this.body, this.body.getId(), ExpressionKey.body(moment).key(), this.body.level().getGameTime());
         }
      }
   }

   public static long runSeedOf(Entity body) {
      return body.getId() * -7046029254386353131L;
   }

   public static boolean replaysThisTick(VisualEmitter visual, AttackPhaseClock.Phase phase, int tick) {
      int rate = Math.max(1, visual.emission().rateTicks());

      return switch (phase) {
         case IDLE -> false;
         case CHARGE -> visual.isChargePhase() && tick % rate == 0;
         case RELEASE -> visual.isReleasePhase() || visual.isFirePhase();
         case FIRE -> visual.isFirePhase() && tick % rate == 0;
      };
   }

   public static AttackPhaseClock.Clocks clocksOf(AttackDefinition def) {
      int charge = def.delivery() instanceof ChargedDelivery cd ? cd.chargeTimeTicks() : 0;
      int windup = def.delivery() instanceof StreamDelivery sd ? sd.windupTicks() : 0;
      return new AttackPhaseClock.Clocks(charge, windup, 20);
   }

   @Nullable
   public AttackState serverState() {
      return this.newAttackState;
   }

   @Override
   public void save(CompoundTag tag) {
      tag.putInt("OAH_CurrentFuel", this.getCurrentFuel());
      tag.putInt("OAH_BreathCooldown", this.getBreathCooldownTicks());
      tag.putInt("OAH_FullRechargeCooldown", this.getFullRechargeCooldownTicks());
   }

   @Override
   public void load(CompoundTag tag) {
      if (tag.contains("OAH_CurrentFuel")) {
         this.setCurrentFuel(tag.getInt("OAH_CurrentFuel"));
         this.setBreathCooldownTicks(tag.getInt("OAH_BreathCooldown"));
         this.setFullRechargeCooldownTicks(tag.getInt("OAH_FullRechargeCooldown"));
      }
   }

   private static class OmniAttackState implements AttackState {
      private int cooldown = 0;
      private float chargeScale = 0.0F;
      private int phase = 0;
      private int timer = 0;
      private boolean active = false;
      private float fuel = 0.0F;

      @Override
      public int getCooldown() {
         return this.cooldown;
      }

      @Override
      public void setCooldown(int t) {
         this.cooldown = t;
      }

      @Override
      public float getChargeScale() {
         return this.chargeScale;
      }

      @Override
      public void setChargeScale(float s) {
         this.chargeScale = s;
      }

      @Override
      public int getPhase() {
         return this.phase;
      }

      @Override
      public void setPhase(int p) {
         this.phase = p;
      }

      @Override
      public int getTimer() {
         return this.timer;
      }

      @Override
      public void setTimer(int t) {
         this.timer = t;
      }

      @Override
      public boolean isActive() {
         return this.active;
      }

      @Override
      public void setActive(boolean a) {
         this.active = a;
      }

      @Override
      public float getFuel() {
         return this.fuel;
      }

      @Override
      public void setFuel(float f) {
         this.fuel = f;
      }
   }
}
