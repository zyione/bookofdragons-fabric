package net.magister.liberanimalium.body.gameplay;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import net.magister.liberanimalium.body.SpeciesAssets;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.GsonHelper;
import net.minecraft.util.profiling.ProfilerFiller;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SpeciesGameplayLoader extends SimpleJsonResourceReloadListener {
   private static final Logger LOGGER = LoggerFactory.getLogger("SpeciesGameplayLoader");
   private static final Gson GSON = new GsonBuilder().create();
   public static final Map<String, String> RAW_JSON = new HashMap<>();

   public SpeciesGameplayLoader() {
      super(GSON, "fauna/species");
   }

   protected void apply(Map<ResourceLocation, JsonElement> objects, ResourceManager resourceManager, ProfilerFiller profiler) {
      Map<String, JsonObject> files = new HashMap<>();
      RAW_JSON.clear();

      for (Entry<String, Entry<ResourceLocation, JsonElement>> entry : SpeciesAssets.byId(objects, "[SpeciesGameplay] species file", LOGGER).entrySet()) {
         String id = entry.getKey();
         if (!id.startsWith("_")) {
            JsonObject root = GsonHelper.convertToJsonObject(entry.getValue().getValue(), "species file " + id);
            if (SpeciesGameplayProfile.isSpeciesFile(root)) {
               files.put(id, root);
               RAW_JSON.put(id, root.toString());
            }
         }
      }

      SpeciesGameplayRegistry.installAll(SpeciesGameplayProfile.parseAll(files), files);
      LOGGER.debug("[SpeciesGameplay] Loaded {} gameplay profiles", SpeciesGameplayRegistry.size());
   }
}
