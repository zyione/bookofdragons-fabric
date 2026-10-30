package net.magister.bookofdragons.client.gui.book;

import com.mojang.blaze3d.systems.RenderSystem;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;

public class BookRenderHelper {
   public static final int COLOR_TEXT_DARK = 4144959;
   public static final int COLOR_TITLE = 2039583;
   private static final ResourceLocation BOOK_BG_TEXTURE = new ResourceLocation("bookofdragons", "textures/gui/book/book.png");
   private static final ResourceLocation UNDERLINE_TEXTURE = new ResourceLocation("bookofdragons", "textures/gui/book/underline.png");
   public static final int UNDERLINE_W = 159;
   public static final int UNDERLINE_H = 11;
   private static final ResourceLocation INDEX_ARROW_TEXTURE = new ResourceLocation("bookofdragons", "textures/gui/book/indexarrow.png");
   public static final int INDEX_ARROW_SIZE = 20;
   private static final ResourceLocation PAGETURN_SMALL_LEFT = new ResourceLocation("bookofdragons", "textures/gui/book/pageturnsmallleft.png");
   private static final ResourceLocation PAGETURN_SMALL_RIGHT = new ResourceLocation(
      "bookofdragons", "textures/gui/book/pageturnsmallright.png"
   );
   private static final ResourceLocation PAGETURN_LARGE_LEFT = new ResourceLocation("bookofdragons", "textures/gui/book/pageturnlargeleft.png");
   private static final ResourceLocation PAGETURN_LARGE_RIGHT = new ResourceLocation(
      "bookofdragons", "textures/gui/book/pageturnlargeright.png"
   );
   public static final int PAGETURN_LARGE_W = 29;
   public static final int PAGETURN_LARGE_H = 28;
   public static final ResourceLocation PANEL_TEXTURE = new ResourceLocation("bookofdragons", "textures/gui/book/tooltipbacking.png");
   public static final int PANEL_SIZE = 72;
   private static final int SHADOW = 3;
   private static final float SHADOW_ALPHA = 0.35F;
   private static final int PANEL_PAD = 6;

   public static void drawCenteredText(GuiGraphics graphics, Font font, Component text, int centerX, int y, int color) {
      Component title = BookText.title(text);
      graphics.drawString(font, title, centerX - font.width(title) / 2, y, color, false);
   }

   public static boolean isMouseOver(double mouseX, double mouseY, int x, int y, int width, int height) {
      return mouseX >= x && mouseX < x + width && mouseY >= y && mouseY < y + height;
   }

   public static void renderBookBackground(GuiGraphics graphics, int leftX, int topY) {
      graphics.blit(BOOK_BG_TEXTURE, leftX, topY, 0.0F, 0.0F, 384, 256, 384, 256);
   }

   public static void renderOverlayTexture(GuiGraphics graphics, ResourceLocation texture, int x, int y, int w, int h) {
      graphics.blit(texture, x, y, 0.0F, 0.0F, w, h, w, h);
   }

   public static void renderUnderline(GuiGraphics graphics, int x, int y) {
      graphics.blit(UNDERLINE_TEXTURE, x, y, 0.0F, 0.0F, 159, 11, 159, 11);
   }

   public static void renderPanel(GuiGraphics graphics, int x, int y, int w, int h) {
      renderPanel(graphics, PANEL_TEXTURE, 72, x, y, w, h);
   }

   public static void drawPanel(GuiGraphics graphics, Font font, List<Component> lines, int x, int y, int maxWidth, int color) {
      List<FormattedCharSequence> wrapped = new ArrayList<>();

      for (int i = 0; i < lines.size(); i++) {
         Component line = i == 0 ? BookText.title(lines.get(i)) : BookText.of(lines.get(i));
         wrapped.addAll(font.split(line, maxWidth));
      }

      if (!wrapped.isEmpty()) {
         int pitch = BookText.line(font);
         int textW = 0;

         for (FormattedCharSequence line : wrapped) {
            textW = Math.max(textW, font.width(line));
         }

         int w = textW + 12;
         int h = wrapped.size() * pitch + 12 - 2;
         int screenW = Minecraft.getInstance().getWindow().getGuiScaledWidth();
         int screenH = Minecraft.getInstance().getWindow().getGuiScaledHeight();
         int px = Math.max(2, Math.min(x + 10, screenW - w - 2));
         int py = Math.max(2, Math.min(y - 6, screenH - h - 2));
         graphics.pose().pushPose();
         graphics.pose().translate(0.0F, 0.0F, 400.0F);
         RenderSystem.enableBlend();
         RenderSystem.defaultBlendFunc();
         RenderSystem.setShaderColor(0.0F, 0.0F, 0.0F, 0.35F);
         renderPanel(graphics, px + 3, py + 3, w, h);
         RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
         renderPanel(graphics, px, py, w, h);
         RenderSystem.disableBlend();

         for (int i = 0; i < wrapped.size(); i++) {
            graphics.drawString(font, wrapped.get(i), px + 6, py + 6 + i * pitch, color, false);
         }

         graphics.pose().popPose();
      }
   }

   public static void renderPanel(GuiGraphics graphics, ResourceLocation texture, int size, int x, int y, int w, int h) {
      int third = size / 3;
      int left = Math.min(third, w / 2);
      int right = Math.min(third, w - left);
      int top = Math.min(third, h / 2);
      int bottom = Math.min(third, h - top);
      int innerW = w - left - right;
      int innerH = h - top - bottom;
      graphics.blit(texture, x, y, 0.0F, 0.0F, left, top, size, size);
      graphics.blit(texture, x + w - right, y, size - right, 0.0F, right, top, size, size);
      graphics.blit(texture, x, y + h - bottom, 0.0F, size - bottom, left, bottom, size, size);
      graphics.blit(texture, x + w - right, y + h - bottom, size - right, size - bottom, right, bottom, size, size);

      for (int dx = 0; dx < innerW; dx += third) {
         int span = Math.min(third, innerW - dx);
         graphics.blit(texture, x + left + dx, y, third, 0.0F, span, top, size, size);
         graphics.blit(texture, x + left + dx, y + h - bottom, third, size - bottom, span, bottom, size, size);
      }

      for (int dy = 0; dy < innerH; dy += third) {
         int span = Math.min(third, innerH - dy);
         graphics.blit(texture, x, y + top + dy, 0.0F, third, left, span, size, size);
         graphics.blit(texture, x + w - right, y + top + dy, size - right, third, right, span, size, size);

         for (int dx = 0; dx < innerW; dx += third) {
            graphics.blit(texture, x + left + dx, y + top + dy, third, third, Math.min(third, innerW - dx), span, size, size);
         }
      }
   }

   public static void renderUnderline(GuiGraphics graphics, int x, int y, int w, int h) {
      renderOverlayTexture(graphics, UNDERLINE_TEXTURE, x, y, w, h);
   }

   public static void renderIndexArrow(GuiGraphics graphics, int x, int y) {
      graphics.blit(INDEX_ARROW_TEXTURE, x, y, 0.0F, 0.0F, 20, 20, 20, 20);
   }

   public static void renderPageTurnLarge(GuiGraphics graphics, int x, int y, boolean isLeft, boolean hovered) {
      ResourceLocation tex = hovered ? (isLeft ? PAGETURN_LARGE_LEFT : PAGETURN_LARGE_RIGHT) : (isLeft ? PAGETURN_SMALL_LEFT : PAGETURN_SMALL_RIGHT);
      graphics.blit(tex, x, y, 0.0F, 0.0F, 29, 28, 29, 28);
   }

   public static void renderScaledTexture(GuiGraphics graphics, ResourceLocation texture, int x, int y, int renderW, int renderH, int texW, int texH) {
      if (texW != 0 && texH != 0 && renderW != 0 && renderH != 0) {
         graphics.pose().pushPose();
         graphics.pose().translate(x, y, 0.0F);
         graphics.pose().scale((float)renderW / texW, (float)renderH / texH, 1.0F);
         graphics.blit(texture, 0, 0, 0.0F, 0.0F, texW, texH, texW, texH);
         graphics.pose().popPose();
      }
   }
}
