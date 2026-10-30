package net.magister.liberanimalium.genetics.config;

import java.util.HashMap;
import java.util.Map;

public final class SpeciesGeneticsConfigRegistry {
   private static final Map<String, SpeciesGeneticsConfig> REGISTRY = new HashMap<>();

   private SpeciesGeneticsConfigRegistry() {
   }

   public static void register(String speciesId, SpeciesGeneticsConfig config) {
      REGISTRY.put(speciesId.toLowerCase(), config);
   }

   public static SpeciesGeneticsConfig getConfig(String speciesId) {
      SpeciesGeneticsConfig config = REGISTRY.get(speciesId.toLowerCase());
      return config != null ? config : SpeciesGeneticsConfig.builder(speciesId).enableGradient().enableGlow().build();
   }

   public static boolean has(String speciesId) {
      return REGISTRY.containsKey(speciesId.toLowerCase());
   }

   public static void clear() {
      REGISTRY.clear();
   }

   public static int size() {
      return REGISTRY.size();
   }
}
