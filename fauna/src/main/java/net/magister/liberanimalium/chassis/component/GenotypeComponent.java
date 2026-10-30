package net.magister.liberanimalium.chassis.component;

import java.util.Arrays;
import net.magister.liberanimalium.chassis.FaunaBase;
import net.magister.liberanimalium.genetics.GeneticsNBT;
import net.magister.liberanimalium.genetics.GeneticsSpawner;
import net.magister.liberanimalium.genetics.Genotype;
import net.magister.liberanimalium.genetics.PaletteDraws;
import net.magister.liberanimalium.genetics.config.SpeciesGeneticsConfig;
import net.magister.liberanimalium.genetics.config.SpeciesGeneticsConfigRegistry;
import net.minecraft.nbt.CompoundTag;
import org.slf4j.LoggerFactory;

public class GenotypeComponent implements IFaunaComponent {
   protected FaunaBase body;
   private Genotype genotype = null;
   private PaletteDraws paletteDraws = null;

   @Override
   public void onAttach(FaunaBase host) {
      this.body = host;
   }

   @Override
   public void serverTick() {
   }

   @Override
   public void save(CompoundTag tag) {
      if (this.genotype != null) {
         String speciesId = this.body.speciesId();
         CompoundTag geneticsTag = GeneticsNBT.save(this.genotype, speciesId, this.paletteDraws);
         tag.put("Genetics", geneticsTag);
      }
   }

   @Override
   public void load(CompoundTag tag) {
      if (tag.contains("Genetics")) {
         try {
            CompoundTag geneticsTag = tag.getCompound("Genetics");
            String speciesId = GeneticsNBT.getSpeciesId(geneticsTag);
            SpeciesGeneticsConfig config = SpeciesGeneticsConfigRegistry.getConfig(speciesId);
            PaletteDraws loadedDraws = geneticsTag.contains("PaletteDraws") ? PaletteDraws.fromNBT(geneticsTag.getCompound("PaletteDraws")) : null;
            int[] saved = GeneticsNBT.layout(geneticsTag);
            int[] current = config.getExpressionCounts();
            byte[] bytes = GeneticsNBT.bytes(geneticsTag);
            GeneticsSpawner spawner = new GeneticsSpawner(this.body.getRandom());
            if (saved != null && !Arrays.equals(saved, current)) {
               PaletteDraws draws = loadedDraws != null ? loadedDraws : new PaletteDraws();
               Genotype relaid = spawner.relay(config, speciesId, saved, GeneticsNBT.unpackAll(bytes), draws);
               LoggerFactory.getLogger(GenotypeComponent.class)
                  .debug(
                     "[GENETICS] {} genotype carried into the current layout ({} -> {} pairs)",
                     new Object[]{speciesId, bytes.length / 2, relaid.getPairCount()}
                  );
               this.setGenotypeAndDraws(relaid, draws);
            } else if (saved == null && bytes.length != config.getTotalAlleleCount()) {
               GeneticsSpawner.SpawnResult fresh = spawner.generateWildGenotype(config, speciesId);
               LoggerFactory.getLogger(GenotypeComponent.class)
                  .warn(
                     "[GENETICS] {} genotype saved without a layout and {} bytes long where the config needs {}: regenerated",
                     new Object[]{speciesId, bytes.length, config.getTotalAlleleCount()}
                  );
               this.setGenotypeAndDraws(fresh.genotype, fresh.paletteDraws);
            } else {
               this.setGenotypeAndDraws(GeneticsNBT.load(geneticsTag, config), loadedDraws);
            }
         } catch (Exception e) {
            LoggerFactory.getLogger(GenotypeComponent.class).error("[GENETICS ERROR] Failed to load genotype: {}", e.getMessage());
         }
      }
   }

   public Genotype getGenotype() {
      return this.genotype;
   }

   public boolean hasGenotype() {
      return this.genotype != null;
   }

   public PaletteDraws getPaletteDraws() {
      return this.paletteDraws;
   }

   public void setGenotypeAndDraws(Genotype genotype, PaletteDraws paletteDraws) {
      this.genotype = genotype;
      this.paletteDraws = paletteDraws;
      if (genotype != null && !this.body.level().isClientSide()) {
         String speciesId = this.body.speciesId();
         this.body.getEntityData().set(FaunaBase.getGenotypeData(), GeneticsNBT.save(genotype, speciesId, paletteDraws));
      }

      this.body.recalculateStats();
   }

   public void handleGenotypeSync(CompoundTag tag) {
      if (tag != null && !tag.isEmpty()) {
         String speciesId = GeneticsNBT.getSpeciesId(tag);
         SpeciesGeneticsConfig config = SpeciesGeneticsConfigRegistry.getConfig(speciesId);
         this.genotype = GeneticsNBT.load(tag, config);
         if (tag.contains("PaletteDraws")) {
            this.paletteDraws = PaletteDraws.fromNBT(tag.getCompound("PaletteDraws"));
         } else {
            this.paletteDraws = null;
         }

         this.body.recalculateStats();
      }
   }
}
