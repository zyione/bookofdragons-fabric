package net.magister.liberanimalium.chassis.component.simulation;

import net.minecraft.util.Mth;

public final class Vitals {
   public static final int STARVING = 10;
   public static final float SLEEP_MULTIPLE = 2.0F;
   public static final float MEAL_HEAL_SHARE = 0.5F;
   public static final float HP_PER_NUTRITION = 1.0F;

   private Vitals() {
   }

   public static float healingFactor(int foodLevel, boolean asleep) {
      float fed = (foodLevel - 10) / 90.0F;
      float factor = Mth.clamp(fed, 0.0F, 1.0F);
      return asleep ? factor * 2.0F : factor;
   }

   public static float nutritionSpentHealing(int nutrition, float health, float maxHealth) {
      float missing = Math.max(0.0F, maxHealth - health);
      if (!(missing <= 0.0F) && nutrition > 0) {
         float affordable = nutrition * 0.5F;
         return Math.min(affordable, missing / 1.0F);
      } else {
         return 0.0F;
      }
   }

   public static float healthFromNutrition(float nutritionSpent) {
      return Math.max(0.0F, nutritionSpent) * 1.0F;
   }
}
