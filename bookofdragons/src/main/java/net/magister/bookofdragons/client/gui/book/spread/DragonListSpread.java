package net.magister.bookofdragons.client.gui.book.spread;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import net.magister.bookofdragons.client.gui.book.BookLayoutEditor;
import net.magister.bookofdragons.client.gui.book.BookManager;
import net.magister.bookofdragons.client.gui.book.BookRenderHelper;
import net.magister.bookofdragons.client.gui.book.BookText;
import net.magister.bookofdragons.client.gui.book.ClassProgress;
import net.magister.bookofdragons.client.gui.book.ClientDragonRegistry;
import net.magister.bookofdragons.entity.data.DragonNames;
import net.magister.bookofdragons.entity.data.DragonType;
import net.magister.bookofdragons.network.message.SyncTamedDragonsMessage;
import net.magister.liberanimalium.body.stats.BondLevel;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;

public class DragonListSpread extends EditableBookSpread {
   public static final int PER_PAGE = 5;
   public static final int PER_SPREAD = 10;
   private static final int HEAD = 32;
   private static final int CLASS_ICON = 16;
   private static final int HOVER = 553648127;
   private static final int[] BOND_COLORS = new int[]{6050634, 8019002, 3104650, 3045946, 11565584};
   private static final int DEVOTED = 12600858;
   private static final BookLayoutEditor EDITOR = new BookLayoutEditor();
   private final BookManager book;
   private final int spreadIndex;
   private SyncTamedDragonsMessage.TamedDragonRecord hovered;

   public DragonListSpread(BookManager book, int spreadIndex) {
      super(EDITOR);
      this.book = book;
      this.spreadIndex = spreadIndex;
   }

   private static List<SyncTamedDragonsMessage.TamedDragonRecord> dragons() {
      List<SyncTamedDragonsMessage.TamedDragonRecord> out = new ArrayList<>(ClientDragonRegistry.getDragons());
      out.sort(
         Comparator.comparingLong(SyncTamedDragonsMessage.TamedDragonRecord::tamedAt)
            .reversed()
            .thenComparing(SyncTamedDragonsMessage.TamedDragonRecord::displayName)
      );
      return out;
   }

   @Override
   public void render(GuiGraphics graphics, Font font, int leftX, int topY, int mouseX, int mouseY, float partialTick) {
      if (this.spreadIndex == 0) {
         this.drawTitle(graphics, font, false, Component.translatable("book.bookofdragons.chapter.album"), true, leftX, topY);
      }

      List<SyncTamedDragonsMessage.TamedDragonRecord> dragons = dragons();
      this.hovered = null;
      int first = this.spreadIndex * 10;
      this.page(graphics, font, "LP_LIST", dragons, first, leftX, topY, mouseX, mouseY);
      this.page(graphics, font, "RP_LIST", dragons, first + 5, leftX, topY, mouseX, mouseY);
      if (this.spreadIndex == 0 && dragons.isEmpty()) {
         this.drawText(graphics, font, "LP_LIST", List.of(Component.translatable("book.bookofdragons.dragons.empty")), 4144959, leftX, topY);
      }

      this.renderEditorOverlay(graphics, font, leftX, topY, mouseX, mouseY);
      if (this.hovered != null) {
         this.drawPanel(graphics, font, card(this.hovered), mouseX, mouseY, 160, 4144959);
      }
   }

   private void page(
      GuiGraphics graphics,
      Font font,
      String list,
      List<SyncTamedDragonsMessage.TamedDragonRecord> dragons,
      int first,
      int leftX,
      int topY,
      int mouseX,
      int mouseY
   ) {
      for (int i = 0; i < 5 && first + i < dragons.size(); i++) {
         SyncTamedDragonsMessage.TamedDragonRecord record = dragons.get(first + i);
         int[] row = this.row(list, i, leftX, topY);
         if (!BookLayoutEditor.ENABLED && BookRenderHelper.isMouseOver(mouseX, mouseY, row[0], row[1], row[2], row[3])) {
            graphics.fill(row[0] - 2, row[1], row[0] + row[2] + 2, row[1] + row[3], 553648127);
            this.hovered = record;
         }

         DragonType type = typeOf(record);
         int y = row[1] + (row[3] - 32) / 2;
         if (type != null) {
            graphics.blit(head(type), row[0], y, 32, 32, 0.0F, 0.0F, 64, 64, 64, 64);
            ClassProgress.drawIcon(graphics, ClassProgress.classOf(type), row[0] + row[2] - 16, row[1] + (row[3] - 16) / 2, 16);
         }

         int nameX = row[0] + 32 + 4;
         int nameW = row[2] - 32 - 4 - 16 - 4;
         String text = DragonNames.shown(record.displayName()).getString();

         Component name;
         for (name = BookText.literal(text); font.width(name) > nameW && text.length() > 1; name = BookText.literal(text)) {
            text = text.substring(0, text.length() - 1);
         }

         graphics.drawString(font, name, nameX, row[1] + (row[3] - 7) / 2, bondColor(record), false);
      }
   }

   private int[] row(String list, int index, int leftX, int topY) {
      int h = this.eh(list, 185) / 5;
      return new int[]{this.sx(list, leftX), this.sy(list, topY) + index * h, this.ew(list, 156), h};
   }

   private static DragonType typeOf(SyncTamedDragonsMessage.TamedDragonRecord record) {
      try {
         return DragonType.valueOf(record.type());
      } catch (IllegalArgumentException e) {
         return null;
      }
   }

   private static ResourceLocation head(DragonType type) {
      String id = BuiltInRegistries.ENTITY_TYPE.getKey(type.getEntityType()).getPath();
      return new ResourceLocation("bookofdragons", "textures/item/" + id + "_spawn_egg.png");
   }

   private static BondLevel bond(int affection) {
      BondLevel out = BondLevel.WILD;

      for (BondLevel level : BondLevel.values()) {
         if (affection >= level.getMinAffection()) {
            out = level;
         }
      }

      return out;
   }

   public static int bondColor(int affection) {
      return affection >= 1000 ? 12600858 : BOND_COLORS[Math.min(BOND_COLORS.length - 1, bond(affection).ordinal())];
   }

   public static Component bondName(int affection) {
      String key = affection >= 1000 ? "devoted" : bond(affection).name().toLowerCase(Locale.ROOT);
      return Component.translatable("book.bookofdragons.bond." + key).withStyle(s -> s.withColor(bondColor(affection)));
   }

   private static int bondColor(SyncTamedDragonsMessage.TamedDragonRecord record) {
      return bondColor(record.affection());
   }

   private static List<Component> card(SyncTamedDragonsMessage.TamedDragonRecord record) {
      List<Component> out = new ArrayList<>();
      out.add(DragonNames.shown(record.displayName()));
      DragonType type = typeOf(record);
      if (type != null) {
         out.add(Component.translatable(type.getEntityType().getDescriptionId()));
      }

      out.add(bondName(record.affection()));
      return out;
   }

   @Override
   protected boolean handleMouseClicked(double mouseX, double mouseY, int button, int leftX, int topY) {
      if (button == 0 && this.hovered != null) {
         this.book.openDragon(this.hovered.uuid());
         Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.BOOK_PAGE_TURN, 1.0F));
         return true;
      } else {
         return false;
      }
   }

   @Override
   public void left() {
      this.hovered = null;
   }

   static {
      EDITOR.add("LP_LIST", "Dragons (left)", 4246630, false, 24, 40, 156, 165, 1.0F);
      EDITOR.add("RP_LIST", "Dragons (right)", 4246630, true, 2, 14, 156, 165, 1.0F);
      stored("dragons", EDITOR);
   }
}
