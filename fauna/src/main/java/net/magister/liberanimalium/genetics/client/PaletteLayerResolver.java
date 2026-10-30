package net.magister.liberanimalium.genetics.client;

import net.magister.liberanimalium.body.SpeciesAssets;
import net.magister.liberanimalium.genetics.Allele;
import net.magister.liberanimalium.genetics.GeneticsLayer;
import net.magister.liberanimalium.genetics.Genotype;
import net.magister.liberanimalium.genetics.Locus;
import net.magister.liberanimalium.genetics.ModifierConfig;
import net.magister.liberanimalium.genetics.PaletteDraws;
import net.magister.liberanimalium.genetics.PhenotypeResolver;
import net.magister.liberanimalium.genetics.config.SpeciesGeneticsConfig;
import net.minecraft.resources.ResourceLocation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PaletteLayerResolver {
   private static final Logger LOGGER = LoggerFactory.getLogger("PaletteLayerResolver");
   private static final String BASE_PATH = "textures/entity/genetics/";
   private final PhenotypeResolver phenotypeResolver;

   public PaletteLayerResolver() {
      this.phenotypeResolver = new PhenotypeResolver();
   }

   public PaletteLayerResolver(PhenotypeResolver phenotypeResolver) {
      this.phenotypeResolver = phenotypeResolver;
   }

   public ResourceLocation getGrayscaleTexture(
      GeneticsLayer layer, Genotype genotype, SpeciesGeneticsConfig config, String speciesId, PaletteDraws paletteDraws
   ) {
      return this.getGrayscaleTexture(layer, genotype, config, speciesId, 0, paletteDraws);
   }

   public ResourceLocation getGrayscaleTexture(
      GeneticsLayer layer, Genotype genotype, SpeciesGeneticsConfig config, String speciesId, int structureExprIndex, PaletteDraws paletteDraws
   ) {
      LOGGER.trace("[GeneticsDebug] Resolving Grayscale: layer={}, exprIndex={}", layer, structureExprIndex);
      if (layer.hasStructureVariants() && paletteDraws != null) {
         Locus structureLocus = layer.getStructureLocus();
         ResourceLocation draw = paletteDraws.getC1(structureLocus, structureExprIndex);
         if (draw != null) {
            return draw;
         }

         LOGGER.debug(
            "Grayscale key missing in PaletteDraws: layer={}, locus={}, exprIndex={}, key={}",
            new Object[]{layer, structureLocus, structureExprIndex, structureLocus.name() + "_" + structureExprIndex + "_c1"}
         );
      } else if (layer.hasStructureVariants() && paletteDraws == null) {
         LOGGER.debug("Grayscale failed: paletteDraws is null for layer={}, locus={}", layer, layer.getStructureLocus());
      } else if (!layer.hasStructureVariants()) {
         String path = "textures/entity/genetics/" + speciesId + "/" + layer.getTexturePrefix() + ".png";
         return SpeciesAssets.locate(path);
      }

      LOGGER.debug("Missing grayscale draw for layer={}, returning null.", layer);
      return null;
   }

   public PaletteLayerResolver.PaletteBlendResult getPaletteBlend(
      GeneticsLayer layer, Genotype genotype, SpeciesGeneticsConfig config, String speciesId, PaletteDraws paletteDraws
   ) {
      Locus colorLocus = layer.getColorLocus();
      int colorPairIndex = colorLocus != null ? config.getPairIndexOffset(colorLocus) : 0;
      return this.getPaletteBlend(layer, genotype, config, speciesId, colorPairIndex, 0, paletteDraws);
   }

   public PaletteLayerResolver.PaletteBlendResult getPaletteBlend(
      GeneticsLayer layer, Genotype genotype, SpeciesGeneticsConfig config, String speciesId, int colorPairIndex, int colorExprIndex, PaletteDraws paletteDraws
   ) {
      Locus colorLocus = layer.getColorLocus();
      if (paletteDraws == null) {
         return null;
      }

      ResourceLocation c1 = paletteDraws.getC1(colorLocus, colorExprIndex);
      ResourceLocation c2 = paletteDraws.getC2(colorLocus, colorExprIndex);
      if (c1 == null) {
         return null;
      }

      if (c2 == null) {
         c2 = c1;
      }

      if (c1.equals(c2)) {
         return new PaletteLayerResolver.PaletteBlendResult(c1, 100);
      }

      if (colorPairIndex + 2 >= genotype.getPairCount()) {
         return new PaletteLayerResolver.PaletteBlendResult(c1, 100);
      }

      Allele mod1A = genotype.getAllele1(colorPairIndex + 1);
      Allele mod1B = genotype.getAllele2(colorPairIndex + 1);
      String tier1 = ModifierConfig.resolveTier(mod1A, mod1B);
      Allele mod2A = genotype.getAllele1(colorPairIndex + 2);
      Allele mod2B = genotype.getAllele2(colorPairIndex + 2);
      String tier2 = ModifierConfig.resolveTier(mod2A, mod2B);
      Allele colorAllele1 = genotype.getAllele1(colorPairIndex);
      Allele colorAllele2 = genotype.getAllele2(colorPairIndex);
      PhenotypeResolver.ColorBlendResult blend = this.phenotypeResolver.resolveColorBlend(tier1, tier2, colorAllele1.equals(colorAllele2));
      if (blend.isSinglePalette) {
         return paletteDraws.isDormant(colorLocus, colorExprIndex, "c1")
            ? new PaletteLayerResolver.PaletteBlendResult(c2, 100)
            : new PaletteLayerResolver.PaletteBlendResult(c1, 100);
      }

      LOGGER.trace(
         "[GeneticsDebug] Blend resolved codominant: c1={} ({}%), c2={} ({}%) | Tiers: {}, {}",
         new Object[]{c1, blend.allele1Percent, c2, blend.allele2Percent, tier1, tier2}
      );
      return new PaletteLayerResolver.PaletteBlendResult(c1, c2, blend.allele1Percent, blend.allele2Percent);
   }

   public ResourceLocation getNativeTexture(GeneticsLayer layer, Genotype genotype, SpeciesGeneticsConfig config, String speciesId, PaletteDraws paletteDraws) {
      LOGGER.trace("[GeneticsDebug] Resolving NativeTexture: layer={}", layer);
      if (paletteDraws != null) {
         Locus colorLocus = layer.getColorLocus();
         if (colorLocus != null) {
            ResourceLocation draw = paletteDraws.getC1(colorLocus, 0);
            if (draw != null) {
               return draw;
            }

            LOGGER.debug("NativeTexture null in PaletteDraws: layer={}, colorLocus={}", layer, colorLocus);
         }
      }

      LOGGER.debug("Missing native draw for layer={}, returning null.", layer);
      return null;
   }

   public boolean shouldRenderLayer(GeneticsLayer layer, SpeciesGeneticsConfig config) {
      if (config.isLayerDisabled(layer)) {
         return false;
      } else if (layer == GeneticsLayer.BASE) {
         return true;
      } else if (layer == GeneticsLayer.EYES) {
         return true;
      } else if (layer == GeneticsLayer.UNDERBELLY) {
         return true;
      } else if (layer == GeneticsLayer.GRADIENT) {
         return config.hasOptionalLayer(SpeciesGeneticsConfig.OptionalLayer.GRADIENT);
      } else if (layer == GeneticsLayer.WING_GRADIENT) {
         return config.hasOptionalLayer(SpeciesGeneticsConfig.OptionalLayer.WING_GRADIENT);
      } else if (layer == GeneticsLayer.MEMBRANE) {
         return config.hasOptionalLayer(SpeciesGeneticsConfig.OptionalLayer.MEMBRANE);
      } else {
         return layer == GeneticsLayer.GLOW ? config.hasOptionalLayer(SpeciesGeneticsConfig.OptionalLayer.GLOW) : true;
      }
   }

   public static class PaletteBlendResult {
      public final ResourceLocation palette1;
      public final ResourceLocation palette2;
      public final int percent1;
      public final int percent2;
      public final boolean isSinglePalette;

      public PaletteBlendResult(ResourceLocation p1, int pct1) {
         this.palette1 = p1;
         this.palette2 = null;
         this.percent1 = pct1;
         this.percent2 = 0;
         this.isSinglePalette = true;
      }

      public PaletteBlendResult(ResourceLocation p1, ResourceLocation p2, int pct1, int pct2) {
         this.palette1 = p1;
         this.palette2 = p2;
         this.percent1 = pct1;
         this.percent2 = pct2;
         this.isSinglePalette = false;
      }
   }
}
