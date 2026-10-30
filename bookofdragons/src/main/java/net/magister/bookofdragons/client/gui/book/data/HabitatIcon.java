package net.magister.bookofdragons.client.gui.book.data;

import net.magister.bookofdragons.entity.data.DragonType;
import net.minecraft.resources.ResourceLocation;

public enum HabitatIcon {
   MOUNTAIN("overlayhabitatmountain"),
   FOREST("overlayhabitatforest"),
   PLAINS("overlayhabitatplains"),
   SWAMP("overlayhabitatswamp"),
   BARRENS("overlayhabitatbarrens"),
   COASTAL("overlayhabitatcoastal"),
   UNKNOWN("overlayhabitat");

   public static final int SIZE = 62;
   private final String textureName;

   HabitatIcon(String textureName) {
      this.textureName = textureName;
   }

   public ResourceLocation getTexture() {
      return new ResourceLocation("bookofdragons", "textures/gui/book/" + this.textureName + ".png");
   }

   public static HabitatIcon forDragon(DragonType type) {
      return switch (SpeciesFacts.habitatKind(type)) {
         case "cold", "high" -> MOUNTAIN;
         case "wood", "lush" -> FOREST;
         case "wild" -> PLAINS;
         case "wet", "river" -> SWAMP;
         case "dry", "cave" -> BARRENS;
         case "sea", "shore" -> COASTAL;
         default -> UNKNOWN;
      };
   }
}
