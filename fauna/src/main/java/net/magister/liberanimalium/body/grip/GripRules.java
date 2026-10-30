package net.magister.liberanimalium.body.grip;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.IOException;
import java.io.Reader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;
import net.magister.liberanimalium.body.SpeciesAssets;
import net.magister.liberanimalium.body.data.FaunaWeightClass;
import net.magister.liberanimalium.body.gameplay.SpeciesGameplayProfile;
import net.magister.liberanimalium.body.physics.PhysicsSheet;
import net.magister.liberanimalium.chassis.FaunaBase;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimplePreparableReloadListener;
import net.minecraft.tags.TagKey;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class GripRules {
   private static final Logger LOGGER = LoggerFactory.getLogger("GripRules");
   public static final String PATH = "fauna/grip.json";
   public static final double DEFAULT_DENSITY = 150.0;
   private static volatile GripRules.Rules rules = GripRules.Rules.EMPTY;
   private static volatile Map<String, String> raw = Map.of();

   private GripRules() {
   }

   public static double size(Entity entity) {
      float base = entity.getType().getHeight();
      return base > 0.0F ? entity.getBbHeight() / base : 1.0;
   }

   public static double mass(Entity entity) {
      if (entity instanceof FaunaBase body) {
         double classMass = weight(body).mass();
         if (classMass > 0.0) {
            double size = size(body);
            return classMass * size * size * size;
         }
      }

      GripRules.Rules r = rules;
      ResourceLocation id = BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType());
      Double listed = id == null ? null : r.byType.get(id);
      if (listed == null) {
         for (Entry<TagKey<EntityType<?>>, Double> tag : r.byTag.entrySet()) {
            if (entity.getType().is(tag.getKey())) {
               listed = tag.getValue();
               break;
            }
         }
      }

      if (listed != null) {
         return listed;
      }

      double width = entity.getBbWidth();
      return width * width * entity.getBbHeight() * r.density;
   }

   public static double structureMass(double sableMass) {
      return sableMass * rules.density;
   }

   public static double capacity(FaunaBase holder) {
      return mass(holder) * weight(holder).lift();
   }

   public static boolean ungrabbable(Entity entity) {
      GripRules.Rules r = rules;
      ResourceLocation id = BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType());
      if (id != null && r.refusedTypes.contains(id)) {
         return true;
      }

      for (TagKey<EntityType<?>> tag : r.refusedTags) {
         if (entity.getType().is(tag)) {
            return true;
         }
      }

      return false;
   }

   private static PhysicsSheet.Weight weight(FaunaBase body) {
      return PhysicsSheet.of(body).weight(weightClass(body));
   }

   public static FaunaWeightClass weightClass(FaunaBase body) {
      SpeciesGameplayProfile gameplay = body.gameplay();
      return gameplay != null && gameplay.weightClass() != null ? gameplay.weightClass() : body.getWeightClass();
   }

   public static Map<String, String> raw() {
      return raw;
   }

   public static void install(Map<String, JsonObject> files) {
      Map<String, String> text = new LinkedHashMap<>();

      for (Entry<String, JsonObject> file : files.entrySet()) {
         text.put(file.getKey(), file.getValue().toString());
      }

      raw = Collections.unmodifiableMap(text);
      Map<ResourceLocation, Double> byType = new HashMap<>();
      Map<TagKey<EntityType<?>>, Double> byTag = new LinkedHashMap<>();
      Set<ResourceLocation> refusedTypes = new HashSet<>();
      List<TagKey<EntityType<?>>> refusedTags = new ArrayList<>();
      double density = 150.0;

      for (Entry<String, JsonObject> file : files.entrySet()) {
         JsonObject json = file.getValue();
         if (json.has("default_density")) {
            density = json.get("default_density").getAsDouble();
         }

         if (json.has("masses") && json.get("masses").isJsonObject()) {
            for (Entry<String, JsonElement> entry : json.getAsJsonObject("masses").entrySet()) {
               String key = entry.getKey();
               double mass = entry.getValue().getAsDouble();
               if (key.startsWith("#")) {
                  ResourceLocation tag = ResourceLocation.tryParse(key.substring(1));
                  if (tag != null) {
                     byTag.put(TagKey.create(Registries.ENTITY_TYPE, tag), mass);
                  }
               } else {
                  ResourceLocation type = ResourceLocation.tryParse(key);
                  if (type != null) {
                     byType.put(type, mass);
                  }
               }
            }
         }

         if (json.has("ungrabbable") && json.get("ungrabbable").isJsonArray()) {
            for (JsonElement entry : json.getAsJsonArray("ungrabbable")) {
               String key = entry.getAsString();
               if (key.startsWith("#")) {
                  ResourceLocation tag = ResourceLocation.tryParse(key.substring(1));
                  if (tag != null) {
                     refusedTags.add(TagKey.create(Registries.ENTITY_TYPE, tag));
                  }
               } else {
                  ResourceLocation type = ResourceLocation.tryParse(key);
                  if (type != null) {
                     refusedTypes.add(type);
                  }
               }
            }
         }
      }

      rules = new GripRules.Rules(Map.copyOf(byType), byTag, density, Set.copyOf(refusedTypes), List.copyOf(refusedTags));
      if (!files.isEmpty()) {
         LOGGER.debug(
            "[Grip] loaded the grip rules of {}: {} masses, {} refused",
            new Object[]{files.keySet(), byType.size() + byTag.size(), refusedTypes.size() + refusedTags.size()}
         );
      }
   }

   public static final class Loader extends SimplePreparableReloadListener<Map<String, JsonObject>> {
      protected Map<String, JsonObject> prepare(ResourceManager manager, ProfilerFiller profiler) {
         Map<String, JsonObject> files = new LinkedHashMap<>();

         for (String namespace : SpeciesAssets.namespaces()) {
            ResourceLocation location = new ResourceLocation(namespace, "fauna/grip.json");
            Resource resource = (Resource)manager.getResource(location).orElse(null);
            if (resource != null) {
               try (Reader reader = resource.openAsReader()) {
                  files.put(namespace, JsonParser.parseReader(reader).getAsJsonObject());
               } catch (IOException | RuntimeException e) {
                  GripRules.LOGGER.error("[Grip] could not read {}: {}", location, e.toString());
               }
            }
         }

         return files;
      }

      protected void apply(Map<String, JsonObject> files, ResourceManager manager, ProfilerFiller profiler) {
         GripRules.install(files);
      }
   }

   private record Rules(
      Map<ResourceLocation, Double> byType,
      Map<TagKey<EntityType<?>>, Double> byTag,
      double density,
      Set<ResourceLocation> refusedTypes,
      List<TagKey<EntityType<?>>> refusedTags
   ) {
      static final GripRules.Rules EMPTY = new GripRules.Rules(Map.of(), Map.of(), 150.0, Set.of(), List.of());
   }
}
