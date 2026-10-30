package net.magister.liberanimalium.genetics;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.Map.Entry;
import java.util.function.IntUnaryOperator;
import net.magister.liberanimalium.genetics.config.SpeciesGeneticsConfig;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;

public class GeneticsCrossover {
   private final RandomSource random;

   public GeneticsCrossover(RandomSource random) {
      this.random = random;
   }

   public GeneticsCrossover.CrossoverResult cross(Genotype parent1, Genotype parent2, SpeciesGeneticsConfig config) {
      return this.cross(parent1, parent2, new PaletteDraws(), new PaletteDraws(), config, config.getSpeciesId());
   }

   public GeneticsCrossover.CrossoverResult cross(
      Genotype parent1, Genotype parent2, PaletteDraws parent1Draws, PaletteDraws parent2Draws, SpeciesGeneticsConfig config, String speciesId
   ) {
      Genotype.Builder childBuilder = Genotype.builder();
      PaletteDraws childDraws = new PaletteDraws();
      int pairIndex = 0;

      for (Locus locus : Locus.GENOTYPE_ORDER) {
         if (locus != Locus.m) {
            int expressionCount = config.getExpressionCount(locus);

            for (int expr = 0; expr < expressionCount; expr++) {
               int pairsThisExpression = switch (locus.getCategory()) {
                  case COLOR, STRUCTURE -> 3;
                  default -> 1;
               };
               if (config.isLocusLocked(locus)) {
                  Allele locked = config.getLockedAllele(locus);

                  for (int p = 0; p < pairsThisExpression; p++) {
                     if (p == 0) {
                        childBuilder.addHomozygousPair(locked);
                     } else {
                        childBuilder.addHomozygousPair(Allele.of("m1", 1));
                     }

                     pairIndex++;
                  }
               } else {
                  for (int p = 0; p < pairsThisExpression; p++) {
                     if (pairIndex < parent1.getAlleleCount() / 2 && pairIndex < parent2.getAlleleCount() / 2) {
                        Allele[] parent1Pair = parent1.getAllelePair(pairIndex);
                        Allele[] parent2Pair = parent2.getAllelePair(pairIndex);
                        Allele childAllele1 = this.random.nextBoolean() ? parent1Pair[0] : parent1Pair[1];
                        Allele childAllele2 = this.random.nextBoolean() ? parent2Pair[0] : parent2Pair[1];
                        if (this.random.nextFloat() < config.getAlleleMutationChance()) {
                           childAllele1 = this.getRandomAlleleForPair(config, locus, p);
                        }

                        if (this.random.nextFloat() < config.getAlleleMutationChance()) {
                           childAllele2 = this.getRandomAlleleForPair(config, locus, p);
                        }

                        childBuilder.addPair(childAllele1, childAllele2);
                        pairIndex++;
                     } else {
                        Allele[] defaults = locus.getDefaultAlleles();
                        Allele fallback = defaults.length > 0 ? defaults[0] : Allele.of("m1", 1);
                        childBuilder.addHomozygousPair(fallback);
                        pairIndex++;
                     }
                  }
               }
            }
         }
      }

      Genotype childGenotype = childBuilder.build();
      this.generateOffspringDraws(childGenotype, childDraws, parent1Draws, parent2Draws, config, speciesId);
      return new GeneticsCrossover.CrossoverResult(childGenotype, childDraws);
   }

   private int generateOffspringDraws(
      Genotype childGenotype, PaletteDraws childDraws, PaletteDraws parent1Draws, PaletteDraws parent2Draws, SpeciesGeneticsConfig config, String speciesId
   ) {
      int paletteMutations = 0;
      int pairIndex = 0;

      for (Locus locus : Locus.GENOTYPE_ORDER) {
         if (locus != Locus.m) {
            int expressionCount = config.getExpressionCount(locus);
            Locus.LocusCategory category = locus.getCategory();

            for (int expr = 0; expr < expressionCount; expr++) {
               if (category != Locus.LocusCategory.COLOR && category != Locus.LocusCategory.STRUCTURE) {
                  pairIndex++;
               } else {
                  int locusPairIdx = pairIndex;
                  int modPair1Idx = pairIndex + 1;
                  int modPair2Idx = pairIndex + 2;
                  Allele modA1 = childGenotype.getAllele1Safe(modPair1Idx, Allele.of("m1", 1));
                  Allele modA2 = childGenotype.getAllele2Safe(modPair1Idx, Allele.of("m1", 1));
                  Allele modB1 = childGenotype.getAllele1Safe(modPair2Idx, Allele.of("m1", 1));
                  Allele modB2 = childGenotype.getAllele2Safe(modPair2Idx, Allele.of("m1", 1));
                  String tier1 = ModifierConfig.resolveTier(modA1, modA2);
                  String tier2 = ModifierConfig.resolveTier(modB1, modB2);
                  String channel = locus.name();
                  Map<String, List<ResourceLocation>> pool = PalettePoolRegistry.resolve(speciesId, channel, null);
                  ResourceLocation parentC1 = this.pickParentPalette(parent1Draws, parent2Draws, locus, expr, "c1");
                  ResourceLocation c1Draw = this.drawWithInheritance(pool, tier1, parentC1);
                  if (this.random.nextFloat() < config.getPaletteMutationChance()) {
                     if (category == Locus.LocusCategory.STRUCTURE) {
                        c1Draw = PalettePoolRegistry.drawFromPool(pool, tier1, this.random);
                     } else {
                        c1Draw = this.drawWithMutation(pool, tier1, parentC1, config);
                     }

                     paletteMutations++;
                  }

                  ResourceLocation parentC2 = this.pickParentPalette(parent1Draws, parent2Draws, locus, expr, "c2");
                  ResourceLocation c2Draw = this.drawWithInheritance(pool, tier2, parentC2);
                  if (this.random.nextFloat() < config.getPaletteMutationChance()) {
                     if (category == Locus.LocusCategory.STRUCTURE) {
                        c2Draw = PalettePoolRegistry.drawFromPool(pool, tier2, this.random);
                     } else {
                        c2Draw = this.drawWithMutation(pool, tier2, parentC2, config);
                     }

                     paletteMutations++;
                  }

                  Allele locusPairA1 = childGenotype.getAllele1Safe(locusPairIdx, Allele.of("b1", 1));
                  Allele locusPairA2 = childGenotype.getAllele2Safe(locusPairIdx, Allele.of("b1", 1));
                  boolean homozygous = locusPairA1.equals(locusPairA2);
                  String dormantSlot = null;
                  if (homozygous) {
                     int rank1 = ModifierConfig.tierDominanceRank(tier1);
                     int rank2 = ModifierConfig.tierDominanceRank(tier2);
                     dormantSlot = rank1 > rank2 ? "c1" : "c2";
                  }

                  childDraws.setDraw(locus, expr, c1Draw, c2Draw, dormantSlot);
                  pairIndex += 3;
               }
            }
         }
      }

      return paletteMutations;
   }

   private ResourceLocation pickParentPalette(PaletteDraws parent1Draws, PaletteDraws parent2Draws, Locus locus, int exprIndex, String slot) {
      PaletteDraws chosen = this.random.nextBoolean() ? parent1Draws : parent2Draws;
      ResourceLocation draw = chosen.getColorDraw(locus, exprIndex, slot);
      if (draw == null) {
         PaletteDraws other = chosen == parent1Draws ? parent2Draws : parent1Draws;
         draw = other.getColorDraw(locus, exprIndex, slot);
      }

      return draw;
   }

   private ResourceLocation drawWithInheritance(Map<String, List<ResourceLocation>> pool, String tier, ResourceLocation parentPaletteId) {
      return this.drawByDistance(pool, tier, parentPaletteId, tierDist -> Math.max(1, 8 >> Math.min(tierDist, 3)));
   }

   private ResourceLocation drawWithMutation(
      Map<String, List<ResourceLocation>> pool, String tier, ResourceLocation parentPaletteId, SpeciesGeneticsConfig config
   ) {
      return this.drawByDistance(pool, tier, parentPaletteId, tierDist -> config.getDeviationTierWeights().getOrDefault(tierDist, Math.max(1, tierDist + 1)));
   }

   private ResourceLocation drawByDistance(Map<String, List<ResourceLocation>> pool, String tier, ResourceLocation parentPaletteId, IntUnaryOperator weightAt) {
      List<ResourceLocation> candidates = pool.get(tier);
      if (candidates != null && !candidates.isEmpty()) {
         if (parentPaletteId == null) {
            return candidates.get(this.random.nextInt(candidates.size()));
         }

         TreeMap<Integer, List<ResourceLocation>> buckets = PaletteTaxonomyRegistry.findCandidatesByTier(candidates, parentPaletteId, -1);
         if (buckets.isEmpty()) {
            return candidates.get(this.random.nextInt(candidates.size()));
         }

         List<Entry<Integer, List<ResourceLocation>>> entries = new ArrayList<>(buckets.entrySet());
         int[] weights = new int[entries.size()];
         int totalWeight = 0;

         for (int i = 0; i < entries.size(); i++) {
            int weight = weightAt.applyAsInt(entries.get(i).getKey());
            weights[i] = weight;
            totalWeight += weight;
         }

         if (totalWeight <= 0) {
            return candidates.get(this.random.nextInt(candidates.size()));
         }

         int roll = this.random.nextInt(totalWeight);

         for (int i = 0; i < entries.size(); i++) {
            roll -= weights[i];
            if (roll < 0) {
               List<ResourceLocation> bucket = entries.get(i).getValue();
               return bucket.get(this.random.nextInt(bucket.size()));
            }
         }

         return candidates.get(this.random.nextInt(candidates.size()));
      } else {
         return PalettePoolRegistry.drawFromPool(pool, tier, this.random);
      }
   }

   private Allele getRandomAlleleForPair(SpeciesGeneticsConfig config, Locus locus, int pairPosition) {
      if (pairPosition == 0) {
         List<Allele> pool = config.getAllowedAlleles(locus);
         return pool.get(this.random.nextInt(pool.size()));
      } else {
         return config.getModifierConfig(locus).rollAllele(this.random);
      }
   }

   public static class CrossoverResult {
      private final Genotype genotype;
      private final PaletteDraws paletteDraws;

      public CrossoverResult(Genotype genotype, PaletteDraws paletteDraws) {
         this.genotype = genotype;
         this.paletteDraws = paletteDraws;
      }

      public Genotype getGenotype() {
         return this.genotype;
      }

      public PaletteDraws getPaletteDraws() {
         return this.paletteDraws;
      }
   }
}
