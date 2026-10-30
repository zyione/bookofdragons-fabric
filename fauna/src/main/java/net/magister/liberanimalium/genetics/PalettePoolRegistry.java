package net.magister.liberanimalium.genetics;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.biome.Biome;

public final class PalettePoolRegistry {
   private static final Map<String, Map<String, Map<String, List<ResourceLocation>>>> BASE_POOLS = new HashMap<>();
   private static final Map<String, Map<String, List<PalettePoolRegistry.BiomeOverride>>> BIOME_OVERRIDES = new HashMap<>();
   private static final Map<Integer, ResourceLocation> HASH_LOOKUP = new HashMap<>();

   private PalettePoolRegistry() {
   }

   public static void register(String speciesId, String channelId, Map<String, List<ResourceLocation>> pool) {
      BASE_POOLS.computeIfAbsent(speciesId.toLowerCase(), k -> new HashMap<>()).put(channelId.toLowerCase(), pool);

      for (List<ResourceLocation> list : pool.values()) {
         for (ResourceLocation rl : list) {
            registerHash(rl);
         }
      }
   }

   public static void registerBiomeOverride(
      String speciesId, String channelId, Map<String, List<ResourceLocation>> pool, Set<ResourceLocation> biomes, Set<TagKey<Biome>> tags
   ) {
      BIOME_OVERRIDES.computeIfAbsent(speciesId.toLowerCase(), k -> new HashMap<>())
         .computeIfAbsent(channelId.toLowerCase(), k -> new ArrayList<>())
         .add(new PalettePoolRegistry.BiomeOverride(biomes, tags, pool));

      for (List<ResourceLocation> list : pool.values()) {
         for (ResourceLocation rl : list) {
            registerHash(rl);
         }
      }
   }

   public static void registerHash(ResourceLocation location) {
      if (location != null) {
         HASH_LOOKUP.put(location.toString().hashCode(), location);
      }
   }

   public static ResourceLocation getByHash(int hash) {
      return HASH_LOOKUP.get(hash);
   }

   public static Map<String, List<ResourceLocation>> resolve(String speciesId, String channelId, Holder<Biome> currentBiome) {
      String species = speciesId.toLowerCase();
      String channel = channelId.toLowerCase();
      if (currentBiome != null) {
         Map<String, List<PalettePoolRegistry.BiomeOverride>> speciesOverrides = BIOME_OVERRIDES.get(species);
         if (speciesOverrides != null) {
            List<PalettePoolRegistry.BiomeOverride> channelOverrides = speciesOverrides.get(channel);
            if (channelOverrides != null) {
               for (PalettePoolRegistry.BiomeOverride override : channelOverrides) {
                  if (override.matches(currentBiome)) {
                     return override.pool;
                  }
               }
            }
         }
      }

      Map<String, Map<String, List<ResourceLocation>>> speciesPools = BASE_POOLS.get(species);
      if (speciesPools != null) {
         Map<String, List<ResourceLocation>> pool = speciesPools.get(channel);
         if (pool != null) {
            return pool;
         }
      }

      return Collections.emptyMap();
   }

   public static boolean hasPools(String speciesId) {
      Map<String, Map<String, List<ResourceLocation>>> speciesPools = BASE_POOLS.get(speciesId.toLowerCase());
      return speciesPools != null && !speciesPools.isEmpty();
   }

   public static Map<String, List<ResourceLocation>> getPoolMap(String speciesId, String channelId) {
      String species = speciesId.toLowerCase();
      String channel = channelId.toLowerCase();
      Map<String, Map<String, List<ResourceLocation>>> speciesPools = BASE_POOLS.get(species);
      return speciesPools != null ? speciesPools.get(channel) : null;
   }

   public static ResourceLocation drawFromPool(Map<String, List<ResourceLocation>> pool, String tier, RandomSource random) {
      List<ResourceLocation> palettes = pool.get(tier);
      if (palettes != null && !palettes.isEmpty()) {
         return palettes.get(random.nextInt(palettes.size()));
      }

      String[] fallbackOrder = new String[]{"very_rare", "rare", "uncommon", "common"};

      for (String fallback : fallbackOrder) {
         palettes = pool.get(fallback);
         if (palettes != null && !palettes.isEmpty()) {
            return palettes.get(random.nextInt(palettes.size()));
         }
      }

      return null;
   }

   public static void clear() {
      BASE_POOLS.clear();
      BIOME_OVERRIDES.clear();
      HASH_LOOKUP.clear();
   }

   private static class BiomeOverride {
      final Set<ResourceLocation> biomes;
      final Set<TagKey<Biome>> tags;
      final Map<String, List<ResourceLocation>> pool;

      BiomeOverride(Set<ResourceLocation> biomes, Set<TagKey<Biome>> tags, Map<String, List<ResourceLocation>> pool) {
         this.biomes = biomes;
         this.tags = tags;
         this.pool = pool;
      }

      boolean matches(Holder<Biome> biome) {
         for (ResourceLocation id : this.biomes) {
            if (biome.is(id)) {
               return true;
            }
         }

         for (TagKey<Biome> tag : this.tags) {
            if (biome.is(tag)) {
               return true;
            }
         }

         return false;
      }
   }
}
