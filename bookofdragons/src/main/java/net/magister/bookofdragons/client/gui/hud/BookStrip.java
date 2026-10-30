package net.magister.bookofdragons.client.gui.hud;

import com.mojang.blaze3d.systems.RenderSystem;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import net.magister.bookofdragons.client.gui.book.BookRenderHelper;
import net.magister.bookofdragons.client.gui.book.BookText;
import net.magister.liberanimalium.platform.event.ClientEvents;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.Mth;

public final class BookStrip {
   private static final int HOLD_MS = 7000;
   private static final int SLIDE_MS = 350;
   private static final int TOP = 6;
   private static final int PAD = 7;
   private static final int MAX_WIDTH = 236;
   private static final int MAX_LINES = 2;
   private static final int GAP = 3;
   private static final int SHADOW = 3;
   private static final float SHADOW_ALPHA = 0.35F;
   private static final Deque<Component> LINES = new ArrayDeque<>();
   private static long shownAt = Long.MIN_VALUE;

   private BookStrip() {
   }

   public static void clientInit() {
      ClientEvents.HUD_AFTER_HOTBAR.register(BookStrip::draw);
   }

   public static void say(Component line) {
      long now = Util.getMillis();
      boolean showing = shownAt != Long.MIN_VALUE && now - shownAt < 7350L;
      if (!showing) {
         LINES.clear();
         shownAt = now;
      } else if (now - shownAt > 350L) {
         shownAt = now - 350L;
      }

      if (LINES.isEmpty() || !LINES.peekLast().getString().equals(line.getString())) {
         if (LINES.size() >= 2) {
            LINES.removeFirst();
         }

         LINES.addLast(line);
      }
   }

   private static void draw(GuiGraphics graphics, float partialTick) {
      if (!LINES.isEmpty()) {
         Minecraft mc = Minecraft.getInstance();
         long age = Util.getMillis() - shownAt;
         if (age > 7700L) {
            LINES.clear();
         } else if (!mc.options.hideGui) {
            Font font = mc.font;
            int pitch = BookText.line(font);
            List<List<FormattedCharSequence>> blocks = new ArrayList<>();
            int textW = 0;
            int textH = 0;

            for (Component line : LINES) {
               List<FormattedCharSequence> wrapped = font.split(BookText.of(line), 236);

               for (FormattedCharSequence row : wrapped) {
                  textW = Math.max(textW, font.width(row));
               }

               textH += wrapped.size() * pitch + (blocks.isEmpty() ? 0 : 3);
               blocks.add(wrapped);
            }

            int w = textW + 14;
            int h = textH + 14 - 2;
            float shown = age < 350L ? (float)age / 350.0F : (age > 7350L ? 1.0F - (float)(age - 7000L - 350L) / 350.0F : 1.0F);
            shown = Mth.clamp(shown, 0.0F, 1.0F);
            float eased = 1.0F - (1.0F - shown) * (1.0F - shown);
            int x = (mc.getWindow().getGuiScaledWidth() - w) / 2;
            int y = Math.round(6.0F - (1.0F - eased) * (h + 6 + 3 + 2));
            graphics.pose().pushPose();
            graphics.pose().translate(0.0F, 0.0F, 200.0F);
            RenderSystem.enableBlend();
            RenderSystem.defaultBlendFunc();
            RenderSystem.setShaderColor(0.0F, 0.0F, 0.0F, 0.35F);
            BookRenderHelper.renderPanel(graphics, x + 3, y + 3, w, h);
            RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
            BookRenderHelper.renderPanel(graphics, x, y, w, h);
            RenderSystem.disableBlend();
            int ty = y + 7;

            for (int b = 0; b < blocks.size(); b++) {
               if (b > 0) {
                  ty += 3;
               }

               for (FormattedCharSequence row : blocks.get(b)) {
                  graphics.drawString(font, row, x + (w - font.width(row)) / 2, ty, 4144959, false);
                  ty += pitch;
               }
            }

            graphics.pose().popPose();
         }
      }
   }
}
