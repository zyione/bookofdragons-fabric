package net.magister.bookofdragons.client.gui.book.spread;

import java.util.List;
import net.magister.bookofdragons.client.gui.book.BookLayoutEditor;
import net.magister.bookofdragons.client.gui.book.BookManager;
import net.magister.bookofdragons.client.gui.book.BookRenderHelper;
import net.magister.bookofdragons.client.gui.book.BookText;
import net.magister.bookofdragons.client.gui.book.ClassProgress;
import net.magister.bookofdragons.config.ModClientConfig;
import net.magister.bookofdragons.entity.base.dragon.DragonClass;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;

public class IndexSpread extends EditableBookSpread {
   private static final int ROW = 18;
   private static final int ICON = 16;
   private static final ResourceLocation MY_DRAGONS = new ResourceLocation("bookofdragons", "textures/item/nightfury_egg.png");
   private static final ResourceLocation MANUAL = new ResourceLocation("minecraft", "textures/item/saddle.png");
   private static final int HOVER = 553648127;
   private static final int ACCESS_SIZE = 16;
   private final BookManager bookManager;

   public IndexSpread(BookManager bookManager) {
      super(new BookLayoutEditor());
      this.bookManager = bookManager;
      this.layoutEditor.add("LP_INTRO", "Intro text", 1073794048, 52224, false, true, 28, 45, 153, 120);
      this.layoutEditor.add("LP_ACCESS", "Accessibility button", 1087111372, 13369548, false, true, 24, 210, 16, 16);
      this.layoutEditor.add("RP_CLASSES", "Classes", 1073776895, 26316, true, true, 8, 44, 150, 140);
      this.layoutEditor.add("RP_ALBUM", "My Dragons", 1073776895, 26316, true, true, 8, 192, 150, 18);
      this.layoutEditor.add("RP_MANUAL", "Rider's manual", 1073776895, 26316, true, true, 8, 210, 150, 18);
      stored("index", this.layoutEditor);
   }

   @Override
   public void render(GuiGraphics graphics, Font font, int leftX, int topY, int mouseX, int mouseY, float partialTick) {
      int rpX = leftX + this.getRpOffset();
      this.drawTitle(graphics, font, false, Component.translatable("book.bookofdragons.title"), true, leftX, topY);
      this.drawText(graphics, font, "LP_INTRO", List.of(Component.translatable("book.bookofdragons.index.intro")), 4144959, leftX, topY);
      this.drawTitle(graphics, font, true, Component.translatable("book.bookofdragons.index.toc"), true, leftX, topY);
      List<DragonClass> classes = ClassProgress.present();

      for (int i = 0; i < classes.size(); i++) {
         int[] row = this.classRow(i, leftX, topY);
         DragonClass c = classes.get(i);
         ClassProgress.drawIcon(graphics, c, row[0], row[1] + 1, 16);
         this.line(graphics, font, row, ClassProgress.name(c), mouseX, mouseY);
      }

      int[] album = this.albumRow(leftX, topY);
      graphics.blit(MY_DRAGONS, album[0], album[1] + 1, 0.0F, 0.0F, 16, 16, 16, 16);
      this.line(graphics, font, album, Component.translatable("book.bookofdragons.chapter.album"), mouseX, mouseY);
      int[] manual = this.manualRow(leftX, topY);
      graphics.blit(MANUAL, manual[0], manual[1] + 1, 0.0F, 0.0F, 16, 16, 16, 16);
      this.line(graphics, font, manual, Component.translatable("book.bookofdragons.chapter.manual"), mouseX, mouseY);
      this.renderAccessButton(graphics, font, leftX, topY, mouseX, mouseY);
      this.renderEditorOverlay(graphics, font, leftX, topY, mouseX, mouseY);
   }

   private void line(GuiGraphics graphics, Font font, int[] row, Component name, int mouseX, int mouseY) {
      if (!BookLayoutEditor.ENABLED && BookRenderHelper.isMouseOver(mouseX, mouseY, row[0], row[1], row[2], row[3])) {
         graphics.fill(row[0] - 2, row[1], row[0] + row[2] + 2, row[1] + row[3], 553648127);
      }

      graphics.drawString(font, BookText.of(name), row[0] + 16 + 4, row[1] + 5, 4144959, false);
   }

   private int[] classRow(int index, int leftX, int topY) {
      return new int[]{this.sx("RP_CLASSES", leftX), this.sy("RP_CLASSES", topY) + index * 18, this.ew("RP_CLASSES", 150), 18};
   }

   private int[] albumRow(int leftX, int topY) {
      return new int[]{this.sx("RP_ALBUM", leftX), this.sy("RP_ALBUM", topY), this.ew("RP_ALBUM", 150), 18};
   }

   private int[] manualRow(int leftX, int topY) {
      return new int[]{this.sx("RP_MANUAL", leftX), this.sy("RP_MANUAL", topY), this.ew("RP_MANUAL", 150), 18};
   }

   private void renderAccessButton(GuiGraphics graphics, Font font, int leftX, int topY, int mouseX, int mouseY) {
      int x = this.sx("LP_ACCESS", leftX);
      int y = this.sy("LP_ACCESS", topY);
      int w = this.ew("LP_ACCESS", 16);
      int h = this.eh("LP_ACCESS", 16);
      this.drawBacking(graphics, x, y, w, h);
      Component label = BookText.literal("Aa");
      graphics.drawString(font, label, x + (w - font.width(label)) / 2, y + (h - 8) / 2, 4144959, false);
      if (!BookLayoutEditor.ENABLED && BookRenderHelper.isMouseOver(mouseX, mouseY, x, y, w, h)) {
         graphics.fill(x, y, x + w, y + h, 553648127);
         this.drawPanel(graphics, font, List.of(Component.translatable("book.bookofdragons.accessibility")), mouseX, mouseY, 160, 4144959);
      }
   }

   @Override
   protected boolean handleMouseClicked(double mouseX, double mouseY, int button, int leftX, int topY) {
      if (button != 0) {
         return false;
      }

      if (BookRenderHelper.isMouseOver(
         mouseX, mouseY, this.sx("LP_ACCESS", leftX), this.sy("LP_ACCESS", topY), this.ew("LP_ACCESS", 16), this.eh("LP_ACCESS", 16)
      )) {
         ModClientConfig.setReadableBookFont(!ModClientConfig.isReadableBookFont());
         Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));
         return true;
      }

      List<DragonClass> classes = ClassProgress.present();

      for (int i = 0; i < classes.size(); i++) {
         int[] row = this.classRow(i, leftX, topY);
         if (BookRenderHelper.isMouseOver(mouseX, mouseY, row[0], row[1], row[2], row[3])) {
            return this.turnTo(this.bookManager.classStart(classes.get(i)));
         }
      }

      int[] album = this.albumRow(leftX, topY);
      if (BookRenderHelper.isMouseOver(mouseX, mouseY, album[0], album[1], album[2], album[3])) {
         return this.turnTo(this.bookManager.getTamedDragonsChapterIndex());
      }

      int[] manual = this.manualRow(leftX, topY);
      return BookRenderHelper.isMouseOver(mouseX, mouseY, manual[0], manual[1], manual[2], manual[3]) ? this.turnTo(this.bookManager.manualStart()) : false;
   }

   private boolean turnTo(int index) {
      if (index < 0) {
         return false;
      }

      this.bookManager.navigateTo(index);
      Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.BOOK_PAGE_TURN, 1.0F));
      return true;
   }
}
