package net.magister.liberanimalium.genetics.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.Reader;
import java.util.ArrayList;
import java.util.Collection;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;
import net.magister.liberanimalium.body.SpeciesAssets;
import net.magister.liberanimalium.genetics.GeneticsLayer;
import net.magister.liberanimalium.genetics.LinkRule;
import net.magister.liberanimalium.genetics.LinkRuleRegistry;
import net.magister.liberanimalium.genetics.Locus;
import net.magister.liberanimalium.genetics.ModifierConfig;
import net.magister.liberanimalium.genetics.PaletteCoord;
import net.magister.liberanimalium.genetics.PalettePoolRegistry;
import net.magister.liberanimalium.genetics.PaletteTaxonomyRegistry;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.tags.TagKey;
import net.minecraft.util.GsonHelper;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.level.biome.Biome;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SpeciesGeneticsJsonLoader extends SimpleJsonResourceReloadListener {
   private static final Logger LOGGER = LoggerFactory.getLogger("SpeciesGeneticsJsonLoader");
   private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
   public static final Map<String, SpeciesGeneticsJsonLoader.RawLayers> RAW_JSON_LAYERS = new HashMap<>();

   public SpeciesGeneticsJsonLoader() {
      super(GSON, "fauna/genetics");
   }

   protected void apply(Map<ResourceLocation, JsonElement> objects, ResourceManager resourceManager, ProfilerFiller profiler) {
      SpeciesGeneticsConfigRegistry.clear();
      LinkRuleRegistry.clear();
      PaletteTaxonomyRegistry.clear();
      PalettePoolRegistry.clear();
      RAW_JSON_LAYERS.clear();
      int speciesCount = 0;

      Map<ResourceLocation, Resource> resources;
      try {
         resources = resourceManager.listResources("fauna/genetics", id -> id.getPath().endsWith(".json"));
      } catch (Exception e) {
         LOGGER.error("[Genetics] Failed to list species genetics resources", e);
         return;
      }

      Map<ResourceLocation, ResourceLocation> bySpecies = new HashMap<>();

      for (ResourceLocation location : resources.keySet()) {
         String fileName = location.getPath();
         String speciesId = fileName.substring(fileName.lastIndexOf(47) + 1, fileName.length() - 5);
         bySpecies.put(new ResourceLocation(location.getNamespace(), speciesId), location);
      }

      for (Entry<String, Entry<ResourceLocation, ResourceLocation>> chosen : SpeciesAssets.byId(bySpecies, "[Genetics] genetics file", LOGGER).entrySet()) {
         String speciesId = chosen.getKey();
         ResourceLocation location = chosen.getValue().getValue();
         if (!speciesId.startsWith("_")) {
            try {
               List<Resource> stack = resourceManager.getResourceStack(location);
               SpeciesGeneticsConfig.Builder builder = SpeciesGeneticsConfig.builder(speciesId);
               List<String> rawLayers = new ArrayList<>();

               for (Resource resource : stack) {
                  try (Reader reader = resource.openAsReader()) {
                     JsonElement element = JsonParser.parseReader(reader);
                     rawLayers.add(element.toString());
                     JsonObject root = GsonHelper.convertToJsonObject(element, "species config");
                     parseSpeciesConfigInto(location.getNamespace(), speciesId, root, builder);
                     registerLinkRules(location.getNamespace(), speciesId, root);
                     if (root.has("palette_taxonomy")) {
                        JsonObject taxonomy = GsonHelper.getAsJsonObject(root, "palette_taxonomy");
                        parsePaletteTaxonomy(location.getNamespace(), speciesId, taxonomy);
                     }
                  } catch (Exception ex) {
                     LOGGER.error(
                        "[Genetics] Failed to load one layer of species config {} from pack {}: {}",
                        new Object[]{location, resource.sourcePackId(), ex.getMessage()}
                     );
                  }
               }

               SpeciesGeneticsConfig config = builder.build();
               SpeciesGeneticsConfigRegistry.register(speciesId, config);
               RAW_JSON_LAYERS.put(speciesId, new SpeciesGeneticsJsonLoader.RawLayers(location.getNamespace(), rawLayers));
               speciesCount++;
            } catch (Exception ex) {
               LOGGER.error("[Genetics] Failed to fully process resource stack for {}: {}", new Object[]{location, ex.getMessage(), ex});
            }
         }
      }

      LOGGER.debug("[Genetics] Loaded species={} taxonomy_entries={} (across data packs)", speciesCount, PaletteTaxonomyRegistry.size());
   }

   public static void parseSpeciesConfigInto(String namespace, String speciesId, JsonObject root, SpeciesGeneticsConfig.Builder builder) {
      if (root.has("art_version")) {
         builder.artVersion(root.get("art_version").getAsInt());
      }

      if (root.has("locked_loci")) {
         for (Entry<String, JsonElement> e : GsonHelper.getAsJsonObject(root, "locked_loci").entrySet()) {
            builder.lockLocus(Locus.valueOf(e.getKey()), e.getValue().getAsString());
         }
      }

      if (root.has("allowed_alleles")) {
         for (Entry<String, JsonElement> e : GsonHelper.getAsJsonObject(root, "allowed_alleles").entrySet()) {
            JsonArray ids = e.getValue().getAsJsonArray();
            String[] alleles = new String[ids.size()];

            for (int i = 0; i < alleles.length; i++) {
               alleles[i] = ids.get(i).getAsString();
            }

            builder.allowedAlleles(Locus.valueOf(e.getKey()), alleles);
         }
      }

      if (root.has("disabled_layers")) {
         for (JsonElement layer : GsonHelper.getAsJsonArray(root, "disabled_layers")) {
            builder.disableLayer(GeneticsLayer.valueOf(layer.getAsString()));
         }
      }

      if (root.has("mutation")) {
         JsonObject mutObj = GsonHelper.getAsJsonObject(root, "mutation");
         float fallbackAllele = builder.getAlleleMutationChance();
         float fallbackPalette = builder.getPaletteMutationChance();
         float alleleChance = GsonHelper.getAsFloat(mutObj, "allele_chance", fallbackAllele);
         float paletteChance = GsonHelper.getAsFloat(mutObj, "palette_chance", fallbackPalette);
         Map<Integer, Integer> devWeights = new HashMap<>(builder.getDeviationTierWeights());
         if (mutObj.has("deviation_tier_weights")) {
            JsonObject devObj = GsonHelper.getAsJsonObject(mutObj, "deviation_tier_weights");

            for (Entry<String, JsonElement> entry : devObj.entrySet()) {
               devWeights.put(Integer.parseInt(entry.getKey()), entry.getValue().getAsInt());
            }
         }

         builder.mutation(alleleChance, paletteChance, devWeights);
      }

      if (root.has("attributes")) {
         JsonObject attrs = GsonHelper.getAsJsonObject(root, "attributes");

         for (Entry<String, JsonElement> attrEntry : attrs.entrySet()) {
            Locus locus = Locus.valueOf(attrEntry.getKey());
            JsonObject rangeObj = attrEntry.getValue().getAsJsonObject();
            int floor = GsonHelper.getAsInt(rangeObj, "floor");
            int ceiling = GsonHelper.getAsInt(rangeObj, "ceiling");
            builder.attributeRange(locus, floor, ceiling);
         }
      }

      if (root.has("color_loci")) {
         JsonObject colorLoci = GsonHelper.getAsJsonObject(root, "color_loci");

         for (Entry<String, JsonElement> locusEntry : colorLoci.entrySet()) {
            String locusName = locusEntry.getKey();
            Locus locus = Locus.valueOf(locusName);
            JsonObject locusObj = locusEntry.getValue().getAsJsonObject();
            if (locusObj.has("expression_count")) {
               int exprCount = GsonHelper.getAsInt(locusObj, "expression_count");
               builder.addColorLayer(locus, exprCount);
            }

            if (locusObj.has("pool")) {
               parsePool(namespace, speciesId, locusName, locus, locusObj.getAsJsonObject("pool"), builder, "/palettes/");
            }

            if (locusObj.has("link_group")) {
               String group = GsonHelper.getAsString(locusObj, "link_group");
               ResourceLocation ruleId = linkRuleId(namespace, speciesId, group);
               builder.linkChannel(locusName, group, ruleId);
            }
         }
      }

      if (root.has("structure_loci")) {
         JsonObject structLoci = GsonHelper.getAsJsonObject(root, "structure_loci");

         for (Entry<String, JsonElement> locusEntry : structLoci.entrySet()) {
            String locusName = locusEntry.getKey();
            Locus locus = Locus.valueOf(locusName);
            JsonObject locusObj = locusEntry.getValue().getAsJsonObject();
            if (locusObj.has("expression_count")) {
               int exprCount = GsonHelper.getAsInt(locusObj, "expression_count");
               builder.addStructureLayer(locus, exprCount);
            }

            if (locusObj.has("multi_channel_count")) {
               int mcc = GsonHelper.getAsInt(locusObj, "multi_channel_count");
               builder.setMultiChannelCount(locus, mcc);
            }

            if (locusObj.has("pool")) {
               parsePool(namespace, speciesId, locusName, locus, locusObj.getAsJsonObject("pool"), builder, "/");
            }
         }
      }

      if (root.has("biome_pools")) {
         JsonObject biomePools = GsonHelper.getAsJsonObject(root, "biome_pools");

         for (Entry<String, JsonElement> locusEntry : biomePools.entrySet()) {
            String locusName = locusEntry.getKey();
            Locus locus = Locus.valueOf(locusName);
            String folder = locus.getCategory() == Locus.LocusCategory.COLOR ? "/palettes/" : "/";

            for (JsonElement overrideElement : locusEntry.getValue().getAsJsonArray()) {
               JsonObject override = overrideElement.getAsJsonObject();
               Set<ResourceLocation> biomeIds = new HashSet<>();
               Set<TagKey<Biome>> biomeTags = new HashSet<>();

               for (JsonElement biome : override.getAsJsonArray("biomes")) {
                  String text = biome.getAsString();
                  ResourceLocation id = ResourceLocation.tryParse(text.startsWith("#") ? text.substring(1) : text);
                  if (id == null) {
                     throw new IllegalArgumentException("biome_pools." + locusName + ": not an id: " + text);
                  }

                  if (text.startsWith("#")) {
                     biomeTags.add(TagKey.create(Registries.BIOME, id));
                  } else {
                     biomeIds.add(id);
                  }
               }

               Map<String, List<ResourceLocation>> pool = new HashMap<>();

               for (Entry<String, JsonElement> tierEntry : override.getAsJsonObject("entries").entrySet()) {
                  List<ResourceLocation> list = new ArrayList<>();

                  for (JsonElement e : tierEntry.getValue().getAsJsonArray()) {
                     list.add(new ResourceLocation(namespace, "textures/entity/genetics/" + speciesId + folder + e.getAsString() + ".png"));
                  }

                  pool.put(tierEntry.getKey().toLowerCase(), list);
               }

               PalettePoolRegistry.registerBiomeOverride(speciesId, locusName, pool, biomeIds, biomeTags);
            }
         }
      }

      JsonObject colorLociForDetection = root.has("color_loci") ? root.getAsJsonObject("color_loci") : new JsonObject();
      JsonObject structLociForDetection = root.has("structure_loci") ? root.getAsJsonObject("structure_loci") : new JsonObject();
      if (colorLociForDetection.has("n") || structLociForDetection.has("h")) {
         builder.enableGradient();
      }

      if (colorLociForDetection.has("v") || structLociForDetection.has("k")) {
         builder.enableWingGradient();
      }

      if (colorLociForDetection.has("c") || structLociForDetection.has("q")) {
         builder.enableMembrane();
      }

      if (colorLociForDetection.has("g")) {
         builder.enableGlow();
      }
   }

   private static void parsePool(
      String namespace, String speciesId, String locusName, Locus locus, JsonObject poolObj, SpeciesGeneticsConfig.Builder builder, String folder
   ) {
      if (poolObj.has("weights")) {
         ModifierConfig modConfig = parseModifierWeights(poolObj.getAsJsonObject("weights"));
         builder.setLocusModifierConfig(locus, modConfig);
      }

      if (poolObj.has("entries")) {
         JsonObject entriesObj = poolObj.getAsJsonObject("entries");
         Map<String, List<ResourceLocation>> existingPoolMap = PalettePoolRegistry.getPoolMap(speciesId, locusName);
         if (existingPoolMap == null) {
            existingPoolMap = new HashMap<>();
         } else {
            Map<String, List<ResourceLocation>> merged = new HashMap<>();
            existingPoolMap.forEach((k, v) -> merged.put(k, new ArrayList<>((Collection<? extends ResourceLocation>)v)));
            existingPoolMap = merged;
         }

         for (Entry<String, JsonElement> tierEntry : entriesObj.entrySet()) {
            String tier = tierEntry.getKey().toLowerCase();
            JsonArray arr = tierEntry.getValue().getAsJsonArray();
            List<ResourceLocation> resList = existingPoolMap.computeIfAbsent(tier, k -> new ArrayList<>());

            for (JsonElement e : arr) {
               resList.add(new ResourceLocation(namespace, "textures/entity/genetics/" + speciesId + folder + e.getAsString() + ".png"));
            }
         }

         PalettePoolRegistry.register(speciesId, locusName, existingPoolMap);
      }
   }

   private static ModifierConfig parseModifierWeights(JsonObject weights) {
      Map<String, Integer> map = new HashMap<>();
      int w1 = weights.has("COMMON") ? weights.get("COMMON").getAsInt() : GsonHelper.getAsInt(weights, "m1", 40);
      int w2 = weights.has("UNCOMMON") ? weights.get("UNCOMMON").getAsInt() : GsonHelper.getAsInt(weights, "m2", 30);
      int w3 = weights.has("RARE") ? weights.get("RARE").getAsInt() : GsonHelper.getAsInt(weights, "m3", 20);
      int w4 = weights.has("VERY_RARE") ? weights.get("VERY_RARE").getAsInt() : GsonHelper.getAsInt(weights, "m4", 10);
      map.put("m1", w1);
      map.put("m2", w2);
      map.put("m3", w3);
      map.put("m4", w4);
      return ModifierConfig.fromMap(map);
   }

   public static ResourceLocation linkRuleId(String namespace, String speciesId, String name) {
      return new ResourceLocation(namespace, speciesId + "/" + name);
   }

   public static void registerLinkRules(String namespace, String speciesId, JsonObject root) {
      if (root.has("linkage")) {
         JsonObject linkage = GsonHelper.getAsJsonObject(root, "linkage");
         if (linkage.has("rules")) {
            for (Entry<String, JsonElement> rule : GsonHelper.getAsJsonObject(linkage, "rules").entrySet()) {
               LinkRuleRegistry.register(linkRuleId(namespace, speciesId, rule.getKey()), parseLinkRule(rule.getValue().getAsJsonObject()));
            }
         }
      }
   }

   public static LinkRule parseLinkRule(JsonObject ruleObj) {
      JsonObject modeWeightsObj = GsonHelper.getAsJsonObject(ruleObj, "mode_weights");
      Map<LinkRule.Mode, Integer> modeWeights = new EnumMap<>(LinkRule.Mode.class);
      modeWeights.put(LinkRule.Mode.SAME, GsonHelper.getAsInt(modeWeightsObj, "SAME", 0));
      modeWeights.put(LinkRule.Mode.SIMILAR, GsonHelper.getAsInt(modeWeightsObj, "SIMILAR", 0));
      modeWeights.put(LinkRule.Mode.NONE, GsonHelper.getAsInt(modeWeightsObj, "NONE", 0));
      JsonObject tierWeightsObj = GsonHelper.getAsJsonObject(ruleObj, "tier_weights");
      Map<Integer, Integer> tierWeights = new HashMap<>();

      for (Entry<String, JsonElement> te : tierWeightsObj.entrySet()) {
         int tier = Integer.parseInt(te.getKey());
         int weight = te.getValue().getAsInt();
         tierWeights.put(tier, weight);
      }

      return new LinkRule(modeWeights, tierWeights);
   }

   public static int parsePaletteTaxonomy(String namespace, String speciesId, JsonObject taxonomy) {
      int count = 0;

      for (Entry<String, JsonElement> paletteEntry : taxonomy.entrySet()) {
         String paletteName = paletteEntry.getKey();
         JsonObject coord = paletteEntry.getValue().getAsJsonObject();
         String primary = GsonHelper.getAsString(coord, "primary");
         int sat = GsonHelper.getAsInt(coord, "sat");
         int temp = GsonHelper.getAsInt(coord, "temp");
         int light = GsonHelper.getAsInt(coord, "light");
         ResourceLocation paletteId = new ResourceLocation(
            namespace, "textures/entity/genetics/" + speciesId + "/palettes/" + paletteName + ".png"
         );
         PaletteTaxonomyRegistry.register(paletteId, new PaletteCoord(primary, sat, temp, light));
         count++;
      }

      return count;
   }

   public record RawLayers(String namespace, List<String> json) {
   }
}
