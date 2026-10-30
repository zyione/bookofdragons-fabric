package net.magister.bookofdragons.client.gui.book.spread;

import com.mojang.blaze3d.platform.NativeImage;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Optional;
import net.magister.bookofdragons.client.gui.book.BookLayoutEditor;
import net.magister.bookofdragons.client.gui.book.BookText;
import net.magister.bookofdragons.client.gui.book.data.RiderManual;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.item.ItemStack;

public class RiderManualSpread extends EditableBookSpread {
   private static final BookLayoutEditor EDITOR = new BookLayoutEditor();
   private static final int INK = 4144959;
   private static final int KEY_INK = 10035230;
   private static final int ICON = 12;
   private static final ResourceLocation DRAGON = new ResourceLocation("bookofdragons", "textures/gui/book/dragon.png");
   private static boolean[][] dragonMask;
   private static final int GAP = 2;
   private static final int ROW_GAP = 3;
   private static final int MIN_TEXT = 48;
   private final List<RiderManual.Page> pages;
   private final int first;

   public RiderManualSpread(List<RiderManual.Page> pages, int first) {
      super(EDITOR);
      this.pages = pages;
      this.first = first;
   }

   @Override
   public void render(GuiGraphics graphics, Font font, int leftX, int topY, int mouseX, int mouseY, float partialTick) {
      this.page(graphics, font, false, this.first, leftX, topY);
      this.page(graphics, font, true, this.first + 1, leftX, topY);
      this.renderEditorOverlay(graphics, font, leftX, topY, mouseX, mouseY);
   }

   private void page(GuiGraphics graphics, Font font, boolean right, int index, int leftX, int topY) {
      if (index < this.pages.size()) {
         RiderManual.Page page = this.pages.get(index);
         this.drawTitle(graphics, font, right, Component.translatable(page.title()), true, leftX, topY);
         String slot = right ? "RP_TEXT" : "LP_TEXT";
         int x = this.sx(slot, leftX);
         int y = this.sy(slot, topY);
         int width = this.ew(slot, 156);
         int bottom = y + this.eh(slot, 190);

         for (RiderManual.Entry entry : page.entries()) {
            int used = this.row(graphics, font, entry, x, y, width, bottom);
            if (used <= 0) {
               break;
            }

            y += used + 3;
         }
      }
   }

   private int row(GuiGraphics graphics, Font font, RiderManual.Entry entry, int x, int y, int width, int bottom) {
      int pitch = BookText.line(font);
      int tall = entry.dragon() ? Math.max(12, dragonMask().length) : 12;
      if (y + tall > bottom) {
         return 0;
      }

      int iy = y + (tall - 12) / 2;
      int cx = x;
      if (!entry.held().isEmpty()) {
         cx = item(graphics, entry.held(), cx, iy) + 2;
      }

      if (entry.mouse()) {
         cx = mouse(graphics, -1, cx, iy) + 2;
      }

      for (int i = 0; i < entry.keys().size(); i++) {
         if (i > 0 && !entry.join().equals(" ")) {
            cx = word(graphics, font, Component.literal(entry.join()), cx, iy) + 2;
         }

         RiderManual.Bound bound = RiderManual.bound(entry.keys().get(i));
         cx = (bound.mouseButton() >= 0 ? mouse(graphics, bound.mouseButton(), cx, iy) : cap(graphics, font, bound.label(), cx, iy)) + 2;
      }

      Component mark = RiderManual.pressMark(entry.press());
      if (mark != null) {
         cx = word(graphics, font, mark, cx, iy) + 2;
      }

      if (!entry.at().isEmpty() || entry.dragon()) {
         cx = word(graphics, font, Component.translatable("book.bookofdragons.manual.on"), cx, iy) + 2;
         if (entry.dragon()) {
            cx = dragon(graphics, cx, y + (tall - dragonMask().length) / 2) + 2;
         } else {
            cx = item(graphics, entry.at(), cx, iy) + 2;
         }
      }

      boolean pictured = cx > x;
      int textX = pictured ? cx + 2 : x;
      if (x + width - textX < 48) {
         textX = x;
      }

      int textY = pictured && textX == x ? y + tall + 1 : y + (tall - pitch) / 2 + 1;
      List<FormattedCharSequence> lines = font.split(BookText.of(Component.translatable(entry.text())), x + width - textX);
      int end = Math.max(pictured ? y + tall : y, textY + lines.size() * pitch);
      if (end > bottom) {
         return 0;
      }

      for (int i = 0; i < lines.size(); i++) {
         graphics.drawString(font, lines.get(i), textX, textY + i * pitch, 4144959, false);
      }

      return end - y;
   }

   private static int item(GuiGraphics graphics, ItemStack stack, int x, int y) {
      float scale = 0.75F;
      graphics.pose().pushPose();
      graphics.pose().translate(x, y, 0.0F);
      graphics.pose().scale(scale, scale, 1.0F);
      graphics.renderItem(stack, 0, 0);
      graphics.pose().popPose();
      return x + 12;
   }

   private static void ink(GuiGraphics graphics, int x, int y, int rgb) {
      int pressure = 180 + ((x * 73856093 ^ y * 19349663) >>> 3 & 75);
      graphics.fill(x, y, x + 1, y + 1, Math.min(pressure, 255) << 24 | rgb);
   }

   private static void across(GuiGraphics graphics, int x0, int x1, int y, int rgb) {
      for (int x = x0; x < x1; x++) {
         ink(graphics, x, y, rgb);
      }
   }

   private static void down(GuiGraphics graphics, int x, int y0, int y1, int rgb) {
      for (int y = y0; y < y1; y++) {
         ink(graphics, x, y, rgb);
      }
   }

   private static void box(GuiGraphics graphics, int x, int y, int w, int h) {
      across(graphics, x + 1, x + w - 1, y, 10035230);
      across(graphics, x + 1, x + w - 1, y + h - 1, 10035230);
      down(graphics, x, y + 1, y + h - 1, 10035230);
      down(graphics, x + w - 1, y + 1, y + h - 1, 10035230);
   }

   private static int cap(GuiGraphics graphics, Font font, Component label, int x, int y) {
      Component text = BookText.of(label);
      int w = font.width(text) + 4;
      int h = 11;
      box(graphics, x, y, w, h);
      graphics.drawString(font, text, x + 2, y + (h - BookText.line(font)) / 2 + 1, 10035230, false);
      return x + w;
   }

   private static int mouse(GuiGraphics graphics, int button, int x, int y) {
      int w = 7;
      int h = 11;
      box(graphics, x, y, w, h);
      across(graphics, x + 1, x + w - 1, y + 4, 10035230);
      down(graphics, x + w / 2, y + 1, y + 4, 10035230);
      if (button == 0) {
         for (int r = y + 1; r < y + 4; r++) {
            across(graphics, x + 1, x + w / 2, r, 10035230);
         }
      }

      if (button == 1) {
         for (int r = y + 1; r < y + 4; r++) {
            across(graphics, x + w / 2 + 1, x + w - 1, r, 10035230);
         }
      }

      if (button == 2) {
         down(graphics, x + w / 2, y + 1, y + 4, 4144959);
      }

      return x + w;
   }

   private static int dragon(GuiGraphics graphics, int x, int y) {
      boolean[][] mask = dragonMask();

      for (int r = 0; r < mask.length; r++) {
         for (int c = 0; c < mask[r].length; c++) {
            if (mask[r][c]) {
               ink(graphics, x + c, y + r, 10035230);
            }
         }
      }

      return x + (mask.length == 0 ? 0 : mask[0].length);
   }

   private static boolean[][] dragonMask() {
      if (dragonMask != null) {
         return dragonMask;
      }

      boolean[][] mask = new boolean[0][0];
      Optional<Resource> resource = Minecraft.getInstance().getResourceManager().getResource(DRAGON);
      if (resource.isPresent()) {
         try (InputStream in = resource.get().open()) {
            NativeImage image = NativeImage.read(in);

            try {
               mask = new boolean[image.getHeight()][image.getWidth()];

               for (int r = 0; r < image.getHeight(); r++) {
                  for (int c = 0; c < image.getWidth(); c++) {
                     mask[r][c] = image.getPixelRGBA(c, r) >>> 24 > 127;
                  }
               }
            } catch (Throwable var8) {
               if (image != null) {
                  try {
                     image.close();
                  } catch (Throwable var7) {
                     var8.addSuppressed(var7);
                  }
               }

               throw var8;
            }

            if (image != null) {
               image.close();
            }
         } catch (IOException unreadable) {
            mask = new boolean[0][0];
         }
      }

      dragonMask = mask;
      return mask;
   }

   private static int word(GuiGraphics graphics, Font font, Component text, int x, int y) {
      Component shown = BookText.of(text);
      graphics.drawString(font, shown, x, y + (12 - BookText.line(font)) / 2 + 1, 10035230, false);
      return x + font.width(shown);
   }

   static {
      EDITOR.addText("LP_TEXT", "Manual (left)", 6710886, false, 24, 30, 156, 190, 1.0F);
      EDITOR.addText("RP_TEXT", "Manual (right)", 6710886, true, 2, 30, 156, 190, 1.0F);
      stored("rider_manual", EDITOR);
   }
}
