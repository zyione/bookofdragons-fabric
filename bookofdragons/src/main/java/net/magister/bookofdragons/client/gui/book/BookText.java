package net.magister.bookofdragons.client.gui.book;

import net.magister.bookofdragons.config.ModClientConfig;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.Font;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.ResourceLocation;

public final class BookText {
   private static final BookText.Hand SLANTED = new BookText.Hand("monogram_italic", "Monogram Italic", 9);
   private static final BookText.Hand UPRIGHT = new BookText.Hand("monogram", "Monogram", 9);
   private static boolean gameFont;

   private BookText() {
   }

   public static void cycle() {
      gameFont = !gameFont;
   }

   private static BookText.Hand current() {
      if (gameFont) {
         return null;
      } else {
         return ModClientConfig.isReadableBookFont() ? UPRIGHT : SLANTED;
      }
   }

   public static Component name() {
      BookText.Hand h = current();
      return Component.literal(h == null ? "Minecraft" : h.name());
   }

   public static Style style() {
      BookText.Hand h = current();
      return h == null ? Style.EMPTY : Style.EMPTY.withFont(h.font());
   }

   public static int line(Font font) {
      BookText.Hand h = current();
      return h == null ? 9 : h.line();
   }

   public static MutableComponent of(Component text) {
      BookText.Hand h = current();
      MutableComponent out = text.copy();
      return h == null ? out : out.withStyle(s -> s.withFont(h.font()));
   }

   public static MutableComponent literal(String text) {
      return of(Component.literal(text));
   }

   public static MutableComponent title(Component text) {
      return of(text).withStyle(ChatFormatting.BOLD);
   }

   private record Hand(String id, String name, int line) {
      ResourceLocation font() {
         return new ResourceLocation("bookofdragons", "hand_" + this.id);
      }
   }
}
