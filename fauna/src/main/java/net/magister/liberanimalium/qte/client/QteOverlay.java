package net.magister.liberanimalium.qte.client;

import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;
import net.magister.liberanimalium.qte.QteRenderState;
import net.magister.liberanimalium.qte.QteType;
import net.magister.liberanimalium.ride.client.RideKeys;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.util.Mth;

public final class QteOverlay {
   private static final Map<QteType, QteSkin> SKINS = Collections.synchronizedMap(new EnumMap<>(QteType.class));
   private static final String[] ARROWS = new String[]{"←", "→", "↑", "↓"};
   private static final float ARROW_SCALE = 2.5F;
   private static final float SKEW_LETTER_SCALE = 3.0F;
   private static final int SKEW_PIPS = 5;

   private QteOverlay() {
   }

   public static void skin(QteType type, QteSkin skin) {
      SKINS.put(type, skin);
   }

   public static void render(GuiGraphics g, int entityId, int screenWidth, int screenHeight) {
      QteRenderState state = ClientQteState.get(entityId);
      if (state != null) {
         QteSkin skin = SKINS.get(state.type());
         if (skin != null) {
            skin.render(g, state, screenWidth, screenHeight);
         } else {
            switch (state.type()) {
               case TAME_CIRCLE:
                  tameCircle(g, state, screenWidth, screenHeight);
                  break;
               case ARROW_SEQUENCE:
                  arrowSequence(g, state, screenWidth, screenHeight);
            }
         }
      }
   }

   private static void arrowSequence(GuiGraphics g, QteRenderState s, int sw, int sh) {
      int length = (int)s.get(0);
      int pressed = (int)s.get(1);
      boolean mistake = s.get(2) < 10.0F;
      Font font = Minecraft.getInstance().font;
      int cell = (int)(font.width("↑") * 2.5F) + 6;
      int x = (sw - cell * length) / 2;
      int y = sh / 2 + 40;
      g.pose().pushPose();

      for (int i = 0; i < length; i++) {
         int dir = Math.max(0, Math.min(ARROWS.length - 1, (int)s.get(3 + i)));
         int colour = mistake ? -44976 : (i < pressed ? -11468944 : (i == pressed ? -1 : -8355712));
         g.pose().pushPose();
         g.pose().translate(x + i * cell, y, 0.0F);
         g.pose().scale(2.5F, 2.5F, 1.0F);
         g.drawString(font, ARROWS[dir], 0, 0, colour, true);
         g.pose().popPose();
      }

      g.pose().popPose();
   }

   private static void tameCircle(GuiGraphics g, QteRenderState s, int sw, int sh) {
      float iconX = s.get(0);
      float iconY = s.get(1);
      float tame = s.get(2);
      float soul = s.get(3);
      int skewLetter = Math.round(s.get(5));
      float skewProgress = s.get(6);
      int cx = sw / 2;
      int cy = sh / 2 - 8;
      int radius = 40;
      int arenaSize = TameCircleAtlas.ARENA.w();
      tcBlit(g, TameCircleAtlas.ARENA, cx - arenaSize / 2, cy - arenaSize / 2, arenaSize, arenaSize);
      int ix = cx + Math.round(Mth.clamp(iconX, -1.0F, 1.0F) * radius);
      int iy = cy - Math.round(Mth.clamp(iconY, -1.0F, 1.0F) * radius);
      int iconSize = TameCircleAtlas.ICON.w();
      tcBlit(g, TameCircleAtlas.ICON, ix - iconSize / 2, iy - iconSize / 2, iconSize, iconSize);
      int barH = radius * 2;
      int barY = cy - radius;
      tcMeter(g, cx - radius - 22, barY, barH, soul, TameCircleAtlas.FILL_SOUL);
      tcMeter(g, cx + radius + 6, barY, barH, tame, TameCircleAtlas.FILL_TAME);
      if (skewLetter >= 0) {
         String letter = null;
         Minecraft mc = Minecraft.getInstance();
         if (mc.player != null && mc.player.getVehicle() != null) {
            RideKeys.Bound bound = RideKeys.of(mc.player.getVehicle());
            if (skewLetter < bound.struggleLetters().length && bound.struggleLetters()[skewLetter] != null) {
               letter = bound.struggleLetters()[skewLetter].getTranslatedKeyMessage().getString();
            }
         }

         if (letter == null && skewLetter < "FGHJKL".length()) {
            letter = String.valueOf("FGHJKL".charAt(skewLetter));
         }

         if (letter != null) {
            Font font = Minecraft.getInstance().font;
            long time = Minecraft.getInstance().level != null ? Minecraft.getInstance().level.getGameTime() : 0L;
            int colour = time / 4L % 2L == 0L ? -8090 : -1;
            g.pose().pushPose();
            g.pose().translate(cx, cy, 0.0F);
            g.pose().scale(3.0F, 3.0F, 1.0F);
            g.drawCenteredString(font, letter, 0, -9 / 2, colour);
            g.pose().popPose();
         }

         int pips = Math.max(1, 5);
         int filled = Math.round(skewProgress * pips);
         int pipW = 6;
         int pipGap = 3;
         int rowW = pips * pipW + (pips - 1) * pipGap;
         int px = cx - rowW / 2;
         int py = cy + radius + 6;

         for (int i = 0; i < pips; i++) {
            int x0 = px + i * (pipW + pipGap);
            g.fill(x0, py, x0 + pipW, py + 4, i < filled ? -8090 : -2130706433);
         }
      }
   }

   private static void tcBlit(GuiGraphics g, QteAtlas.Region r, int x, int y, int w, int h) {
      g.blit(TameCircleAtlas.SHEET, x, y, w, h, r.u(), r.v(), r.w(), r.h(), 256, 128);
   }

   private static void tcMeter(GuiGraphics g, int x, int y, int h, float fill, QteAtlas.Region fillRegion) {
      tcBlit(g, TameCircleAtlas.BAR_FRAME, x, y - 2, TameCircleAtlas.BAR_FRAME.w(), TameCircleAtlas.BAR_FRAME.h());
      int fillH = Math.round(Math.min(h, fillRegion.h()) * Mth.clamp(fill, 0.0F, 1.0F));
      if (fillH > 0) {
         g.blit(
            TameCircleAtlas.SHEET,
            x + 2,
            y + (h - fillH),
            fillRegion.w(),
            fillH,
            fillRegion.u(),
            fillRegion.v() + fillRegion.h() - fillH,
            fillRegion.w(),
            fillH,
            256,
            128
         );
      }
   }
}
