package net.magister.liberanimalium.fauna.behavior;

import java.util.UUID;
import net.magister.liberanimalium.fauna.core.Seeds;
import net.minecraft.util.Mth;

public final class Duel {
   private Duel() {
   }

   public static float encounterJitter(UUID a, UUID b, long encounterTick) {
      float unit = Seeds.unitPair(a, b, encounterTick);
      return (float)((unit * 2.0 - 1.0) * 0.05F);
   }

   public static float orientedJitter(UUID self, UUID other, long encounterTick) {
      float magnitude = encounterJitter(self, other, encounterTick);
      return self.compareTo(other) <= 0 ? magnitude : -magnitude;
   }

   public static boolean backsDown(float domGap) {
      return domGap <= -0.35F;
   }

   public static float yieldHpFloor(float bravery, float domDeficit) {
      float base = Mth.lerp(Mth.clamp(bravery, 0.0F, 1.0F), 0.65F, 0.5F);
      return base + 0.1F * Mth.clamp(domDeficit, 0.0F, 1.0F);
   }

   public static boolean yields(float healthFraction, float bravery, float domDeficit) {
      return healthFraction <= yieldHpFloor(bravery, domDeficit);
   }
}
