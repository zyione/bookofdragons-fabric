package net.magister.liberanimalium.variant;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.TreeMap;
import java.util.Map.Entry;
import org.jetbrains.annotations.Nullable;
import net.magister.liberanimalium.body.gameplay.VariantGameplay;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CreatureVariants extends SimpleJsonResourceReloadListener {
   private static final Logger LOGGER = LoggerFactory.getLogger("Variants");
   private static final Gson GSON = new GsonBuilder().create();
   private static volatile Map<ResourceLocation, CreatureVariant> byId = Map.of();
   private static volatile Map<ResourceLocation, String> rawJson = Map.of();

   public CreatureVariants() {
      super(GSON, "fauna/variants");
   }

   @Nullable
   public static CreatureVariant get(ResourceLocation id) {
      return byId.get(id);
   }

   public static Collection<CreatureVariant> all() {
      return byId.values();
   }

   public static void installAll(Collection<CreatureVariant> variants) {
      Map<ResourceLocation, CreatureVariant> ids = new LinkedHashMap<>();

      for (CreatureVariant v : variants) {
         ids.put(v.id(), v);
      }

      byId = Map.copyOf(ids);
      VariantGameplay.invalidate();
   }

   public static Map<ResourceLocation, String> rawJson() {
      return rawJson;
   }

   protected void apply(Map<ResourceLocation, JsonElement> files, ResourceManager manager, ProfilerFiller profiler) {
      install(files);
   }

   public static void install(Map<ResourceLocation, JsonElement> files) {
      Map<ResourceLocation, CreatureVariant> loaded = new LinkedHashMap<>();
      Map<ResourceLocation, String> raw = new LinkedHashMap<>();

      for (Entry<ResourceLocation, JsonElement> file : new TreeMap<>(files).entrySet()) {
         String path = file.getKey().getPath();
         if (!path.startsWith("_") && !path.contains("/_")) {
            try {
               CreatureVariant variant = CreatureVariant.parse(file.getKey(), file.getValue().getAsJsonObject());
               if (!variant.missing().isEmpty()) {
                  LOGGER.warn("[Variants] {}: its profile leaves out {}; the defaults fill them", file.getKey(), variant.missing());
               }

               loaded.put(variant.id(), variant);
               raw.put(file.getKey(), file.getValue().toString());
            } catch (RuntimeException e) {
               LOGGER.warn("[Variants] {}: left out: {}", file.getKey(), e.getMessage());
            }
         }
      }

      installAll(loaded.values());
      rawJson = Map.copyOf(raw);
      LOGGER.debug("[Variants] loaded {} variant(s)", loaded.size());
   }
}
