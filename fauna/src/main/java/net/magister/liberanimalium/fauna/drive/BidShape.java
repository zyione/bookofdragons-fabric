package net.magister.liberanimalium.fauna.drive;

import net.minecraft.util.Mth;

public final class BidShape {
   private BidShape() {
   }

   public static float felt(float need, float threshold, float sharpness) {
      return curve(need, threshold, sharpness, 0.7F);
   }

   public static float felt(float need, float threshold) {
      return felt(need, threshold, 4.0F);
   }

   public static float feltUrgent(float need, float threshold) {
      return curve(need, threshold, 4.0F, 1.0F);
   }

   public static float feltUnder(float need, float threshold, float ceiling) {
      return curve(need, threshold, 4.0F, ceiling);
   }

   private static float curve(float need, float threshold, float sharpness, float ceiling) {
      float x = Mth.clamp(need, 0.0F, 1.0F);
      if (x <= 0.0F) {
         return 0.0F;
      }

      float t = Math.max(1.0E-4F, threshold);
      double xn = Math.pow(x, sharpness);
      double tn = Math.pow(t, sharpness);
      return (float)(ceiling * xn / (xn + tn));
   }

   public static float gate(float cue, float need) {
      float desperation = (float)Math.pow(Mth.clamp(need, 0.0F, 1.0F), 2.0);
      return Mth.clamp(Math.max(Mth.clamp(cue, 0.0F, 1.0F), desperation), 0.0F, 1.0F);
   }

   public static float threshold(float traitT, float atKeen, float atReluctant) {
      return Mth.lerp(Mth.clamp(traitT, 0.0F, 1.0F), atReluctant, atKeen);
   }

   public static float beyond(double gap, double quiet, double range) {
      double horizon = Math.max(1.0, range);
      double zone = Mth.clamp(quiet, 0.0, horizon);
      return (float)Mth.clamp((gap - zone) / Math.max(1.0, horizon - zone), 0.0, 1.0);
   }
}
