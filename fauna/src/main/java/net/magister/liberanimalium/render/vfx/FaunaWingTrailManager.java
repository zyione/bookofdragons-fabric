package net.magister.liberanimalium.render.vfx;

import net.magister.liberanimalium.chassis.FaunaBase;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.phys.Vec3;

public class FaunaWingTrailManager {
   private final FaunaBase body;
   private final FaunaWingTrail leftWingTrail;
   private final FaunaWingTrail rightWingTrail;
   private static final int TRAIL_LENGTH = 30;
   private static final float THROTTLE_THRESHOLD = 0.9F;
   private static final float MAX_OPACITY_THROTTLE = 1.3F;
   private boolean isTrailActive = false;
   private float currentAlpha = 0.0F;
   private int lastTickRecorded = -1;

   public FaunaWingTrailManager(FaunaBase body) {
      this.body = body;
      this.leftWingTrail = new FaunaWingTrail(30);
      this.rightWingTrail = new FaunaWingTrail(30);
   }

   public void tick() {
      boolean isFlying = !this.body.onGround();
      boolean validFlightState = isFlying && !this.body.isAccelerating() && !this.body.isBoosting() && !this.body.getStateContext().isDiveBreaking();
      float targetAlpha = 0.0F;
      if (validFlightState) {
         if (this.body.isVehicle()) {
            float throttle = this.body.getThrottleLevel();
            if (throttle >= 0.9F) {
               targetAlpha = Mth.clamp((throttle - 0.9F) / 0.39999998F, 0.1F, 1.0F);
            }
         } else {
            double speed = this.body.getDeltaMovement().length();
            float baseSpeed = (float)this.body.getAttributeValue(Attributes.FLYING_SPEED);
            double speedThreshold = baseSpeed * 0.9F;
            double maxSpeedThreshold = baseSpeed * 1.3F;
            if (speed >= speedThreshold && baseSpeed > 0.0F) {
               targetAlpha = Mth.clamp((float)((speed - speedThreshold) / (maxSpeedThreshold - speedThreshold)), 0.1F, 1.0F);
            }
         }
      }

      this.currentAlpha = Mth.lerp(0.15F, this.currentAlpha, targetAlpha);
      this.isTrailActive = this.currentAlpha > 0.01F;
      if (!this.isTrailActive) {
         this.decay();
      }
   }

   public void updateWingTips(Vec3 leftTip, Vec3 rightTip) {
      if (this.isTrailActive) {
         if (this.body.tickCount != this.lastTickRecorded) {
            this.leftWingTrail.add(leftTip, this.currentAlpha);
            this.rightWingTrail.add(rightTip, this.currentAlpha);
            this.lastTickRecorded = this.body.tickCount;
         }
      }
   }

   private void decay() {
      this.leftWingTrail.decay();
      this.rightWingTrail.decay();
   }

   public FaunaWingTrail getLeftWingTrail() {
      return this.leftWingTrail;
   }

   public FaunaWingTrail getRightWingTrail() {
      return this.rightWingTrail;
   }
}
