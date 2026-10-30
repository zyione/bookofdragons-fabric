package net.magister.bookofdragons.client.gui.book;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import net.magister.bookofdragons.client.gui.book.data.SpeciesFacts;
import net.magister.bookofdragons.entity.base.dragon.DragonClass;
import net.magister.bookofdragons.entity.data.DragonType;
import net.magister.liberanimalium.body.gameplay.SpeciesGameplayRegistry;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

public final class ClassProgress {
   public static final List<DragonClass> ORDER = List.of(
      DragonClass.STOKER, DragonClass.BOULDER, DragonClass.SHARP, DragonClass.TRACKER, DragonClass.TIDAL, DragonClass.MYSTERY, DragonClass.STRIKE
   );
   private static final ResourceLocation ICONS = new ResourceLocation("bookofdragons", "textures/gui/book/class.png");
   private static final int ICON = 128;
   public static final int DYES = 6;

   private ClassProgress() {
   }

   public static DragonClass classOf(DragonType type) {
      return DragonClass.ofSpecies(type.getSerializedName());
   }

   public static List<DragonType> species(DragonClass dragonClass) {
      List<DragonType> out = new ArrayList<>();

      for (DragonType type : DragonType.values()) {
         if (SpeciesGameplayRegistry.getProfile(type.getSerializedName()) != null && classOf(type) == dragonClass) {
            out.add(type);
         }
      }

      return out;
   }

   public static List<DragonClass> present() {
      List<DragonClass> out = new ArrayList<>();

      for (DragonClass c : ORDER) {
         if (!species(c).isEmpty()) {
            out.add(c);
         }
      }

      return out;
   }

   public static float completion(DragonClass dragonClass) {
      List<DragonType> species = species(dragonClass);
      if (species.isEmpty()) {
         return 0.0F;
      }

      float known = 0.0F;

      for (DragonType type : species) {
         known += SpeciesFacts.progress(type);
      }

      return known / species.size();
   }

   public static int dye(float completion) {
      return completion >= 0.999F ? 5 : Math.min(4, (int)(completion * 5.0F));
   }

   public static int percent(float completion) {
      return Math.round(completion * 100.0F);
   }

   public static Component name(DragonClass dragonClass) {
      String id = dragonClass.name().toLowerCase(Locale.ROOT);
      return Component.translatableWithFallback("book.bookofdragons.class." + id, Character.toUpperCase(id.charAt(0)) + id.substring(1));
   }

   public static void drawIcon(GuiGraphics graphics, DragonClass dragonClass, int x, int y, int size) {
      int index = ORDER.indexOf(dragonClass);
      if (index >= 0) {
         graphics.blit(ICONS, x, y, size, size, index * 128, 0.0F, 128, 128, 128 * ORDER.size(), 128);
      }
   }
}
