package net.magister.liberanimalium.compat;

import org.jetbrains.annotations.Nullable;
import net.magister.liberanimalium.compat.v20.FoodImpl;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.Item;

public final class Food {
   private Food() {
   }

   @Nullable
   public static FoodProperties of(Item item) {
      return FoodImpl.of(item);
   }

   public static int nutrition(FoodProperties food) {
      return FoodImpl.nutrition(food);
   }

   public static float saturation(FoodProperties food) {
      return FoodImpl.saturation(food);
   }
}
