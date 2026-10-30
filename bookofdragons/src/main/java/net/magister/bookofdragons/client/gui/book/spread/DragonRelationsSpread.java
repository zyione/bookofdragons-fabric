package net.magister.bookofdragons.client.gui.book.spread;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import net.magister.bookofdragons.client.gui.book.BookLayoutEditor;
import net.magister.bookofdragons.client.gui.book.BookRenderHelper;
import net.magister.bookofdragons.client.gui.book.BookText;
import net.magister.bookofdragons.client.gui.book.ClientDragonPages;
import net.magister.bookofdragons.client.gui.book.NotesPad;
import net.magister.bookofdragons.entity.data.DragonNames;
import net.magister.bookofdragons.network.message.DragonPageMessage;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;

public class DragonRelationsSpread extends NotesSpread {
   private static final int INK = 4144959;
   private static final int FADED = 9075298;
   private static final int HOSTILE = 10035230;
   private static final int WARY = 10511914;
   private static final int FRIENDLY = 3045946;
   private static final int HOVER = 553648127;
   private static final BookLayoutEditor EDITOR = new BookLayoutEditor();
   private final UUID dragon;
   private DragonPageMessage.Acquaintance hovered;

   public DragonRelationsSpread(UUID dragon, NotesPad pad, int firstNotesPage) {
      super(EDITOR, pad, firstNotesPage);
      this.dragon = dragon;
   }

   @Override
   public void render(GuiGraphics graphics, Font font, int leftX, int topY, int mouseX, int mouseY, float partialTick) {
      this.hovered = null;
      if (this.firstSpread()) {
         ClientDragonPages.ask(this.dragon);
         DragonPageMessage.Page page = ClientDragonPages.get(this.dragon);
         this.drawTitle(graphics, font, false, Component.translatable("book.bookofdragons.dragon.met"), true, leftX, topY);
         this.list(
            graphics,
            font,
            "LP_MET",
            page == null ? List.of() : page.met(),
            "book.bookofdragons.dragon.none_met",
            page != null && page.live(),
            leftX,
            topY,
            mouseX,
            mouseY
         );
         this.section(graphics, font, "LP_KIN", "book.bookofdragons.dragon.kin", leftX, topY);
         this.list(
            graphics,
            font,
            "LP_KIN",
            page == null ? List.of() : page.kin(),
            "book.bookofdragons.dragon.no_kin",
            page != null && page.live(),
            leftX,
            topY,
            mouseX,
            mouseY
         );
         this.renderNotes(graphics, font, "RP_NOTES0", 0, leftX, topY);
      } else {
         this.renderNotes(graphics, font, "LP_NOTES", this.firstNotesPage, leftX, topY);
         this.renderNotes(graphics, font, "RP_NOTES", this.firstNotesPage + 1, leftX, topY);
      }

      this.renderEditorOverlay(graphics, font, leftX, topY, mouseX, mouseY);
      if (this.hovered != null) {
         this.drawPanel(graphics, font, card(this.hovered), mouseX, mouseY, 170, 4144959);
      }
   }

   private void section(GuiGraphics graphics, Font font, String slot, String title, int leftX, int topY) {
      this.drawCentered(graphics, font, slot + "_TITLE", Component.translatable(title), 2039583, leftX, topY);
      String line = slot + "_LINE";
      BookRenderHelper.renderUnderline(graphics, this.sx(line, leftX), this.sy(line, topY), this.ew(line, 0), this.eh(line, 0));
   }

   private void list(
      GuiGraphics graphics,
      Font font,
      String slot,
      List<DragonPageMessage.Acquaintance> all,
      String empty,
      boolean live,
      int leftX,
      int topY,
      int mouseX,
      int mouseY
   ) {
      int x = this.sx(slot, leftX);
      int y = this.sy(slot, topY);
      int w = this.ew(slot, 154);
      int h = this.eh(slot, 90);
      int pitch = BookText.line(font) + 2;
      if (live && !all.isEmpty()) {
         for (int i = 0; i < all.size() && (i + 1) * pitch <= h; i++) {
            DragonPageMessage.Acquaintance a = all.get(i);
            int ry = y + i * pitch;
            if (!BookLayoutEditor.ENABLED && BookRenderHelper.isMouseOver(mouseX, mouseY, x, ry, w, pitch)) {
               graphics.fill(x - 2, ry - 1, x + w + 2, ry + pitch - 1, 553648127);
               this.hovered = a;
            }

            Component word = standing(a);
            Component wordText = BookText.of(word);
            int wordW = font.width(wordText);
            String name = DragonNames.shown(a.name()).getString();

            Component nameText;
            for (nameText = BookText.literal(name); font.width(nameText) > w - wordW - 6 && name.length() > 1; nameText = BookText.literal(name)) {
               name = name.substring(0, name.length() - 1);
            }

            graphics.drawString(font, nameText, x, ry, 4144959, false);
            graphics.drawString(font, wordText, x + w - wordW, ry, 4144959, false);
         }
      } else {
         this.drawText(graphics, font, slot, List.of(Component.translatable(live ? empty : "book.bookofdragons.dragon.away")), 9075298, leftX, topY);
      }
   }

   private static Component standing(DragonPageMessage.Acquaintance a) {
      String type = a.relation();
      if (!type.isEmpty()) {
         int color = switch (type) {
            case "attacks", "rival", "eats" -> 10035230;
            case "afraid", "uncomfortable" -> 10511914;
            case "pack", "plays_with" -> 3045946;
            default -> 9075298;
         };
         return Component.translatable("book.bookofdragons.relation." + type).withStyle(s -> s.withColor(color));
      } else {
         return opinion(a);
      }
   }

   private static Component opinion(DragonPageMessage.Acquaintance a) {
      boolean known = a.secondsAgo() >= 0 || !a.relation().isEmpty();
      String key;
      int color;
      if (!known && a.familiarity() < 0.05F && Math.abs(a.like()) < 0.05F && a.fear() < 0.05F) {
         key = "unmet";
         color = 9075298;
      } else if (a.like() >= 0.5F) {
         key = "close";
         color = 3045946;
      } else if (a.fear() >= 0.3F) {
         key = "wary";
         color = 10511914;
      } else if (a.like() <= -0.3F) {
         key = "dislikes";
         color = 10035230;
      } else if (a.like() >= 0.15F) {
         key = "friendly";
         color = 3045946;
      } else {
         key = "neutral";
         color = 9075298;
      }

      return Component.translatable("book.bookofdragons.opinion." + key).withStyle(s -> s.withColor(color));
   }

   private static List<Component> card(DragonPageMessage.Acquaintance a) {
      List<Component> out = new ArrayList<>();
      out.add(DragonNames.shown(a.name()));
      out.add(Component.translatable("book.bookofdragons.dragon.like", new Object[]{Math.round(a.like() * 100.0F)}));
      out.add(Component.translatable("book.bookofdragons.dragon.fear", new Object[]{Math.round(a.fear() * 100.0F)}));
      out.add(Component.translatable("book.bookofdragons.dragon.familiar", new Object[]{Math.round(a.familiarity() * 100.0F)}));
      if (a.secondsAgo() >= 0) {
         out.add(seen(a.secondsAgo()));
      }

      return out;
   }

   private static Component seen(int seconds) {
      if (seconds < 5) {
         return Component.translatable("book.bookofdragons.dragon.seen_now");
      } else {
         return seconds < 120
            ? Component.translatable("book.bookofdragons.dragon.seen_seconds", new Object[]{seconds})
            : Component.translatable("book.bookofdragons.dragon.seen_minutes", new Object[]{seconds / 60});
      }
   }

   @Override
   public void left() {
      this.hovered = null;
      super.left();
   }

   static {
      EDITOR.addText("LP_MET", "Lately seen", 8930508, false, 26, 38, 154, 90, 1.0F);
      EDITOR.addText("LP_KIN_TITLE", "Other dragons title", 11162880, false, 22, 134, 160, 10, 1.0F);
      EDITOR.add("LP_KIN_LINE", "Other dragons underline", 16737792, false, 25, 146, 159, 11, 1.0F);
      EDITOR.addText("LP_KIN", "Other dragons", 2271812, false, 26, 160, 154, 62, 1.0F);
      EDITOR.addText("RP_NOTES0_TEXT", "Notes (first)", 6710886, true, 2, 30, 156, 170, 1.0F);
      EDITOR.addText("LP_NOTES_TEXT", "Notes (left)", 6710886, false, 24, 30, 156, 170, 1.0F);
      EDITOR.addText("RP_NOTES_TEXT", "Notes (right)", 6710886, true, 2, 30, 156, 170, 1.0F);
      stored("dragon_more", EDITOR);
   }
}
