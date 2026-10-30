package net.magister.liberanimalium.body.locomotion;

import net.magister.liberanimalium.body.physics.PhysicsSheet;
import net.magister.liberanimalium.chassis.FaunaBase;
import net.magister.liberanimalium.chassis.component.FaunaFlightRotationComponent;
import net.magister.liberanimalium.chassis.component.RollComponent;
import net.magister.liberanimalium.network.ControlMessageRoll;
import net.magister.liberanimalium.network.FaunaNetwork;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

final class Attitude2D {
   private final FaunaBase body;
   private float spinYaw2D;
   private float spinPitch2D;
   private boolean ballistic2DActive;
   private float bank2D;
   private int roll2DSync;
   private static final double VANE_MIN_HORIZONTAL = 0.05;

   Attitude2D(FaunaBase body) {
      this.body = body;
   }

   void rider(LivingEntity rider, float forwardInput, float strafeInput, boolean folded, boolean stalled, float pitchTarget, float extraRoll) {
      FaunaFlightRotationComponent dfrc = this.body.componentRegistry.get(FaunaFlightRotationComponent.class);
      if (dfrc == null || !dfrc.isRolling()) {
         if (folded) {
            this.ballistic(true);
            this.stepRoll(dfrc, 0.0F, extraRoll);
         } else {
            if (!stalled) {
               this.ballistic2DActive = false;
            }

            if (!this.body.isFreeLooking()) {
               float cameraYaw = rider.getYRot();
               float currentYaw = this.body.getYRot();
               boolean isCruising = this.body.getThrottleLevel() > 0.01F;
               boolean hasAnyInput = Math.abs(forwardInput) > 0.01F || Math.abs(strafeInput) > 0.01F || isCruising;
               if (forwardInput > 0.0F) {
                  float maxTurnSpeed = FlightEngine.maxTurnSpeed(this.body, this.body.getDeltaMovement().length());
                  float yawDiff = Mth.wrapDegrees(cameraYaw - currentYaw);
                  float yawChange = Mth.clamp(yawDiff, -maxTurnSpeed, maxTurnSpeed);
                  this.body.setYRot(currentYaw + yawChange);
                  this.body.inDeadzone = true;
                  this.body.deadzoneDelayTimer = 0;
               } else if (hasAnyInput) {
                  float maxTurnSpeed = FlightEngine.maxTurnSpeed(this.body, this.body.getDeltaMovement().length());
                  float yawDiff = Mth.wrapDegrees(cameraYaw - currentYaw);
                  float yawChange = Mth.clamp(yawDiff, -maxTurnSpeed, maxTurnSpeed);
                  this.body.setYRot(currentYaw + yawChange);
                  this.body.inDeadzone = true;
                  this.body.deadzoneDelayTimer = 0;
               } else {
                  float yawDifference = Mth.wrapDegrees(cameraYaw - currentYaw);
                  float absDifference = Math.abs(yawDifference);
                  boolean isAirborne = !this.body.onGround();
                  PhysicsSheet.Rider2d r2 = PhysicsSheet.of(this.body).rider2d();
                  float deadzoneAngle = isAirborne ? r2.deadzoneAir() : r2.deadzoneGround();
                  int deadzoneDelay = isAirborne ? r2.deadzoneDelayAir() : r2.deadzoneDelayGround();
                  if (absDifference > deadzoneAngle) {
                     if (this.body.inDeadzone) {
                        this.body.deadzoneDelayTimer = deadzoneDelay;
                        this.body.inDeadzone = false;
                     } else if (this.body.deadzoneDelayTimer > 0) {
                        this.body.deadzoneDelayTimer--;
                     }
                  } else if (!this.body.inDeadzone) {
                     this.body.inDeadzone = true;
                     this.body.deadzoneDelayTimer = 0;
                  }

                  if (!this.body.inDeadzone && this.body.deadzoneDelayTimer == 0) {
                     float maxTurnSpeed = FlightEngine.maxTurnSpeed(this.body, this.body.getDeltaMovement().length());
                     float yawChange = Mth.clamp(yawDifference, -maxTurnSpeed, maxTurnSpeed);
                     this.body.setYRot(currentYaw + yawChange);
                  }
               }

               if (this.body.isFlying()) {
                  FlightEngine.chasePitch(this.body, pitchTarget);
               } else {
                  this.body.setXRot(pitchTarget);
               }

               if (stalled) {
                  this.ballistic(false);
               }

               this.stepRoll(dfrc, Mth.wrapDegrees(cameraYaw - this.body.getYRot()), extraRoll);
            } else {
               PhysicsSheet.Attitude look = PhysicsSheet.of(this.body).attitude();
               float currentPitch = this.body.getXRot();
               if (Math.abs(currentPitch) < look.freeLookSnap()) {
                  this.body.setXRot(0.0F);
               } else {
                  this.body.setXRot(Mth.rotLerp(look.freeLookLevel(), currentPitch, 0.0F));
               }

               RollComponent rolls = this.body.componentRegistry.get(RollComponent.class);
               if (dfrc == null || rolls == null || !rolls.spinning() && !rolls.dodging()) {
                  if (dfrc != null) {
                     float currentRoll = dfrc.getRoll();
                     dfrc.stepRoll(Math.abs(currentRoll) < look.freeLookSnap() ? 0.0F : Mth.lerp(look.freeLookLevel(), currentRoll, 0.0F));
                     this.bank2D = 0.0F;
                     this.sendRoll(dfrc);
                  }
               } else {
                  this.stepRoll(dfrc, 0.0F, 0.0F);
               }
            }

            this.body.setYRot(this.body.getYRot());
            this.body.setXRot(this.body.getXRot());
            this.body.yBodyRot = this.body.getYRot();
            this.body.yHeadRot = this.body.yBodyRot;
         }
      }
   }

   private void ballistic(boolean carrySpin) {
      PhysicsSheet.Flight flight = PhysicsSheet.of(this.body).flight();
      if (!this.ballistic2DActive) {
         this.spinYaw2D = Mth.wrapDegrees(this.body.getYRot() - this.body.yRotO);
         this.spinPitch2D = this.body.getXRot() - this.body.xRotO;
         this.ballistic2DActive = true;
      }

      float yaw = this.body.getYRot();
      float pitch = this.body.getXRot();
      if (carrySpin) {
         yaw += this.spinYaw2D;
         pitch += this.spinPitch2D;
         this.spinYaw2D = this.spinYaw2D * flight.spinDrag();
         this.spinPitch2D = this.spinPitch2D * flight.spinDrag();
      }

      Vec3 v = this.body.getDeltaMovement();
      double speed = v.length();
      if (speed > 0.001) {
         float k = (float)Math.min(1.0, flight.vane() * Math.min(1.0, speed / flight.vaneSpeed()));
         double horizontal = Math.sqrt(v.x * v.x + v.z * v.z);
         float fallPitch = (float)(-Math.toDegrees(Math.atan2(v.y, horizontal)));
         pitch += (fallPitch - pitch) * k;
         if (horizontal > 0.05) {
            float fallYaw = (float)Math.toDegrees(Math.atan2(-v.x, v.z));
            yaw += Mth.wrapDegrees(fallYaw - yaw) * k;
         }
      }

      this.body.setYRot(yaw);
      this.body.setXRot(Mth.clamp(pitch, -90.0F, 90.0F));
      this.body.yBodyRot = yaw;
      this.body.yHeadRot = yaw;
   }

   private void stepRoll(FaunaFlightRotationComponent dfrc, float yawError, float extraRoll) {
      if (dfrc != null) {
         PhysicsSheet.Rider2d r2 = PhysicsSheet.of(this.body).rider2d();
         float target = -Mth.clamp(yawError / r2.bankSaturation(), -1.0F, 1.0F) * r2.bankMax();
         this.bank2D = this.bank2D + (target - this.bank2D) * r2.bankEase();
         RollComponent rolls = this.body.componentRegistry.get(RollComponent.class);
         float dodge = 0.0F;
         if (rolls != null && rolls.spinning()) {
            rolls.spinStep(0.05F, dfrc.dodgeRollGain());
            dodge = rolls.spinDegrees();
         } else if (rolls != null && rolls.dodging()) {
            dodge = rolls.dodgeSide() * PhysicsSheet.of(this.body).flight().strafeLean() * rolls.dodgeSpeedShare();
         }

         dfrc.stepRoll(this.bank2D + extraRoll + dodge);
         this.sendRoll(dfrc);
      }
   }

   private void sendRoll(FaunaFlightRotationComponent dfrc) {
      if (this.body.level().isClientSide() && this.body.isControlledByLocalInstance()) {
         if (++this.roll2DSync >= 3) {
            this.roll2DSync = 0;
            FaunaNetwork.sendToServer(new ControlMessageRoll(false, dfrc.getRoll()));
         }
      }
   }
}
