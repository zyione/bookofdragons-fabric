package net.magister.liberanimalium.compat.v20;

import org.jetbrains.annotations.Nullable;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.Item;

public final class FoodImpl {
   private FoodImpl() {
   }

   @Nullable
   public static FoodProperties of(Item item) {
      return item.getFoodProperties();
   }

   public static int nutrition(FoodProperties food) {
      return food.getNutrition();
   }

   public static float saturation(FoodProperties food) {
      return food.getNutrition() * food.getSaturationModifier() * 2.0F;
   }
}
