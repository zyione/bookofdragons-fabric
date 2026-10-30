package net.magister.bookofdragons.client.gui.book.spread;

import net.magister.bookofdragons.client.gui.book.BookLayoutEditor;
import net.magister.bookofdragons.client.gui.book.BookRenderHelper;
import net.magister.bookofdragons.client.gui.book.NotesPad;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;

public abstract class NotesSpread extends EditableBookSpread {
   private static final int INK = 4144959;
   protected final NotesPad pad;
   protected final int firstNotesPage;

   protected NotesSpread(BookLayoutEditor editor, NotesPad pad, int firstNotesPage) {
      super(editor);
      this.pad = pad;
      this.firstNotesPage = firstNotesPage;
   }

   protected boolean firstSpread() {
      return this.firstNotesPage == 0;
   }

   @Override
   protected boolean usesSlot(String name) {
      boolean notes = name.startsWith("LP_NOTES_") || name.startsWith("RP_NOTES_");
      return this.firstSpread() != notes;
   }

   protected void renderNotes(GuiGraphics graphics, Font font, String slot, int page, int leftX, int topY) {
      if (page < this.pad.pageCount()) {
         this.drawTitle(
            graphics, font, slot.startsWith("RP_"), Component.translatable("book.bookofdragons.notes.title", new Object[]{page + 1}), false, leftX, topY
         );
         String text = slot + "_TEXT";
         this.pad.render(graphics, font, page, this.sx(text, leftX), this.sy(text, topY), this.ew(text, 156), this.eh(text, 170), this.ts(text), 4144959);
      }
   }

   @Override
   protected boolean handleMouseClicked(double mouseX, double mouseY, int button, int leftX, int topY) {
      if (button != 0) {
         return false;
      }

      String[] slots = this.firstSpread() ? new String[]{"RP_NOTES0_TEXT"} : new String[]{"LP_NOTES_TEXT", "RP_NOTES_TEXT"};
      int[] pages = this.firstSpread() ? new int[]{0} : new int[]{this.firstNotesPage, this.firstNotesPage + 1};

      for (int i = 0; i < slots.length; i++) {
         if (BookRenderHelper.isMouseOver(mouseX, mouseY, this.sx(slots[i], leftX), this.sy(slots[i], topY), this.ew(slots[i], 0), this.eh(slots[i], 0))
            && pages[i] < this.pad.pageCount()) {
            this.pad.focus(pages[i]);
            return true;
         }
      }

      this.pad.unfocus();
      return false;
   }

   @Override
   public boolean charTyped(char character, int modifiers) {
      return this.pad.charTyped(character);
   }

   @Override
   public boolean keyPressed(int keyCode, int scanCode, int modifiers, int leftX, int topY) {
      return this.pad.keyPressed(keyCode) ? true : super.keyPressed(keyCode, scanCode, modifiers, leftX, topY);
   }

   @Override
   public void left() {
      this.pad.unfocus();
      this.pad.save();
   }
}
