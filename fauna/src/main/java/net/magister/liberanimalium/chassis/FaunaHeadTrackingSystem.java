package net.magister.liberanimalium.chassis;

import java.util.List;
import org.jetbrains.annotations.Nullable;
import mod.azure.azurelib.model.AzBakedModel;
import mod.azure.azurelib.model.AzBone;
import net.magister.liberanimalium.anim.rig.BoneFk;
import net.magister.liberanimalium.anim.rig.Footing;
import net.magister.liberanimalium.anim.rig.HeadTrackSpec;
import net.magister.liberanimalium.body.locomotion.ClimbEngine;
import net.magister.liberanimalium.body.locomotion.WalkEngine;
import net.magister.liberanimalium.body.state.TransportMode;
import net.magister.liberanimalium.chassis.component.FaunaFlightRotationComponent;
import net.magister.liberanimalium.chassis.component.RollComponent;
import net.magister.liberanimalium.fauna.api.SpeciesProfile;
import net.magister.liberanimalium.fauna.sense.SightSense;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

public class FaunaHeadTrackingSystem {
   private final FaunaBase body;
   private Vec3 lookTarget = null;
   private LivingEntity targetEntity = null;
   private FaunaHeadTrackingSystem.LookPriority currentPriority = FaunaHeadTrackingSystem.LookPriority.NONE;
   private float targetHeadYaw = 0.0F;
   private float targetHeadPitch = 0.0F;
   private float smoothedHeadYaw = 0.0F;
   private float smoothedHeadPitch = 0.0F;
   private float groundGaze = 0.0F;
   private float smoothedHeadYawO = 0.0F;
   private float smoothedHeadPitchO = 0.0F;
   private boolean lookEngaged = false;
   private int outOfRangeTicks = 0;
   private int neckSegmentCount = 1;
   private static final float BASE_HEAD_YAW = 45.0F;
   private static final float BASE_HEAD_PITCH_UP = 30.0F;
   private static final float BASE_HEAD_PITCH_DOWN = 40.0F;
   private static final float YAW_PER_SEGMENT = 25.0F;
   private static final float PITCH_UP_PER_SEGMENT = 8.0F;
   private static final float PITCH_DOWN_PER_SEGMENT = 10.0F;
   private static final float MAX_HEAD_YAW = 170.0F;
   private static final float TRACK_GAIN = 0.6F;
   private static final float TRACK_MAX_STEP_DEG = 25.0F;
   static final float GAZE_EASE_TICKS = 9.0F;
   static final float LOOK_EASE_TICKS = 5.0F;
   private static final float EASE_MAX_STEP_DEG = 15.0F;
   private float yawVelocity = 0.0F;
   private float pitchVelocity = 0.0F;
   private static final float REENGAGE_FRACTION = 0.8F;
   private static final int LOOK_HOLD_TICKS = 6000;
   private static final float TURN_START_SHARE = 0.75F;
   private static final float TURN_SETTLE_SHARE = 0.25F;
   private static final float TURN_GAIN = 0.2F;
   private static final float TURN_STEP_DEG = 4.0F;
   private static final double TURN_STANDING_SPEED = 0.02;
   private static final float TURN_SEEN_DEG = 0.5F;
   private boolean turningToLook = false;
   private float lastBodyTurn = 0.0F;
   private final HeadGazes gazes;
   private static final double MOUNT_MARGIN = 0.3;
   private float lastBodyFrameYaw = Float.NaN;
   private Vec3 glancePos;
   private int glanceUntil;
   private float glanceStrength;
   private boolean cockRight;
   private static final float COCK_SWITCH_MARGIN_DEG = 25.0F;

   public HeadGazes gazes() {
      return this.gazes;
   }

   public FaunaHeadTrackingSystem(FaunaBase body) {
      this.body = body;
      this.gazes = new HeadGazes(body, this);
   }

   public void setNeckSegmentCount(int count) {
      this.neckSegmentCount = Math.max(1, count);
   }

   private int effectiveNeckSegments() {
      int declared = this.body.bodyPlan().chain("neck").size();
      return declared > 0 ? declared : Math.max(1, this.neckSegmentCount);
   }

   public float getMaxHeadYaw() {
      return Math.min(170.0F, 45.0F + this.effectiveNeckSegments() * 25.0F);
   }

   static float track(float current, float target) {
      float step = Mth.clamp((target - current) * 0.6F, -25.0F, 25.0F);
      return current + step;
   }

   static float[] ease(float current, float target, float velocity, float easeTicks) {
      float omega = 2.0F / easeTicks;
      float x = omega;
      float decay = 1.0F / (1.0F + x + 0.48F * x * x + 0.235F * x * x * x);
      float maxChange = 15.0F * easeTicks;
      float change = Mth.clamp(current - target, -maxChange, maxChange);
      float goal = current - change;
      float temp = velocity + omega * change;
      float nextVelocity = (velocity - omega * temp) * decay;
      float next = goal + (change + temp) * decay;
      if (target - current > 0.0F == next > target) {
         next = target;
         nextVelocity = 0.0F;
      }

      float step = Mth.clamp(next - current, -15.0F, 15.0F);
      return new float[]{current + step, Mth.clamp(nextVelocity, -15.0F, 15.0F)};
   }

   static boolean aims(FaunaHeadTrackingSystem.LookPriority rung) {
      return rung.level >= FaunaHeadTrackingSystem.LookPriority.ATTACK_TARGET.level;
   }

   private void follow(FaunaHeadTrackingSystem.LookPriority rung) {
      if (aims(rung)) {
         float yaw = track(this.smoothedHeadYaw, this.targetHeadYaw);
         float pitch = track(this.smoothedHeadPitch, this.targetHeadPitch);
         this.yawVelocity = yaw - this.smoothedHeadYaw;
         this.pitchVelocity = pitch - this.smoothedHeadPitch;
         this.smoothedHeadYaw = yaw;
         this.smoothedHeadPitch = pitch;
      } else {
         float easeTicks = rung.level >= FaunaHeadTrackingSystem.LookPriority.ENTITY.level ? 5.0F : 9.0F;
         float[] yaw = ease(this.smoothedHeadYaw, this.targetHeadYaw, this.yawVelocity, easeTicks);
         float[] pitch = ease(this.smoothedHeadPitch, this.targetHeadPitch, this.pitchVelocity, easeTicks);
         this.smoothedHeadYaw = yaw[0];
         this.yawVelocity = yaw[1];
         this.smoothedHeadPitch = pitch[0];
         this.pitchVelocity = pitch[1];
      }
   }

   float maxPitchUp() {
      return this.getMaxHeadPitchUp();
   }

   float maxPitchDown() {
      return this.getMaxHeadPitchDown();
   }

   private float getMaxHeadPitchUp() {
      return 30.0F + this.effectiveNeckSegments() * 8.0F;
   }

   private float getMaxHeadPitchDown() {
      return 40.0F + this.effectiveNeckSegments() * 10.0F;
   }

   public void lookAt(Vec3 position, FaunaHeadTrackingSystem.LookPriority priority) {
      if (priority.level >= this.currentPriority.level && !this.onMount(position)) {
         this.lookTarget = position;
         this.targetEntity = null;
         this.currentPriority = priority;
      }
   }

   public void lookAt(Entity entity, FaunaHeadTrackingSystem.LookPriority priority) {
      if (entity != null && entity != this.body.getVehicle() && priority.level >= this.currentPriority.level) {
         this.targetEntity = entity instanceof LivingEntity living ? living : null;
         this.lookTarget = entity.getEyePosition();
         this.currentPriority = priority;
      }
   }

   private boolean onMount(Vec3 point) {
      Entity mount = this.body.getVehicle();
      return mount != null && point != null && mount.getBoundingBox().inflate(0.3).contains(point);
   }

   public void clearTarget(FaunaHeadTrackingSystem.LookPriority priority) {
      if (this.currentPriority.level <= priority.level) {
         this.lookTarget = null;
         this.targetEntity = null;
         this.currentPriority = FaunaHeadTrackingSystem.LookPriority.NONE;
      }
   }

   public void tick() {
      this.smoothedHeadYawO = this.smoothedHeadYaw;
      this.smoothedHeadPitchO = this.smoothedHeadPitch;
      this.updateGroundGaze();
      this.gazes.tick();
      if (this.body.isVehicle()) {
         Entity passenger = this.body.getControllingPassenger();
         if (passenger instanceof Player player) {
            this.handlePlayerCursorTracking(player);
            if (!this.body.level().isClientSide) {
               this.body.setNetworkLookRung(FaunaHeadTrackingSystem.LookPriority.PLAYER_CURSOR);
            }

            return;
         }
      }

      FaunaHeadTrackingSystem.LookPriority rung;
      if (this.body.level().isClientSide) {
         this.targetHeadYaw = this.body.getNetworkHeadYaw();
         this.targetHeadPitch = this.body.getNetworkHeadPitch();
         rung = this.body.getNetworkLookRung();
      } else {
         rung = this.resolveTarget();
         this.turnToLook(rung);
      }

      float maxYaw = this.applyLookCone();
      float bodyNow = this.bodyFrameYaw();
      this.lastBodyTurn = 0.0F;
      if (!Float.isNaN(this.lastBodyFrameYaw)) {
         this.lastBodyTurn = Mth.wrapDegrees(bodyNow - this.lastBodyFrameYaw);
         this.smoothedHeadYaw = stabilise(this.smoothedHeadYaw, this.lastBodyTurn, maxYaw);
      }

      this.lastBodyFrameYaw = bodyNow;
      this.follow(rung);
   }

   private FaunaHeadTrackingSystem.LookPriority resolveTarget() {
      if (this.body.getSyncedIsIncapacitated()) {
         this.targetEntity = null;
         this.lookTarget = null;
         this.currentPriority = FaunaHeadTrackingSystem.LookPriority.NONE;
         this.targetHeadYaw = 0.0F;
         this.targetHeadPitch = 0.0F;
         this.body.setNetworkHeadYaw(0.0F);
         this.body.setNetworkHeadPitch(0.0F);
         this.body.setNetworkLookRung(FaunaHeadTrackingSystem.LookPriority.NONE);
         return FaunaHeadTrackingSystem.LookPriority.NONE;
      }

      if (this.targetEntity != null && this.targetEntity.isAlive()) {
         this.lookTarget = this.targetEntity.getEyePosition();
      } else if (this.targetEntity != null) {
         this.targetEntity = null;
         if (this.currentPriority != FaunaHeadTrackingSystem.LookPriority.PLAYER_CURSOR) {
            this.lookTarget = null;
            this.currentPriority = FaunaHeadTrackingSystem.LookPriority.NONE;
         }
      }

      if (this.currentPriority == FaunaHeadTrackingSystem.LookPriority.PLAYER_CURSOR) {
         this.currentPriority = FaunaHeadTrackingSystem.LookPriority.NONE;
         this.lookTarget = null;
         this.targetEntity = null;
      }

      LivingEntity engaged = this.body.weaponTarget();
      if (engaged != null && engaged.isAlive()) {
         this.targetEntity = engaged;
         this.lookTarget = engaged.getEyePosition();
         this.currentPriority = FaunaHeadTrackingSystem.LookPriority.ATTACK_TARGET;
      } else if (this.currentPriority == FaunaHeadTrackingSystem.LookPriority.ATTACK_TARGET) {
         this.targetEntity = null;
         this.lookTarget = null;
         this.currentPriority = FaunaHeadTrackingSystem.LookPriority.NONE;
      }

      if (this.currentPriority.level < FaunaHeadTrackingSystem.LookPriority.ATTACK_TARGET.level) {
         Vec3 noisePos = this.reactedToNoisePos();
         if (noisePos != null) {
            this.lookAt(noisePos, FaunaHeadTrackingSystem.LookPriority.NOISE);
         } else if (this.currentPriority == FaunaHeadTrackingSystem.LookPriority.NOISE) {
            this.clearTarget(FaunaHeadTrackingSystem.LookPriority.NOISE);
         }
      }

      if (this.currentPriority.level < FaunaHeadTrackingSystem.LookPriority.ATTACK_TARGET.level && this.onMount(this.lookTarget)) {
         this.clearTarget(this.currentPriority);
      }

      if (this.lookTarget != null) {
         this.calculateRotationToTarget(this.lookTarget);
         this.cockForBlindSpot();
      } else {
         this.targetHeadYaw = 0.0F;
         this.targetHeadPitch = 0.0F;
      }

      this.body.setNetworkHeadYaw(this.targetHeadYaw);
      this.body.setNetworkHeadPitch(this.targetHeadPitch);
      FaunaHeadTrackingSystem.LookPriority rung = this.lookTarget != null ? this.currentPriority : FaunaHeadTrackingSystem.LookPriority.NONE;
      this.body.setNetworkLookRung(rung);
      return rung;
   }

   private float applyLookCone() {
      float maxYaw = this.getMaxHeadYaw();
      float absYaw = Math.abs(this.targetHeadYaw);
      if (this.lookEngaged) {
         if (absYaw > maxYaw) {
            if (++this.outOfRangeTicks > 6000) {
               this.lookEngaged = false;
               this.outOfRangeTicks = 0;
            }
         } else {
            this.outOfRangeTicks = 0;
         }
      } else if (absYaw < maxYaw * 0.8F || this.turnsToward(this.targetHeadYaw)) {
         this.lookEngaged = true;
         this.outOfRangeTicks = 0;
      }

      if (this.lookEngaged) {
         this.targetHeadYaw = Mth.clamp(this.targetHeadYaw, -maxYaw, maxYaw);
         this.targetHeadPitch = Mth.clamp(this.targetHeadPitch, -this.getMaxHeadPitchUp(), this.getMaxHeadPitchDown());
      } else {
         this.targetHeadYaw = 0.0F;
         this.targetHeadPitch = 0.0F;
      }

      return maxYaw;
   }

   private boolean turnsToward(float relativeYaw) {
      return this.lastBodyTurn * relativeYaw > 0.0F && Math.abs(this.lastBodyTurn) >= 0.5F && this.standing();
   }

   private boolean standing() {
      double dx = this.body.getX() - this.body.xo;
      double dz = this.body.getZ() - this.body.zo;
      return this.body.onGround() && dx * dx + dz * dz < 4.0E-4;
   }

   private void turnToLook(FaunaHeadTrackingSystem.LookPriority rung) {
      boolean may = this.lookTarget != null
         && rung != FaunaHeadTrackingSystem.LookPriority.NONE
         && rung.level < FaunaHeadTrackingSystem.LookPriority.ATTACK_TARGET.level
         && this.body.getTransportMode() == TransportMode.GROUNDED
         && !this.body.isPassenger()
         && !this.body.isVehicle()
         && !this.body.asleep()
         && !this.body.getSyncedIsIncapacitated()
         && this.standing();
      if (!may) {
         this.turningToLook = false;
      } else {
         float maxYaw = this.getMaxHeadYaw();
         float abs = Math.abs(this.targetHeadYaw);
         if (!this.turningToLook && abs > maxYaw * 0.75F) {
            this.turningToLook = true;
         } else if (this.turningToLook && abs < maxYaw * 0.25F) {
            this.turningToLook = false;
         }

         if (this.turningToLook) {
            float fromSteering = this.targetHeadYaw - Mth.wrapDegrees(this.body.getYRot() - this.bodyFrameYaw());
            float step = Mth.clamp(fromSteering * 0.2F, -4.0F, 4.0F);
            this.body.setYRot(Mth.wrapDegrees(this.body.getYRot() + step));
         }
      }
   }

   static float stabilise(float smoothedRelativeYaw, float bodyDelta, float maxYaw) {
      return Mth.clamp(smoothedRelativeYaw - bodyDelta, -maxYaw, maxYaw);
   }

   private void handlePlayerCursorTracking(Player player) {
      this.currentPriority = FaunaHeadTrackingSystem.LookPriority.PLAYER_CURSOR;
      float playerYaw = player.getYRot();
      float playerPitch = player.getXRot();
      float bodyYaw = this.body.getYRot();
      this.targetHeadYaw = Mth.wrapDegrees(playerYaw - bodyYaw);
      this.targetHeadPitch = playerPitch;
      ClimbEngine climb = this.body.isClimbing() ? this.body.componentRegistry.get(ClimbEngine.class) : null;
      if (climb != null) {
         float[] look = climb.lookFromBody(playerYaw, playerPitch);
         this.targetHeadYaw = look[0];
         this.targetHeadPitch = look[1];
      }

      FaunaFlightRotationComponent rotComp = this.body.componentRegistry.get(FaunaFlightRotationComponent.class);
      if (rotComp != null && rotComp.isRolling()) {
         this.Targeting3DFlightOvershoot(rotComp);
      }

      this.targetHeadYaw = Mth.clamp(this.targetHeadYaw, -this.getMaxHeadYaw(), this.getMaxHeadYaw());
      this.targetHeadPitch = Mth.clamp(this.targetHeadPitch, -this.getMaxHeadPitchUp(), this.getMaxHeadPitchDown());
      this.follow(FaunaHeadTrackingSystem.LookPriority.PLAYER_CURSOR);
   }

   private void updateGroundGaze() {
      float want = 0.0F;
      if (this.body.isVehicle() && this.body.getControllingPassenger() instanceof Player && !this.body.isFlying()) {
         Footing foot = this.body.footing();
         double speed = this.body.isClimbing()
            ? Math.hypot(foot.x() - foot.groundOld().x, foot.z() - foot.groundOld().z)
            : Math.hypot(this.body.getX() - this.body.xo, this.body.getZ() - this.body.zo);
         want = (float)Math.min(1.0, speed / 0.1);
      }

      float rate = 0.15F;
      this.groundGaze = this.groundGaze + Mth.clamp(want - this.groundGaze, -rate, rate);
   }

   private void Targeting3DFlightOvershoot(FaunaFlightRotationComponent rotComp) {
      float roll = Mth.wrapDegrees(rotComp.getRoll());
      float simulatedYaw = roll * 0.4F;
      float absRoll = Math.abs(roll);
      if (absRoll > 90.0F) {
         float upsideDownFactor = 1.0F - (absRoll - 90.0F) / 90.0F;
         simulatedYaw *= upsideDownFactor;
      }

      RollComponent rolls = this.body.componentRegistry.get(RollComponent.class);
      if (rolls != null && rolls.rollingOver()) {
         simulatedYaw = 0.0F;
      }

      this.targetHeadYaw += simulatedYaw;
      this.targetHeadYaw = Mth.wrapDegrees(this.targetHeadYaw);
   }

   public void glanceAt(Vec3 position, int ticks, float strength) {
      boolean holding = this.glancePos != null && this.body.tickCount < this.glanceUntil;
      if (!holding || !(strength < this.glanceStrength)) {
         this.glancePos = position;
         this.glanceUntil = this.body.tickCount + Math.max(0, ticks);
         this.glanceStrength = strength;
      }
   }

   private Vec3 reactedToNoisePos() {
      if (this.glancePos != null && this.body.tickCount >= this.glanceUntil) {
         this.glancePos = null;
      }

      return this.glancePos;
   }

   public boolean canAimAt(Vec3 targetPos) {
      float[] yp = HeadLookSolver.solve(this.body.getEyePosition(), this.bodyFrameYaw(), targetPos);
      return Math.abs(yp[0]) <= this.getMaxHeadYaw();
   }

   private void cockForBlindSpot() {
      if (cocksAt(this.currentPriority)) {
         SpeciesProfile.Senses senses = this.body.species().senses();
         float blind = SightSense.blindOf(senses);
         if (!(blind <= 0.0F)) {
            float cock = cockOffset(blind, senses.perfectVisionDeg());
            this.cockRight = cockRight(this.targetHeadYaw, this.smoothedHeadYaw, cock, this.cockRight);
            this.targetHeadYaw = this.targetHeadYaw + (this.cockRight ? cock : -cock);
         }
      }
   }

   public static boolean cocksAt(FaunaHeadTrackingSystem.LookPriority priority) {
      return priority.level < FaunaHeadTrackingSystem.LookPriority.ATTACK_TARGET.level;
   }

   public static float cockOffset(float blind, float perfectDeg) {
      return (blind + perfectDeg) * 0.5F;
   }

   public static boolean cockRight(float targetYaw, float smoothedYaw, float cock, boolean currentlyRight) {
      float toRight = Math.abs(targetYaw + cock - smoothedYaw);
      float toLeft = Math.abs(targetYaw - cock - smoothedYaw);
      return currentlyRight ? toRight <= toLeft + 25.0F : toRight + 25.0F < toLeft;
   }

   private void calculateRotationToTarget(Vec3 target) {
      float[] yp = HeadLookSolver.solve(this.body.getEyePosition(), this.bodyFrameYaw(), target);
      this.targetHeadYaw = yp[0];
      this.targetHeadPitch = yp[1];
   }

   float bodyFrameYaw() {
      return WalkEngine.travelFacing(this.body, this.body.steeredByRider());
   }

   public float headWorldYaw() {
      return Mth.wrapDegrees(this.bodyFrameYaw() + this.smoothedHeadYaw);
   }

   public float headWorldPitch() {
      return this.smoothedHeadPitch;
   }

   public Vec3 headLookDirection() {
      return Vec3.directionFromRotation(this.headWorldPitch(), this.headWorldYaw());
   }

   public String debugAimLine() {
      LivingEntity engaged = this.body.weaponTarget();
      float frameYaw = this.bodyFrameYaw();
      Vec3 aim = this.headLookDirection();
      String want = "none";
      String errDeg = "n/a";
      if (engaged != null) {
         Vec3 ideal = engaged.getEyePosition().subtract(this.body.getEyePosition());
         if (ideal.lengthSqr() > 1.0E-6) {
            ideal = ideal.normalize();
            double dot = Mth.clamp(aim.dot(ideal), -1.0, 1.0);
            want = String.format("(%.2f,%.2f,%.2f)", ideal.x, ideal.y, ideal.z);
            errDeg = String.format("%.1f", Math.toDegrees(Math.acos(dot)));
         }
      }

      return String.format(
         "[AIM %s] id=%d mode=%s yRot=%.1f bodyFrame=%.1f headOff(y=%.1f p=%.1f) worldAim(y=%.1f p=%.1f) prio=%s engaged=%s aim=(%.2f,%.2f,%.2f) want=%s ERR=%s deg",
         this.body.level().isClientSide ? "C" : "S",
         this.body.getId(),
         this.body.getTransportMode(),
         this.body.getYRot(),
         frameYaw,
         this.smoothedHeadYaw,
         this.smoothedHeadPitch,
         this.headWorldYaw(),
         this.headWorldPitch(),
         this.currentPriority,
         engaged == null ? "null" : engaged.getName().getString(),
         aim.x,
         aim.y,
         aim.z,
         want,
         errDeg
      );
   }

   public void applyToBones(AzBone[] neckBones, AzBone headBone, int segmentCount, HeadTrackSpec spec, AzBakedModel model) {
      this.applyToBones(neckBones, headBone, segmentCount, spec, 0.0F, 1.0F, null, model);
   }

   public void applyToBones(
      AzBone[] neckBones,
      AzBone headBone,
      int segmentCount,
      HeadTrackSpec spec,
      float bodyPitchDeg,
      float partialTick,
      @Nullable String bodyBone,
      AzBakedModel model
   ) {
      if (headBone != null) {
         this.applyAngles(
            neckBones,
            headBone,
            segmentCount,
            spec,
            bodyPitchDeg,
            Mth.lerp(partialTick, this.smoothedHeadYawO, this.smoothedHeadYaw),
            Mth.lerp(partialTick, this.smoothedHeadPitchO, this.smoothedHeadPitch),
            bodyBone,
            model
         );
      }
   }

   public void applyAngles(
      AzBone[] neckBones,
      AzBone headBone,
      int segmentCount,
      HeadTrackSpec spec,
      float bodyPitchDeg,
      float yaw,
      float pitch,
      @Nullable String bodyBone,
      AzBakedModel model
   ) {
      if (headBone != null) {
         this.setNeckSegmentCount(segmentCount);
         float yawRad = -yaw * (float) (Math.PI / 180.0);
         float keep = 1.0F - 0.8F * this.groundGaze;
         float drawn = bodyPitchDeg + (pitch - bodyPitchDeg) * keep;
         float pitchRad = -(drawn - bodyPitchDeg) * (float) (Math.PI / 180.0);
         int slots = spec.neckSlots();
         float totalWeight = 0.0F;

         for (int i = 0; i < segmentCount && i < slots; i++) {
            if (neckBones[i] != null) {
               totalWeight += spec.weight(i);
            }
         }

         totalWeight += spec.weight(Math.min(segmentCount, slots));
         if (totalWeight == 0.0F) {
            totalWeight = 1.0F;
         }

         AzBone first = headBone;

         for (int i = 0; i < segmentCount && i < slots; i++) {
            if (neckBones[i] != null) {
               first = neckBones[i];
               break;
            }
         }

         String reference = bodyBone != null ? bodyBone : (first.getParent() == null ? null : first.getParent().getName());
         float done = 0.0F;

         for (int i = 0; i < segmentCount && i < slots; i++) {
            if (neckBones[i] != null) {
               float weight = spec.weight(i) / totalWeight;
               turn(neckBones[i], reference, model, LookFrame.share(yawRad, pitchRad, done, done + weight));
               done += weight;
            }
         }

         turn(headBone, reference, model, LookFrame.share(yawRad, pitchRad, done, done + (1.0 - done) * spec.headReach()));
      }
   }

   private static void turn(AzBone bone, String reference, AzBakedModel model, double[] delta) {
      float[] e = LookFrame.turn(between(bone, reference, model), bone.getRotX(), bone.getRotY(), bone.getRotZ(), delta);
      bone.setRotX(e[0]);
      bone.setRotY(e[1]);
      bone.setRotZ(e[2]);
   }

   static double[] between(AzBone bone, String reference, AzBakedModel model) {
      List<AzBone> chain = BoneFk.ancestorsTopDown(bone, model);
      int start = 0;
      if (reference != null) {
         for (int i = chain.size() - 2; i >= 0; i--) {
            if (chain.get(i).getName().equals(reference)) {
               start = i + 1;
               break;
            }
         }
      }

      double[] product = LookFrame.IDENTITY;

      for (int i = start; i < chain.size() - 1; i++) {
         AzBone p = chain.get(i);
         product = LookFrame.mul(product, LookFrame.of(p.getRotX(), p.getRotY(), p.getRotZ()));
      }

      return product;
   }

   public float getSmoothedHeadYaw() {
      return this.smoothedHeadYaw;
   }

   public float getSmoothedHeadPitch() {
      return this.smoothedHeadPitch;
   }

   public enum LookPriority {
      NONE(0),
      AMBIENT(0),
      ENTITY(1),
      NOISE(2),
      ATTACK_TARGET(3),
      PLAYER_CURSOR(4);

      public final int level;

      LookPriority(int level) {
         this.level = level;
      }
   }
}
