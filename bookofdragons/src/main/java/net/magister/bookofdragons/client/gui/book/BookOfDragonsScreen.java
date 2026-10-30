package net.magister.bookofdragons.client.gui.book;

import java.util.List;
import java.util.UUID;
import net.magister.bookofdragons.network.ControlNetwork;
import net.magister.bookofdragons.network.message.RequestTamedDragonsMessage;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;

public class BookOfDragonsScreen extends Screen {
   public static final int BOOK_WIDTH = 384;
   public static final int BOOK_HEIGHT = 256;
   private static final int ARROW_LEFT_X_OFFSET = 19;
   private static final int ARROW_LEFT_Y_OFFSET = 205;
   private static final int ARROW_RIGHT_X_OFFSET = 335;
   private static final int ARROW_RIGHT_Y_OFFSET = 203;
   private static final BookLayoutEditor ARROW_EDITOR = new BookLayoutEditor();
   private long handShownAt;
   private final BookManager bookManager = new BookManager();
   private final BookTabs tabs = new BookTabs();

   public BookOfDragonsScreen() {
      super(Component.translatable("book.bookofdragons.title"));
   }

   public BookOfDragonsScreen(UUID targetDragonUUID) {
      this();
   }

   protected void init() {
      super.init();
      this.bookManager.init();
      BookDepth.reset();
      ControlNetwork.sendToServer(new RequestTamedDragonsMessage());
   }

   public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
      this.renderBackground(graphics);
      int leftX = (this.width - 384) / 2;
      int topY = (this.height - 256) / 2;
      this.bookManager.refresh();
      BookRenderHelper.renderBookBackground(graphics, leftX, topY);
      this.tabs.render(graphics, this.bookManager.tabs(), this.bookManager.getCurrentIndex(), leftX, topY, mouseX, mouseY);
      this.bookManager.getCurrent().render(graphics, this.font, leftX, topY, mouseX, mouseY, partialTick);
      this.renderPageTurnArrows(graphics, leftX, topY, mouseX, mouseY);
      this.renderIndexButton(graphics, leftX, topY, mouseX, mouseY);
      this.renderHandName(graphics, leftX, topY);
      this.tabs.renderTooltip(graphics, this.font, mouseX, mouseY);
      if (BookLayoutEditor.ENABLED) {
         ARROW_EDITOR.renderOverlay(graphics, this.font, leftX, topY, 0, mouseX, mouseY);
         BookTabs.EDITOR.renderOverlay(graphics, this.font, leftX, topY, 0, mouseX, mouseY, BookTabs::shows);
      }

      super.render(graphics, mouseX, mouseY, partialTick);
   }

   private void renderHandName(GuiGraphics graphics, int leftX, int topY) {
      if (Util.getMillis() - this.handShownAt <= 2000L) {
         graphics.drawCenteredString(this.font, BookText.name(), leftX + 192, topY + 256 + 4, 16777215);
      }
   }

   private void renderIndexButton(GuiGraphics graphics, int leftX, int topY, int mouseX, int mouseY) {
      if (this.bookManager.getCurrentIndex() != 0) {
         int x = leftX + ARROW_EDITOR.getX("INDEX");
         int y = topY + ARROW_EDITOR.getY("INDEX");
         BookRenderHelper.renderIndexArrow(graphics, x, y);
         if (!BookLayoutEditor.ENABLED && this.overIndexButton(leftX, topY, mouseX, mouseY)) {
            graphics.fill(x, y, x + ARROW_EDITOR.getW("INDEX"), y + ARROW_EDITOR.getH("INDEX"), 822083583);
            BookRenderHelper.drawPanel(graphics, this.font, List.of(Component.translatable("book.bookofdragons.index.toc")), mouseX, mouseY, 160, 4144959);
         }
      }
   }

   private boolean overIndexButton(int leftX, int topY, double mouseX, double mouseY) {
      return this.bookManager.getCurrentIndex() != 0
         && BookRenderHelper.isMouseOver(
            mouseX, mouseY, leftX + ARROW_EDITOR.getX("INDEX"), topY + ARROW_EDITOR.getY("INDEX"), ARROW_EDITOR.getW("INDEX"), ARROW_EDITOR.getH("INDEX")
         );
   }

   private void renderPageTurnArrows(GuiGraphics graphics, int leftX, int topY, int mouseX, int mouseY) {
      int lax = leftX + arrowX("ARROW_LEFT", 19);
      int rax = leftX + arrowX("ARROW_RIGHT", 335);
      int lay = topY + arrowY("ARROW_LEFT", 205);
      int ray = topY + arrowY("ARROW_RIGHT", 203);
      if (this.bookManager.canGoPrev()) {
         boolean hovered = BookRenderHelper.isMouseOver(mouseX, mouseY, lax, lay, 29, 28);
         BookRenderHelper.renderPageTurnLarge(graphics, lax, lay, true, hovered);
      }

      if (this.bookManager.canGoNext()) {
         boolean hovered = BookRenderHelper.isMouseOver(mouseX, mouseY, rax, ray, 29, 28);
         BookRenderHelper.renderPageTurnLarge(graphics, rax, ray, false, hovered);
      }
   }

   public boolean mouseClicked(double mouseX, double mouseY, int button) {
      int leftX = (this.width - 384) / 2;
      int topY = (this.height - 256) / 2;
      if (!BookLayoutEditor.ENABLED
         || !ARROW_EDITOR.mouseClicked(mouseX, mouseY, button, leftX, topY, 0)
            && !BookTabs.EDITOR.mouseClicked(mouseX, mouseY, button, leftX, topY, 0, BookTabs::shows)) {
         if (button == 0 && !BookLayoutEditor.ENABLED) {
            int start = this.tabs.clicked(this.bookManager.tabs(), this.bookManager.getCurrentIndex(), leftX, topY, mouseX, mouseY);
            if (start >= 0) {
               this.bookManager.navigateTo(start);
               return true;
            }

            if (this.overIndexButton(leftX, topY, mouseX, mouseY)) {
               this.bookManager.navigateTo(0);
               Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.BOOK_PAGE_TURN, 1.0F));
               return true;
            }
         }

         if (button == 0) {
            int lax = leftX + arrowX("ARROW_LEFT", 19);
            int rax = leftX + arrowX("ARROW_RIGHT", 335);
            int lay = topY + arrowY("ARROW_LEFT", 205);
            int ray = topY + arrowY("ARROW_RIGHT", 203);
            if (this.bookManager.canGoPrev() && BookRenderHelper.isMouseOver(mouseX, mouseY, lax, lay, 29, 28)) {
               this.bookManager.prev();
               Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.BOOK_PAGE_TURN, 1.0F));
               return true;
            }

            if (this.bookManager.canGoNext() && BookRenderHelper.isMouseOver(mouseX, mouseY, rax, ray, 29, 28)) {
               this.bookManager.next();
               Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.BOOK_PAGE_TURN, 1.0F));
               return true;
            }
         }

         return this.bookManager.getCurrent().mouseClicked(mouseX, mouseY, button, (this.width - 384) / 2, (this.height - 256) / 2)
            ? true
            : super.mouseClicked(mouseX, mouseY, button);
      } else {
         return true;
      }
   }

   public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
      int leftX = (this.width - 384) / 2;
      int topY = (this.height - 256) / 2;
      if (!BookLayoutEditor.ENABLED
         || !ARROW_EDITOR.mouseDragged(mouseX, mouseY, leftX, topY, 0) && !BookTabs.EDITOR.mouseDragged(mouseX, mouseY, leftX, topY, 0)) {
         return this.bookManager.getCurrent().mouseDragged(mouseX, mouseY, button, dragX, dragY, leftX, topY)
            ? true
            : super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
      } else {
         return true;
      }
   }

   public boolean mouseReleased(double mouseX, double mouseY, int button) {
      if (BookLayoutEditor.ENABLED) {
         ARROW_EDITOR.mouseReleased();
         BookTabs.EDITOR.mouseReleased();
      }

      int leftX = (this.width - 384) / 2;
      int topY = (this.height - 256) / 2;
      this.bookManager.getCurrent().mouseReleased(mouseX, mouseY, button, leftX, topY);
      return super.mouseReleased(mouseX, mouseY, button);
   }

   public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
      if (keyCode == 76 && (modifiers & 2) != 0 && BookLayoutStore.editable()) {
         BookLayoutEditor.ENABLED = !BookLayoutEditor.ENABLED;
         return true;
      } else if (keyCode == 70 && (modifiers & 2) != 0 && BookLayoutStore.editable()) {
         BookText.cycle();
         this.handShownAt = Util.getMillis();
         return true;
      } else {
         int leftX = (this.width - 384) / 2;
         int topY = (this.height - 256) / 2;
         if (this.bookManager.getCurrent().keyPressed(keyCode, scanCode, modifiers, leftX, topY)) {
            return true;
         } else if (BookLayoutEditor.ENABLED && keyCode == 80) {
            ARROW_EDITOR.dump();
            BookTabs.EDITOR.dump();
            this.bookManager.dumpAllEditors();
            return true;
         } else {
            return super.keyPressed(keyCode, scanCode, modifiers);
         }
      }
   }

   public boolean charTyped(char character, int modifiers) {
      return this.bookManager.getCurrent().charTyped(character, modifiers) ? true : super.charTyped(character, modifiers);
   }

   public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
      return this.scrolled(mouseX, mouseY, delta) ? true : super.mouseScrolled(mouseX, mouseY, delta);
   }

   private boolean scrolled(double mouseX, double mouseY, double amount) {
      int leftX = (this.width - 384) / 2;
      int topY = (this.height - 256) / 2;
      return !BookLayoutEditor.ENABLED
            || !ARROW_EDITOR.mouseScrolled(mouseX, mouseY, amount, leftX, topY, 0, s -> true)
               && !BookTabs.EDITOR.mouseScrolled(mouseX, mouseY, amount, leftX, topY, 0, BookTabs::shows)
         ? this.bookManager.getCurrent().mouseScrolled(mouseX, mouseY, amount, leftX, topY)
         : true;
   }

   public void removed() {
      this.bookManager.saveAll();
      super.removed();
   }

   public boolean isPauseScreen() {
      return false;
   }

   private static int arrowX(String slot, int def) {
      return ARROW_EDITOR.get(slot) != null ? ARROW_EDITOR.getX(slot) : def;
   }

   private static int arrowY(String slot, int def) {
      return ARROW_EDITOR.get(slot) != null ? ARROW_EDITOR.getY(slot) : def;
   }

   static {
      ARROW_EDITOR.add("ARROW_LEFT", "Left arrow", 1073807359, 65535, false, false, 19, 205, 29, 28);
      ARROW_EDITOR.add("ARROW_RIGHT", "Right arrow", 1090518784, 16776960, false, false, 335, 203, 29, 28);
      ARROW_EDITOR.add("INDEX", "Back to contents", 1090453759, 16711935, false, false, 182, 2, 20, 20);
      BookLayoutStore.attach("arrows", ARROW_EDITOR);
   }
}
