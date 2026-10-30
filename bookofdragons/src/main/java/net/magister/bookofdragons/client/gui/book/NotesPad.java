package net.magister.bookofdragons.client.gui.book;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import net.magister.bookofdragons.datagen.data.DragonDiscoveryData;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.font.TextFieldHelper;
import net.minecraft.network.chat.Component;

public final class NotesPad {
   private final List<String> pages = new ArrayList<>();
   private final Runnable onPagesChanged;
   private final Consumer<List<String>> store;
   private final TextFieldHelper helper;
   private int focus = -1;
   private boolean dirty;
   private Font font;
   private int width = 150;
   private int height = 160;

   public NotesPad(List<String> saved, Runnable onPagesChanged, Consumer<List<String>> store) {
      this.onPagesChanged = onPagesChanged;
      this.store = store;
      this.pages.addAll(saved);
      if (this.pages.isEmpty()) {
         this.pages.add("");
      }

      Minecraft mc = Minecraft.getInstance();
      this.helper = new TextFieldHelper(this::text, this::setText, TextFieldHelper.createClipboardGetter(mc), TextFieldHelper.createClipboardSetter(mc), this::fits);
   }

   public int pageCount() {
      return this.pages.size();
   }

   public int focusedPage() {
      return this.focus;
   }

   public void focus(int page) {
      if (page >= 0 && page < this.pages.size()) {
         this.focus = page;
         this.helper.setCursorToEnd();
      }
   }

   public void unfocus() {
      this.focus = -1;
   }

   private String text() {
      return this.focus >= 0 && this.focus < this.pages.size() ? this.pages.get(this.focus) : "";
   }

   private void setText(String text) {
      if (this.focus >= 0) {
         this.pages.set(this.focus, text);
         this.dirty = true;
      }
   }

   private boolean fits(String text) {
      return text.length() > 1024 ? false : this.font == null || lines(this.font, text, this.width).size() * BookText.line(this.font) <= this.height;
   }

   public boolean charTyped(char c) {
      if (this.focus < 0) {
         return false;
      }

      if (c >= ' ' && c != 127 && c != 167) {
         String before = this.text();
         this.helper.charTyped(c);
         if (this.text().equals(before) && this.helper.getCursorPos() == before.length() && this.overflow()) {
            this.helper.charTyped(c);
         }

         return true;
      } else {
         return true;
      }
   }

   public boolean keyPressed(int key) {
      if (this.focus < 0 || key == 256) {
         return false;
      }

      if (key != 257 && key != 335) {
         if (key == 259 && this.text().isEmpty() && this.focus > 0 && this.focus == this.pages.size() - 1) {
            this.pages.remove(this.focus);
            this.focus--;
            this.helper.setCursorToEnd();
            this.dirty = true;
            this.onPagesChanged.run();
            return true;
         } else {
            this.helper.keyPressed(key);
            return true;
         }
      } else {
         String before = this.text();
         this.helper.insertText("\n");
         if (this.text().equals(before) && this.helper.getCursorPos() == before.length()) {
            this.overflow();
         }

         return true;
      }
   }

   private boolean overflow() {
      int next = this.focus + 1;
      if (next >= 100) {
         return false;
      }

      if (next == this.pages.size()) {
         this.pages.add("");
      } else if (!this.pages.get(next).isEmpty()) {
         return false;
      }

      this.focus = next;
      this.helper.setCursorToEnd();
      this.dirty = true;
      this.onPagesChanged.run();
      return true;
   }

   public void save() {
      if (this.dirty) {
         this.dirty = false;
         this.store.accept(DragonDiscoveryData.cleanNotes(this.pages));
      }
   }

   private static List<int[]> lines(Font font, String text, int width) {
      List<int[]> out = new ArrayList<>();
      font.getSplitter().splitLines(text, width, BookText.style(), true, (style, start, end) -> out.add(new int[]{start, end}));
      return out;
   }

   public void render(GuiGraphics graphics, Font font, int page, int x, int y, int w, int h, float scale, int color) {
      if (page >= 0 && page < this.pages.size()) {
         int pw = Math.max(16, Math.round(w / scale));
         int ph = Math.max(9, Math.round(h / scale));
         if (page == this.focus) {
            this.font = font;
            this.width = pw;
            this.height = ph;
         }

         String text = this.pages.get(page);
         List<int[]> lines = lines(font, text, pw);
         int pitch = BookText.line(font);
         graphics.pose().pushPose();
         graphics.pose().translate(x, y, 0.0F);
         graphics.pose().scale(scale, scale, 1.0F);
         if (text.isEmpty() && page != this.focus) {
            graphics.drawString(font, BookText.of(Component.translatable("book.bookofdragons.notes.hint")), 0, 0, 8421504, false);
         }

         int cursor = page == this.focus ? this.helper.getCursorPos() : -1;
         int caretX = 0;
         int caretY = 0;

         for (int i = 0; i < lines.size(); i++) {
            int start = lines.get(i)[0];
            int end = lines.get(i)[1];
            String line = text.substring(start, end);
            String shown = line.endsWith("\n") ? line.substring(0, line.length() - 1) : line;
            int ly = i * pitch;
            if (ly + pitch > ph) {
               break;
            }

            graphics.drawString(font, BookText.of(Component.literal(shown)), 0, ly, color, false);
            if (cursor >= start && cursor <= start + shown.length()) {
               caretX = font.width(BookText.of(Component.literal(text.substring(start, cursor))));
               caretY = ly;
            } else if (line.endsWith("\n") && cursor == end) {
               caretX = 0;
               caretY = ly + pitch;
            }
         }

         if (cursor >= 0 && System.currentTimeMillis() / 500L % 2L == 0L) {
            graphics.fill(caretX, caretY - 1, caretX + 1, caretY + pitch - 1, 0xFF000000 | color);
         }

         graphics.pose().popPose();
      }
   }
}
