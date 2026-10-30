package net.magister.liberanimalium.chassis.component;

import net.magister.liberanimalium.body.locomotion.FlightGait;
import net.magister.liberanimalium.body.locomotion.WaterDepth;
import net.magister.liberanimalium.body.physics.PhysicsSheet;
import net.magister.liberanimalium.body.state.FlightTells;
import net.magister.liberanimalium.chassis.FaunaBase;
import net.magister.liberanimalium.network.ControlMessageThrottleSync;
import net.magister.liberanimalium.network.FaunaNetwork;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.ai.attributes.Attributes;

public class ThrottleComponent implements IFaunaComponent {
   protected FaunaBase body;
   public static final float THROTTLE_OVERDRIVE_MAX = 2.0F;
   private final ThrottleComponent.RampState ramp = new ThrottleComponent.RampState();
   private boolean seedThrottleOnNextAiTick;

   public void noteFlightGranted() {
      this.seedThrottleOnNextAiTick = true;
   }

   public void catchFall(double speed) {
      this.seedThrottleOnNextAiTick = false;
      this.matchSpeed(speed);
   }

   @Override
   public void onAttach(FaunaBase host) {
      this.body = host;
   }

   public float tickThrottleMode(float forwardInput) {
      if (!this.body.supportsThreeDMedium()) {
         return forwardInput;
      }

      RiderInputComponent riderInput = this.body.componentRegistry.get(RiderInputComponent.class);
      if (riderInput == null) {
         return forwardInput;
      }

      boolean isAcceleratingInput = forwardInput > 0.1F;
      boolean isDeceleratingInput = forwardInput < -0.1F;
      return this.runThrottleDynamics(
         isAcceleratingInput, isDeceleratingInput, riderInput.isBoosting(), riderInput.isBraking(), riderInput.isCruiseRequested(), true, 1.0F
      );
   }

   public float tickThrottleModeAI(float targetFraction) {
      if (!this.body.supportsThreeDMedium()) {
         return targetFraction;
      }

      RiderInputComponent riderInput = this.body.componentRegistry.get(RiderInputComponent.class);
      if (riderInput == null) {
         return targetFraction;
      }

      if (this.seedThrottleOnNextAiTick) {
         this.seedThrottleOnNextAiTick = false;
         this.body.setThrottleLevel(PhysicsSheet.of(this.body).takeoff().cruiseSeed());
      }

      float currentThrottleValue = this.body.getThrottleLevel();
      float band = PhysicsSheet.of(this.body).throttle().aiTrackingBand();
      boolean isAcceleratingInput = targetFraction > currentThrottleValue + band;
      boolean isDeceleratingInput = targetFraction < currentThrottleValue - band;
      float decelScale = Math.max(1.0F, 2.0F * currentThrottleValue);
      float adjusted = this.runThrottleDynamics(
         isAcceleratingInput, isDeceleratingInput, riderInput.isBoosting(), riderInput.isBraking(), riderInput.isCruiseRequested(), false, decelScale
      );
      boolean diving = !this.body.isAquatic() && FlightGait.divingAt(this.body.getStateContext().getCalculatedPitch());
      this.body.setAccelerating(targetFraction > band && !diving);
      return adjusted;
   }

   private float runThrottleDynamics(
      boolean isAcceleratingInput, boolean isDeceleratingInput, boolean boost, boolean brake, boolean cruiseRequested, boolean spendsStamina, float decelScale
   ) {
      float currentThrottleValue = this.body.getThrottleLevel();
      PhysicsSheet.Flight flight = this.body.flightNumbers();
      ThrottleComponent.ThrottleInputs in = new ThrottleComponent.ThrottleInputs(
         currentThrottleValue,
         this.body.getStatSheet().acceleration(),
         spendsStamina && this.body.getStaminaSystem().isStaminaDepleted(),
         FlightTells.climbing(this.body),
         this.body.isAquatic() ? this.body.getXRot() : this.body.getStateContext().getCalculatedPitch(),
         this.body.isActivelyRolling(),
         this.body.diveCap(),
         flight.forwardOverdriveCap(),
         flight.overdriveBleed(),
         flight.glideBleed(),
         flight.climbBleed() * this.body.getStatSheet().climbBleed(),
         flight.diveGain() * this.body.getStatSheet().diveGain() * (this.body.isAquatic() ? 1.0F : 1.0F - (float)WaterDepth.share(this.body)),
         decelScale,
         this.body.throttleNumbers()
      );
      ThrottleComponent.ThrottleResult result = throttleStep(isAcceleratingInput, isDeceleratingInput, boost, brake, cruiseRequested, in, this.ramp);
      float nextThrottle = result.nextThrottle();
      isAcceleratingInput = result.acceleratingInput();
      if (nextThrottle != currentThrottleValue || isAcceleratingInput != this.body.isAccelerating() || isDeceleratingInput != this.body.isDecelerating()) {
         this.body.setThrottleLevel(nextThrottle);
         this.body.setAccelerating(isAcceleratingInput);
         this.body.setDecelerating(isDeceleratingInput);
         if (this.body.level().isClientSide() && this.body.isControlledByLocalInstance()) {
            FaunaNetwork.sendToServer(new ControlMessageThrottleSync(nextThrottle, isAcceleratingInput, isDeceleratingInput));
         }
      }

      return this.body.getThrottleLevel();
   }

   public static ThrottleComponent.ThrottleResult throttleStep(
      boolean isAcceleratingInput,
      boolean isDeceleratingInput,
      boolean boost,
      boolean brake,
      boolean cruiseRequested,
      ThrottleComponent.ThrottleInputs in,
      ThrottleComponent.RampState ramp
   ) {
      float currentThrottleValue = in.currentThrottleValue();
      float accelStat = in.accelStat();
      PhysicsSheet.Throttle cfg = in.cfg();
      float throttleIncrement = accelStat * cfg.step();
      float boostRate = accelStat * cfg.boost();
      float brakeRate = accelStat * cfg.brake();
      float nextThrottle = currentThrottleValue;
      boolean staminaDepleted = in.staminaDepleted();
      if (cruiseRequested && !isAcceleratingInput && !isDeceleratingInput && !brake) {
         if (in.climbing()) {
            ramp.cruiseAccelerating = true;
            ramp.cruiseHoldTicks = cfg.cruiseHoldTicks();
         } else if (currentThrottleValue <= cfg.cruiseThrottle()) {
            ramp.cruiseAccelerating = true;
            ramp.cruiseHoldTicks = cfg.cruiseHoldTicks();
         }

         if (ramp.cruiseHoldTicks > 0) {
            ramp.cruiseHoldTicks--;
         } else {
            ramp.cruiseAccelerating = false;
         }

         if (ramp.cruiseAccelerating) {
            isAcceleratingInput = true;
         }
      } else {
         ramp.cruiseAccelerating = false;
         ramp.cruiseHoldTicks = 0;
      }

      if (isAcceleratingInput && !staminaDepleted) {
         ramp.poweredTicks = Math.min(cfg.powerRampTicks(), ramp.poweredTicks + 1);
         float powMult = (float)ramp.poweredTicks / cfg.powerRampTicks();
         if (nextThrottle < 1.0F) {
            nextThrottle = Math.min(1.0F, nextThrottle + throttleIncrement * powMult);
         }
      } else {
         ramp.poweredTicks = 0;
      }

      if (isDeceleratingInput) {
         nextThrottle -= throttleIncrement * in.decelScale();
      }

      if (brake && !staminaDepleted) {
         nextThrottle -= brakeRate;
      }

      if (boost && !staminaDepleted) {
         ramp.boostTicks = Math.min(cfg.boostRampTicks(), ramp.boostTicks + 1);
         float boostMult = (float)ramp.boostTicks / cfg.boostRampTicks();
         nextThrottle += boostRate * boostMult;
      } else {
         ramp.boostTicks = 0;
      }

      float pitch = in.pitch();
      if (pitch < -1.0F) {
         float bleedFactor = Mth.clamp(-pitch / 90.0F, 0.0F, 1.0F);
         nextThrottle -= bleedFactor * in.climbBleed();
      } else if (pitch > 1.0F) {
         float gainFactor = Mth.clamp(pitch / 90.0F, 0.0F, 1.0F);
         nextThrottle += gainFactor * in.diveGain();
      }

      if (in.activelyRolling() && pitch <= 1.0F) {
         nextThrottle -= cfg.rollBleed();
      }

      float diveOverdriveCap = in.diveOverdriveCap();
      float forwardOverdriveCap = in.forwardOverdriveCap();
      float maxOverdriveCap = Math.max(diveOverdriveCap, forwardOverdriveCap);
      boolean isFlyingLevel = Math.abs(pitch) < cfg.overdrivePitchStart();
      boolean isBoosting = boost;
      boolean isHighThrottle = currentThrottleValue >= cfg.overdriveGate();
      boolean isLevelOverdriveActive = isFlyingLevel && isHighThrottle && isBoosting;
      float situationalMaxThrottle;
      if (pitch > cfg.overdrivePitchStart()) {
         float overdriveFactor = Mth.clamp((pitch - cfg.overdrivePitchStart()) / (cfg.overdrivePitchFull() - cfg.overdrivePitchStart()), 0.0F, 1.0F);
         situationalMaxThrottle = 1.0F + (diveOverdriveCap - 1.0F) * overdriveFactor;
      } else if (isLevelOverdriveActive) {
         situationalMaxThrottle = forwardOverdriveCap;
      } else {
         situationalMaxThrottle = 1.0F;
      }

      if (nextThrottle > situationalMaxThrottle && nextThrottle > currentThrottleValue) {
         nextThrottle = Math.max(situationalMaxThrottle, currentThrottleValue);
      }

      if (nextThrottle > situationalMaxThrottle) {
         nextThrottle = Math.max(situationalMaxThrottle, nextThrottle - in.overdriveBleed());
      }

      boolean isForwardActive = isAcceleratingInput || isBoosting;
      boolean isBrakingActive = isDeceleratingInput || brake;
      if (!isForwardActive && !isBrakingActive && nextThrottle > 0.01F) {
         nextThrottle = Math.max(0.0F, nextThrottle - in.glideBleed());
      }

      nextThrottle = Mth.clamp(nextThrottle, 0.0F, maxOverdriveCap);
      return new ThrottleComponent.ThrottleResult(nextThrottle, isAcceleratingInput);
   }

   public void matchSpeed(double speed) {
      float top = (float)this.body.getAttributeValue(Attributes.FLYING_SPEED);
      if (!(top <= 0.0F)) {
         float next = Mth.clamp((float)speed / top, 0.0F, this.body.diveCap());
         if (next != this.body.getThrottleLevel()) {
            this.body.setThrottleLevel(next);
            if (this.body.level().isClientSide() && this.body.isControlledByLocalInstance()) {
               FaunaNetwork.sendToServer(new ControlMessageThrottleSync(next, this.body.isAccelerating(), this.body.isDecelerating()));
            }
         }
      }
   }

   public void tickGroundedReset() {
      if (this.body.supportsThreeDMedium()) {
         if (this.body.isVehicle()
            || !this.body.isFlying()
            || this.body.faunaMind() == null
            || !this.body.faunaMind().route().hasRoute() && !this.body.faunaMind().route().solving()) {
            if ((!this.body.inThreeDMedium() || this.body.isFlying() && this.body.isInWater() && !this.body.skimsWater())
               && (this.body.getThrottleLevel() > 0.01F || this.body.isAccelerating() || this.body.isDecelerating())
               && !this.body.getLocomotionOrchestrator().isTransitionActive()) {
               this.body.setThrottleLevel(0.0F);
               this.body.setAccelerating(false);
               this.body.setDecelerating(false);
               RiderInputComponent riderInput = this.body.componentRegistry.get(RiderInputComponent.class);
               if (riderInput != null) {
                  riderInput.setCruiseRequested(false);
               }

               if (this.body.level().isClientSide() && this.body.isControlledByLocalInstance()) {
                  FaunaNetwork.sendToServer(new ControlMessageThrottleSync(0.0F, false, false));
               }
            }
         }
      }
   }

   @Override
   public void serverTick() {
   }

   @Override
   public void save(CompoundTag tag) {
   }

   @Override
   public void load(CompoundTag tag) {
   }

   public static final class RampState {
      int poweredTicks = 0;
      int boostTicks = 0;
      boolean cruiseAccelerating = false;
      int cruiseHoldTicks = 0;
   }

   public record ThrottleInputs(
      float currentThrottleValue,
      float accelStat,
      boolean staminaDepleted,
      boolean climbing,
      float pitch,
      boolean activelyRolling,
      float diveOverdriveCap,
      float forwardOverdriveCap,
      float overdriveBleed,
      float glideBleed,
      float climbBleed,
      float diveGain,
      float decelScale,
      PhysicsSheet.Throttle cfg
   ) {
   }

   public record ThrottleResult(float nextThrottle, boolean acceleratingInput) {
   }
}
