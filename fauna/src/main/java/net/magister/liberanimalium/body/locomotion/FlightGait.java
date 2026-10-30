package net.magister.liberanimalium.body.locomotion;

import net.minecraft.world.phys.Vec3;

public final class FlightGait {
   public static final double MIN_NOSE_THROTTLE = 0.1;
   public static final float NOSE_UNAVAILABLE = Float.NaN;
   public static final float MAX_PITCH_DEGREES = 65.0F;
   public static final float DIVE_ATTITUDE_DEGREES = 25.0F;
   public static final float PATH_TELL_FOLLOW = 0.33333334F;

   private FlightGait() {
   }

   public static float noseFor(double verticalDemand, double throttle) {
      if (throttle < 0.1) {
         return Float.NaN;
      }

      double slope = verticalDemand / throttle;
      return (float)(-Math.toDegrees(Math.atan(slope)));
   }

   public static boolean hasNose(float nose) {
      return !Float.isNaN(nose);
   }

   public static boolean divingAt(float calculatedPitch) {
      return calculatedPitch >= 25.0F;
   }

   public static float pathPitch(double verticalVelocity, double horizontalSpeed, double hoverThreshold) {
      double horizontal = Math.max(0.0, horizontalSpeed);
      if (horizontal < 1.0E-4 && Math.abs(verticalVelocity) < 1.0E-4) {
         return 0.0F;
      }

      double path = Math.toDegrees(Math.atan2(-verticalVelocity, horizontal));
      double speed = Math.sqrt(horizontal * horizontal + verticalVelocity * verticalVelocity);
      double authority = Math.min(1.0, speed / Math.max(1.0E-4, hoverThreshold));
      return (float)(path * authority);
   }

   public static Vec3 followVelocity(Vec3 smoothed, Vec3 now) {
      return smoothed == null ? now : smoothed.add(now.subtract(smoothed).scale(0.33333334F));
   }
}
