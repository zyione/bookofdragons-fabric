package net.magister.bookofdragons.client.gui.book;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.function.Predicate;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class BookLayoutEditor {
   private static final Logger LOGGER = LogManager.getLogger("BookLayoutEditor");
   public static boolean ENABLED = false;
   public static final float MIN_TEXT_SCALE = 0.9F;
   private final List<BookLayoutEditor.Slot> slots = new ArrayList<>();
   private int selectedIndex = -1;
   private boolean dragging;
   private boolean resizing;
   private double lastMX;
   private double lastMY;
   private static final int GRIP = 8;

   public void clear() {
      this.slots.clear();
      this.selectedIndex = -1;
   }

   public void add(String name, String label, int fillColor, int outlineColor, boolean rightPage, boolean resizable, int x, int y, int w, int h) {
      this.slots.add(new BookLayoutEditor.Slot(name, label, fillColor, outlineColor, rightPage, true, x, y, Math.max(w, 8), Math.max(h, 8)));
   }

   public void addChild(String name, String parent, String label, int color, boolean resizable, int x, int y, int w, int h) {
      BookLayoutEditor.Slot p = this.get(parent);
      BookLayoutEditor.Slot child = new BookLayoutEditor.Slot(
         name, label, 1073741824 | color & 16777215, color & 16777215, p != null && p.rightPage, resizable, x, y, Math.max(w, 8), Math.max(h, 8)
      );
      child.parent = parent;
      this.slots.add(child);
   }

   public void counted(String name, int min, int max, int start) {
      BookLayoutEditor.Slot s = this.get(name);
      if (s != null) {
         s.minScale = min;
         s.maxScale = max;
         s.scaleStep = 1.0F;
         s.scale = start;
      }
   }

   public String selected() {
      return this.selectedIndex >= 0 && this.selectedIndex < this.slots.size() ? this.slots.get(this.selectedIndex).name : null;
   }

   public void add(String name, String label, int color, boolean rightPage, int x, int y, int w, int h, float scale) {
      this.add(name, label, 1073741824 | color & 16777215, color & 16777215, rightPage, true, x, y, w, h);
      this.slots.get(this.slots.size() - 1).scale = scale;
   }

   public void addText(String name, String label, int color, boolean rightPage, int x, int y, int w, int h, float scale) {
      this.add(name, label, color, rightPage, x, y, w, h, Math.max(0.9F, scale));
      this.slots.get(this.slots.size() - 1).minScale = 0.9F;
   }

   public List<BookLayoutEditor.Slot> slots() {
      return Collections.unmodifiableList(this.slots);
   }

   public BookLayoutEditor.Slot get(String name) {
      for (BookLayoutEditor.Slot s : this.slots) {
         if (s.name.equals(name)) {
            return s;
         }
      }

      return null;
   }

   public int getX(String name) {
      BookLayoutEditor.Slot s = this.get(name);
      return s != null ? this.absX(s) : 0;
   }

   public int getY(String name) {
      BookLayoutEditor.Slot s = this.get(name);
      return s != null ? this.absY(s) : 0;
   }

   private int absX(BookLayoutEditor.Slot s) {
      BookLayoutEditor.Slot p = s.parent == null ? null : this.get(s.parent);
      return p != null && p != s ? this.absX(p) + s.x : s.x;
   }

   private int absY(BookLayoutEditor.Slot s) {
      BookLayoutEditor.Slot p = s.parent == null ? null : this.get(s.parent);
      return p != null && p != s ? this.absY(p) + s.y : s.y;
   }

   public int getW(String name) {
      BookLayoutEditor.Slot s = this.get(name);
      return s != null ? s.w : 16;
   }

   public int getH(String name) {
      BookLayoutEditor.Slot s = this.get(name);
      return s != null ? s.h : 16;
   }

   public float getScale(String name) {
      BookLayoutEditor.Slot s = this.get(name);
      return s != null ? s.scale : 1.0F;
   }

   public void renderOverlay(GuiGraphics graphics, Font font, int leftX, int topY, int rpOffset, int mouseX, int mouseY) {
      this.renderOverlay(graphics, font, leftX, topY, rpOffset, mouseX, mouseY, s -> true);
   }

   public void renderOverlay(GuiGraphics graphics, Font font, int leftX, int topY, int rpOffset, int mouseX, int mouseY, Predicate<String> shown) {
      if (ENABLED) {
         for (int i = 0; i < this.slots.size(); i++) {
            BookLayoutEditor.Slot slot = this.slots.get(i);
            if (shown.test(slot.name)) {
               int ax = leftX + (slot.rightPage ? rpOffset : 0) + this.absX(slot);
               int ay = topY + this.absY(slot);
               boolean hov = this.isOverBody(slot, mouseX, mouseY, leftX, topY, rpOffset);
               boolean sel = i == this.selectedIndex;
               graphics.fill(ax, ay, ax + slot.w, ay + slot.h, slot.fillColor);
               int alpha = sel ? -16777216 : (hov ? -872415232 : -1728053248);
               int oc = alpha | slot.outlineColor & 16777215;
               graphics.fill(ax, ay, ax + slot.w, ay + 1, oc);
               graphics.fill(ax, ay + slot.h - 1, ax + slot.w, ay + slot.h, oc);
               graphics.fill(ax, ay, ax + 1, ay + slot.h, oc);
               graphics.fill(ax + slot.w - 1, ay, ax + slot.w, ay + slot.h, oc);
               if (slot.resizable) {
                  int gx = ax + slot.w - 8;
                  int gy = ay + slot.h - 8;
                  graphics.fill(gx, gy, ax + slot.w, ay + slot.h, -1);
                  graphics.fill(gx + 1, gy + 1, ax + slot.w - 1, ay + slot.h - 1, oc);
               }

               if (hov || sel) {
                  graphics.drawString(font, slot.label, ax + 2, ay + 2, -1, true);
               }
            }
         }

         if (this.selectedIndex >= 0 && this.selectedIndex < this.slots.size()) {
            BookLayoutEditor.Slot s = this.slots.get(this.selectedIndex);
            String scale = s.scaleStep >= 1.0F ? "depth=" + Math.round(s.scale) : String.format(Locale.ROOT, "scale=%.2f", s.scale);
            String info = String.format(Locale.ROOT, "%s  x=%d y=%d w=%d h=%d %s %s", s.name, s.x, s.y, s.w, s.h, scale, s.rightPage ? "[RP]" : "[LP]");
            graphics.fill(leftX, topY - 14, leftX + font.width(info) + 6, topY - 2, -872415232);
            graphics.drawString(font, info, leftX + 3, topY - 12, -256, false);
         }

         String hint = "§e[LAYOUT EDITOR]§r  drag=move  corner=resize  wheel=text size  saved on release  Ctrl+F=font  Ctrl+L=off";
         int hy = topY + 256 + 3;
         graphics.fill(leftX, hy, leftX + font.width(hint) + 6, hy + 11, -872415232);
         graphics.drawString(font, hint, leftX + 3, hy + 2, -3355444, false);
      }
   }

   public boolean mouseClicked(double mouseX, double mouseY, int button, int leftX, int topY, int rpOffset) {
      return this.mouseClicked(mouseX, mouseY, button, leftX, topY, rpOffset, s -> true);
   }

   public boolean mouseClicked(double mouseX, double mouseY, int button, int leftX, int topY, int rpOffset, Predicate<String> shown) {
      if (ENABLED && button == 0) {
         int mx = (int)mouseX;
         int my = (int)mouseY;

         for (int i = this.slots.size() - 1; i >= 0; i--) {
            BookLayoutEditor.Slot slot = this.slots.get(i);
            if (shown.test(slot.name)) {
               boolean grip = this.isOverGrip(slot, mx, my, leftX, topY, rpOffset);
               if (grip || this.isOverBody(slot, mx, my, leftX, topY, rpOffset)) {
                  this.selectedIndex = i;
                  this.resizing = grip;
                  this.dragging = !grip;
                  this.lastMX = mouseX;
                  this.lastMY = mouseY;
                  return true;
               }
            }
         }

         this.selectedIndex = -1;
         this.dragging = this.resizing = false;
         return false;
      } else {
         return false;
      }
   }

   public boolean mouseDragged(double mouseX, double mouseY, int leftX, int topY, int rpOffset) {
      if (ENABLED && (this.dragging || this.resizing) && this.selectedIndex >= 0) {
         int dx = (int)(mouseX - this.lastMX);
         int dy = (int)(mouseY - this.lastMY);
         if (dx == 0 && dy == 0) {
            return true;
         }

         BookLayoutEditor.Slot slot = this.slots.get(this.selectedIndex);
         if (this.dragging) {
            slot.x += dx;
            slot.y += dy;
         } else {
            slot.w = Math.max(8, slot.w + dx);
            slot.h = Math.max(8, slot.h + dy);
         }

         this.lastMX = mouseX;
         this.lastMY = mouseY;
         return true;
      } else {
         return false;
      }
   }

   public void mouseReleased() {
      if (this.dragging || this.resizing) {
         BookLayoutStore.save();
      }

      this.dragging = this.resizing = false;
   }

   public boolean mouseScrolled(double mouseX, double mouseY, double amount, int leftX, int topY, int rpOffset, Predicate<String> shown) {
      if (ENABLED && amount != 0.0) {
         BookLayoutEditor.Slot target = null;

         for (int i = this.slots.size() - 1; i >= 0 && target == null; i--) {
            BookLayoutEditor.Slot slot = this.slots.get(i);
            if (shown.test(slot.name) && this.isOverBody(slot, (int)mouseX, (int)mouseY, leftX, topY, rpOffset)) {
               target = slot;
               this.selectedIndex = i;
            }
         }

         if (target == null) {
            return false;
         }

         float step = amount > 0.0 ? target.scaleStep : -target.scaleStep;
         target.scale = Math.max(target.minScale, Math.min(target.maxScale, Math.round((target.scale + step) * 100.0F) / 100.0F));
         BookLayoutStore.save();
         return true;
      } else {
         return false;
      }
   }

   public boolean keyPressed(int keyCode) {
      if (!ENABLED) {
         return false;
      } else if (keyCode == 80) {
         this.dump();
         return true;
      } else {
         return false;
      }
   }

   public void dump() {
      for (BookLayoutEditor.Slot s : this.slots) {
         LOGGER.debug("  {} -> ({}, {}) {}x{} scale {}", s.name, s.x, s.y, s.w, s.h, s.scale);
      }
   }

   private boolean isOverBody(BookLayoutEditor.Slot slot, int mx, int my, int leftX, int topY, int rpOffset) {
      int ax = leftX + (slot.rightPage ? rpOffset : 0) + this.absX(slot);
      int ay = topY + this.absY(slot);
      return mx >= ax && mx < ax + slot.w && my >= ay && my < ay + slot.h;
   }

   private boolean isOverGrip(BookLayoutEditor.Slot slot, int mx, int my, int leftX, int topY, int rpOffset) {
      if (!slot.resizable) {
         return false;
      }

      int ax = leftX + (slot.rightPage ? rpOffset : 0) + this.absX(slot);
      int ay = topY + this.absY(slot);
      return mx >= ax + slot.w - 8 && mx < ax + slot.w && my >= ay + slot.h - 8 && my < ay + slot.h;
   }

   public static class Slot {
      public final String name;
      public final String label;
      public final int fillColor;
      public final int outlineColor;
      public final boolean rightPage;
      public final boolean resizable;
      public int x;
      public int y;
      public int w;
      public int h;
      public float scale = 1.0F;
      public float minScale = 0.25F;
      public String parent;
      public float scaleStep = 0.05F;
      public float maxScale = 4.0F;

      public Slot(String name, String label, int fillColor, int outlineColor, boolean rightPage, boolean resizable, int x, int y, int w, int h) {
         this.name = name;
         this.label = label;
         this.fillColor = fillColor;
         this.outlineColor = outlineColor;
         this.rightPage = rightPage;
         this.resizable = resizable;
         this.x = x;
         this.y = y;
         this.w = w;
         this.h = h;
      }
   }
}
