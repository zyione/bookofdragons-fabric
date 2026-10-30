package net.magister.bookofdragons.client.gui.book.spread;

import java.util.ArrayList;
import java.util.List;
import net.magister.bookofdragons.client.gui.book.BookLayoutEditor;
import net.magister.bookofdragons.client.gui.book.BookRenderHelper;
import net.magister.bookofdragons.client.gui.book.BookTips;
import net.magister.bookofdragons.client.gui.book.NotesPad;
import net.magister.bookofdragons.client.gui.book.RadarGraph;
import net.magister.bookofdragons.client.gui.book.data.SpeciesFacts;
import net.magister.bookofdragons.entity.data.DragonType;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

public class SpeciesPagesSpread extends EditableBookSpread {
   private static final int INK = 4144959;
   private static final int GRAPH_GRID = 1073741824;
   private static final int GRAPH_FILL = -2140452309;
   private static final int GRAPH_LINE = -9745877;
   private static final ResourceLocation TEMPERAMENT_BACKING = new ResourceLocation(
      "bookofdragons", "textures/gui/book/overlaytemperament.png"
   );
   private static final ResourceLocation TAMING_BACKING = new ResourceLocation("bookofdragons", "textures/gui/book/overlaytaming.png");
   private static final int BACKING_W = 167;
   private static final int BACKING_H = 96;
   private static final BookLayoutEditor EDITOR = new BookLayoutEditor();
   private final DragonType type;
   private final NotesPad pad;
   private final int firstNotesPage;
   private int hoveredTrait = -1;

   public SpeciesPagesSpread(DragonType type, NotesPad pad, int firstNotesPage) {
      super(EDITOR);
      this.type = type;
      this.pad = pad;
      this.firstNotesPage = firstNotesPage;
   }

   private boolean natureSpread() {
      return this.firstNotesPage == 0;
   }

   @Override
   protected boolean usesSlot(String name) {
      boolean notes = name.startsWith("LP_NOTES_") || name.startsWith("RP_NOTES_");
      return this.natureSpread() != notes;
   }

   @Override
   public void render(GuiGraphics graphics, Font font, int leftX, int topY, int mouseX, int mouseY, float partialTick) {
      if (this.natureSpread()) {
         int tier = SpeciesFacts.tier(this.type);
         this.section(graphics, font, "LP_TEMP", "temperament", TEMPERAMENT_BACKING, SpeciesFacts.temperamentJournal(this.type, tier), tier, leftX, topY);
         if (tier >= 1) {
            this.renderTraits(graphics, font, leftX, topY);
         }

         this.hoveredTrait = tier >= 1 && !BookLayoutEditor.ENABLED
            ? RadarGraph.labelAt(
               font,
               traitLabels(),
               SpeciesFacts.TRAITS.size(),
               this.sx("LP_TRAITS", leftX),
               this.sy("LP_TRAITS", topY),
               this.ew("LP_TRAITS", 158),
               this.eh("LP_TRAITS", 90),
               this.es("LP_TRAITS"),
               mouseX,
               mouseY
            )
            : -1;
         this.section(graphics, font, "RP_TAME", "taming", TAMING_BACKING, SpeciesFacts.tamingJournal(this.type, tier), tier, leftX, topY);
         if (tier >= 1) {
            this.drawItems(graphics, "RP_TAME_ITEMS", SpeciesFacts.knownTreasures(this.type), leftX, topY);
         }

         this.renderNotes(graphics, font, "RP_NOTES0", 0, leftX, topY);
      } else {
         this.renderNotes(graphics, font, "LP_NOTES", this.firstNotesPage, leftX, topY);
         this.renderNotes(graphics, font, "RP_NOTES", this.firstNotesPage + 1, leftX, topY);
         this.hoveredTrait = -1;
      }

      this.renderEditorOverlay(graphics, font, leftX, topY, mouseX, mouseY);
      if (this.hoveredTrait >= 0) {
         this.drawPanel(graphics, font, BookTips.trait(SpeciesFacts.TRAITS.get(this.hoveredTrait)), mouseX, mouseY, 170, 4144959);
      }
   }

   private static List<Component> traitLabels() {
      List<Component> labels = new ArrayList<>();

      for (String trait : SpeciesFacts.TRAITS) {
         labels.add(Component.translatable("book.bookofdragons.trait." + trait));
      }

      return labels;
   }

   private void section(
      GuiGraphics graphics, Font font, String slot, String title, ResourceLocation backingTexture, List<Component> journal, int tier, int leftX, int topY
   ) {
      this.drawTitle(graphics, font, slot.startsWith("RP_"), Component.translatable("book.bookofdragons.field.title." + title), true, leftX, topY);
      if (tier >= 1) {
         String backing = slot + "_OVL";
         BookRenderHelper.renderOverlayTexture(
            graphics, backingTexture, this.sx(backing, leftX), this.sy(backing, topY), this.ew(backing, 167), this.eh(backing, 96)
         );
      }

      this.drawJournal(graphics, font, slot + "_TEXT", journal, 4144959, leftX, topY);
   }

   private void renderNotes(GuiGraphics graphics, Font font, String slot, int page, int leftX, int topY) {
      if (page < this.pad.pageCount()) {
         Component title = Component.translatable("book.bookofdragons.notes.title", new Object[]{page + 1});
         if (slot.equals("RP_NOTES0")) {
            this.drawCentered(graphics, font, slot + "_TITLE", title, 2039583, leftX, topY);
         } else {
            this.drawTitle(graphics, font, slot.startsWith("RP_"), title, false, leftX, topY);
         }

         String text = slot + "_TEXT";
         this.pad.render(graphics, font, page, this.sx(text, leftX), this.sy(text, topY), this.ew(text, 156), this.eh(text, 170), this.ts(text), 4144959);
      }
   }

   private void renderTraits(GuiGraphics graphics, Font font, int leftX, int topY) {
      RadarGraph.draw(
         graphics,
         font,
         traitLabels(),
         SpeciesFacts.traits(this.type),
         this.sx("LP_TRAITS", leftX),
         this.sy("LP_TRAITS", topY),
         this.ew("LP_TRAITS", 158),
         this.eh("LP_TRAITS", 90),
         this.es("LP_TRAITS"),
         4144959,
         1073741824,
         -2140452309,
         -9745877
      );
   }

   @Override
   protected boolean handleMouseClicked(double mouseX, double mouseY, int button, int leftX, int topY) {
      if (button != 0) {
         return false;
      }

      String[] slots = this.natureSpread() ? new String[]{"RP_NOTES0_TEXT"} : new String[]{"LP_NOTES_TEXT", "RP_NOTES_TEXT"};
      int[] pages = this.natureSpread() ? new int[]{0} : new int[]{this.firstNotesPage, this.firstNotesPage + 1};

      for (int i = 0; i < slots.length; i++) {
         if (this.over(slots[i], mouseX, mouseY, leftX, topY) && pages[i] < this.pad.pageCount()) {
            this.pad.focus(pages[i]);
            return true;
         }
      }

      this.pad.unfocus();
      return false;
   }

   private boolean over(String slot, double mouseX, double mouseY, int leftX, int topY) {
      return BookRenderHelper.isMouseOver(mouseX, mouseY, this.sx(slot, leftX), this.sy(slot, topY), this.ew(slot, 0), this.eh(slot, 0));
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

   static {
      EDITOR.add("LP_TEMP_OVL", "Temperament backing", 16737792, false, 20, 32, 167, 96, 1.0F);
      EDITOR.addText("LP_TEMP_TEXT", "Temperament", 8930508, false, 28, 40, 151, 80, 1.0F);
      EDITOR.add("LP_TRAITS", "Traits", 2263244, false, 24, 132, 158, 90, 0.5F);
      EDITOR.add("RP_TAME_OVL", "Taming backing", 16737792, true, -4, 32, 167, 96, 1.0F);
      EDITOR.addText("RP_TAME_TEXT", "Taming", 2271812, true, 4, 40, 151, 80, 1.0F);
      EDITOR.add("RP_TAME_ITEMS", "Treasures", 13378116, true, 86, 110, 68, 16, 1.0F);
      EDITOR.addText("RP_NOTES0_TITLE", "Notes title (first)", 11162880, true, 0, 134, 160, 10, 1.0F);
      EDITOR.addText("RP_NOTES0_TEXT", "Notes (first)", 6710886, true, 2, 148, 156, 78, 1.0F);
      EDITOR.addText("LP_NOTES_TEXT", "Notes (left)", 6710886, false, 24, 30, 156, 170, 1.0F);
      EDITOR.addText("RP_NOTES_TEXT", "Notes (right)", 6710886, true, 2, 30, 156, 170, 1.0F);
      stored("species_pages", EDITOR);
   }
}
