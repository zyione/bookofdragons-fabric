package net.magister.liberanimalium.sound;

import com.google.gson.Gson;
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

public class FaunaSoundLoader extends SimpleJsonResourceReloadListener {
   private static final Logger LOGGER = LoggerFactory.getLogger("FaunaSoundLoader");
   private static final Gson GSON = new Gson();
   public static final Map<String, String> RAW_JSON = new HashMap<>();

   public FaunaSoundLoader() {
      super(GSON, "fauna/sounds");
   }

   protected void apply(Map<ResourceLocation, JsonElement> files, ResourceManager manager, ProfilerFiller profiler) {
      RAW_JSON.clear();
      FaunaSoundRegistry.clear();

      for (Entry<String, Entry<ResourceLocation, JsonElement>> entry : SpeciesAssets.byId(files, "[Sounds] voice table", LOGGER).entrySet()) {
         String species = entry.getKey();
         if (!species.startsWith("_")) {
            try {
               JsonObject root = GsonHelper.convertToJsonObject(entry.getValue().getValue(), "voice table");
               FaunaSoundRegistry.register(species, FaunaSoundTable.parse(root));
               RAW_JSON.put(species, root.toString());
            } catch (Exception e) {
               LOGGER.error("[Sounds] {} failed to parse: {}", entry.getValue().getKey(), e.getMessage());
            }
         }
      }

      LOGGER.debug("[Sounds] loaded {} species voice table(s)", RAW_JSON.size());
   }
}
