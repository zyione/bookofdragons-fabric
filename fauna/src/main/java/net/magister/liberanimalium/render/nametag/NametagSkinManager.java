package net.magister.liberanimalium.render.nametag;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import net.magister.liberanimalium.genetics.GeneticsPreset;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.GsonHelper;
import net.minecraft.util.profiling.ProfilerFiller;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class NametagSkinManager extends SimpleJsonResourceReloadListener {
   private static final Logger LOGGER = LoggerFactory.getLogger("NametagSkinManager");
   private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
   private static final Map<String, Map<String, NametagSkinConfig>> REGISTRY = new ConcurrentHashMap<>();

   public NametagSkinManager() {
      super(GSON, "nametags");
   }

   protected void apply(Map<ResourceLocation, JsonElement> objects, ResourceManager resourceManager, ProfilerFiller profiler) {
      REGISTRY.clear();
      NametagLooks.clear();
      int count = 0;

      for (Entry<ResourceLocation, JsonElement> entry : objects.entrySet()) {
         ResourceLocation location = entry.getKey();
         String speciesId = location.getPath();
         if (!speciesId.startsWith("_")) {
            try {
               JsonObject root = entry.getValue().getAsJsonObject();
               Map<String, NametagSkinConfig> speciesMap = REGISTRY.computeIfAbsent(speciesId, k -> new ConcurrentHashMap<>());

               for (Entry<String, JsonElement> skinEntry : root.entrySet()) {
                  String customName = skinEntry.getKey().toLowerCase();
                  JsonObject skinObj = skinEntry.getValue().getAsJsonObject();
                  boolean applySaddle = GsonHelper.getAsBoolean(skinObj, "apply_saddle", false);
                  boolean hasTexture = skinObj.has("texture");
                  boolean hasGenetics = skinObj.has("genetics");
                  if (hasTexture == hasGenetics) {
                     throw new IllegalArgumentException("'" + customName + "' names a texture or a genetics preset, one of the two");
                  }

                  NametagSkinConfig config = hasTexture
                     ? new NametagSkinConfig(new ResourceLocation(GsonHelper.getAsString(skinObj, "texture")), applySaddle, GeneticsPreset.NONE)
                     : new NametagSkinConfig(null, applySaddle, GeneticsPreset.parse(GsonHelper.getAsJsonObject(skinObj, "genetics")));
                  speciesMap.put(customName, config);
                  count++;
               }
            } catch (Exception e) {
               LOGGER.error("[NametagSkins] Failed to parse {}: {}", location, e.getMessage());
            }
         }
      }

      LOGGER.debug("[NametagSkins] Loaded {} custom nametag skins.", count);
   }

   public static NametagSkinConfig getConfig(String speciesId, String customName) {
      if (speciesId != null && customName != null) {
         Map<String, NametagSkinConfig> speciesMap = REGISTRY.get(speciesId);
         return speciesMap != null ? speciesMap.get(customName.toLowerCase()) : null;
      } else {
         return null;
      }
   }
}
