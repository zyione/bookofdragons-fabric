package net.magister.bookofdragons.client.gui.book.spread;

import java.util.List;
import net.magister.bookofdragons.client.gui.book.BookLayoutEditor;
import net.magister.bookofdragons.client.gui.book.BookLayoutStore;
import net.magister.bookofdragons.client.gui.book.BookRenderHelper;
import net.magister.bookofdragons.client.gui.book.BookText;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.item.ItemStack;

public abstract class EditableBookSpread implements IBookSpread {
   public static final BookLayoutEditor TITLES = new BookLayoutEditor();
   protected final BookLayoutEditor layoutEditor;
   private boolean leftTitle;
   private boolean rightTitle;

   protected EditableBookSpread(BookLayoutEditor editor) {
      this.layoutEditor = editor;
   }

   protected static BookLayoutEditor stored(String name, BookLayoutEditor editor) {
      BookLayoutStore.attach(name, editor);
      return editor;
   }

   protected int getRpOffset() {
      return 203;
   }

   public BookLayoutEditor getLayoutEditor() {
      return this.layoutEditor;
   }

   protected boolean usesSlot(String name) {
      return true;
   }

   protected void renderEditorOverlay(GuiGraphics graphics, Font font, int leftX, int topY, int mouseX, int mouseY) {
      if (BookLayoutEditor.ENABLED) {
         TITLES.renderOverlay(graphics, font, leftX, topY, this.getRpOffset(), mouseX, mouseY, this::hasTitle);
         this.layoutEditor.renderOverlay(graphics, font, leftX, topY, this.getRpOffset(), mouseX, mouseY, this::usesSlot);
      }
   }

   private boolean hasTitle(String slot) {
      return slot.startsWith("RP_") ? this.rightTitle : this.leftTitle;
   }

   protected void drawTitle(GuiGraphics graphics, Font font, boolean right, Component text, boolean underline, int leftX, int topY) {
      if (right) {
         this.rightTitle = true;
      } else {
         this.leftTitle = true;
      }

      String side = right ? "RP_" : "LP_";
      int pageX = leftX + (right ? this.getRpOffset() : 0);
      String title = side + "TITLE";
      float scale = Math.max(0.9F, TITLES.getScale(title));
      Component shown = BookText.title(text);
      graphics.pose().pushPose();
      graphics.pose().translate(pageX + TITLES.getX(title) + TITLES.getW(title) / 2, topY + TITLES.getY(title), 0.0F);
      graphics.pose().scale(scale, scale, 1.0F);
      graphics.drawString(font, shown, -font.width(shown) / 2, 0, 2039583, false);
      graphics.pose().popPose();
      if (underline) {
         String line = side + "LINE";
         BookRenderHelper.renderUnderline(graphics, pageX + TITLES.getX(line), topY + TITLES.getY(line), TITLES.getW(line), TITLES.getH(line));
      }
   }

   protected static int titleY(boolean right) {
      return TITLES.getY(right ? "RP_TITLE" : "LP_TITLE");
   }

   protected int ex(String slot, int def) {
      return this.layoutEditor.get(slot) != null ? this.layoutEditor.getX(slot) : def;
   }

   protected int ey(String slot, int def) {
      return this.layoutEditor.get(slot) != null ? this.layoutEditor.getY(slot) : def;
   }

   protected int ew(String slot, int def) {
      return this.layoutEditor.get(slot) != null ? this.layoutEditor.getW(slot) : def;
   }

   protected int eh(String slot, int def) {
      return this.layoutEditor.get(slot) != null ? this.layoutEditor.getH(slot) : def;
   }

   protected float es(String slot) {
      return this.layoutEditor.getScale(slot);
   }

   protected float ts(String slot) {
      return Math.max(0.9F, this.es(slot));
   }

   protected int sx(String slot, int leftX) {
      BookLayoutEditor.Slot s = this.layoutEditor.get(slot);
      return leftX + (s != null && s.rightPage ? this.getRpOffset() : 0) + (s != null ? this.layoutEditor.getX(slot) : 0);
   }

   protected int sy(String slot, int topY) {
      return topY + this.layoutEditor.getY(slot);
   }

   protected int drawText(GuiGraphics graphics, Font font, String slot, List<Component> lines, int color, int leftX, int topY) {
      float scale = this.ts(slot);
      int x = this.sx(slot, leftX);
      int y = this.sy(slot, topY);
      int width = Math.max(8, Math.round(this.ew(slot, 100) / scale));
      int height = Math.round(this.eh(slot, 100) / scale);
      int pitch = BookText.line(font);
      graphics.pose().pushPose();
      graphics.pose().translate(x, y, 0.0F);
      graphics.pose().scale(scale, scale, 1.0F);
      int dy = 0;

      for (Component line : lines) {
         for (FormattedCharSequence part : font.split(BookText.of(line), width)) {
            if (dy + pitch > height) {
               break;
            }

            graphics.drawString(font, part, 0, dy, color, false);
            dy += pitch;
         }
      }

      graphics.pose().popPose();
      return Math.round(dy * scale);
   }

   protected void drawJournal(GuiGraphics graphics, Font font, String slot, List<Component> sentences, int color, int leftX, int topY) {
      float scale = this.ts(slot);
      int width = Math.max(8, Math.round(this.ew(slot, 100) / scale));
      int lines = Math.max(1, Math.round(this.eh(slot, 100) / scale) / BookText.line(font));
      Component shown = Component.empty();
      MutableComponent paragraph = BookText.of(Component.empty());

      for (int i = 0; i < sentences.size(); i++) {
         if (i > 0) {
            paragraph.append(" ");
         }

         paragraph.append(sentences.get(i));
         if (i > 0 && font.split(paragraph, width).size() > lines) {
            break;
         }

         shown = paragraph.copy();
      }

      this.drawText(graphics, font, slot, List.of(shown), color, leftX, topY);
   }

   protected void drawCentered(GuiGraphics graphics, Font font, String slot, Component text, int color, int leftX, int topY) {
      float scale = this.ts(slot);
      int cx = this.sx(slot, leftX) + this.ew(slot, 0) / 2;
      int y = this.sy(slot, topY);
      Component title = BookText.title(text);
      graphics.pose().pushPose();
      graphics.pose().translate(cx, y, 0.0F);
      graphics.pose().scale(scale, scale, 1.0F);
      graphics.drawString(font, title, -font.width(title) / 2, 0, color, false);
      graphics.pose().popPose();
   }

   protected void drawBacking(GuiGraphics graphics, int x, int y, int w, int h) {
      BookRenderHelper.renderPanel(graphics, x, y, w, h);
   }

   protected void drawPanel(GuiGraphics graphics, Font font, List<Component> lines, int x, int y, int maxWidth, int color) {
      BookRenderHelper.drawPanel(graphics, font, lines, x, y, maxWidth, color);
   }

   protected void drawItems(GuiGraphics graphics, String slot, List<ItemStack> items, int leftX, int topY) {
      float scale = this.es(slot);
      int x = this.sx(slot, leftX);
      int y = this.sy(slot, topY);
      int per = Math.max(1, Math.round(this.ew(slot, 16) / (17.0F * scale)));
      graphics.pose().pushPose();
      graphics.pose().translate(x, y, 0.0F);
      graphics.pose().scale(scale, scale, 1.0F);

      for (int i = 0; i < items.size(); i++) {
         graphics.renderItem(items.get(i), i % per * 17, i / per * 17);
      }

      graphics.pose().popPose();
   }

   @Override
   public final boolean mouseClicked(double mouseX, double mouseY, int button, int leftX, int topY) {
      return !BookLayoutEditor.ENABLED
            || !this.layoutEditor.mouseClicked(mouseX, mouseY, button, leftX, topY, this.getRpOffset(), this::usesSlot)
               && !TITLES.mouseClicked(mouseX, mouseY, button, leftX, topY, this.getRpOffset(), this::hasTitle)
         ? this.handleMouseClicked(mouseX, mouseY, button, leftX, topY)
         : true;
   }

   protected boolean handleMouseClicked(double mouseX, double mouseY, int button, int leftX, int topY) {
      return false;
   }

   @Override
   public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY, int leftX, int topY) {
      return BookLayoutEditor.ENABLED
         && (
            this.layoutEditor.mouseDragged(mouseX, mouseY, leftX, topY, this.getRpOffset())
               || TITLES.mouseDragged(mouseX, mouseY, leftX, topY, this.getRpOffset())
         );
   }

   @Override
   public void mouseReleased(double mouseX, double mouseY, int button, int leftX, int topY) {
      if (BookLayoutEditor.ENABLED) {
         this.layoutEditor.mouseReleased();
         TITLES.mouseReleased();
      }
   }

   @Override
   public boolean mouseScrolled(double mouseX, double mouseY, double amount, int leftX, int topY) {
      return BookLayoutEditor.ENABLED
         && (
            this.layoutEditor.mouseScrolled(mouseX, mouseY, amount, leftX, topY, this.getRpOffset(), this::usesSlot)
               || TITLES.mouseScrolled(mouseX, mouseY, amount, leftX, topY, this.getRpOffset(), this::hasTitle)
         );
   }

   @Override
   public boolean keyPressed(int keyCode, int scanCode, int modifiers, int leftX, int topY) {
      return BookLayoutEditor.ENABLED && this.layoutEditor.keyPressed(keyCode);
   }

   static {
      TITLES.addText("LP_TITLE", "Page title, left (every spread)", 11162880, false, 22, 16, 160, 10, 1.0F);
      TITLES.add("LP_LINE", "Title underline, left (every spread)", 16737792, false, 25, 24, 159, 11, 1.0F);
      TITLES.addText("RP_TITLE", "Page title, right (every spread)", 11162880, true, 0, 16, 160, 10, 1.0F);
      TITLES.add("RP_LINE", "Title underline, right (every spread)", 16737792, true, -1, 24, 159, 11, 1.0F);
      BookLayoutStore.attach("titles", TITLES);
   }
}
