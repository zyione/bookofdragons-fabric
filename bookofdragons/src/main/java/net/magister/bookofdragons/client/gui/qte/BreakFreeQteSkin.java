package net.magister.bookofdragons.client.gui.qte;

import net.magister.liberanimalium.qte.QteRenderState;
import net.magister.liberanimalium.qte.client.QteSkin;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.util.Mth;

public final class BreakFreeQteSkin implements QteSkin {
   private static final String[] ARROWS = new String[]{"←", "→", "↑", "↓"};
   private static final float SCALE = 2.0F;
   private static final int PAD = 3;
   private static final int GAP = 2;
   private static final float SHAKE = 1.5F;
   private static final long SHAKE_STEP_MS = 40L;

   public void render(GuiGraphics g, QteRenderState s, int screenWidth, int screenHeight) {
      int length = Math.max(0, (int)s.get(0));
      int pressed = (int)s.get(1);
      boolean mistake = s.get(2) < 10.0F;
      Font font = Minecraft.getInstance().font;
      int cell = Math.round(font.width("↑") * 2.0F) + 6;
      int height = Math.round(9.0F * 2.0F) + 6;
      int x0 = (screenWidth - (length * cell + Math.max(0, length - 1) * 2)) / 2;
      int y0 = screenHeight / 2 + 40;
      if (mistake) {
         int roll = Mth.murmurHash3Mixer((int)(System.currentTimeMillis() / 40L));
         x0 += Math.round(1.5F * ((roll & 65535) / 32767.5F - 1.0F));
         y0 += Math.round(1.5F * ((roll >>> 16) / 32767.5F - 1.0F));
      }

      long time = Minecraft.getInstance().level != null ? Minecraft.getInstance().level.getGameTime() : System.currentTimeMillis() / 50L;
      boolean pulse = time / 4L % 2L == 0L;

      for (int i = 0; i < length; i++) {
         int x = x0 + i * (cell + 2);
         int edge = mistake
            ? BuckingQteSkin.TAME[1]
            : (i < pressed ? BuckingQteSkin.SOUL[0] : (i == pressed ? BuckingQteSkin.SOUL[1] : BuckingQteSkin.SOUL[2]));
         int colour = mistake ? BuckingQteSkin.TAME[0] : (i < pressed ? BuckingQteSkin.SOUL[0] : (i == pressed ? (pulse ? -8090 : -1) : -2130706433));
         g.fill(x, y0, x + cell, y0 + height, edge);
         g.fill(x + 1, y0 + 1, x + cell - 1, y0 + height - 1, -14412528);
         String arrow = ARROWS[Mth.clamp((int)s.get(3 + i), 0, ARROWS.length - 1)];
         g.pose().pushPose();
         g.pose().translate(x + cell / 2.0F, y0 + 3 + 2.0F, 0.0F);
         g.pose().scale(2.0F, 2.0F, 1.0F);
         g.drawString(font, arrow, -font.width(arrow) / 2, 0, colour, true);
         g.pose().popPose();
      }
   }
}
