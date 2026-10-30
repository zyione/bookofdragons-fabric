package net.magister.liberanimalium.spawn;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.IOException;
import java.io.Reader;
import java.util.LinkedHashMap;
import java.util.Map;
import net.magister.liberanimalium.body.SpeciesAssets;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimplePreparableReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SpawnSettingsLoader extends SimplePreparableReloadListener<Map<String, JsonObject>> {
   private static final Logger LOGGER = LoggerFactory.getLogger("SpawnSettings");
   public static final String PATH = "spawn_settings.json";

   protected Map<String, JsonObject> prepare(ResourceManager manager, ProfilerFiller profiler) {
      Map<String, JsonObject> files = new LinkedHashMap<>();

      for (String host : SpeciesAssets.namespaces()) {
         ResourceLocation location = new ResourceLocation(host, "spawn_settings.json");
         Resource resource = (Resource)manager.getResource(location).orElse(null);
         if (resource != null) {
            try (Reader reader = resource.openAsReader()) {
               files.put(host, JsonParser.parseReader(reader).getAsJsonObject());
            } catch (IOException | RuntimeException e) {
               LOGGER.warn("[SpawnSettings] could not read {}; {} keeps the defaults", new Object[]{location, host, e});
            }
         }
      }

      return files;
   }

   protected void apply(Map<String, JsonObject> files, ResourceManager manager, ProfilerFiller profiler) {
      Map<String, SpawnSettings> settings = new LinkedHashMap<>();
      files.forEach((host, json) -> settings.put(host, SpawnSettings.parse(json, host)));
      SpawnSettings.installAll(settings);
      LOGGER.debug("[SpawnSettings] loaded the spawn settings of {}", settings.keySet());
   }
}
