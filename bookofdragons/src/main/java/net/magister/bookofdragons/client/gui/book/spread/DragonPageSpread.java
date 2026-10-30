package net.magister.bookofdragons.client.gui.book.spread;

import com.mojang.blaze3d.systems.RenderSystem;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import org.jetbrains.annotations.Nullable;
import net.magister.bookofdragons.client.gui.book.BookLayoutEditor;
import net.magister.bookofdragons.client.gui.book.BookRenderHelper;
import net.magister.bookofdragons.client.gui.book.BookText;
import net.magister.bookofdragons.client.gui.book.BookTips;
import net.magister.bookofdragons.client.gui.book.ClientDragonPages;
import net.magister.bookofdragons.client.gui.book.ClientDragonRegistry;
import net.magister.bookofdragons.client.gui.book.DragonPortrait;
import net.magister.bookofdragons.client.gui.book.DragonStatGrader;
import net.magister.bookofdragons.client.gui.book.RadarGraph;
import net.magister.bookofdragons.client.gui.book.data.SpeciesFacts;
import net.magister.bookofdragons.entity.base.dragon.DragonBase;
import net.magister.bookofdragons.entity.base.dragon.GrowthStage;
import net.magister.bookofdragons.entity.data.DragonNames;
import net.magister.bookofdragons.entity.data.DragonType;
import net.magister.bookofdragons.network.ControlNetwork;
import net.magister.bookofdragons.network.message.DragonPageMessage;
import net.magister.bookofdragons.network.message.GiveDragonMessage;
import net.magister.bookofdragons.network.message.OpenDragonGUIFromBookMessage;
import net.magister.bookofdragons.network.message.ReleaseDragonMessage;
import net.magister.bookofdragons.network.message.SyncTamedDragonsMessage;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

public class DragonPageSpread extends EditableBookSpread {
   private static final String[] STATS = new String[]{"CON", "ATH", "AGI", "PRW", "POT", "VIG"};
   private static final double OPEN_RANGE_SQ = 100.0;
   private static final int INK = 4144959;
   private static final int FADED = 9075298;
   private static final ResourceLocation NEED_ICONS = new ResourceLocation("bookofdragons", "textures/gui/book/needs.png");
   private static final int ICON = 9;
   private static final int[] STATE_COLORS = new int[]{3045946, 7035438, 10511914, 10035230};
   private static final int HOVER = 822083583;
   private static final ResourceLocation BACKDROP = new ResourceLocation("bookofdragons", "textures/gui/book/iconbacking.png");
   private static final int BACKDROP_SIZE = 88;
   private static final ResourceLocation UNDERLINE_2 = new ResourceLocation("bookofdragons", "textures/gui/book/underline2.png");
   private static final ResourceLocation UNDERLINE_3 = new ResourceLocation("bookofdragons", "textures/gui/book/underline3.png");
   private static final String[] NEEDS = new String[]{"health", "hunger", "stamina", "rest"};
   private static final BookLayoutEditor EDITOR = new BookLayoutEditor();
   private final UUID dragon;
   private static final int COMPASS = 26;
   private static final int NEEDLE_CELL = 2;
   private static final String[] POINTS = new String[]{"north", "north_east", "east", "south_east", "south", "south_west", "west", "north_west"};
   private static final int TICKS_A_DAY = 24000;
   private static final long CONFIRM_MS = 3000L;
   private static final int WARN = 10035230;
   private long releaseArmed = Long.MIN_VALUE;
   private long giveArmed = Long.MIN_VALUE;
   private int giveIndex;

   public DragonPageSpread(UUID dragon) {
      super(EDITOR);
      this.dragon = dragon;
   }

   private SyncTamedDragonsMessage.TamedDragonRecord record() {
      for (SyncTamedDragonsMessage.TamedDragonRecord r : ClientDragonRegistry.getDragons()) {
         if (r.uuid().equals(this.dragon)) {
            return r;
         }
      }

      return null;
   }

   @Override
   public void render(GuiGraphics graphics, Font font, int leftX, int topY, int mouseX, int mouseY, float partialTick) {
      ClientDragonPages.ask(this.dragon);
      DragonPageMessage.Page page = ClientDragonPages.get(this.dragon);
      SyncTamedDragonsMessage.TamedDragonRecord record = this.record();
      Component name = DragonNames.shown(page != null ? page.name() : (record != null ? record.displayName() : null));
      DragonType type = typeOf(page != null ? page.type() : (record != null ? record.type() : null));
      int bond = page != null ? page.bond() : (record != null ? record.affection() : 0);
      this.drawTitle(graphics, font, false, name, true, leftX, topY);
      if (type != null) {
         BookRenderHelper.renderScaledTexture(
            graphics, BACKDROP, this.sx("LP_BACKDROP", leftX), this.sy("LP_BACKDROP", topY), this.ew("LP_BACKDROP", 88), this.eh("LP_BACKDROP", 88), 88, 88
         );
         this.line(graphics, UNDERLINE_2, "LP_UNDERLINE2", leftX, topY);
         this.line(graphics, UNDERLINE_3, "LP_UNDERLINE3", leftX, topY);
         DragonPortrait.draw(
            graphics,
            this.dragon,
            type,
            page != null ? page.stage() : 0,
            page != null ? page.genetics() : null,
            this.sx("LP_FIGURE", leftX),
            this.sy("LP_FIGURE", topY),
            this.ew("LP_FIGURE", 158),
            this.eh("LP_FIGURE", 110)
         );
         Component species = Component.translatable(type.getEntityType().getDescriptionId());
         Component second = (Component)(page != null
            ? GrowthStage.fromStageId(page.stage()).getDisplayName().copy().append(" · ").append(DragonListSpread.bondName(bond))
            : DragonListSpread.bondName(bond));
         this.renderInfo(graphics, font, List.of(species, second), leftX, topY);
      }

      this.renderStats(graphics, font, page, leftX, topY);
      this.renderCompass(graphics, font, page, leftX, topY, partialTick);
      this.drawTitle(graphics, font, true, Component.translatable("book.bookofdragons.dragon.temperament"), true, leftX, topY);
      List<Component> labels = new ArrayList<>();

      for (String trait : SpeciesFacts.TRAITS) {
         labels.add(Component.translatable("book.bookofdragons.trait." + trait));
      }

      if (page != null && page.temper().length == SpeciesFacts.TRAITS.size()) {
         RadarGraph.draw(
            graphics,
            font,
            labels,
            page.temper(),
            this.sx("RP_TRAITS", leftX),
            this.sy("RP_TRAITS", topY),
            this.ew("RP_TRAITS", 154),
            this.eh("RP_TRAITS", 80),
            this.es("RP_TRAITS"),
            4144959,
            1073741824,
            -2140452309,
            -9745877
         );
      }

      this.drawCentered(graphics, font, "RP_NEEDS_TITLE", Component.translatable("book.bookofdragons.dragon.needs"), 2039583, leftX, topY);
      this.line(graphics, "RP_NEEDS_LINE", leftX, topY);
      this.renderNeeds(graphics, font, page, leftX, topY);
      this.renderOpen(graphics, font, leftX, topY, mouseX, mouseY);
      this.renderHandOver(graphics, font, leftX, topY, mouseX, mouseY);
      this.renderEditorOverlay(graphics, font, leftX, topY, mouseX, mouseY);
      if (!BookLayoutEditor.ENABLED) {
         if (this.over("RP_GIVE", 46, mouseX, mouseY, leftX, topY)) {
            this.drawPanel(graphics, font, this.giveCard(), mouseX, mouseY, 170, 4144959);
         } else if (this.over("RP_RELEASE", 46, mouseX, mouseY, leftX, topY)) {
            this.drawPanel(
               graphics,
               font,
               List.of(Component.translatable("book.bookofdragons.dragon.release"), Component.translatable("book.bookofdragons.dragon.release.tip")),
               mouseX,
               mouseY,
               170,
               4144959
            );
         } else if (BookRenderHelper.isMouseOver(
            mouseX, mouseY, this.sx("LP_COMPASS", leftX), this.sy("LP_COMPASS", topY), this.ew("LP_COMPASS", 26), this.eh("LP_COMPASS", 26)
         )) {
            this.drawPanel(graphics, font, this.compassCard(page, partialTick), mouseX, mouseY, 170, 4144959);
         } else {
            int stat = this.statAt(font, mouseX, mouseY, leftX, topY);
            int trait = page != null && page.temper().length == SpeciesFacts.TRAITS.size()
               ? RadarGraph.labelAt(
                  font,
                  labels,
                  labels.size(),
                  this.sx("RP_TRAITS", leftX),
                  this.sy("RP_TRAITS", topY),
                  this.ew("RP_TRAITS", 154),
                  this.eh("RP_TRAITS", 80),
                  this.es("RP_TRAITS"),
                  mouseX,
                  mouseY
               )
               : -1;
            int need = page != null && page.live() ? this.needAt(font, mouseX, mouseY, leftX, topY) : -1;
            if (stat >= 0) {
               this.drawPanel(graphics, font, statCard(page, STATS[stat]), mouseX, mouseY, 170, 4144959);
            } else if (trait >= 0) {
               this.drawPanel(graphics, font, BookTips.trait(SpeciesFacts.TRAITS.get(trait)), mouseX, mouseY, 170, 4144959);
            } else if (need >= 0) {
               this.drawPanel(graphics, font, needCard(page, need), mouseX, mouseY, 170, 4144959);
            }
         }
      }
   }

   @Nullable
   private Vec3 target(DragonPageMessage.Page page, float partialTick) {
      Minecraft mc = Minecraft.getInstance();
      if (mc.player == null) {
         return null;
      } else {
         DragonBase body = ClientDragonPages.loaded(this.dragon);
         if (body != null) {
            return body.getPosition(partialTick);
         } else if (page != null && page.where().known()) {
            return !page.where().dimension().equals(mc.player.level().dimension().location().toString())
               ? null
               : new Vec3(page.where().x(), page.where().y(), page.where().z());
         } else {
            return null;
         }
      }
   }

   @Nullable
   private static String whereWord(DragonPageMessage.Page page, @Nullable Vec3 at) {
      Minecraft mc = Minecraft.getInstance();
      if (at != null && mc.player != null) {
         double dx = at.x - mc.player.getX();
         double dz = at.z - mc.player.getZ();
         double d = Math.sqrt(dx * dx + dz * dz);
         return d < 24.0 ? "close" : (d < 128.0 ? "near" : (d < 512.0 ? "far" : "very_far"));
      } else if (page == null) {
         return null;
      } else if (page.where().kept()) {
         return "kept";
      } else {
         return page.where().known() ? "other_world" : "lost";
      }
   }

   private void renderCompass(GuiGraphics graphics, Font font, DragonPageMessage.Page page, int leftX, int topY, float partialTick) {
      int x = this.sx("LP_COMPASS", leftX);
      int y = this.sy("LP_COMPASS", topY);
      int size = this.ew("LP_COMPASS", 26);
      BookRenderHelper.renderScaledTexture(graphics, BACKDROP, x, y, size, size, 88, 88);
      Vec3 at = this.target(page, partialTick);
      Minecraft mc = Minecraft.getInstance();
      if (at != null && mc.player != null) {
         Vec3 me = mc.player.getPosition(partialTick);
         float toward = (float)(Mth.atan2(-(at.x - me.x), at.z - me.z) * 180.0F / (float)Math.PI);
         float turn = Mth.wrapDegrees(toward - mc.player.getViewYRot(partialTick));
         needle(graphics, x + size / 2, y + size / 2, size / 2 - 3, turn);
      }

      String word = whereWord(page, at);
      if (word != null) {
         Component line = BookText.of(Component.translatable("book.bookofdragons.dragon.where." + word));
         graphics.drawString(font, line, x + (size - font.width(line)) / 2, y + size + 1, 9075298, false);
      }
   }

   private static void needle(GuiGraphics graphics, int cx, int cy, int r, float turn) {
      double a = Math.toRadians(-turn);
      double cos = Math.cos(a);
      double sin = Math.sin(a);

      for (int gy = -r; gy <= r; gy += 2) {
         for (int gx = -r; gx <= r; gx += 2) {
            double px = gx + 1.0;
            double py = gy + 1.0;
            double lx = px * cos - py * sin;
            double ly = px * sin + py * cos;
            int colour = needleAt(lx, ly, r);
            if (colour != 0) {
               graphics.fill(cx + gx, cy + gy, cx + gx + 2, cy + gy + 2, colour);
            }
         }
      }
   }

   private static int needleAt(double x, double y, int r) {
      double head = r * 0.45;
      if (y <= -r + head && y >= -r) {
         double half = (y + r) / head * 3.0;
         return Math.abs(x) <= half ? -12632257 : 0;
      } else if (Math.abs(x) > 1.0) {
         return 0;
      } else if (y < 0.0 && y > -r) {
         return -12632257;
      } else {
         return y >= 0.0 && y <= r - 2 ? -7701918 : 0;
      }
   }

   private List<Component> compassCard(DragonPageMessage.Page page, float partialTick) {
      List<Component> card = new ArrayList<>();
      card.add(Component.translatable("book.bookofdragons.dragon.where"));
      Vec3 at = this.target(page, partialTick);
      Minecraft mc = Minecraft.getInstance();
      if (at != null && mc.player != null) {
         double bearing = Math.toDegrees(Math.atan2(at.x - mc.player.getX(), -(at.z - mc.player.getZ())));
         int point = Math.floorMod(Math.round(bearing / 45.0), 8);
         card.add(
            Component.translatable(
               "book.bookofdragons.dragon.where.toward", new Object[]{Component.translatable("book.bookofdragons.dragon.where.dir." + POINTS[point])}
            )
         );
      }

      String word = whereWord(page, at);
      if (word != null) {
         card.add(Component.translatable("book.bookofdragons.dragon.where." + word));
      }

      return card;
   }

   private void renderInfo(GuiGraphics graphics, Font font, List<Component> lines, int leftX, int topY) {
      int cx = this.sx("LP_INFO", leftX) + this.ew("LP_INFO", 154) / 2;
      int y = this.sy("LP_INFO", topY);
      int pitch = BookText.line(font) + 1;

      for (int i = 0; i < lines.size(); i++) {
         Component line = BookText.of(lines.get(i));
         graphics.drawString(font, line, cx - font.width(line) / 2, y + i * pitch, 4144959, false);
      }
   }

   private int needPitch(Font font) {
      return Math.max(BookText.line(font) + 4, this.eh("RP_NEEDS", 52) / NEEDS.length);
   }

   private int needAt(Font font, int mouseX, int mouseY, int leftX, int topY) {
      int x = this.sx("RP_NEEDS", leftX);
      int y = this.sy("RP_NEEDS", topY);
      int pitch = this.needPitch(font);

      for (int i = 0; i < NEEDS.length; i++) {
         if (BookRenderHelper.isMouseOver(mouseX, mouseY, x, y + i * pitch - 2, this.ew("RP_NEEDS", 150), pitch)) {
            return i;
         }
      }

      return -1;
   }

   private static List<Component> needCard(DragonPageMessage.Page page, int need) {
      Component name = Component.translatable("book.bookofdragons.dragon." + NEEDS[need]);

      Component value = switch (need) {
         case 0 -> Component.translatable("book.bookofdragons.need.of", new Object[]{Math.round(page.health()), Math.round(page.maxHealth())});
         case 1 -> Component.translatable("book.bookofdragons.need.of", new Object[]{Math.round(page.food()), 100});
         case 2 -> Component.translatable("book.bookofdragons.need.percent", new Object[]{Math.round(page.stamina() * 100.0F)});
         default -> Component.translatable("book.bookofdragons.need.percent", new Object[]{Math.round((1.0F - page.sleepDebt()) * 100.0F)});
      };
      List<Component> card = new ArrayList<>(List.of(name, value));
      if (need == 0) {
         card.addAll(injuryLines(page.injury()));
      }

      return card;
   }

   private static List<Component> injuryLines(DragonPageMessage.Injury injury) {
      List<Component> lines = new ArrayList<>();
      if (!injury.hurt()) {
         return lines;
      }

      if (injury.down()) {
         lines.add(Component.translatable("book.bookofdragons.injury.down"));
      }

      if (injury.weaker() > 0.0F) {
         lines.add(Component.translatable("book.bookofdragons.injury.weaker", new Object[]{percent(injury.weaker())}));
      }

      if (!injury.broken().isEmpty()) {
         MutableComponent broken = Component.empty();

         for (String key : injury.broken()) {
            if (!broken.getSiblings().isEmpty()) {
               broken.append(", ");
            }

            broken.append(Component.translatable(key));
         }

         lines.add(Component.translatable("book.bookofdragons.injury.broken", new Object[]{broken}));
      }

      if (injury.mendTicks() > 0) {
         int days = (int)Math.ceil(injury.mendTicks() / 24000.0);
         lines.add(
            days <= 1
               ? Component.translatable("book.bookofdragons.injury.mends_day")
               : Component.translatable("book.bookofdragons.injury.mends_days", new Object[]{days})
         );
      }

      return lines;
   }

   private static int percent(float share) {
      return Math.round(share * 100.0F);
   }

   private static List<Component> statCard(DragonPageMessage.Page page, String stat) {
      List<Component> card = new ArrayList<>(BookTips.stat(stat));
      if (page != null && page.injury().weaker() > 0.0F) {
         card.add(Component.translatable("book.bookofdragons.injury.stat", new Object[]{percent(page.injury().weaker())}));
      }

      return card;
   }

   private int statAt(Font font, int mouseX, int mouseY, int leftX, int topY) {
      int x = this.sx("LP_STATS", leftX);
      int y = this.sy("LP_STATS", topY);
      int half = this.ew("LP_STATS", 154) / 2;
      int pitch = BookText.line(font) + 2;

      for (int i = 0; i < STATS.length; i++) {
         if (BookRenderHelper.isMouseOver(mouseX, mouseY, x + i / 3 * half, y + i % 3 * pitch - 1, 40, pitch)) {
            return i;
         }
      }

      return -1;
   }

   private void line(GuiGraphics graphics, String slot, int leftX, int topY) {
      BookRenderHelper.renderUnderline(graphics, this.sx(slot, leftX), this.sy(slot, topY), this.ew(slot, 0), this.eh(slot, 0));
   }

   private void line(GuiGraphics graphics, ResourceLocation texture, String slot, int leftX, int topY) {
      BookRenderHelper.renderOverlayTexture(graphics, texture, this.sx(slot, leftX), this.sy(slot, topY), this.ew(slot, 0), this.eh(slot, 0));
   }

   private void renderStats(GuiGraphics graphics, Font font, DragonPageMessage.Page page, int leftX, int topY) {
      int x = this.sx("LP_STATS", leftX);
      int y = this.sy("LP_STATS", topY);
      int half = this.ew("LP_STATS", 154) / 2;
      int pitch = BookText.line(font) + 2;
      boolean known = page != null && page.stats().length == STATS.length;

      for (int i = 0; i < STATS.length; i++) {
         char grade = known ? DragonStatGrader.gradeFromScore(Math.round(page.stats()[i])) : '?';
         int color = known ? DragonStatGrader.gradeColor(grade) : 9075298;
         int cx = x + i / 3 * half;
         int cy = y + i % 3 * pitch;
         graphics.drawString(font, BookText.literal(STATS[i]), cx, cy, 4144959, false);
         graphics.drawString(font, BookText.literal(String.valueOf(grade)), cx + 30, cy, color, false);
      }
   }

   private void renderNeeds(GuiGraphics graphics, Font font, DragonPageMessage.Page page, int leftX, int topY) {
      if (page != null) {
         if (!page.live()) {
            this.drawText(graphics, font, "RP_NEEDS", List.of(Component.translatable("book.bookofdragons.dragon.away")), 9075298, leftX, topY);
         } else {
            String[] keys = NEEDS;
            DragonPageSpread.State[] states = new DragonPageSpread.State[]{health(page), fed(page), stamina(page), rest(page)};
            int x = this.sx("RP_NEEDS", leftX);
            int y = this.sy("RP_NEEDS", topY);
            int pitch = this.needPitch(font);
            int labelW = 0;

            for (String key : keys) {
               labelW = Math.max(labelW, font.width(BookText.of(Component.translatable("book.bookofdragons.dragon." + key))));
            }

            int textX = x + 9 + 4;

            for (int i = 0; i < keys.length; i++) {
               if (states[i] != null) {
                  int ry = y + i * pitch;
                  RenderSystem.enableBlend();
                  graphics.blit(NEED_ICONS, x, ry - 1, i * 9, 0.0F, 9, 9, 9 * keys.length, 9);
                  RenderSystem.disableBlend();
                  graphics.drawString(font, BookText.of(Component.translatable("book.bookofdragons.dragon." + keys[i])), textX, ry, 4144959, false);
                  Component word = BookText.of(Component.translatable("book.bookofdragons.need." + states[i].word()));
                  int wordX = textX + labelW + 8;
                  graphics.drawString(font, word, wordX, ry, STATE_COLORS[states[i].level()], false);
                  if (i == 0 && page.injury().weaker() > 0.0F) {
                     Component weaker = BookText.of(Component.translatable("book.bookofdragons.injury.short", new Object[]{percent(page.injury().weaker())}));
                     graphics.drawString(font, weaker, wordX + font.width(word) + 4, ry, STATE_COLORS[states[i].level()], false);
                  }
               }
            }
         }
      }
   }

   private static DragonPageSpread.State health(DragonPageMessage.Page page) {
      if (page.injury().down()) {
         return new DragonPageSpread.State("collapsed", 3);
      } else if (page.injury().hurt()) {
         return new DragonPageSpread.State("injured", 2);
      } else {
         float share = page.maxHealth() > 0.0F ? page.health() / page.maxHealth() : 0.0F;
         if (share >= 0.75F) {
            return new DragonPageSpread.State("hale", 0);
         } else if (share >= 0.5F) {
            return new DragonPageSpread.State("scratched", 1);
         } else {
            return share >= 0.25F ? new DragonPageSpread.State("wounded", 2) : new DragonPageSpread.State("near_death", 3);
         }
      }
   }

   private static DragonPageSpread.State fed(DragonPageMessage.Page page) {
      float food = page.food();
      if (food < 0.0F) {
         return null;
      } else if (food >= 85.0F) {
         return new DragonPageSpread.State("full", 0);
      } else if (food >= 60.0F) {
         return new DragonPageSpread.State("fed", 0);
      } else if (food >= 40.0F) {
         return new DragonPageSpread.State("peckish", 1);
      } else {
         return food >= 30.0F ? new DragonPageSpread.State("hungry", 2) : new DragonPageSpread.State("starving", 3);
      }
   }

   private static DragonPageSpread.State stamina(DragonPageMessage.Page page) {
      float share = page.stamina();
      if (share < 0.0F) {
         return null;
      } else if (share >= 0.75F) {
         return new DragonPageSpread.State("fresh", 0);
      } else if (share >= 0.45F) {
         return new DragonPageSpread.State("steady", 1);
      } else {
         return share >= 0.2F ? new DragonPageSpread.State("winded", 2) : new DragonPageSpread.State("spent", 3);
      }
   }

   private static DragonPageSpread.State rest(DragonPageMessage.Page page) {
      if (page.sleepDebt() < 0.0F) {
         return null;
      } else {
         float rested = 1.0F - page.sleepDebt();
         if (rested >= 0.75F) {
            return new DragonPageSpread.State("rested", 0);
         } else if (rested >= 0.5F) {
            return new DragonPageSpread.State("drowsy", 1);
         } else {
            return rested >= 0.25F ? new DragonPageSpread.State("weary", 2) : new DragonPageSpread.State("exhausted", 3);
         }
      }
   }

   private static DragonType typeOf(String name) {
      if (name == null) {
         return null;
      }

      try {
         return DragonType.valueOf(name);
      } catch (IllegalArgumentException e) {
         return null;
      }
   }

   private boolean near() {
      DragonBase body = ClientDragonPages.loaded(this.dragon);
      Minecraft mc = Minecraft.getInstance();
      return body != null && mc.player != null && body.distanceToSqr(mc.player) <= 100.0;
   }

   private void renderOpen(GuiGraphics graphics, Font font, int leftX, int topY, int mouseX, int mouseY) {
      int x = this.sx("RP_OPEN", leftX);
      int y = this.sy("RP_OPEN", topY);
      int w = this.ew("RP_OPEN", 50);
      int h = this.eh("RP_OPEN", 16);
      this.drawBacking(graphics, x, y, w, h);
      Component label = BookText.of(Component.translatable("book.bookofdragons.dragon.open"));
      boolean near = this.near();
      graphics.drawString(font, label, x + (w - font.width(label)) / 2, y + (h - 8) / 2, near ? 4144959 : 9075298, false);
      if (near && !BookLayoutEditor.ENABLED && BookRenderHelper.isMouseOver(mouseX, mouseY, x, y, w, h)) {
         graphics.fill(x, y, x + w, y + h, 822083583);
      }
   }

   private static boolean armed(long since) {
      return since != Long.MIN_VALUE && Util.getMillis() - since < 3000L;
   }

   private boolean over(String slot, int width, double mouseX, double mouseY, int leftX, int topY) {
      return BookRenderHelper.isMouseOver(mouseX, mouseY, this.sx(slot, leftX), this.sy(slot, topY), this.ew(slot, width), this.eh(slot, 16));
   }

   private static List<Player> receivers() {
      Minecraft mc = Minecraft.getInstance();
      List<Player> near = new ArrayList<>();
      if (mc.level != null && mc.player != null) {
         double range = 16.0;

         for (Player other : mc.level.players()) {
            if (other != mc.player && !other.isSpectator() && other.distanceToSqr(mc.player) <= range * range) {
               near.add(other);
            }
         }

         near.sort(Comparator.comparingDouble(p -> p.distanceToSqr(mc.player)));
         return near;
      } else {
         return near;
      }
   }

   private Player receiver() {
      List<Player> near = receivers();
      return near.isEmpty() ? null : near.get(Math.floorMod(this.giveIndex, near.size()));
   }

   private List<Component> giveCard() {
      Player to = this.receiver();
      if (!this.near()) {
         return List.of(Component.translatable("book.bookofdragons.dragon.give"), Component.translatable("book.bookofdragons.dragon.too_far"));
      } else {
         return to == null
            ? List.of(Component.translatable("book.bookofdragons.dragon.give"), Component.translatable("book.bookofdragons.dragon.give_nobody"))
            : List.of(
               Component.translatable("book.bookofdragons.dragon.give_to", new Object[]{to.getDisplayName()}), Component.translatable("book.bookofdragons.dragon.give.tip")
            );
      }
   }

   private void renderHandOver(GuiGraphics graphics, Font font, int leftX, int topY, int mouseX, int mouseY) {
      boolean near = this.near();
      this.button(
         graphics, font, "RP_GIVE", near && this.receiver() != null, armed(this.giveArmed), "book.bookofdragons.dragon.give", leftX, topY, mouseX, mouseY
      );
      this.button(graphics, font, "RP_RELEASE", near, armed(this.releaseArmed), "book.bookofdragons.dragon.release", leftX, topY, mouseX, mouseY);
   }

   private void button(GuiGraphics graphics, Font font, String slot, boolean live, boolean armed, String key, int leftX, int topY, int mouseX, int mouseY) {
      int x = this.sx(slot, leftX);
      int y = this.sy(slot, topY);
      int w = this.ew(slot, 46);
      int h = this.eh(slot, 16);
      this.drawBacking(graphics, x, y, w, h);
      Component label = BookText.of(Component.translatable(armed ? "book.bookofdragons.dragon.sure" : key));
      graphics.drawString(font, label, x + (w - font.width(label)) / 2, y + (h - 8) / 2, !live ? 9075298 : (armed ? 10035230 : 4144959), false);
      if (live && !BookLayoutEditor.ENABLED && BookRenderHelper.isMouseOver(mouseX, mouseY, x, y, w, h)) {
         graphics.fill(x, y, x + w, y + h, 822083583);
      }
   }

   @Override
   protected boolean handleMouseClicked(double mouseX, double mouseY, int button, int leftX, int topY) {
      if (!this.near()) {
         return false;
      }

      if (this.over("RP_GIVE", 46, mouseX, mouseY, leftX, topY)) {
         return this.clickGive(button);
      }

      if (this.over("RP_RELEASE", 46, mouseX, mouseY, leftX, topY)) {
         return this.clickRelease(button);
      }

      if (button != 0) {
         return false;
      }

      if (!BookRenderHelper.isMouseOver(mouseX, mouseY, this.sx("RP_OPEN", leftX), this.sy("RP_OPEN", topY), this.ew("RP_OPEN", 50), this.eh("RP_OPEN", 16))) {
         return false;
      }

      ControlNetwork.sendToServer(new OpenDragonGUIFromBookMessage(this.dragon));
      click();
      return true;
   }

   private boolean clickGive(int button) {
      Player to = this.receiver();
      if (to == null) {
         return false;
      }

      if (button == 1) {
         this.giveIndex++;
         this.giveArmed = Long.MIN_VALUE;
         click();
         return true;
      }

      if (button != 0) {
         return false;
      }

      if (armed(this.giveArmed)) {
         ControlNetwork.sendToServer(new GiveDragonMessage(this.dragon, to.getUUID()));
         this.giveArmed = Long.MIN_VALUE;
      } else {
         this.giveArmed = Util.getMillis();
         this.releaseArmed = Long.MIN_VALUE;
      }

      click();
      return true;
   }

   private boolean clickRelease(int button) {
      if (button != 0) {
         return false;
      }

      if (armed(this.releaseArmed)) {
         ControlNetwork.sendToServer(new ReleaseDragonMessage(this.dragon));
         this.releaseArmed = Long.MIN_VALUE;
      } else {
         this.releaseArmed = Util.getMillis();
         this.giveArmed = Long.MIN_VALUE;
      }

      click();
      return true;
   }

   private static void click() {
      Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));
   }

   static {
      EDITOR.add("LP_BACKDROP", "Portrait backdrop", 1087154688, false, 59, 47, 88, 88, 1.0F);
      EDITOR.add("LP_FIGURE", "Portrait", 1073794048, false, 24, 36, 158, 110, 1.0F);
      EDITOR.add("LP_UNDERLINE2", "Portrait underline 2", 16737792, false, 27, 138, 156, 7, 1.0F);
      EDITOR.add("LP_UNDERLINE3", "Portrait underline 3", 16737792, false, 26, 171, 158, 7, 1.0F);
      EDITOR.addText("LP_INFO", "Species, stage, bond", 8930508, false, 26, 150, 154, 22, 1.0F);
      EDITOR.addText("LP_STATS", "Grades", 2263244, false, 26, 176, 154, 40, 1.0F);
      EDITOR.add("LP_COMPASS", "Compass", 1087119940, false, 152, 38, 26, 26, 1.0F);
      EDITOR.add("RP_TRAITS", "Temperament", 2263244, true, 4, 36, 154, 80, 0.5F);
      EDITOR.addText("RP_NEEDS_TITLE", "Needs title", 11162880, true, 0, 120, 160, 10, 1.0F);
      EDITOR.add("RP_NEEDS_LINE", "Needs underline", 16737792, true, 1, 132, 159, 11, 1.0F);
      EDITOR.addText("RP_NEEDS", "Needs", 2271812, true, 6, 146, 150, 52, 1.0F);
      EDITOR.add("RP_OPEN", "Open button", 13378116, true, 6, 204, 50, 16, 1.0F);
      EDITOR.add("RP_GIVE", "Give button", 13378116, true, 59, 204, 46, 16, 1.0F);
      EDITOR.add("RP_RELEASE", "Release button", 13378116, true, 108, 204, 46, 16, 1.0F);
      stored("dragon", EDITOR);
   }

   private record State(String word, int level) {
   }
}
