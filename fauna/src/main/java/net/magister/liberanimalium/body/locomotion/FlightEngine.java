package net.magister.liberanimalium.body.locomotion;

import net.magister.liberanimalium.body.physics.PhysicsSheet;
import net.magister.liberanimalium.body.state.FaunaStateContext;
import net.magister.liberanimalium.chassis.FaunaBase;
import net.magister.liberanimalium.chassis.component.DiveBreakComponent;
import net.magister.liberanimalium.chassis.component.FaunaFlightRotationComponent;
import net.magister.liberanimalium.chassis.component.RollComponent;
import net.magister.liberanimalium.chassis.component.ThrottleComponent;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

public class FlightEngine implements ModeEngine {
   private FaunaBase body;
   private Attitude2D attitude2D;
   private static final int SLIP_STEPS = 4;
   private boolean stalled;
   private boolean folded;

   @Override
   public void onAttach(FaunaBase host) {
      this.body = host;
      this.attitude2D = new Attitude2D(host);
   }

   @Override
   public LocomotionMode mode() {
      return LocomotionMode.FLY;
   }

   @Override
   public void tick(SteeringIntent intent) {
      LivingEntity rider = this.body.getControllingPassenger();
      if (rider == null) {
         this.stalled = false;
         this.folded = false;
         this.tickAI(intent);
      } else {
         FaunaFlightRotationComponent attitude = this.body.componentRegistry.get(FaunaFlightRotationComponent.class);
         boolean threeD = attitude != null && attitude.isRolling();
         RollComponent rolls = this.body.componentRegistry.get(RollComponent.class);
         boolean dodging = rolls != null && rolls.dodging();
         double top = this.body.getAttributeValue(Attributes.FLYING_SPEED);
         float adjustedStrafe = threeD && !dodging
            ? strafeShare(attitude.strafeDrive(), PhysicsSheet.of(this.body).flight().strafeSpeed(), this.body.getThrottleLevel() * top, top)
            : 0.0F;
         float forward = intent.forward();
         if (forward < 0.0F && this.body.getLocomotionOrchestrator().ejectGrace()) {
            forward = 0.0F;
         }

         ThrottleComponent throttle = this.body.componentRegistry.get(ThrottleComponent.class);
         float adjustedForward = throttle != null ? throttle.tickThrottleMode(forward) : forward;
         boolean wasFalling = this.stalled || this.folded;
         this.updateStallAndFold(rider);
         this.updateRiderRotationDeadzone(rider, intent.forward(), intent.strafe());
         if (this.body.hasImpulse) {
            this.body.hasImpulse = false;
         } else {
            Vec3 written = this.calculateTargetFlightVelocity(adjustedStrafe, adjustedForward);
            Vec3 dodge = rolls == null ? Vec3.ZERO : rolls.dodgeStep();
            if (dodge != Vec3.ZERO) {
               this.body.move(MoverType.SELF, dodge);
            }

            if (throttle != null && (this.folded || wasFalling && !this.stalled)) {
               throttle.matchSpeed(this.body.getDeltaMovement().length());
            }

            if (this.body.horizontalCollision) {
               Vec3 impact = WallHit.impact(written, this.body.getDeltaMovement());
               if (WallHit.isHit(impact) && !this.slip(written, impact)) {
                  float severity = WallHit.severity(impact, this.body.getAttributeValue(Attributes.FLYING_SPEED));
                  this.body.getLocomotionOrchestrator().requestWallHit(impact, severity, ModeChangeCause.WALL_HIT);
               }
            }
         }
      }
   }

   private boolean slip(Vec3 written, Vec3 lost) {
      double reach = this.body.maxUpStep();
      Vec3 side = new Vec3(-lost.z, 0.0, lost.x).normalize();
      Vec3[] ways = new Vec3[]{new Vec3(0.0, 1.0, 0.0), new Vec3(0.0, -1.0, 0.0), side, side.scale(-1.0)};
      AABB box = this.body.getBoundingBox();

      for (int i = 1; i <= 4; i++) {
         double distance = reach * i / 4.0;

         for (Vec3 way : ways) {
            Vec3 nudge = way.scale(distance);
            if (this.body.level().noCollision(this.body, box.move(nudge)) && this.body.level().noCollision(this.body, box.move(nudge.add(lost)))) {
               this.body.move(MoverType.SELF, nudge.add(lost));
               this.body.setDeltaMovement(written);
               return true;
            }
         }
      }

      return false;
   }

   public boolean isStalled() {
      return this.stalled;
   }

   public boolean isFolded() {
      return this.folded;
   }

   private void updateStallAndFold(LivingEntity rider) {
      if (this.body.isFlying() && !this.body.getLocomotionOrchestrator().isTransitionActive()) {
         this.folded = rider.isShiftKeyDown();
         PhysicsSheet.Flight flight = PhysicsSheet.of(this.body).flight();
         this.stalled = stallNext(
            this.stalled,
            this.body.getXRot(),
            this.body.getThrottleLevel(),
            this.body.getStatSheet().stallSpeed(),
            flight.stallAngle(),
            flight.stallRecoverAngle()
         );
      } else {
         this.stalled = false;
         this.folded = false;
      }
   }

   static boolean stallNext(boolean stalled, float pitch, float throttle, float stallSpeed, float stallAngle, float recoverAngle) {
      return stalled ? pitch < recoverAngle : stallSpeed > 0.0F && pitch <= -stallAngle && throttle < stallSpeed;
   }

   private void tickAI(SteeringIntent intent) {
      float yawError = Mth.wrapDegrees(intent.yawTarget() - this.body.getYRot());
      rotationChase(this.body, intent.yawTarget(), intent.pitchTarget());
      FaunaFlightRotationComponent rotationComp = this.body.componentRegistry.get(FaunaFlightRotationComponent.class);
      if (rotationComp != null) {
         PhysicsSheet.AiBank bank = PhysicsSheet.of(this.body).aiBank();
         float targetRoll = -Mth.clamp(yawError * bank.perDegree(), -bank.max(), bank.max());
         float rollRate = Mth.clamp(PhysicsSheet.of(this.body).flight().bankRate() * this.body.getStatSheet().agility(), bank.rateMin(), bank.rateMax())
            * bank.rateScale();
         rotationComp.stepRoll(Mth.approach(rotationComp.getRoll(), targetRoll, rollRate));
      }

      ThrottleComponent throttle = this.body.componentRegistry.get(ThrottleComponent.class);
      float adjustedForward = throttle != null ? throttle.tickThrottleModeAI(intent.forward()) : intent.forward();
      this.calculateTargetFlightVelocity(0.0F, adjustedForward, intent.vertical());
   }

   static void rotationChase(FaunaBase body, float yawTarget, float pitchTarget) {
      float maxTurnSpeed = calculateMaxTurnSpeed(body);
      float currentYaw = body.getYRot();
      float yawDiff = Mth.wrapDegrees(yawTarget - currentYaw);
      float yawChange = Mth.clamp(yawDiff, -maxTurnSpeed, maxTurnSpeed);
      body.setYRot(currentYaw + yawChange);
      chasePitch(body, pitchTarget);
   }

   static void chasePitch(FaunaBase body, float pitchTarget) {
      float currentPitch = body.getXRot();
      float pitchDiff = Mth.wrapDegrees(pitchTarget - currentPitch);
      PhysicsSheet.Chase chase = PhysicsSheet.of(body).chase();
      float maxPitchSpeed = PhysicsSheet.of(body).flight().pitchRate() * body.getStatSheet().agility() * chase.pitchScale();
      float cap = pitchDiff < 0.0F ? maxPitchSpeed * chase.climbFactor() : maxPitchSpeed * chase.diveFactor();
      cap = Math.min(cap, pitchLimit(body));
      body.setXRot(currentPitch + Mth.clamp(pitchDiff, -cap, cap));
   }

   public static float pitchLimit(FaunaBase body) {
      return body.isAquatic() ? Float.MAX_VALUE : pitchLimit(body.flightNumbers().pitchPull(), body.getStatSheet().agility(), body.getDeltaMovement().length());
   }

   static float pitchLimit(float pull, float agility, double speed) {
      return pull <= 0.0F ? Float.MAX_VALUE : (float)Math.min(180.0, Math.toDegrees(pull * agility / Math.max(speed, 0.001)));
   }

   public Vec3 calculateTargetFlightVelocity(float adjustedStrafe, float adjustedForward) {
      return this.calculateTargetFlightVelocity(adjustedStrafe, adjustedForward, 0.0F);
   }

   public Vec3 calculateTargetFlightVelocity(float adjustedStrafe, float adjustedForward, float vertical) {
      float baseSpeed = (float)this.body.getAttributeValue(Attributes.FLYING_SPEED);
      FaunaStateContext context = this.body.getStateContext();
      boolean isDiveBreakingState = context.isDiveBreaking() && (this.body.steeredByRider() || FlightGait.divingAt(context.getCalculatedPitch()));
      float groundDist = context.getCachedGroundDistance();
      float throttleMultiplier = this.body.getThrottleLevel();
      float targetSpeed = baseSpeed * throttleMultiplier;
      if (isDiveBreakingState) {
         DiveBreakComponent diveBreak = this.body.componentRegistry.get(DiveBreakComponent.class);
         if (diveBreak != null) {
            float speedMultiplier = diveBreak.getDiveBreakSpeedMultiplier(groundDist);
            targetSpeed *= speedMultiplier;
         }
      }

      Vec3 currentVelocity = this.body.getDeltaMovement();
      PhysicsSheet.Flight flightSheet = PhysicsSheet.of(this.body).flight();
      float dynamicVerticalDrag = flightSheet.verticalDrag();
      Vec3 newVelocity = servo(this.body, currentVelocity, targetSpeed, baseSpeed, adjustedForward, adjustedStrafe, vertical);
      if (!this.stalled && !this.folded) {
         if (WaterDepth.share(this.body) > 0.0) {
            PhysicsSheet.Water water = PhysicsSheet.of(this.body).water();
            double share = WaterDepth.share(this.body);
            double keep = 1.0 - water.skimDrag() - water.plungeDrag() * share;
            double y = waterVertical(newVelocity, share, keep, water.skimBounce());
            newVelocity = new Vec3(newVelocity.x * keep, y + water.plungeLift() * share, newVelocity.z * keep);
            ThrottleComponent throttle = this.body.componentRegistry.get(ThrottleComponent.class);
            float top = (float)this.body.getAttributeValue(Attributes.FLYING_SPEED);
            if (throttle != null && newVelocity.length() < this.body.getThrottleLevel() * top) {
               throttle.matchSpeed(newVelocity.length());
            }
         }
      } else {
         newVelocity = fall(currentVelocity, this.body.getStatSheet().gravity(), this.body.diveCap() * baseSpeed);
      }

      this.body.setDeltaMovement(newVelocity);
      this.body.move(MoverType.SELF, this.body.getDeltaMovement());
      return newVelocity;
   }

   static double waterVertical(Vec3 velocity, double share, double keep, double bounce) {
      if (velocity.y >= 0.0) {
         return velocity.y * keep;
      }

      double floor = velocity.y * (1.0 - share) - velocity.y * bounce * share;
      double glance = Math.sqrt(velocity.x * velocity.x + velocity.z * velocity.z) / velocity.length();
      return Mth.lerp(glance, velocity.y * keep, floor);
   }

   static Vec3 fall(Vec3 velocity, double gravity, double terminal) {
      Vec3 next = new Vec3(velocity.x, velocity.y - gravity, velocity.z);
      double speed = next.length();
      return speed > terminal && speed > 1.0E-9 ? next.scale(terminal / speed) : next;
   }

   public static float strafeShare(float input, float strafeSpeed, double airspeed, double top) {
      if (top <= 0.0) {
         return 0.0F;
      }

      double hover = 0.15F;
      double room = airspeed < hover ? 0.9F * Math.sqrt(hover * hover - airspeed * airspeed) : 0.0;
      return (float)(input * Math.max(strafeSpeed * airspeed, room) / top);
   }

   public void updateRiderRotationDeadzone(LivingEntity rider, float forwardInput, float strafeInput) {
      this.attitude2D
         .rider(rider, forwardInput, strafeInput, this.folded, this.stalled, rider.getXRot(), this.body.getAccumulatedRoll() * (180.0F / (float)Math.PI));
   }

   static Vec3 servo(FaunaBase body, Vec3 currentVelocity, float speed, float baseSpeed, float forward, float strafe, float vertical) {
      float pitchRad = (float)Math.toRadians(body.getXRot());
      float cosPitch = Mth.cos(pitchRad);
      float sinPitch = Mth.sin(pitchRad);
      double yDir = 0.0;
      float forwardMultiplier = 1.0F;
      if (!body.isFreeLooking() && forward > 0.0F) {
         yDir = -sinPitch;
         forwardMultiplier = Math.abs(cosPitch);
      }

      Vec3 rawDirection = new Vec3(0.0, yDir, forward > 0.0F ? forwardMultiplier : 0.0);
      Vec3 finalDirection = Vec3.ZERO;
      if (rawDirection.lengthSqr() > 1.0E-7) {
         finalDirection = rawDirection.normalize().yRot(-body.getYRot() * (float) (Math.PI / 180.0));
      }

      float inputMagnitude = forward > 0.0F ? 1.0F : 0.0F;
      Vec3 targetVelocity = finalDirection.scale(speed * inputMagnitude);
      if (strafe != 0.0F) {
         targetVelocity = targetVelocity.add(new Vec3(strafe * baseSpeed, 0.0, 0.0).yRot(-body.getYRot() * (float) (Math.PI / 180.0)));
      }

      if (vertical != 0.0F) {
         targetVelocity = targetVelocity.add(0.0, Mth.clamp(vertical, -1.0F, 1.0F) * baseSpeed, 0.0);
      }

      PhysicsSheet.Flight flightSheet = PhysicsSheet.of(body).flight();
      float dynamicInertiaFactor = flightSheet.response() * body.getStatSheet().agility();
      float dynamicVerticalDrag = flightSheet.verticalDrag();
      float throttle = body.getThrottleLevel();
      float inertiaScale = 1.0F - Mth.clamp(throttle, 0.0F, 2.0F) * PhysicsSheet.of(body).throttle().responsePenalty();
      dynamicInertiaFactor *= inertiaScale;
      Vec3 newVelocity = currentVelocity.lerp(targetVelocity, dynamicInertiaFactor);
      return new Vec3(newVelocity.x, newVelocity.y * dynamicVerticalDrag, newVelocity.z);
   }

   private static float calculateMaxTurnSpeed(FaunaBase body) {
      return maxTurnSpeed(body, body.getDeltaMovement().length());
   }

   public static float maxTurnSpeed(FaunaBase body, double speed) {
      PhysicsSheet.Flight flight = body.flightNumbers();
      return (float)(flight.turnRate() * body.getStatSheet().agility() / (1.0 + speed * flight.turnSpeedPenalty()));
   }
}
