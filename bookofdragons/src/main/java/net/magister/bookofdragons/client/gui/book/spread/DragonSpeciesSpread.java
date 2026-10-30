package net.magister.bookofdragons.client.gui.book.spread;

import java.util.List;
import java.util.Locale;
import net.magister.bookofdragons.client.gui.book.BookLayoutEditor;
import net.magister.bookofdragons.client.gui.book.BookRenderHelper;
import net.magister.bookofdragons.client.gui.book.BookText;
import net.magister.bookofdragons.client.gui.book.BookTips;
import net.magister.bookofdragons.client.gui.book.ClassProgress;
import net.magister.bookofdragons.client.gui.book.DragonStatGrader;
import net.magister.bookofdragons.client.gui.book.data.FoodIcon;
import net.magister.bookofdragons.client.gui.book.data.HabitatIcon;
import net.magister.bookofdragons.client.gui.book.data.SpeciesFacts;
import net.magister.bookofdragons.entity.base.dragon.DragonClass;
import net.magister.bookofdragons.entity.data.DragonType;
import net.magister.liberanimalium.body.data.FaunaWeightClass;
import net.magister.liberanimalium.body.gameplay.SpeciesGameplayRegistry;
import net.magister.liberanimalium.body.stats.PrimaryAttribute;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

public class DragonSpeciesSpread extends EditableBookSpread {
   private static final int PANEL_WIDTH = 200;
   private static final int ICON_HOVER = 822083583;
   private final DragonType type;
   private String panel;
   private int panelX;
   private int panelY;
   private static final int LP_PORTRAIT_X = 104;
   private static final int LP_PORTRAIT_Y = 39;
   private static final int LP_PORTRAIT_SIZE = 75;
   private static final int LP_STATS_X = 25;
   private static final int LP_STATS_Y = 38;
   private static final int LP_STATS_W = 76;
   private static final int LP_STATS_H = 92;
   private static final int LP_STATS_TEXT_X = 36;
   private static final int LP_STATS_TEXT_Y = 47;
   private static final int LP_STATS_LINE_H = 13;
   private static final int LP_CLASS_X = 104;
   private static final int LP_CLASS_Y = 113;
   private static final int LP_WEIGHT_X = 104;
   private static final int LP_WEIGHT_Y = 123;
   private static final int LP_DESC_X = 23;
   private static final int LP_DESC_Y = 132;
   private static final int LP_DESC_W = 167;
   private static final int LP_DESC_H = 96;
   private static final int LP_DESC_TEXT_X = 28;
   private static final int LP_DESC_TEXT_Y = 141;
   private static final int LP_DESC_TEXT_W = 155;
   private static final int RP_HAB_OVL_X = -6;
   private static final int RP_HAB_OVL_Y = 9;
   private static final int RP_HAB_OVL_W = 100;
   private static final int RP_HAB_H = 106;
   private static final int RP_HAB_ICON_X = 93;
   private static final int RP_HAB_ICON_Y = 19;
   private static final int RP_HAB_TEXT_X = 2;
   private static final int RP_HAB_TEXT_Y = 20;
   private static final int RP_HAB_TEXT_W = 86;
   private static final int RP_DIET_OVL_X = 61;
   private static final int RP_DIET_OVL_Y = 116;
   private static final int RP_DIET_OVL_W = 90;
   private static final int RP_DIET_OVL_H = 105;
   private static final int RP_DIET_ICON_X = -3;
   private static final int RP_DIET_ICON_Y = 116;
   private static final int RP_DIET_FAV_ICON_X = 11;
   private static final int RP_DIET_FAV_ICON_Y = 167;
   private static final int RP_DIET_TEXT_X = 70;
   private static final int RP_DIET_TEXT_Y = 120;
   private static final int RP_DIET_TEXT_W = 75;
   private static final ResourceLocation TEX_STATS_OVERLAY = new ResourceLocation("bookofdragons", "textures/gui/book/overlaystats.png");
   private static final ResourceLocation TEX_MAINTEXT_OVERLAY = new ResourceLocation("bookofdragons", "textures/gui/book/overlaymaintext.png");
   private static final ResourceLocation TEX_HABITAT_OVERLAY = new ResourceLocation(
      "bookofdragons", "textures/gui/book/overlayhabitattext.png"
   );
   private static final ResourceLocation TEX_DIET_OVERLAY = new ResourceLocation("bookofdragons", "textures/gui/book/overlaydiettext.png");
   private static final ResourceLocation TEX_SMUDGE_HABITAT = new ResourceLocation("bookofdragons", "textures/gui/book/smudgehabitat.png");
   private static final ResourceLocation TEX_SMUDGE_FOOD = new ResourceLocation("bookofdragons", "textures/gui/book/smudgefood.png");
   private static final ResourceLocation TEX_BANNER = new ResourceLocation("bookofdragons", "textures/gui/book/classbanners.png");
   private static final int BANNER_W = 20;
   private static final int BANNER_H = 34;
   private static final int BANNER_STEP = 22;
   private static final PrimaryAttribute[] STAT_ORDER = new PrimaryAttribute[]{
      PrimaryAttribute.CON, PrimaryAttribute.ATH, PrimaryAttribute.AGI, PrimaryAttribute.PRW, PrimaryAttribute.POT, PrimaryAttribute.VIG
   };
   private static final BookLayoutEditor SHARED_EDITOR = new BookLayoutEditor();

   public DragonSpeciesSpread(DragonType type) {
      super(SHARED_EDITOR);
      this.type = type;
   }

   @Override
   public void render(GuiGraphics graphics, Font font, int leftX, int topY, int mouseX, int mouseY, float partialTick) {
      int tier = SpeciesFacts.tier(this.type);
      this.renderLeftPage(graphics, font, leftX, topY, tier);
      this.renderRightPage(graphics, font, leftX, topY, tier, mouseX, mouseY);
      this.renderEditorOverlay(graphics, font, leftX, topY, mouseX, mouseY);
      if (this.panel != null) {
         List<Component> lines = this.panel.equals("habitat") ? SpeciesFacts.habitatData(this.type, tier) : SpeciesFacts.dietData(this.type, tier);
         this.drawPanel(graphics, font, lines, this.panelX, this.panelY, 200, 4144959);
      } else if (BookLayoutEditor.ENABLED || !this.over("LP_CLASS", mouseX, mouseY, leftX, topY) && !this.over("LP_CLASS_BANNER", mouseX, mouseY, leftX, topY)) {
         if (!BookLayoutEditor.ENABLED) {
            PrimaryAttribute stat = this.statAt(mouseX, mouseY, leftX, topY);
            if (stat != null) {
               this.drawPanel(graphics, font, BookTips.stat(stat.name()), mouseX, mouseY, 200, 4144959);
            }
         }
      } else {
         this.drawPanel(
            graphics,
            font,
            List.of(this.className(), Component.translatable("book.bookofdragons.progress", new Object[]{ClassProgress.percent(SpeciesFacts.progress(this.type))})),
            mouseX,
            mouseY,
            200,
            4144959
         );
      }
   }

   private PrimaryAttribute statAt(int mouseX, int mouseY, int leftX, int topY) {
      int x = leftX + this.ex("LP_STATS_TEXT", 36);
      int y = topY + this.ey("LP_STATS_TEXT", 47);
      int w = this.ew("LP_STATS_TEXT", 56);

      for (int i = 0; i < STAT_ORDER.length; i++) {
         if (BookRenderHelper.isMouseOver(mouseX, mouseY, x, y + i * 13 - 2, w, 13)) {
            return STAT_ORDER[i];
         }
      }

      return null;
   }

   private void renderLeftPage(GuiGraphics graphics, Font font, int leftX, int topY, int tier) {
      this.drawTitle(graphics, font, false, Component.translatable(this.type.getEntityType().getDescriptionId()), true, leftX, topY);
      this.renderPortrait(graphics, leftX + this.ex("LP_PORTRAIT", 104), topY + this.ey("LP_PORTRAIT", 39), this.ew("LP_PORTRAIT", 75), tier >= 1);
      BookRenderHelper.renderOverlayTexture(
         graphics, TEX_STATS_OVERLAY, leftX + this.ex("LP_STATS", 25), topY + this.ey("LP_STATS", 38), this.ew("LP_STATS", 76), this.eh("LP_STATS", 92)
      );
      this.renderStats(graphics, font, leftX + this.ex("LP_STATS_TEXT", 36), topY + this.ey("LP_STATS_TEXT", 47), tier);
      int dye = ClassProgress.dye(SpeciesFacts.progress(this.type));
      graphics.blit(TEX_BANNER, this.sx("LP_CLASS_BANNER", leftX), this.sy("LP_CLASS_BANNER", topY), dye * 22, 0.0F, 20, 34, 130, 34);
      ClassProgress.drawIcon(graphics, this.dragonClass(), this.sx("LP_CLASS", leftX), this.sy("LP_CLASS", topY), this.ew("LP_CLASS", 16));
      FaunaWeightClass weightClass = SpeciesGameplayRegistry.weightOf(this.type.getSerializedName());
      graphics.drawString(
         font,
         BookText.of(
            Component.translatable(
               "book.bookofdragons.label.weight_value",
               new Object[]{Component.translatable("book.bookofdragons.weight." + weightClass.name().toLowerCase(Locale.ROOT))}
            )
         ),
         leftX + this.ex("LP_WEIGHT", 104),
         topY + this.ey("LP_WEIGHT", 123),
         4144959,
         false
      );
      BookRenderHelper.renderOverlayTexture(
         graphics, TEX_MAINTEXT_OVERLAY, leftX + this.ex("LP_DESC", 23), topY + this.ey("LP_DESC", 132), this.ew("LP_DESC", 167), this.eh("LP_DESC", 96)
      );
      String descKey = tier >= 1
         ? "book.bookofdragons.species." + this.type.getSerializedName() + ".description.tamed"
         : "book.bookofdragons.species." + this.type.getSerializedName() + ".description.wild";
      this.drawText(graphics, font, "LP_DESC_TEXT", List.of(Component.translatable(descKey)), 4144959, leftX, topY);
   }

   private void renderStats(GuiGraphics graphics, Font font, int baseX, int baseY, int tier) {
      for (int i = 0; i < STAT_ORDER.length; i++) {
         PrimaryAttribute attr = STAT_ORDER[i];
         int ty = baseY + i * 13;
         if (tier >= 2) {
            char grade = DragonStatGrader.grade(this.type, attr);
            graphics.drawString(font, BookText.literal(attr.name() + ": " + grade), baseX, ty, DragonStatGrader.gradeColor(grade), false);
         } else {
            graphics.drawString(font, BookText.literal(attr.name() + ": ???"), baseX, ty, 8421504, false);
         }
      }
   }

   private void renderRightPage(GuiGraphics graphics, Font font, int leftX, int topY, int tier, int mouseX, int mouseY) {
      int rpX = leftX + this.getRpOffset();
      BookRenderHelper.renderOverlayTexture(
         graphics,
         TEX_HABITAT_OVERLAY,
         rpX + this.ex("RP_HAB_OVL", -6),
         topY + this.ey("RP_HAB_OVL", 9),
         this.ew("RP_HAB_OVL", 100),
         this.eh("RP_HAB_OVL", 106)
      );
      BookRenderHelper.renderOverlayTexture(
         graphics,
         TEX_DIET_OVERLAY,
         rpX + this.ex("RP_DIET_OVL", 61),
         topY + this.ey("RP_DIET_OVL", 116),
         this.ew("RP_DIET_OVL", 90),
         this.eh("RP_DIET_OVL", 105)
      );
      if (tier >= 1) {
         BookRenderHelper.renderOverlayTexture(
            graphics,
            HabitatIcon.forDragon(this.type).getTexture(),
            rpX + this.ex("RP_HAB_ICON", 93),
            topY + this.ey("RP_HAB_ICON", 19),
            this.ew("RP_HAB_ICON", 62),
            this.eh("RP_HAB_ICON", 62)
         );
         FoodIcon generalFood = FoodIcon.generalFoodForDragon(this.type);
         BookRenderHelper.renderOverlayTexture(
            graphics,
            generalFood.getTexture(),
            rpX + this.ex("RP_DIET_ICON", -3),
            topY + this.ey("RP_DIET_ICON", 116),
            this.ew("RP_DIET_ICON", generalFood.getWidth()),
            this.eh("RP_DIET_ICON", generalFood.getHeight())
         );
         if (SpeciesFacts.knowsFavorite(this.type)) {
            FoodIcon favFood = FoodIcon.forDragon(this.type);
            if (favFood != generalFood) {
               BookRenderHelper.renderOverlayTexture(
                  graphics,
                  favFood.getTexture(),
                  rpX + this.ex("RP_DIET_FAV_ICON", 11),
                  topY + this.ey("RP_DIET_FAV_ICON", 167),
                  this.ew("RP_DIET_FAV_ICON", favFood.getWidth()),
                  this.eh("RP_DIET_FAV_ICON", favFood.getHeight())
               );
            }
         }

         this.drawItems(graphics, "RP_DIET_ITEMS", SpeciesFacts.knownFoods(this.type), leftX, topY);
      } else {
         BookRenderHelper.renderOverlayTexture(
            graphics,
            TEX_SMUDGE_HABITAT,
            rpX + this.ex("RP_HAB_ICON", 93),
            topY + this.ey("RP_HAB_ICON", 19),
            this.ew("RP_HAB_ICON", 62),
            this.eh("RP_HAB_ICON", 62)
         );
         BookRenderHelper.renderOverlayTexture(
            graphics,
            TEX_SMUDGE_FOOD,
            rpX + this.ex("RP_DIET_ICON", -3),
            topY + this.ey("RP_DIET_ICON", 116),
            this.ew("RP_DIET_ICON", 62),
            this.eh("RP_DIET_ICON", 62)
         );
      }

      String hovered = BookLayoutEditor.ENABLED ? null : this.iconSlotAt(mouseX, mouseY, leftX, topY, tier);
      if (hovered != null) {
         int x = this.sx(hovered, leftX);
         int y = this.sy(hovered, topY);
         graphics.fill(x, y, x + this.ew(hovered, 0), y + this.eh(hovered, 0), 822083583);
      }

      this.layoutEditor.get("RP_HAB_TITLE").y = titleY(true);
      this.drawCentered(graphics, font, "RP_HAB_TITLE", Component.translatable("book.bookofdragons.field.title.habitat"), 2039583, leftX, topY);
      this.drawJournal(graphics, font, "RP_HAB_TEXT", SpeciesFacts.habitatJournal(this.type, tier), 4144959, leftX, topY);
      this.drawCentered(graphics, font, "RP_DIET_TITLE", Component.translatable("book.bookofdragons.field.title.diet"), 2039583, leftX, topY);
      this.drawJournal(graphics, font, "RP_DIET_TEXT", SpeciesFacts.dietJournal(this.type, tier), 4144959, leftX, topY);
   }

   private String iconSlotAt(double mouseX, double mouseY, int leftX, int topY, int tier) {
      if (this.over("RP_HAB_ICON", mouseX, mouseY, leftX, topY)) {
         return "RP_HAB_ICON";
      } else if (this.over("RP_DIET_ICON", mouseX, mouseY, leftX, topY)) {
         return "RP_DIET_ICON";
      } else {
         return tier >= 1
               && SpeciesFacts.knowsFavorite(this.type)
               && FoodIcon.forDragon(this.type) != FoodIcon.generalFoodForDragon(this.type)
               && this.over("RP_DIET_FAV_ICON", mouseX, mouseY, leftX, topY)
            ? "RP_DIET_FAV_ICON"
            : null;
      }
   }

   private boolean over(String slot, double mouseX, double mouseY, int leftX, int topY) {
      return BookRenderHelper.isMouseOver(mouseX, mouseY, this.sx(slot, leftX), this.sy(slot, topY), this.ew(slot, 0), this.eh(slot, 0));
   }

   @Override
   protected boolean handleMouseClicked(double mouseX, double mouseY, int button, int leftX, int topY) {
      if (button != 0) {
         return false;
      } else {
         String slot = this.iconSlotAt(mouseX, mouseY, leftX, topY, SpeciesFacts.tier(this.type));
         String hit = slot == null ? null : (slot.equals("RP_HAB_ICON") ? "habitat" : "diet");
         if (hit != null && !hit.equals(this.panel)) {
            this.panel = hit;
            this.panelX = (int)mouseX;
            this.panelY = (int)mouseY;
            return true;
         } else {
            boolean open = this.panel != null;
            this.panel = null;
            return open || hit != null;
         }
      }
   }

   @Override
   public void left() {
      this.panel = null;
   }

   private DragonClass dragonClass() {
      return DragonClass.ofSpecies(this.type.getSerializedName());
   }

   private Component className() {
      return ClassProgress.name(this.dragonClass());
   }

   private void renderPortrait(GuiGraphics graphics, int x, int y, int size, boolean discovered) {
      String id = this.type.getSerializedName().replace("_", "");
      String suffix = discovered ? "" : "_undiscovered";
      ResourceLocation tex = new ResourceLocation("bookofdragons", "textures/gui/book/dragon_portrait_" + id + suffix + ".png");
      BookRenderHelper.renderScaledTexture(graphics, tex, x, y, size, size, 128, 128);
   }

   static {
      SHARED_EDITOR.add("LP_PORTRAIT", "Portrait", 1073794048, 52224, false, false, 104, 39, 75, 75);
      SHARED_EDITOR.add("LP_STATS", "Stats overlay", 1073768191, 26367, false, true, 25, 38, 76, 92);
      SHARED_EDITOR.add("LP_STATS_TEXT", "Stats text", 1073785599, 43775, false, true, 36, 47, 56, 74);
      SHARED_EDITOR.add("LP_CLASS_BANNER", "Class banner", 1090505728, 16763904, false, false, 102, 109, 20, 34);
      SHARED_EDITOR.add("LP_CLASS", "Class icon", 1090518784, 13421568, false, false, 104, 113, 16, 16);
      SHARED_EDITOR.add("LP_WEIGHT", "Weight label", 1090497024, 13404160, false, false, 104, 123, 80, 9);
      SHARED_EDITOR.add("LP_DESC", "Desc overlay", 1090479616, 16737792, false, true, 23, 132, 167, 96);
      SHARED_EDITOR.addText("LP_DESC_TEXT", "Desc text", 16759654, false, 28, 141, 155, 80, 1.0F);
      SHARED_EDITOR.add("RP_HAB_OVL", "Habitat frame", 1084883199, 11141375, true, true, -6, 9, 100, 106);
      SHARED_EDITOR.add("RP_HAB_ICON", "Habitat icon", 1090453640, 16711816, true, false, 93, 19, 62, 62);
      SHARED_EDITOR.addText("RP_HAB_TITLE", "Habitat title", 11162880, true, 2, 12, 86, 10, 1.0F);
      SHARED_EDITOR.addText("RP_HAB_TEXT", "Habitat", 16746700, true, 2, 24, 86, 88, 0.9F);
      SHARED_EDITOR.add("RP_DIET_OVL", "Diet frame", 1079312520, 8913100, true, true, 61, 116, 90, 105);
      SHARED_EDITOR.add("RP_DIET_ICON", "Diet icon (general)", 1090462208, 16720384, true, false, -3, 116, 62, 62);
      SHARED_EDITOR.add("RP_DIET_FAV_ICON", "Diet icon (favorite)", 1090475264, 16733440, true, false, 11, 167, 52, 50);
      SHARED_EDITOR.addText("RP_DIET_TITLE", "Diet title", 11162880, true, 70, 120, 75, 10, 1.0F);
      SHARED_EDITOR.addText("RP_DIET_TEXT", "Diet", 16742229, true, 70, 132, 75, 60, 0.9F);
      SHARED_EDITOR.add("RP_DIET_ITEMS", "Foods", 13378116, true, 70, 194, 80, 18, 1.0F);
      stored("species", SHARED_EDITOR);
   }
}
