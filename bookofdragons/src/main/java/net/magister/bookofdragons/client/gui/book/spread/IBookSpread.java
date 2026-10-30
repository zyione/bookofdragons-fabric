package net.magister.bookofdragons.client.gui.book.spread;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;

public interface IBookSpread {
   void render(GuiGraphics var1, Font var2, int var3, int var4, int var5, int var6, float var7);

   default boolean mouseClicked(double mouseX, double mouseY, int button, int leftX, int topY) {
      return false;
   }

   default boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY, int leftX, int topY) {
      return false;
   }

   default void mouseReleased(double mouseX, double mouseY, int button, int leftX, int topY) {
   }

   default boolean keyPressed(int keyCode, int scanCode, int modifiers, int leftX, int topY) {
      return false;
   }

   default boolean charTyped(char character, int modifiers) {
      return false;
   }

   default boolean mouseScrolled(double mouseX, double mouseY, double amount, int leftX, int topY) {
      return false;
   }

   default void left() {
   }
}
