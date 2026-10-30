package net.magister.liberanimalium.fauna.know;

import net.minecraft.util.Mth;

public final class MannerTuning {
   public static final float PRESENT = 0.15F;
   public static final float DISPLAY = 0.85F;
   public static final float CROUCH_DISCOUNT = 0.35F;
   public static final float ARMOUR_WEIGHT = 0.3F;
   public static final float SOFT_MAX_SHARE = 0.4F;
   public static final float REFERENCE_SPEED = 0.36F;
   public static final int PRESS_HALF_LIFE_TICKS = 80;
   public static final double PRESS_CHARGE_PER_TICK = 0.00625;
   public static final float REACH_LOUD = 2.0F;
   public static final float GAIN_PRESENT = 1.0F;
   public static final float GAIN_LOUD = 1.8F;
   public static final double PRESS_RISE_PER_TICK = 1.0 - Math.pow(0.5, 0.025);

   private MannerTuning() {
   }

   public static double pressRetentionPerTick(float nervousness) {
      double halfLife = 80.0 * Mth.lerp(Mth.clamp(nervousness, 0.0F, 1.0F), 0.6, 1.4);
      return Math.pow(0.5, 1.0 / halfLife);
   }
}
