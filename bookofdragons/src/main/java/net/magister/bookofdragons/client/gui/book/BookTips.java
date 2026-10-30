package net.magister.bookofdragons.client.gui.book;

import java.util.List;
import java.util.Locale;
import net.minecraft.network.chat.Component;

public final class BookTips {
   private BookTips() {
   }

   public static List<Component> stat(String shortName) {
      String key = "book.bookofdragons.stat." + shortName.toLowerCase(Locale.ROOT);
      return List.of(Component.translatable(key), Component.translatable(key + ".tip"));
   }

   public static List<Component> trait(String trait) {
      String key = "book.bookofdragons.trait." + trait;
      return List.of(Component.translatable(key), Component.translatable(key + ".tip"));
   }
}
