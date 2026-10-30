package net.magister.liberanimalium.fauna.drive;

import net.magister.liberanimalium.fauna.know.Territory;
import net.minecraft.util.Mth;

public final class Nearness {
   private Nearness() {
   }

   public static float discount(double distance, double span, float floor) {
      float near = Territory.homeAdvantage(distance, (float)span);
      return Mth.lerp(near, floor, 1.0F);
   }
}
