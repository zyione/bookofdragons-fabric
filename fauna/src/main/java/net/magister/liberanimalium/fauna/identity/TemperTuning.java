package net.magister.liberanimalium.fauna.identity;

import net.minecraft.util.Mth;

public final class TemperTuning {
   public static final float EXTREME_EXPONENT = 0.6F;
   public static final float MODERATE_EXPONENT = 1.6666666F;
   public static final float CORRELATION = 0.6F;
   public static final float PACE_SPREAD = 0.15F;
   public static final long SALT_BRAVERY = 3030900749L;
   public static final long SALT_ENERGY = 3840310289L;
   public static final long SALT_SYMPATHY = 1394055697L;
   public static final long SALT_AGGRESSION = 2791564625L;
   public static final long SALT_DOMINANCE = 3492813335L;
   public static final long SALT_NERVOUSNESS = 1588004178L;

   private TemperTuning() {
   }

   public static float paceMultiplier(float energy) {
      return Mth.lerp(Mth.clamp(energy, 0.0F, 1.0F), 0.85F, 1.15F);
   }
}
