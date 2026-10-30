package net.magister.bookofdragons.client.gui.book.spread;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import net.magister.bookofdragons.client.gui.book.BookLayoutEditor;
import net.magister.bookofdragons.client.gui.book.BookRenderHelper;
import net.magister.bookofdragons.client.gui.book.DragonPortrait;
import net.magister.bookofdragons.client.gui.book.NotesPad;
import net.magister.bookofdragons.entity.data.DragonType;
import net.magister.liberanimalium.body.gameplay.SpeciesGameplayProfile;
import net.magister.liberanimalium.body.gameplay.SpeciesGameplayRegistry;
import net.magister.liberanimalium.body.gameplay.VariantGameplay;
import net.magister.liberanimalium.genetics.GeneticsNBT;
import net.magister.liberanimalium.genetics.GeneticsSpawner;
import net.magister.liberanimalium.genetics.GeneticsSpawner.SpawnResult;
import net.magister.liberanimalium.genetics.config.SpeciesGeneticsConfig;
import net.magister.liberanimalium.genetics.config.SpeciesGeneticsConfigRegistry;
import net.magister.liberanimalium.variant.CreatureVariant;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;

public class VariantSpread extends NotesSpread {
   private static final int INK = 4144959;
   private static final ResourceLocation BACKDROP = new ResourceLocation("bookofdragons", "textures/gui/book/iconbacking.png");
   private static final int BACKDROP_SIZE = 88;
   private static final BookLayoutEditor EDITOR = new BookLayoutEditor();
   private final CreatureVariant variant;
   private final DragonType type;
   private final UUID portraitId;
   private CompoundTag look;

   public VariantSpread(CreatureVariant variant, DragonType type, NotesPad pad, int firstNotesPage) {
      super(EDITOR, pad, firstNotesPage);
      this.variant = variant;
      this.type = type;
      this.portraitId = UUID.nameUUIDFromBytes(("variant:" + variant.id()).getBytes(StandardCharsets.UTF_8));
   }

   public static Component name(CreatureVariant variant) {
      String name = variant.name();
      return name.isEmpty() ? Component.literal(variant.id().getPath()) : Component.translatableWithFallback(name, name);
   }

   @Override
   public void render(GuiGraphics graphics, Font font, int leftX, int topY, int mouseX, int mouseY, float partialTick) {
      if (this.firstSpread()) {
         this.drawTitle(graphics, font, false, name(this.variant), true, leftX, topY);
         BookRenderHelper.renderScaledTexture(
            graphics, BACKDROP, this.sx("LP_BACKDROP", leftX), this.sy("LP_BACKDROP", topY), this.ew("LP_BACKDROP", 88), this.eh("LP_BACKDROP", 88), 88, 88
         );
         DragonPortrait.draw(
            graphics,
            this.portraitId,
            this.type,
            2,
            this.look(),
            this.sx("LP_FIGURE", leftX),
            this.sy("LP_FIGURE", topY),
            this.ew("LP_FIGURE", 158),
            this.eh("LP_FIGURE", 110)
         );
         this.drawText(graphics, font, "LP_INFO", this.differences(), 4144959, leftX, topY);
         this.renderNotes(graphics, font, "RP_NOTES0", 0, leftX, topY);
      } else {
         this.renderNotes(graphics, font, "LP_NOTES", this.firstNotesPage, leftX, topY);
         this.renderNotes(graphics, font, "RP_NOTES", this.firstNotesPage + 1, leftX, topY);
      }

      this.renderEditorOverlay(graphics, font, leftX, topY, mouseX, mouseY);
   }

   private List<Component> differences() {
      List<Component> lines = new ArrayList<>();
      lines.add(Component.translatable("book.bookofdragons.variant.of", new Object[]{Component.translatable(this.type.getEntityType().getDescriptionId())}));
      String species = this.type.getSerializedName();
      SpeciesGameplayProfile own = SpeciesGameplayRegistry.getProfile(species);
      SpeciesGameplayProfile mine = VariantGameplay.of(this.variant, species);
      if (own != null && mine != null) {
         if (Math.abs(mine.scale() - own.scale()) > 0.001F) {
            lines.add(Component.translatable("book.bookofdragons.variant.size", new Object[]{Math.round(mine.scale() / own.scale() * 100.0F)}));
         }

         if (mine.weightClass() != null && mine.weightClass() != own.weightClass()) {
            lines.add(Component.translatable("book.bookofdragons.label.weight_value", new Object[]{mine.weightClass().name()}));
         }

         if (!mine.melee().equals(own.melee())) {
            lines.add(Component.translatable("book.bookofdragons.variant.own_attacks"));
         }
      }

      if (this.variant.profile() != null) {
         lines.add(Component.translatable("book.bookofdragons.variant.own_mind"));
      }

      return lines;
   }

   private CompoundTag look() {
      if (this.look != null) {
         return this.look;
      }

      String species = this.type.getSerializedName();
      SpeciesGeneticsConfig config = SpeciesGeneticsConfigRegistry.getConfig(species);
      if (config == null) {
         return null;
      }

      GeneticsSpawner spawner = new GeneticsSpawner(RandomSource.create(this.variant.id().toString().hashCode()));
      SpawnResult drawn = spawner.generateWildGenotype(config, species, null, this.variant.genetics());
      this.look = GeneticsNBT.save(drawn.genotype, species, drawn.paletteDraws);
      return this.look;
   }

   static {
      EDITOR.add("LP_BACKDROP", "Portrait backdrop", 1087154688, false, 59, 47, 88, 88, 1.0F);
      EDITOR.add("LP_FIGURE", "Portrait", 1073794048, false, 24, 36, 158, 110, 1.0F);
      EDITOR.addText("LP_INFO", "Kind and differences", 8930508, false, 26, 150, 154, 70, 1.0F);
      EDITOR.addText("RP_NOTES0_TEXT", "Notes (first)", 6710886, true, 2, 30, 156, 170, 1.0F);
      EDITOR.addText("LP_NOTES_TEXT", "Notes (left)", 6710886, false, 24, 30, 156, 170, 1.0F);
      EDITOR.addText("RP_NOTES_TEXT", "Notes (right)", 6710886, true, 2, 30, 156, 170, 1.0F);
      stored("variant", EDITOR);
   }
}
