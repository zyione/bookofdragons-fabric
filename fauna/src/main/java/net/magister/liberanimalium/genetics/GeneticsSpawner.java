package net.magister.liberanimalium.genetics;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.Map.Entry;
import net.magister.liberanimalium.genetics.config.SpeciesGeneticsConfig;
import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.biome.Biome;

public class GeneticsSpawner {
   private final RandomSource random;

   public GeneticsSpawner(RandomSource random) {
      this.random = random;
   }

   public GeneticsSpawner() {
      this(RandomSource.create());
   }

   public GeneticsSpawner.SpawnResult generateWildGenotype(SpeciesGeneticsConfig config, String speciesId, Holder<Biome> biome, GeneticsPreset preset) {
      Genotype.Builder genotypeBuilder = Genotype.builder();
      PaletteDraws paletteDraws = new PaletteDraws();
      StringBuilder debugLog = new StringBuilder();
      debugLog.append("=== v4 Spawn: ").append(speciesId).append(" ===\n");
      Map<String, ResourceLocation> linkAnchors = new HashMap<>();

      for (Locus locus : Locus.GENOTYPE_ORDER) {
         int exprCount = config.getExpressionCount(locus);
         ModifierConfig locusModConfig = config.getModifierConfig(locus);

         for (int expr = 0; expr < exprCount; expr++) {
            switch (locus.getCategory()) {
               case COLOR:
                  this.spawnColorExpression(locus, expr, config, locusModConfig, speciesId, biome, preset, genotypeBuilder, paletteDraws, linkAnchors, debugLog);
                  break;
               case STRUCTURE:
                  this.spawnStructureExpression(locus, expr, config, locusModConfig, speciesId, biome, preset, genotypeBuilder, paletteDraws, debugLog);
                  break;
               case ATTRIBUTE:
                  this.spawnAttributeExpression(locus, config, preset, genotypeBuilder, debugLog);
            }
         }
      }

      debugLog.append("=== Spawn complete ===\n");
      return new GeneticsSpawner.SpawnResult(genotypeBuilder.build(), paletteDraws, debugLog.toString());
   }

   public GeneticsSpawner.SpawnResult generateWildGenotype(SpeciesGeneticsConfig config, String speciesId, Holder<Biome> biome) {
      return this.generateWildGenotype(config, speciesId, biome, GeneticsPreset.NONE);
   }

   public GeneticsSpawner.SpawnResult generateWildGenotype(SpeciesGeneticsConfig config, String speciesId) {
      return this.generateWildGenotype(config, speciesId, null, GeneticsPreset.NONE);
   }

   public Genotype relay(SpeciesGeneticsConfig config, String speciesId, int[] oldLayout, Genotype old, PaletteDraws draws) {
      int[] newLayout = config.getExpressionCounts();
      Map<String, ResourceLocation> linkAnchors = new HashMap<>();

      for (Locus locus : Locus.GENOTYPE_ORDER) {
         if (locus.getCategory() == Locus.LocusCategory.COLOR) {
            String group = config.getLinkGroup(locus.name());
            ResourceLocation first = draws.getC1(locus, 0);
            if (group != null && first != null) {
               linkAnchors.putIfAbsent(group, first);
            }
         }
      }

      StringBuilder debugLog = new StringBuilder("=== relay: " + speciesId + " ===\n");
      GenotypeRelayout.Roller roller = (locusx, exprIndex, into) -> {
         ModifierConfig locusModConfig = config.getModifierConfig(locusx);
         switch (locusx.getCategory()) {
            case COLOR:
               this.spawnColorExpression(locusx, exprIndex, config, locusModConfig, speciesId, null, GeneticsPreset.NONE, into, draws, linkAnchors, debugLog);
               break;
            case STRUCTURE:
               this.spawnStructureExpression(locusx, exprIndex, config, locusModConfig, speciesId, null, GeneticsPreset.NONE, into, draws, debugLog);
               break;
            case ATTRIBUTE:
               this.spawnAttributeExpression(locusx, config, GeneticsPreset.NONE, into, debugLog);
         }
      };
      return GenotypeRelayout.relay(oldLayout, newLayout, old, roller);
   }

   private void spawnColorExpression(
      Locus locus,
      int exprIndex,
      SpeciesGeneticsConfig config,
      ModifierConfig modConfig,
      String speciesId,
      Holder<Biome> biome,
      GeneticsPreset preset,
      Genotype.Builder builder,
      PaletteDraws draws,
      Map<String, ResourceLocation> linkAnchors,
      StringBuilder debugLog
   ) {
      String channel = locus.name();
      if (!addLocked(locus, exprIndex, config, builder, debugLog)) {
         Allele[] mods = this.rollModifiers(modConfig, preset, channel);
         Allele modA1 = mods[0];
         Allele modA2 = mods[1];
         Allele modB1 = mods[2];
         Allele modB2 = mods[3];
         String tier1 = ModifierConfig.resolveTier(modA1, modA2);
         String tier2 = ModifierConfig.resolveTier(modB1, modB2);
         debugLog.append("  ")
            .append(channel)
            .append("[")
            .append(exprIndex)
            .append("] mods: (")
            .append(modA1)
            .append(",")
            .append(modA2)
            .append(")->")
            .append(tier1)
            .append(" (")
            .append(modB1)
            .append(",")
            .append(modB2)
            .append(")->")
            .append(tier2)
            .append("\n");
         Map<String, List<ResourceLocation>> pool = preset.narrow(channel, PalettePoolRegistry.resolve(speciesId, channel, biome));
         ResourceLocation c1Draw = this.drawWithLinkage(pool, tier1, channel, config, linkAnchors, debugLog);
         ResourceLocation c2Draw = this.drawWithLinkage(pool, tier2, channel, config, linkAnchors, debugLog);
         Allele[] slotAlleles = locus.getDefaultAlleles();
         Allele c1 = slotAlleles[this.random.nextBoolean() ? 0 : 1];
         Allele c2 = slotAlleles[this.random.nextBoolean() ? 0 : 1];
         if (preset.lock(channel) != null) {
            c1 = c2 = slotAllele(slotAlleles, preset.lock(channel));
         }

         builder.addPair(c1, c2);
         builder.addPair(modA1, modA2);
         builder.addPair(modB1, modB2);
         boolean homozygous = c1.equals(c2);
         String dormantSlot = dormantSlot(homozygous, tier1, tier2);
         draws.setColorDraw(locus, exprIndex, c1Draw, c2Draw, dormantSlot);
         debugLog.append("    → locus: (")
            .append(c1.getId())
            .append(",")
            .append(c2.getId())
            .append(")")
            .append(" c1=")
            .append(c1Draw)
            .append(" c2=")
            .append(c2Draw)
            .append(homozygous ? " [" + dormantSlot + " DORMANT]" : "")
            .append("\n");
      }
   }

   private void spawnStructureExpression(
      Locus locus,
      int exprIndex,
      SpeciesGeneticsConfig config,
      ModifierConfig modConfig,
      String speciesId,
      Holder<Biome> biome,
      GeneticsPreset preset,
      Genotype.Builder builder,
      PaletteDraws draws,
      StringBuilder debugLog
   ) {
      String channel = locus.name();
      if (!addLocked(locus, exprIndex, config, builder, debugLog)) {
         Allele[] mods = this.rollModifiers(modConfig, preset, channel);
         Allele modA1 = mods[0];
         Allele modA2 = mods[1];
         Allele modB1 = mods[2];
         Allele modB2 = mods[3];
         String tier1 = ModifierConfig.resolveTier(modA1, modA2);
         String tier2 = ModifierConfig.resolveTier(modB1, modB2);
         Allele[] structureAlleles = locus.getDefaultAlleles();
         Map<String, List<ResourceLocation>> pool = preset.narrow(channel, PalettePoolRegistry.resolve(speciesId, channel, biome));
         ResourceLocation primaryDraw = PalettePoolRegistry.drawFromPool(pool, tier1, this.random);
         ResourceLocation secondaryDraw = PalettePoolRegistry.drawFromPool(pool, tier2, this.random);
         Allele s1 = structureAlleles[this.random.nextBoolean() ? 0 : 1];
         Allele s2 = structureAlleles[this.random.nextBoolean() ? 0 : 1];
         if (preset.lock(channel) != null) {
            s1 = s2 = slotAllele(structureAlleles, preset.lock(channel));
         }

         builder.addPair(s1, s2);
         builder.addPair(modA1, modA2);
         builder.addPair(modB1, modB2);
         boolean homozygous = s1.equals(s2);
         String dormantSlot = dormantSlot(homozygous, tier1, tier2);
         draws.setStructureDraw(locus, exprIndex, primaryDraw, secondaryDraw, dormantSlot);
         debugLog.append("  ")
            .append(channel)
            .append("[")
            .append(exprIndex)
            .append("] struct: (")
            .append(s1.getId())
            .append(",")
            .append(s2.getId())
            .append(") mods: (")
            .append(modA1)
            .append(",")
            .append(modA2)
            .append(")→")
            .append(tier1)
            .append(" (")
            .append(modB1)
            .append(",")
            .append(modB2)
            .append(")→")
            .append(tier2)
            .append(" c1=")
            .append(primaryDraw)
            .append(" c2=")
            .append(secondaryDraw)
            .append(homozygous ? " [" + dormantSlot + " DORMANT]" : "")
            .append("\n");
      }
   }

   private static boolean addLocked(Locus locus, int exprIndex, SpeciesGeneticsConfig config, Genotype.Builder builder, StringBuilder debugLog) {
      if (!config.isLocusLocked(locus)) {
         return false;
      }

      Allele locked = config.getLockedAllele(locus);
      builder.addHomozygousPair(locked);
      builder.addPair(Allele.of("m1", 1), Allele.of("m1", 1));
      builder.addPair(Allele.of("m1", 1), Allele.of("m1", 1));
      debugLog.append("  ").append(locus.name()).append("[").append(exprIndex).append("] LOCKED -> ").append(locked.getId()).append("\n");
      return true;
   }

   private Allele[] rollModifiers(ModifierConfig modConfig, GeneticsPreset preset, String channel) {
      Allele modA1 = modConfig.rollAllele(this.random);
      Allele modA2 = modConfig.rollAllele(this.random);
      Allele modB1 = modConfig.rollAllele(this.random);
      Allele modB2 = modConfig.rollAllele(this.random);
      String presetTier = preset.tier(channel);
      if (presetTier != null) {
         modA1 = modA2 = modB1 = modB2 = GeneticsPreset.modifierOf(presetTier);
      }

      return new Allele[]{modA1, modA2, modB1, modB2};
   }

   private static String dormantSlot(boolean homozygous, String tier1, String tier2) {
      if (!homozygous) {
         return null;
      }

      int rank1 = ModifierConfig.tierDominanceRank(tier1);
      int rank2 = ModifierConfig.tierDominanceRank(tier2);
      return rank1 > rank2 ? "c1" : "c2";
   }

   private static Allele slotAllele(Allele[] alleles, String id) {
      for (Allele allele : alleles) {
         if (allele.getId().equalsIgnoreCase(id)) {
            return allele;
         }
      }

      return alleles[0];
   }

   private void spawnAttributeExpression(Locus locus, SpeciesGeneticsConfig config, GeneticsPreset preset, Genotype.Builder builder, StringBuilder debugLog) {
      Allele fixed = preset.stat(locus);
      if (fixed != null) {
         builder.addHomozygousPair(fixed);
         debugLog.append("  ").append(locus.name()).append(": ").append(fixed).append(" (preset)\n");
      } else if (config.isLocusLocked(locus)) {
         Allele locked = config.getLockedAllele(locus);
         builder.addHomozygousPair(locked);
      } else {
         Allele[] alleles = locus.getDefaultAlleles();
         Allele a1 = alleles[this.random.nextInt(alleles.length)];
         Allele a2 = alleles[this.random.nextInt(alleles.length)];
         builder.addPair(a1, a2);
         debugLog.append("  ").append(locus.name()).append(": ").append(a1).append(",").append(a2).append("\n");
      }
   }

   private ResourceLocation drawWithLinkage(
      Map<String, List<ResourceLocation>> pool,
      String tier,
      String channel,
      SpeciesGeneticsConfig config,
      Map<String, ResourceLocation> linkAnchors,
      StringBuilder debugLog
   ) {
      String linkGroup = config.getLinkGroup(channel);
      ResourceLocation linkRuleId = config.getLinkRuleId(channel);
      if (linkGroup != null && linkRuleId != null) {
         ResourceLocation anchorPaletteId = linkAnchors.get(linkGroup);
         if (anchorPaletteId == null) {
            ResourceLocation drawn = PalettePoolRegistry.drawFromPool(pool, tier, this.random);
            if (drawn != null) {
               linkAnchors.put(linkGroup, drawn);
            }

            debugLog.append("    link[").append(linkGroup).append("] ANCHOR set -> ").append(drawn).append("\n");
            return drawn;
         } else {
            LinkRule rule = LinkRuleRegistry.get(linkRuleId);
            if (rule == null) {
               debugLog.append("    link[").append(linkGroup).append("] rule not found -> raw draw\n");
               return PalettePoolRegistry.drawFromPool(pool, tier, this.random);
            }

            LinkRule.Mode mode = rule.rollMode(this.random);
            debugLog.append("    link[").append(linkGroup).append("] mode=").append(mode);
            switch (mode) {
               case SAME:
                  debugLog.append(" -> reusing anchor ").append(anchorPaletteId).append("\n");
                  return anchorPaletteId;
               case SIMILAR:
                  return this.drawSimilar(pool, tier, anchorPaletteId, rule, debugLog);
               case NONE:
               default:
                  debugLog.append(" -> free draw\n");
                  return PalettePoolRegistry.drawFromPool(pool, tier, this.random);
            }
         }
      } else {
         return PalettePoolRegistry.drawFromPool(pool, tier, this.random);
      }
   }

   private ResourceLocation drawSimilar(
      Map<String, List<ResourceLocation>> pool, String tier, ResourceLocation anchorPaletteId, LinkRule rule, StringBuilder debugLog
   ) {
      List<ResourceLocation> candidates = pool.get(tier);
      if (candidates != null && !candidates.isEmpty()) {
         TreeMap<Integer, List<ResourceLocation>> buckets = PaletteTaxonomyRegistry.findCandidatesByTier(candidates, anchorPaletteId, -1);
         if (buckets.isEmpty()) {
            debugLog.append(" -> no bucketed candidates\n");
            return PalettePoolRegistry.drawFromPool(pool, tier, this.random);
         }

         int targetTier = rule.rollTier(this.random);
         Entry<Integer, List<ResourceLocation>> best = buckets.ceilingEntry(targetTier);
         if (best == null) {
            best = buckets.lastEntry();
         }

         List<ResourceLocation> bucket = PaletteTaxonomyRegistry.nearestByFineDistance(best.getValue(), anchorPaletteId);
         ResourceLocation drawn = bucket.get(this.random.nextInt(bucket.size()));
         debugLog.append(" -> tier=").append(best.getKey()).append(" drawn=").append(drawn).append("\n");
         return drawn;
      } else {
         debugLog.append(" -> no candidates in tier ").append(tier).append("\n");
         return PalettePoolRegistry.drawFromPool(pool, tier, this.random);
      }
   }

   public static class SpawnResult {
      public final Genotype genotype;
      public final PaletteDraws paletteDraws;
      public final String debugLog;

      public SpawnResult(Genotype genotype, PaletteDraws paletteDraws, String debugLog) {
         this.genotype = genotype;
         this.paletteDraws = paletteDraws;
         this.debugLog = debugLog;
      }
   }
}
