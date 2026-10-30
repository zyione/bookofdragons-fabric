package net.magister.bookofdragons.client.gui.qte;

import com.mojang.blaze3d.systems.RenderSystem;
import net.magister.bookofdragons.client.KeyBindings;
import net.magister.bookofdragons.client.gui.book.BookLayoutEditor;
import net.magister.bookofdragons.client.gui.book.BookLayoutStore;
import net.magister.liberanimalium.qte.QteRenderState;
import net.magister.liberanimalium.qte.client.QteSkin;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

public final class BuckingQteSkin implements QteSkin {
   public static final ResourceLocation SHEET = new ResourceLocation("bookofdragons", "textures/gui/bucking_qte.png");
   private static final int SHEET_W = 148;
   private static final int SHEET_H = 146;
   public static final int FRAME_W = 137;
   public static final int FRAME_H = 146;
   private static final int CURSOR = 11;
   public static final int AREA_W = 384;
   public static final int AREA_H = 256;
   static final int EMPTY = -14412528;
   static final int[] TAME = new int[]{-2598340, -5230550, -8774118};
   static final int[] SOUL = new int[]{-667552, -2054098, -6526444};
   private static final float SHAKE_REST = 0.75F;
   private static final float SHAKE_STRAIN = 2.25F;
   private static final long SHAKE_STEP_MS = 40L;
   static final int PIP_ON = -8090;
   static final int PIP_OFF = -2130706433;
   public static final BookLayoutEditor EDITOR = new BookLayoutEditor();

   public void render(GuiGraphics g, QteRenderState state, int screenWidth, int screenHeight) {
      draw(g, state, areaLeft(screenWidth), areaTop(screenHeight));
   }

   public static int areaLeft(int screenWidth) {
      return Math.round(screenWidth * 2.0F / 3.0F) - 192;
   }

   public static int areaTop(int screenHeight) {
      return (screenHeight - 256) / 2;
   }

   public static void draw(GuiGraphics g, QteRenderState s, int leftX, int topY) {
      float iconX = s.get(0);
      float iconY = s.get(1);
      float tame = s.get(2);
      float soul = s.get(3);
      int skewLetter = Math.round(s.get(5));
      float skewProgress = s.get(6);
      RenderSystem.enableBlend();
      bar(g, "BAR_TOP", tame, TAME, leftX, topY);
      bar(g, "BAR_BOTTOM", soul, SOUL, leftX, topY);
      int fx = leftX + EDITOR.getX("FRAME");
      int fy = topY + EDITOR.getY("FRAME");
      g.blit(SHEET, fx, fy, 0.0F, 0.0F, 137, 146, 148, 146);
      int ax = leftX + EDITOR.getX("ARENA");
      int ay = topY + EDITOR.getY("ARENA");
      int aw = EDITOR.getW("ARENA");
      int ah = EDITOR.getH("ARENA");
      float radius = Math.min(aw, ah) / 2.0F;
      float strain = skewLetter >= 0 ? 1.0F : Mth.clamp((float)Math.hypot(iconX, iconY), 0.0F, 1.0F);
      float shake = 0.75F + 2.25F * strain;
      int roll = Mth.murmurHash3Mixer((int)(System.currentTimeMillis() / 40L));
      int cx = ax + aw / 2 + Math.round(Mth.clamp(iconX, -1.0F, 1.0F) * radius + shake * ((roll & 65535) / 32767.5F - 1.0F));
      int cy = ay + ah / 2 - Math.round(Mth.clamp(iconY, -1.0F, 1.0F) * radius + shake * ((roll >>> 16) / 32767.5F - 1.0F));
      g.blit(SHEET, cx - 5, cy - 5, 137.0F, 135.0F, 11, 11, 148, 146);
      RenderSystem.disableBlend();
      if (skewLetter >= 0) {
         String letterText = null;
         if (skewLetter < KeyBindings.TAMING_KEYS.length && KeyBindings.TAMING_KEYS[skewLetter] != null) {
            letterText = KeyBindings.TAMING_KEYS[skewLetter].getTranslatedKeyMessage().getString();
         } else if (skewLetter < "FGHJKL".length()) {
            letterText = String.valueOf("FGHJKL".charAt(skewLetter));
         }

         if (letterText != null) {
            letter(g, letterText, leftX, topY);
            pips(g, skewProgress, leftX, topY);
         }
      }
   }

   private static void bar(GuiGraphics g, String slot, float fill, int[] shades, int leftX, int topY) {
      int x = leftX + EDITOR.getX(slot);
      int y = topY + EDITOR.getY(slot);
      int w = EDITOR.getW(slot);
      int h = EDITOR.getH(slot);
      g.fill(x, y, x + w, y + h, -14412528);
      int filled = Math.round(w * Mth.clamp(fill, 0.0F, 1.0F));
      if (filled > 0) {
         boolean fromLeft = EDITOR.getY(slot) + h / 2 < EDITOR.getY("FRAME") + 73;
         int x0 = fromLeft ? x : x + w - filled;
         int x1 = x0 + filled;
         g.fill(x0, y, x1, y + h, shades[1]);
         g.fill(x0, y, x1, y + 1, shades[0]);
         g.fill(x0, y + h - 1, x1, y + h, shades[2]);
      }
   }

   private static void letter(GuiGraphics g, String letter, int leftX, int topY) {
      Font font = Minecraft.getInstance().font;
      int x = leftX + EDITOR.getX("LETTER");
      int y = topY + EDITOR.getY("LETTER");
      int w = EDITOR.getW("LETTER");
      int h = EDITOR.getH("LETTER");
      float scale = Math.max(1.0F, (float)Math.floor(h / 9.0F));
      long time = Minecraft.getInstance().level != null ? Minecraft.getInstance().level.getGameTime() : System.currentTimeMillis() / 50L;
      int colour = time / 4L % 2L == 0L ? -8090 : -1;
      g.pose().pushPose();
      g.pose().translate(x + w / 2.0F, y + (h - 9.0F * scale) / 2.0F + scale, 0.0F);
      g.pose().scale(scale, scale, 1.0F);
      g.drawString(font, letter, -font.width(letter) / 2, 0, colour, true);
      g.pose().popPose();
   }

   private static void pips(GuiGraphics g, float progress, int leftX, int topY) {
      int count = Math.max(1, 5);
      int x = leftX + EDITOR.getX("PIPS");
      int y = topY + EDITOR.getY("PIPS");
      int w = EDITOR.getW("PIPS");
      int h = EDITOR.getH("PIPS");
      int gap = 2;
      int pip = Math.max(1, (w - gap * (count - 1)) / count);
      int lit = Math.round(Mth.clamp(progress, 0.0F, 1.0F) * count);

      for (int i = 0; i < count; i++) {
         int x0 = x + i * (pip + gap);
         g.fill(x0, y, x0 + pip, y + h, i < lit ? -8090 : -2130706433);
      }
   }

   static {
      EDITOR.add("FRAME", "Bucking frame (whole game)", 1090497024, 16755200, false, true, 123, 55, 137, 146);
      EDITOR.addChild("BAR_TOP", "FRAME", "Tame bar (fill area)", 13382451, true, 21, 21, 70, 11);
      EDITOR.addChild("BAR_BOTTOM", "FRAME", "Soul bar (fill area)", 14725168, true, 23, 128, 68, 12);
      EDITOR.addChild("ARENA", "FRAME", "Arena (cursor range)", 3385907, true, 20, 43, 72, 72);
      EDITOR.addChild("LETTER", "FRAME", "Skew letter", 3381759, true, 104, 61, 32, 22);
      EDITOR.addChild("PIPS", "FRAME", "Skew presses", 10053375, true, 106, 84, 28, 4);
      BookLayoutStore.attach("bucking", EDITOR);
   }
}
