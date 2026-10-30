package net.magister.bookofdragons.client.gui;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;

public final class CardRenderer {
   private CardRenderer() {
   }

   public static void drawCard(GuiGraphics g, int x, int y, int w, int h, int fillColor, int borderColor) {
      g.fill(x + 1, y, x + w - 1, y + h, fillColor);
      g.fill(x, y + 1, x + w, y + h - 1, fillColor);
      g.fill(x + 1, y, x + w - 1, y + 1, borderColor);
      g.fill(x + 1, y + h - 1, x + w - 1, y + h, borderColor);
      g.fill(x, y + 1, x + 1, y + h - 1, borderColor);
      g.fill(x + w - 1, y + 1, x + w, y + h - 1, borderColor);
   }

   public static void drawDropShadow(GuiGraphics g, int x, int y, int w, int h) {
      g.fill(x + 3, y + 3, x + w + 3, y + h + 3, Integer.MIN_VALUE);
   }

   public static void drawGlow(GuiGraphics g, int x, int y, int w, int h, int colorRgb) {
      for (int i = 1; i <= 4; i++) {
         int alpha = 64 - i * 8;
         if (alpha < 0) {
            alpha = 0;
         }

         int color = alpha << 24 | colorRgb & 16777215;
         int e = i * 3;
         g.fill(x - e, y - e, x + w + e, y + h + e, color);
      }
   }

   public static void drawCenteredText(GuiGraphics g, Font font, String text, int x, int y, int w, int color, boolean shadow) {
      int textW = font.width(text);
      g.drawString(font, text, x + (w - textW) / 2, y, color, shadow);
   }
}
