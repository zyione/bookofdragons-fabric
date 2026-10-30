package net.magister.liberanimalium.spawn;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.Map.Entry;
import org.jetbrains.annotations.Nullable;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SpawnRules extends SimpleJsonResourceReloadListener {
   private static final Logger LOGGER = LoggerFactory.getLogger("SpawnRules");
   private static final Gson GSON = new GsonBuilder().create();
   private static volatile Map<String, List<SpawnRule>> byHost = Map.of();
   private static volatile Map<ResourceLocation, SpawnRule> byId = Map.of();
   private static volatile Map<ResourceLocation, String> rawJson = Map.of();

   public SpawnRules() {
      super(GSON, "spawn");
   }

   public static List<SpawnRule> of(String host) {
      return byHost.getOrDefault(host, List.of());
   }

   public static Collection<String> hosts() {
      return byHost.keySet();
   }

   @Nullable
   public static SpawnRule byId(ResourceLocation id) {
      return byId.get(id);
   }

   public static Collection<ResourceLocation> ids() {
      return byId.keySet();
   }

   public static void installAll(Collection<SpawnRule> rules) {
      Map<String, List<SpawnRule>> hosts = new TreeMap<>();
      Map<ResourceLocation, SpawnRule> ids = new LinkedHashMap<>();

      for (SpawnRule rule : rules) {
         hosts.computeIfAbsent(rule.host(), h -> new ArrayList<>()).add(rule);
         ids.put(rule.id(), rule);
      }

      hosts.replaceAll((h, list) -> List.copyOf((Collection<? extends SpawnRule>)list));
      byHost = Map.copyOf(hosts);
      byId = Map.copyOf(ids);
   }

   public static Map<ResourceLocation, String> rawJson() {
      return rawJson;
   }

   protected void apply(Map<ResourceLocation, JsonElement> files, ResourceManager manager, ProfilerFiller profiler) {
      install(files);
   }

   public static void install(Map<ResourceLocation, JsonElement> files) {
      List<SpawnRule> rules = new ArrayList<>();
      Map<ResourceLocation, String> raw = new LinkedHashMap<>();

      for (Entry<ResourceLocation, JsonElement> file : new TreeMap<>(files).entrySet()) {
         String path = file.getKey().getPath();
         if (!path.startsWith("_") && !path.contains("/_")) {
            try {
               rules.addAll(
                  SpawnRule.parseFile(
                     file.getKey(), file.getValue().getAsJsonObject(), problem -> LOGGER.warn("[SpawnRules] {}: {} (left out)", file.getKey(), problem)
                  )
               );
               raw.put(file.getKey(), file.getValue().toString());
            } catch (RuntimeException e) {
               LOGGER.warn("[SpawnRules] {}: left out: {}", file.getKey(), e.getMessage());
            }
         }
      }

      installAll(rules);
      rawJson = Collections.unmodifiableMap(raw);
      LOGGER.debug("[SpawnRules] loaded {} rule(s) for {}", rules.size(), byHost.keySet());
   }
}
