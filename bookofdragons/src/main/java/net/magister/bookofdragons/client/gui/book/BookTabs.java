package net.magister.bookofdragons.client.gui.book;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import net.magister.bookofdragons.entity.base.dragon.DragonClass;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;

public final class BookTabs {
   public static final int LEVELS = 5;
   private static final int RIBBON_W = 34;
   private static final int RIBBON_H = 20;
   private static final int RIBBON_PITCH = 22;
   private static final int SEAT = 10;
   private static final int BOX_W = 32;
   private static final int ICON = 16;
   private static final int SLIDE = 6;
   private static final float SETTLE = 0.25F;
   private static final String ART = "_ART";
   private static final String ICON_SLOT = "_ICON";
   public static final BookLayoutEditor EDITOR = new BookLayoutEditor();
   private static final Map<DragonClass, Boolean> ON_RIGHT;
   private final Map<DragonClass, Float> out = new EnumMap<>(DragonClass.class);
   private final Map<DragonClass, Float> shownX = new EnumMap<>(DragonClass.class);
   private final Map<DragonClass, Boolean> shownRight = new EnumMap<>(DragonClass.class);
   private final Map<DragonClass, int[]> boxes = new EnumMap<>(DragonClass.class);
   private BookTabs.Tab hovered;

   private static String slot(DragonClass dragonClass, boolean right) {
      return "TAB_" + dragonClass.name() + (right ? "_R" : "_L");
   }

   private static String level(boolean right, int level) {
      return "LEVEL_" + (right ? "R" : "L") + level;
   }

   public static boolean shows(String slot) {
      if (slot.startsWith("LEVEL_")) {
         return true;
      }

      DragonClass owner = null;

      for (DragonClass c : ClassProgress.present()) {
         if (slot.startsWith("TAB_" + c.name() + "_")) {
            owner = c;
         }
      }

      if (owner == null) {
         return false;
      }

      String box = box(slot);
      if (box.endsWith("_R") != ON_RIGHT.getOrDefault(owner, true)) {
         return false;
      }

      if (box.equals(slot)) {
         return true;
      }

      String selected = EDITOR.selected();
      return selected != null && box(selected).equals(box);
   }

   private static String box(String slot) {
      if (slot.endsWith("_ART")) {
         return slot.substring(0, slot.length() - "_ART".length());
      } else {
         return slot.endsWith("_ICON") ? slot.substring(0, slot.length() - "_ICON".length()) : slot;
      }
   }

   private static ResourceLocation ribbon(DragonClass dragonClass) {
      return new ResourceLocation(
         "bookofdragons", "textures/gui/book/pagemarker" + (ClassProgress.ORDER.indexOf(dragonClass) % 3 + 1) + "_dyes.png"
      );
   }

   private static boolean right(BookTabs.Tab tab, int current) {
      return current < tab.start();
   }

   private static Map<DragonClass, Integer> levels(List<BookTabs.Tab> tabs, int current) {
      List<BookTabs.Tab> ahead = new ArrayList<>();
      List<BookTabs.Tab> passed = new ArrayList<>();

      for (BookTabs.Tab tab : tabs) {
         (right(tab, current) ? ahead : passed).add(tab);
      }

      ahead.sort(Comparator.comparingInt(BookTabs.Tab::start));
      passed.sort(Comparator.comparingInt(BookTabs.Tab::start).reversed());
      Map<DragonClass, Integer> levels = new EnumMap<>(DragonClass.class);
      place(ahead, true, levels);
      place(passed, false, levels);
      return levels;
   }

   private static void place(List<BookTabs.Tab> side, boolean right, Map<DragonClass, Integer> levels) {
      List<List<BookTabs.Tab>> groups = new ArrayList<>();

      for (BookTabs.Tab tab : side) {
         groups.add(new ArrayList<>(List.of(tab)));
      }

      while (groups.size() > 5) {
         int best = 0;
         int fewest = Integer.MAX_VALUE;

         for (int i = 0; i + 1 < groups.size(); i++) {
            List<BookTabs.Tab> near = groups.get(i);
            BookTabs.Tab between = right ? near.get(near.size() - 1) : groups.get(i + 1).get(0);
            int pages = ClassProgress.species(between.dragonClass()).size();
            if (pages <= fewest) {
               fewest = pages;
               best = i;
            }
         }

         groups.get(best).addAll(groups.remove(best + 1));
      }

      for (int i = 0; i < groups.size(); i++) {
         for (BookTabs.Tab tab : groups.get(i)) {
            levels.put(tab.dragonClass(), i + 1);
         }
      }
   }

   public void render(GuiGraphics graphics, List<BookTabs.Tab> tabs, int current, int leftX, int topY, int mouseX, int mouseY) {
      this.hovered = null;
      this.boxes.clear();
      Map<DragonClass, Integer> levels = levels(tabs, current);

      for (BookTabs.Tab tab : tabs) {
         DragonClass c = tab.dragonClass();
         boolean right = right(tab, current);
         ON_RIGHT.put(c, right);
         String level = level(right, levels.get(c));
         String box = slot(c, right);
         int targetX = EDITOR.getX(level);
         EDITOR.get(box).x = targetX;
         Float shown = this.shownX.get(c);
         boolean sameSide = this.shownRight.get(c) != null && this.shownRight.get(c) == right;
         float x = shown != null && sameSide ? shown + (targetX - shown) * 0.25F : targetX;
         this.shownX.put(c, x);
         this.shownRight.put(c, right);
         int[] clip = new int[]{leftX + Math.round(x), topY + EDITOR.getY(box), EDITOR.getW(level), EDITOR.getH(box)};
         this.boxes.put(c, clip);
         if (current != 0) {
            boolean over = !BookLayoutEditor.ENABLED && BookRenderHelper.isMouseOver(mouseX, mouseY, clip[0], clip[1], clip[2], clip[3]);
            if (over) {
               this.hovered = tab;
            }

            float ext = this.out.getOrDefault(c, 0.0F);
            ext += ((over ? 1.0F : 0.0F) - ext) * 0.35F;
            this.out.put(c, ext);
            int shift = Math.round(6.0F * ext) * (right ? 1 : -1);
            BookLayoutEditor.Slot art = EDITOR.get(box + "_ART");
            int artX = clip[0] + art.x + shift;
            int artY = clip[1] + art.y;
            int v = ClassProgress.dye(ClassProgress.completion(c)) * 22;
            int sheetH = 130;
            graphics.enableScissor(clip[0], clip[1], clip[0] + clip[2], clip[1] + clip[3]);
            if (right) {
               graphics.blit(ribbon(c), artX, artY, 34, 20, 34.0F, v, -34, 20, 34, sheetH);
            } else {
               graphics.blit(ribbon(c), artX, artY, 34, 20, 0.0F, v, 34, 20, 34, sheetH);
            }

            BookLayoutEditor.Slot icon = EDITOR.get(box + "_ICON");
            ClassProgress.drawIcon(graphics, c, clip[0] + icon.x + shift, clip[1] + icon.y, Math.min(icon.w, icon.h));
            BookDepth.drawOver(graphics, Math.round(EDITOR.getScale(level)), leftX, topY);
            graphics.disableScissor();
         }
      }
   }

   public void renderTooltip(GuiGraphics graphics, Font font, int mouseX, int mouseY) {
      if (this.hovered != null) {
         float done = ClassProgress.completion(this.hovered.dragonClass());
         BookRenderHelper.drawPanel(
            graphics,
            font,
            List.of(
               ClassProgress.name(this.hovered.dragonClass()),
               Component.translatable("book.bookofdragons.tab.complete", new Object[]{ClassProgress.percent(done)})
            ),
            mouseX,
            mouseY,
            160,
            4144959
         );
      }
   }

   public int clicked(List<BookTabs.Tab> tabs, int current, int leftX, int topY, double mouseX, double mouseY) {
      if (current == 0) {
         return -1;
      }

      for (BookTabs.Tab tab : tabs) {
         int[] box = this.boxes.get(tab.dragonClass());
         if (box != null && BookRenderHelper.isMouseOver(mouseX, mouseY, box[0], box[1], box[2], box[3])) {
            Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.BOOK_PAGE_TURN, 1.0F));
            return tab.start();
         }
      }

      return -1;
   }

   static {
      for (int k = 1; k <= 5; k++) {
         int y = -12 - 10 * (k - 1);
         EDITOR.add(level(true, k), "Right level " + k + " (across; wheel: depth)", 1077136639, 3394815, false, true, 361 + 4 * (k - 1), y, 32, 8);
         EDITOR.add(level(false, k), "Left level " + k + " (across; wheel: depth)", 1077136639, 3394815, false, true, -9 - 4 * (k - 1), y, 32, 8);
         EDITOR.counted(level(true, k), 0, 7, k);
         EDITOR.counted(level(false, k), 0, 7, k);
      }

      for (int i = 0; i < ClassProgress.ORDER.size(); i++) {
         DragonClass c = ClassProgress.ORDER.get(i);
         String name = c.name().charAt(0) + c.name().substring(1).toLowerCase(Locale.ROOT);
         int y = 26 + i * 30;
         String right = slot(c, true);
         String left = slot(c, false);
         EDITOR.add(right, name + " ribbon height (right)", 1090497024, 16755200, false, true, 361, y, 32, 20);
         EDITOR.add(left, name + " ribbon height (left)", 1090497024, 16755200, false, true, -9, y, 32, 20);
         EDITOR.addChild(right + "_ART", right, name + " ribbon (right)", 3381759, false, -8, 0, 34, 20);
         EDITOR.addChild(left + "_ART", left, name + " ribbon (left)", 3381759, false, 6, 0, 34, 20);
         EDITOR.addChild(right + "_ICON", right, name + " icon (right)", 16724940, true, 0, 2, 16, 16);
         EDITOR.addChild(left + "_ICON", left, name + " icon (left)", 16724940, true, 16, 2, 16, 16);
      }

      BookLayoutStore.attach("tabs", EDITOR);
      ON_RIGHT = new EnumMap<>(DragonClass.class);
   }

   public record Tab(DragonClass dragonClass, int start) {
   }
}
