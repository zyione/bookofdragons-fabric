package net.magister.liberanimalium.fauna.api;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.mojang.logging.LogUtils;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;
import net.magister.liberanimalium.body.SpeciesAssets;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.GsonHelper;
import net.minecraft.util.profiling.ProfilerFiller;
import org.slf4j.Logger;

public final class SpeciesProfiles extends SimpleJsonResourceReloadListener {
   private static final Logger LOGGER = LogUtils.getLogger();
   private static final Gson GSON = new GsonBuilder().create();
   private static volatile Map<String, SpeciesProfile> profiles = Map.of();
   private static volatile Map<ResourceLocation, String> rawJson = Map.of();

   public SpeciesProfiles() {
      super(GSON, "fauna/species");
   }

   public static SpeciesProfile get(String speciesId) {
      return profiles.getOrDefault(speciesId, SpeciesProfile.DEFAULT);
   }

   public static int size() {
      return profiles.size();
   }

   public static Set<String> ids() {
      return Set.copyOf(profiles.keySet());
   }

   public static float maxSightRadius() {
      float max = SpeciesProfile.DEFAULT.senses().sightRadius();

      for (SpeciesProfile profile : profiles.values()) {
         max = Math.max(max, profile.senses().sightRadius());
      }

      return max;
   }

   public static Map<ResourceLocation, String> rawJson() {
      return rawJson;
   }

   protected void apply(Map<ResourceLocation, JsonElement> objects, ResourceManager manager, ProfilerFiller profiler) {
      install(objects);
   }

   public static void install(Map<ResourceLocation, JsonElement> objects) {
      Map<String, SpeciesProfile> next = new HashMap<>();
      Map<ResourceLocation, String> raw = new HashMap<>();

      for (Entry<String, Entry<ResourceLocation, JsonElement>> entry : SpeciesAssets.byId(objects, "[FAUNA] species file", LOGGER).entrySet()) {
         String id = entry.getKey();
         if (!id.startsWith("_")) {
            ResourceLocation location = entry.getValue().getKey();

            try {
               JsonObject root = GsonHelper.convertToJsonObject(entry.getValue().getValue(), "fauna species");
               next.put(id, SpeciesProfile.parse(root, id, location.getNamespace()));
               raw.put(location, root.toString());
            } catch (JsonParseException | IllegalArgumentException e) {
               LOGGER.error("[FAUNA] species '{}' failed to load, using defaults: {}", id, e.getMessage());
            }
         }
      }

      profiles = Map.copyOf(next);
      rawJson = Map.copyOf(raw);
      LOGGER.debug("[FAUNA] loaded {} species profiles", next.size());
   }
}
