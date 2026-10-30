package net.magister.liberanimalium.genetics.config;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;
import net.magister.liberanimalium.genetics.Allele;
import net.magister.liberanimalium.genetics.GeneticsLayer;
import net.magister.liberanimalium.genetics.Locus;
import net.magister.liberanimalium.genetics.ModifierConfig;
import net.minecraft.resources.ResourceLocation;

public class SpeciesGeneticsConfig {
   private final String speciesId;
   private final Map<Locus, Integer> structureCounts;
   private final Map<Locus, Integer> multiChannelCounts;
   private final Map<Locus, Integer> manualCounts;
   private final Map<Locus, Integer> attributeCounts;
   private final Map<Locus, Allele> lockedLoci;
   private final Map<Locus, List<Allele>> allowedAlleles;
   private final Set<SpeciesGeneticsConfig.OptionalLayer> enabledOptionalLayers;
   private final Set<GeneticsLayer> disabledLayers;
   private final ModifierConfig modifierConfig;
   private final Map<Locus, ModifierConfig> locusModifierConfigs;
   private final Map<String, String> linkGroups;
   private final Map<String, ResourceLocation> linkRuleIds;
   private final float alleleMutationChance;
   private final float paletteMutationChance;
   private final Map<Integer, Integer> deviationTierWeights;
   private final int artVersion;
   private final Map<Locus, SpeciesGeneticsConfig.AttributeRange> attributeRanges;

   private SpeciesGeneticsConfig(SpeciesGeneticsConfig.Builder builder) {
      this.speciesId = builder.speciesId;
      this.structureCounts = Collections.unmodifiableMap(new EnumMap<>(builder.structureCounts));
      this.multiChannelCounts = Collections.unmodifiableMap(new EnumMap<>(builder.multiChannelCounts));
      this.manualCounts = Collections.unmodifiableMap(new EnumMap<>(builder.manualCounts));
      this.attributeCounts = Collections.unmodifiableMap(new EnumMap<>(builder.attributeCounts));
      this.lockedLoci = Collections.unmodifiableMap(new EnumMap<>(builder.lockedLoci));
      this.allowedAlleles = new EnumMap<>(Locus.class);

      for (Entry<Locus, List<Allele>> entry : builder.allowedAlleles.entrySet()) {
         this.allowedAlleles.put(entry.getKey(), Collections.unmodifiableList(new ArrayList<>(entry.getValue())));
      }

      this.enabledOptionalLayers = Collections.unmodifiableSet(
         EnumSet.copyOf(builder.enabledOptionalLayers.isEmpty() ? EnumSet.noneOf(SpeciesGeneticsConfig.OptionalLayer.class) : builder.enabledOptionalLayers)
      );
      this.disabledLayers = Collections.unmodifiableSet(
         EnumSet.copyOf(builder.disabledLayers.isEmpty() ? EnumSet.noneOf(GeneticsLayer.class) : builder.disabledLayers)
      );
      this.modifierConfig = builder.modifierConfig;
      this.locusModifierConfigs = Collections.unmodifiableMap(new EnumMap<>(builder.locusModifierConfigs));
      this.linkGroups = Collections.unmodifiableMap(new HashMap<>(builder.linkGroups));
      this.linkRuleIds = Collections.unmodifiableMap(new HashMap<>(builder.linkRuleIds));
      this.alleleMutationChance = builder.alleleMutationChance;
      this.paletteMutationChance = builder.paletteMutationChance;
      this.deviationTierWeights = Collections.unmodifiableMap(new HashMap<>(builder.deviationTierWeights));
      this.artVersion = builder.artVersion;
      this.attributeRanges = Collections.unmodifiableMap(new EnumMap<>(builder.attributeRanges));
   }

   public SpeciesGeneticsConfig.AttributeRange getAttributeRange(Locus locus) {
      return this.attributeRanges.get(locus);
   }

   public String getSpeciesId() {
      return this.speciesId;
   }

   public int getArtVersion() {
      return this.artVersion;
   }

   public ModifierConfig getModifierConfig() {
      return this.modifierConfig;
   }

   public ModifierConfig getModifierConfig(Locus locus) {
      return this.locusModifierConfigs.getOrDefault(locus, this.modifierConfig);
   }

   public float getAlleleMutationChance() {
      return this.alleleMutationChance;
   }

   public float getPaletteMutationChance() {
      return this.paletteMutationChance;
   }

   public Map<Integer, Integer> getDeviationTierWeights() {
      return this.deviationTierWeights;
   }

   public String getLinkGroup(String channelId) {
      return this.linkGroups.get(channelId.toLowerCase());
   }

   public ResourceLocation getLinkRuleId(String channelId) {
      return this.linkRuleIds.get(channelId.toLowerCase());
   }

   public int getExpressionCount(Locus locus) {
      if (locus.getCategory() == Locus.LocusCategory.STRUCTURE) {
         return this.structureCounts.getOrDefault(locus, 1);
      }

      if (locus.getCategory() == Locus.LocusCategory.ATTRIBUTE) {
         return this.attributeCounts.getOrDefault(locus, 1);
      }

      if (locus == Locus.m) {
         return 0;
      }

      if (locus.getCategory() == Locus.LocusCategory.COLOR) {
         int totalExpressions = 0;

         for (GeneticsLayer layer : GeneticsLayer.values()) {
            if (layer.getColorLocus() == locus) {
               if (layer.getStructureLocus() != null) {
                  int structCount = this.getExpressionCount(layer.getStructureLocus());
                  int channels = this.getMultiChannelCount(layer.getStructureLocus());
                  totalExpressions += structCount * channels;
               } else {
                  totalExpressions += this.manualCounts.getOrDefault(locus, 1);
               }
            }
         }

         return totalExpressions;
      } else {
         return 1;
      }
   }

   public int getMultiChannelCount(Locus structureLocus) {
      return this.multiChannelCounts.getOrDefault(structureLocus, 1);
   }

   public int[] getExpressionCounts() {
      int[] counts = new int[Locus.GENOTYPE_ORDER.length];

      for (int i = 0; i < Locus.GENOTYPE_ORDER.length; i++) {
         counts[i] = this.getExpressionCount(Locus.GENOTYPE_ORDER[i]);
      }

      return counts;
   }

   public int getTotalPairCount() {
      int total = 0;

      for (Locus locus : Locus.GENOTYPE_ORDER) {
         int expressionCount = this.getExpressionCount(locus);
         total += expressionCount;
         if (locus.getCategory() == Locus.LocusCategory.COLOR) {
            total += 2 * expressionCount;
         } else if (locus.getCategory() == Locus.LocusCategory.STRUCTURE) {
            total += 2 * expressionCount;
         }
      }

      return total;
   }

   public int getTotalAlleleCount() {
      return this.getTotalPairCount() * 2;
   }

   public boolean isLocusLocked(Locus locus) {
      return this.lockedLoci.containsKey(locus);
   }

   public Allele getLockedAllele(Locus locus) {
      return this.lockedLoci.get(locus);
   }

   public List<Allele> getAllowedAlleles(Locus locus) {
      List<Allele> allowed = this.allowedAlleles.get(locus);
      return allowed != null && !allowed.isEmpty() ? allowed : Arrays.asList(locus.getDefaultAlleles());
   }

   public boolean hasOptionalLayer(SpeciesGeneticsConfig.OptionalLayer layer) {
      return this.enabledOptionalLayers.contains(layer);
   }

   public boolean isLayerDisabled(GeneticsLayer layer) {
      return this.disabledLayers.contains(layer);
   }

   public int getPairIndexOffset(Locus targetLocus) {
      if (targetLocus == Locus.m) {
         return -1;
      }

      int offset = 0;

      for (Locus locus : Locus.GENOTYPE_ORDER) {
         if (locus == targetLocus) {
            return offset;
         }

         int expressionCount = this.getExpressionCount(locus);
         offset += expressionCount;
         if (locus.getCategory() == Locus.LocusCategory.COLOR) {
            offset += 2 * expressionCount;
         } else if (locus.getCategory() == Locus.LocusCategory.STRUCTURE) {
            offset += 2 * expressionCount;
         }
      }

      throw new IllegalArgumentException("Unknown locus: " + targetLocus);
   }

   public static SpeciesGeneticsConfig defaultConfig() {
      return SpeciesGeneticsConfig.Builder.defaultBuilder("unknown").build();
   }

   public static SpeciesGeneticsConfig.Builder builder(String speciesId) {
      return new SpeciesGeneticsConfig.Builder(speciesId);
   }

   public record AttributeRange(int floor, int ceiling) {
   }

   public static class Builder {
      private final String speciesId;
      private final Map<Locus, Integer> structureCounts = new EnumMap<>(Locus.class);
      private final Map<Locus, Integer> multiChannelCounts = new EnumMap<>(Locus.class);
      private final Map<Locus, Integer> manualCounts = new EnumMap<>(Locus.class);
      private final Map<Locus, Integer> attributeCounts = new EnumMap<>(Locus.class);
      private final Map<Locus, Allele> lockedLoci = new EnumMap<>(Locus.class);
      private final Map<Locus, List<Allele>> allowedAlleles = new EnumMap<>(Locus.class);
      private final Set<SpeciesGeneticsConfig.OptionalLayer> enabledOptionalLayers = EnumSet.noneOf(SpeciesGeneticsConfig.OptionalLayer.class);
      private final Set<GeneticsLayer> disabledLayers = EnumSet.noneOf(GeneticsLayer.class);
      private ModifierConfig modifierConfig = ModifierConfig.defaultConfig();
      private final Map<Locus, ModifierConfig> locusModifierConfigs = new EnumMap<>(Locus.class);
      private final Map<String, String> linkGroups = new HashMap<>();
      private final Map<String, ResourceLocation> linkRuleIds = new HashMap<>();
      private int artVersion = 1;
      private final Map<Locus, SpeciesGeneticsConfig.AttributeRange> attributeRanges = new EnumMap<>(Locus.class);
      private float alleleMutationChance = 0.02F;
      private float paletteMutationChance = 0.05F;
      private final Map<Integer, Integer> deviationTierWeights = new HashMap<>();

      public float getAlleleMutationChance() {
         return this.alleleMutationChance;
      }

      public float getPaletteMutationChance() {
         return this.paletteMutationChance;
      }

      public Map<Integer, Integer> getDeviationTierWeights() {
         return this.deviationTierWeights;
      }

      private Builder(String speciesId) {
         this.speciesId = speciesId;

         for (Locus locus : Locus.values()) {
            if (locus.getCategory() == Locus.LocusCategory.STRUCTURE) {
               this.structureCounts.put(locus, 1);
               this.multiChannelCounts.put(locus, 1);
            } else if (locus.getCategory() == Locus.LocusCategory.ATTRIBUTE) {
               this.attributeCounts.put(locus, 1);
            }
         }
      }

      public SpeciesGeneticsConfig.Builder addStructureLayer(Locus locus, int count) {
         if (locus.getCategory() != Locus.LocusCategory.STRUCTURE) {
            throw new IllegalArgumentException("Locus " + locus + " is not a structure locus.");
         }

         if (count < 1) {
            throw new IllegalArgumentException("Count must be >= 1");
         }

         this.structureCounts.put(locus, count);
         return this;
      }

      public SpeciesGeneticsConfig.Builder addColorLayer(Locus locus, int count) {
         if (locus.getCategory() != Locus.LocusCategory.COLOR) {
            throw new IllegalArgumentException("Locus " + locus + " is not a color locus.");
         }

         if (count < 1) {
            throw new IllegalArgumentException("Count must be >= 1");
         }

         this.manualCounts.put(locus, count);
         return this;
      }

      public SpeciesGeneticsConfig.Builder setMultiChannelCount(Locus locus, int channels) {
         if (locus.getCategory() != Locus.LocusCategory.STRUCTURE) {
            throw new IllegalArgumentException("Locus " + locus + " is not a structure locus.");
         }

         if (channels < 1) {
            throw new IllegalArgumentException("Channels must be >= 1");
         }

         this.multiChannelCounts.put(locus, channels);
         return this;
      }

      public SpeciesGeneticsConfig.Builder lockLocus(Locus locus, Allele allele) {
         this.lockedLoci.put(locus, allele);
         return this;
      }

      public SpeciesGeneticsConfig.Builder lockLocus(Locus locus, String alleleId) {
         return this.lockLocus(locus, Allele.of(alleleId));
      }

      public SpeciesGeneticsConfig.Builder allowedAlleles(Locus locus, Allele... alleles) {
         this.allowedAlleles.put(locus, Arrays.asList(alleles));
         return this;
      }

      public SpeciesGeneticsConfig.Builder allowedAlleles(Locus locus, String... alleleIds) {
         List<Allele> alleles = new ArrayList<>();

         for (String id : alleleIds) {
            alleles.add(Allele.of(id));
         }

         this.allowedAlleles.put(locus, alleles);
         return this;
      }

      public SpeciesGeneticsConfig.Builder enableLayer(SpeciesGeneticsConfig.OptionalLayer layer) {
         this.enabledOptionalLayers.add(layer);
         return this;
      }

      public SpeciesGeneticsConfig.Builder disableLayer(GeneticsLayer layer) {
         this.disabledLayers.add(layer);
         return this;
      }

      public SpeciesGeneticsConfig.Builder enableGlow() {
         return this.enableLayer(SpeciesGeneticsConfig.OptionalLayer.GLOW);
      }

      public SpeciesGeneticsConfig.Builder enableGradient() {
         return this.enableLayer(SpeciesGeneticsConfig.OptionalLayer.GRADIENT);
      }

      public SpeciesGeneticsConfig.Builder enableWingGradient() {
         return this.enableLayer(SpeciesGeneticsConfig.OptionalLayer.WING_GRADIENT);
      }

      public SpeciesGeneticsConfig.Builder enableMembrane() {
         return this.enableLayer(SpeciesGeneticsConfig.OptionalLayer.MEMBRANE);
      }

      public SpeciesGeneticsConfig.Builder modifierConfig(ModifierConfig config) {
         this.modifierConfig = config;
         return this;
      }

      public SpeciesGeneticsConfig.Builder setLocusModifierConfig(Locus locus, ModifierConfig config) {
         this.locusModifierConfigs.put(locus, config);
         return this;
      }

      public SpeciesGeneticsConfig.Builder linkChannel(String channelId, String groupName, ResourceLocation ruleId) {
         this.linkGroups.put(channelId.toLowerCase(), groupName);
         this.linkRuleIds.put(channelId.toLowerCase(), ruleId);
         return this;
      }

      public SpeciesGeneticsConfig.Builder mutation(float alleleChance, float paletteChance, Map<Integer, Integer> deviationWeights) {
         this.alleleMutationChance = alleleChance;
         this.paletteMutationChance = paletteChance;
         this.deviationTierWeights.clear();
         this.deviationTierWeights.putAll(deviationWeights);
         return this;
      }

      public SpeciesGeneticsConfig.Builder artVersion(int version) {
         this.artVersion = version;
         return this;
      }

      public SpeciesGeneticsConfig.Builder attributeRange(Locus locus, int floor, int ceiling) {
         this.attributeRanges.put(locus, new SpeciesGeneticsConfig.AttributeRange(floor, ceiling));
         return this;
      }

      public static SpeciesGeneticsConfig.Builder defaultBuilder(String speciesId) {
         return new SpeciesGeneticsConfig.Builder(speciesId);
      }

      public SpeciesGeneticsConfig build() {
         return new SpeciesGeneticsConfig(this);
      }
   }

   public enum OptionalLayer {
      GRADIENT,
      MEMBRANE,
      GLOW,
      WING_GRADIENT;
   }

   public static class PoolConfig {
      public final ModifierConfig weights;
      public final Map<String, List<ResourceLocation>> entries;

      public PoolConfig(ModifierConfig weights, Map<String, List<ResourceLocation>> entries) {
         this.weights = weights;
         this.entries = entries;
      }
   }
}
