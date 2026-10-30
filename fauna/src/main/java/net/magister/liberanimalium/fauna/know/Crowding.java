package net.magister.liberanimalium.fauna.know;

import net.minecraft.util.Mth;

public final class Crowding {
   private Crowding() {
   }

   public static float space(double bodyWidth, float sympathy, float nervousness) {
      return (float)(Math.max(bodyWidth, 1.0) * Mth.lerp(guardedness(sympathy, nervousness), 3.0F, 8.0F));
   }

   public static float huddle(double bodyWidth, float sympathy, float nervousness) {
      return (float)(Math.max(bodyWidth, 1.0) * (1.0 + Mth.lerp(guardedness(sympathy, nervousness), 0.25F, 1.75F)));
   }

   public static float guardedness(float sympathy, float nervousness) {
      float unsociable = 1.0F - Mth.clamp(sympathy, 0.0F, 1.0F);
      return Mth.clamp(unsociable * 0.6F + Mth.clamp(nervousness, 0.0F, 1.0F) * 0.39999998F, 0.0F, 1.0F);
   }

   public static float within(double gap, float space) {
      return Territory.homeAdvantage(gap, space);
   }

   public static float press(double gap, float space, float manner, float find) {
      float depth = within(gap, space);
      return depth <= 0.0F ? 0.0F : Mth.clamp(depth * depth * Mth.clamp(manner, 0.0F, 1.0F) * Mth.clamp(find, 0.0F, 1.0F), 0.0F, 1.0F);
   }

   public static float trust(float like, float alarm) {
      return Mth.clamp(like, 0.0F, 1.0F) * (1.0F - Mth.clamp(alarm, 0.0F, 1.0F));
   }

   public static double solicitStandoff(float space, double sightRadius, float guardedness, float trust) {
      double room = space * 0.8F;
      double wary = sightRadius * 0.3 * Mth.clamp(guardedness, 0.0F, 1.0F);
      double open = Math.max(4.0, Math.max(room, wary));
      return Mth.lerp(Mth.clamp(trust, 0.0F, 1.0F), open, 4.0);
   }
}
