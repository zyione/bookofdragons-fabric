package net.magister.bookofdragons.client.gui.book.data;

import net.magister.bookofdragons.entity.data.DragonType;
import net.magister.liberanimalium.body.gameplay.SpeciesGameplayProfile;
import net.magister.liberanimalium.body.gameplay.SpeciesGameplayRegistry;
import net.minecraft.resources.ResourceLocation;

public enum FoodIcon {
   SALMON("overlayfavfoodsalmon", 52, 50),
   CHICKEN("overlayfavfoodchicken", 52, 50),
   COD("overlayfavfoodcod", 52, 50),
   MUTTON("overlayfavfoodmutton", 52, 50),
   BEEF("overlayfavfoodbeef", 52, 50),
   GENERIC_MEAT("overlaygenericfoodmeat", 62, 62),
   GENERIC_FISH("overlaygenericfoodfish", 62, 62),
   GENERIC("overlayfavfood", 52, 50);

   private final String textureName;
   private final int width;
   private final int height;

   FoodIcon(String textureName, int width, int height) {
      this.textureName = textureName;
      this.width = width;
      this.height = height;
   }

   public ResourceLocation getTexture() {
      return new ResourceLocation("bookofdragons", "textures/gui/book/" + this.textureName + ".png");
   }

   public int getWidth() {
      return this.width;
   }

   public int getHeight() {
      return this.height;
   }

   public static FoodIcon generalFoodForDragon(DragonType type) {
      SpeciesGameplayProfile profile = SpeciesGameplayRegistry.getProfile(type.getSerializedName());
      if (profile == null) {
         return GENERIC;
      }

      for (ResourceLocation food : profile.generalFoods()) {
         String path = food.getPath();
         if (!path.contains("cod") && !path.contains("salmon") && !path.contains("fish")) {
            if (!path.contains("beef") && !path.contains("pork") && !path.contains("mutton") && !path.contains("chicken") && !path.contains("meat")) {
               continue;
            }

            return GENERIC_MEAT;
         }

         return GENERIC_FISH;
      }

      for (ResourceLocation food : profile.favoriteFoods()) {
         String path = food.getPath();
         if (path.contains("cod") || path.contains("salmon") || path.contains("fish")) {
            return GENERIC_FISH;
         }
      }

      return GENERIC_MEAT;
   }

   public static FoodIcon forDragon(DragonType type) {
      SpeciesGameplayProfile profile = SpeciesGameplayRegistry.getProfile(type.getSerializedName());
      if (profile == null) {
         return GENERIC;
      }

      for (ResourceLocation food : profile.favoriteFoods()) {
         FoodIcon icon = fromItemId(food.toString());
         if (icon != GENERIC) {
            return icon;
         }
      }

      for (ResourceLocation food : profile.generalFoods()) {
         String path = food.getPath();
         if (!path.contains("cod") && !path.contains("salmon") && !path.contains("fish")) {
            if (!path.contains("beef") && !path.contains("pork") && !path.contains("mutton") && !path.contains("chicken") && !path.contains("meat")) {
               continue;
            }

            return GENERIC_MEAT;
         }

         return GENERIC_FISH;
      }

      return GENERIC;
   }

   private static FoodIcon fromItemId(String itemId) {
      return switch (itemId) {
         case "minecraft:salmon", "minecraft:cooked_salmon" -> SALMON;
         case "minecraft:chicken", "minecraft:cooked_chicken" -> CHICKEN;
         case "minecraft:cod", "minecraft:cooked_cod" -> COD;
         case "minecraft:mutton", "minecraft:cooked_mutton" -> MUTTON;
         case "minecraft:beef", "minecraft:cooked_beef" -> BEEF;
         case "minecraft:porkchop", "minecraft:cooked_porkchop" -> GENERIC_MEAT;
         default -> itemId.contains("fish") || itemId.contains("salmon") || itemId.contains("cod")
            ? GENERIC_FISH
            : (itemId.contains("beef") ? BEEF : (!itemId.contains("pork") && !itemId.contains("mutton") && !itemId.contains("meat") ? GENERIC : GENERIC_MEAT));
      };
   }
}
