package net.magister.liberanimalium.fauna.sense;

import net.magister.liberanimalium.fauna.api.Hurt;
import net.minecraft.util.Mth;

public final class TouchSense {
   private TouchSense() {
   }

   public static float severity(Hurt hurt, float maxHealth) {
      if (maxHealth <= 0.0F) {
         return 1.0F;
      }

      float fraction = hurt.amount() / maxHealth;
      return Mth.clamp(fraction / 0.25F, 0.0F, 1.0F);
   }
}
