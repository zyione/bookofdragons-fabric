package net.magister.liberanimalium.body.physics;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.IOException;
import java.io.Reader;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;
import net.magister.liberanimalium.body.SpeciesAssets;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimplePreparableReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PhysicsSheetLoader extends SimplePreparableReloadListener<Map<String, JsonObject>> {
   private static final Logger LOGGER = LoggerFactory.getLogger("PhysicsSheetLoader");
   public static final String PATH = "fauna/physics.json";
   public static volatile Map<String, String> RAW_JSON = Map.of();

   protected Map<String, JsonObject> prepare(ResourceManager manager, ProfilerFiller profiler) {
      Map<String, JsonObject> sheets = new LinkedHashMap<>();

      for (String namespace : SpeciesAssets.namespaces()) {
         ResourceLocation location = new ResourceLocation(namespace, "fauna/physics.json");
         Resource resource = (Resource)manager.getResource(location).orElse(null);
         if (resource != null) {
            try (Reader reader = resource.openAsReader()) {
               sheets.put(namespace, JsonParser.parseReader(reader).getAsJsonObject());
            } catch (IOException e) {
               throw new IllegalStateException("could not read " + location, e);
            }
         }
      }

      if (sheets.isEmpty()) {
         LOGGER.warn("[Physics] no host ships {} - the physics sheet (looked in {}); no sheet installed", "fauna/physics.json", SpeciesAssets.namespaces());
      }

      return sheets;
   }

   protected void apply(Map<String, JsonObject> json, ResourceManager manager, ProfilerFiller profiler) {
      if (!json.isEmpty()) {
         Map<String, PhysicsSheet> sheets = new LinkedHashMap<>();
         Map<String, String> raw = new LinkedHashMap<>();

         for (Entry<String, JsonObject> sheet : json.entrySet()) {
            String host = sheet.getKey();

            try {
               sheets.put(host, PhysicsSheet.parse(sheet.getValue()));
               raw.put(host, sheet.getValue().toString());
            } catch (RuntimeException e) {
               PhysicsSheet lastGood = PhysicsSheet.installed(host);
               String lastRaw = RAW_JSON.get(host);
               if (lastGood == null || lastRaw == null) {
                  throw e;
               }

               LOGGER.error("[Physics] {}'s {} did not load; it keeps the sheet it had: {}", new Object[]{host, "fauna/physics.json", e.getMessage()});
               sheets.put(host, lastGood);
               raw.put(host, lastRaw);
            }
         }

         PhysicsSheet.installAll(sheets);
         RAW_JSON = Collections.unmodifiableMap(raw);
         LOGGER.debug("[Physics] loaded the physics sheets of {}", sheets.keySet());
      }
   }
}
