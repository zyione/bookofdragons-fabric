package net.magister.liberanimalium.body.locomotion;

import net.magister.liberanimalium.body.physics.PhysicsSheet;
import net.magister.liberanimalium.body.state.TransportMode;
import net.magister.liberanimalium.body.stats.FaunaStatSheet;
import net.magister.liberanimalium.chassis.FaunaBase;
import net.magister.liberanimalium.interop.SubLevels;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.phys.Vec3;

public class WalkEngine implements ModeEngine {
   public static final float SPRINT_FACTOR = 2.0F;
   public static final float WALK_FACTOR = 0.6F;
   public static final float AI_GROUND_SPEED_SCALE = 1.0F;
   private static final float FRICTION = 0.016F;
   private static final float MAX_YAW_LIMIT = 60.0F;
   private static final double BODY_YAW_MIN_SPEED = 0.02;
   public static final float WALK_BOOST = 1.4F;
   public static final float TURN_BOOST_KNEE = 0.5F;
   public static final double GRAVITY = 0.08;
   public static final double HOP_CLEARANCE = 0.25;
   public static final double TERMINAL_FALL = 0.8;
   private FaunaBase body;
   private float localVelZ = 0.0F;
   private boolean wasGroundLastTick = false;
   private boolean wasRiddenLastTick = false;
   static final float STRAFE_SHARE = 0.55F;

   public static double hopSpeed(double rise) {
      double height = Math.max(0.0, rise) + 0.25;
      return (-0.08 + Math.sqrt(0.0064 + 0.64 * height)) * 0.5;
   }

   public static double fall(double vy) {
      return Math.max(vy - 0.08, -0.8);
   }

   public static double airTicks(double launch) {
      return Math.ceil(2.0 * launch / 0.08 + 1.0);
   }

   public static double leapReach(double rise) {
      if (rise <= 0.0) {
         return 0.0;
      }

      double launch = hopSpeed(rise);
      return launch * airTicks(launch);
   }

   @Override
   public void onAttach(FaunaBase host) {
      this.body = host;
   }

   @Override
   public LocomotionMode mode() {
      return LocomotionMode.WALK;
   }

   @Override
   public void tick(SteeringIntent intent) {
      FaunaStatSheet stats = this.body.getStatSheet();
      if (stats != null) {
         boolean hasControllingRider = this.body.steeredByRider();
         if (this.wasRiddenLastTick != hasControllingRider) {
            this.localVelZ = this.projectVelocityOntoFacing();
            this.wasRiddenLastTick = hasControllingRider;
         }

         this.updateGroundYaw(intent.yawTarget(), stats);
         if (hasControllingRider) {
            this.body.setXRot(intent.pitchTarget());
         }

         this.body.yBodyRot = this.bodyYaw(hasControllingRider);
         this.body.yHeadRot = this.body.yBodyRot;
         float forward = intent.forward();
         float strafe = intent.strafe();
         PhysicsSheet.Ground ground = PhysicsSheet.of(this.body).ground();
         float maxVel = stats.groundSpeed();
         float accelTime = ground.accelTime() * stats.inertia();
         float inertia = ground.response() / stats.inertia();
         float speedCoupl = ground.turnSpeedCoupling();
         boolean sprinting = intent.sprint();
         float topSpeed = gaitTop(maxVel, hasControllingRider, sprinting, forward);
         float accel = rampStep(topSpeed, accelTime);
         float yawError = Math.abs(Mth.wrapDegrees(intent.yawTarget() - this.body.getYRot()));
         float effectiveTopSpeed = turnCut(topSpeed, yawError, speedCoupl);
         this.localVelZ = pace(this.localVelZ, forward, effectiveTopSpeed, accel);
         float strafeTarget = strafe * effectiveTopSpeed * 0.55F;
         float yawRad = -this.body.getYRot() * (float) (Math.PI / 180.0);
         double cosYaw = Math.cos(yawRad);
         double sinYaw = Math.sin(yawRad);
         double worldTargetX = strafeTarget * cosYaw + this.localVelZ * sinYaw;
         double worldTargetZ = -strafeTarget * sinYaw + this.localVelZ * cosYaw;
         Vec3 current = this.body.getDeltaMovement();
         double dot = worldTargetX * current.x + worldTargetZ * current.z;
         double inertiaFactor = inertiaShare(inertia, dot, current.horizontalDistance());
         double newX = Mth.lerp(inertiaFactor, current.x, worldTargetX);
         double newZ = Mth.lerp(inertiaFactor, current.z, worldTargetZ);
         double newY;
         if (this.body.onGround()) {
            newY = current.y < 0.0 ? current.y : (SubLevels.present() ? fall(0.0) : -1.0E-4);
         } else {
            newY = fall(current.y);
         }

         this.body.setDeltaMovement(newX, newY, newZ);
         this.body.move(MoverType.SELF, this.body.getDeltaMovement());
      }
   }

   static float gaitTop(float maxVel, boolean ridden, boolean sprinting, float forward) {
      if (ridden) {
         return sprinting ? maxVel * 2.0F : maxVel * 0.6F;
      }

      float walkSprintFactor = sprinting ? 2.0F : 0.6F;
      return maxVel * walkSprintFactor * Math.max(0.0F, forward) * 1.0F;
   }

   static float rampStep(float topSpeed, float accelTime) {
      return topSpeed * 2.0F / Math.max(accelTime * 20.0F, 1.0F);
   }

   static float turnCut(float topSpeed, float yawError, float coupling) {
      float s = Mth.clamp(yawError / 60.0F, 0.0F, 1.0F);
      s *= s;
      return topSpeed / Math.max(1.0F + coupling * s, 1.0F);
   }

   static float pace(float localVelZ, float forward, float effectiveTopSpeed, float accel) {
      if (Math.abs(forward) > 0.05F) {
         if (forward > 0.0F && localVelZ < effectiveTopSpeed) {
            localVelZ += accel;
            localVelZ = Math.min(localVelZ, effectiveTopSpeed);
         } else if (forward < 0.0F && localVelZ > -(effectiveTopSpeed / 3.0F)) {
            localVelZ -= accel * 0.5F;
            localVelZ = Math.max(localVelZ, -(effectiveTopSpeed / 3.0F));
         }
      } else {
         float sign = Math.signum(localVelZ);
         float decay = Math.min(0.016F, Math.abs(localVelZ));
         localVelZ -= sign * decay;
         if (Math.signum(localVelZ) != sign) {
            localVelZ = 0.0F;
         }
      }

      if (localVelZ > effectiveTopSpeed) {
         localVelZ = Math.max(localVelZ - 0.016F, effectiveTopSpeed);
      } else if (localVelZ < -(effectiveTopSpeed / 3.0F)) {
         localVelZ = Math.min(localVelZ + 0.016F, -(effectiveTopSpeed / 3.0F));
      }

      return localVelZ;
   }

   static double inertiaShare(double inertia, double dot, double speed) {
      return dot < 0.0 && speed > 0.01 ? inertia * 0.18 : inertia;
   }

   public void takeOver() {
      this.localVelZ = this.projectVelocityOntoFacing();
      this.wasRiddenLastTick = this.body.steeredByRider();
   }

   private float projectVelocityOntoFacing() {
      Vec3 vel = this.body.getDeltaMovement();
      float yawRad = -this.body.getYRot() * (float) (Math.PI / 180.0);
      double sinYaw = Math.sin(yawRad);
      double cosYaw = Math.cos(yawRad);
      return (float)(vel.x * sinYaw + vel.z * cosYaw);
   }

   private float bodyYaw(boolean hasControllingRider) {
      return travelFacing(this.body, hasControllingRider);
   }

   public static float travelFacing(LivingEntity mob, boolean hasControllingRider) {
      float desired = desiredFacing(mob, hasControllingRider);
      if (mob instanceof FaunaBase fauna) {
         if (!hasControllingRider && fauna.getTransportMode() == TransportMode.GROUNDED) {
            float current = fauna.getBodyFacing();
            if (Float.isNaN(current)) {
               fauna.setBodyFacing(desired);
               return desired;
            }

            if (!fauna.claimBodyFacingTick()) {
               return current;
            }

            float step = maxBodyTurnStep(fauna);
            float delta = Mth.clamp(Mth.wrapDegrees(desired - current), -step, step);
            float next = Mth.wrapDegrees(current + delta);
            fauna.setBodyFacing(next);
            return next;
         } else {
            fauna.setBodyFacing(desired);
            return desired;
         }
      } else {
         return desired;
      }
   }

   private static float maxBodyTurnStep(FaunaBase body) {
      FaunaStatSheet stats = body.getStatSheet();
      return stats == null ? 60.0F : Math.max(1.0F, stats.groundTurn() * 1.4F);
   }

   private static float desiredFacing(LivingEntity mob, boolean hasControllingRider) {
      if (hasControllingRider) {
         return mob.getYRot();
      } else if (mob instanceof FaunaBase d && d.getTransportMode() != TransportMode.GROUNDED) {
         return mob.getYRot();
      } else {
         Vec3 v = mob.getDeltaMovement();
         return v.horizontalDistance() < 0.02 ? mob.getYRot() : pathYaw(v.x, v.z);
      }
   }

   private void updateGroundYaw(float targetYaw, FaunaStatSheet stats) {
      float turnAmount = computeYawTurnAmount(
         this.body.getYRot(), targetYaw, stats.groundTurn(), this.body.getDeltaMovement().horizontalDistance(), stats.groundSpeed() * 2.0F
      );
      this.body.setYRot(this.body.getYRot() + turnAmount);
   }

   static float computeYawTurnAmount(float currentYaw, float targetYaw, float turnRateStat, double currentSpeed, double topSpeed) {
      float rotDiff = Mth.wrapDegrees(targetYaw - currentYaw);
      rotDiff = Mth.clamp(rotDiff, -60.0F, 60.0F);
      if (Math.abs(rotDiff) < 0.5F) {
         return 0.0F;
      }

      float rotDiffNorm = rotDiff / 60.0F;
      float minRotMod = 0.4F;
      float sqrtAbs = (float)Math.sqrt(Math.abs(rotDiffNorm));
      float rotDiffMod = (sqrtAbs * (1.0F - minRotMod) + minRotMod) * Math.signum(rotDiff);
      double relSpeed = topSpeed > 0.0 ? Math.min(currentSpeed / topSpeed, 1.0) : 0.0;
      float turnRate = turnRateStat * turnBoost(relSpeed);
      float turnAmount = turnRate * rotDiffMod;
      return Mth.clamp(turnAmount, -Math.abs(rotDiff), Math.abs(rotDiff));
   }

   static float turnBoost(double speedShare) {
      if (speedShare <= 0.5) {
         return 1.4F;
      }

      double t = Math.min(1.0, (speedShare - 0.5) / 0.5);
      return (float)(1.4F + -0.39999998F * t);
   }

   public boolean isWasGroundLastTick() {
      return this.wasGroundLastTick;
   }

   public void setWasGroundLastTick(boolean value) {
      this.wasGroundLastTick = value;
   }

   @Override
   public void save(CompoundTag tag) {
      tag.putFloat("LocoVelZ", this.localVelZ);
   }

   @Override
   public void load(CompoundTag tag) {
      this.localVelZ = tag.getFloat("LocoVelZ");
   }

   public static float pathYaw(double vx, double vz) {
      return (float)(Mth.atan2(vz, vx) * (180.0 / Math.PI)) - 90.0F;
   }
}
