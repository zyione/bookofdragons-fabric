package net.magister.liberanimalium.qte.client;

import net.magister.liberanimalium.qte.QteRenderState;
import net.minecraft.client.gui.GuiGraphics;

@FunctionalInterface
public interface QteSkin {
   void render(GuiGraphics var1, QteRenderState var2, int var3, int var4);
}
