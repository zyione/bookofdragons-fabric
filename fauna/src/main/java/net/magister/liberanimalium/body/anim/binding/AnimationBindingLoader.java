package net.magister.liberanimalium.body.anim.binding;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.mojang.logging.LogUtils;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Predicate;
import java.util.function.Supplier;
import org.jetbrains.annotations.Nullable;
import mod.azure.azurelib.animation.easing.AzEasingTypeRegistry;
import mod.azure.azurelib.animation.easing.AzEasingTypes;
import net.magister.liberanimalium.anim.event.EventTable;
import net.magister.liberanimalium.anim.mask.BoneMask;
import net.magister.liberanimalium.anim.rig.RigReport;
import net.magister.liberanimalium.anim.rig.RigSpec;
import net.magister.liberanimalium.anim.rig.StepsSpec;
import net.magister.liberanimalium.anim.v2.BindingsV2;
import net.magister.liberanimalium.anim.v2.LayerSpec;
import net.magister.liberanimalium.anim.validate.BindingReport;
import net.magister.liberanimalium.anim.validate.BindingValidator;
import net.magister.liberanimalium.body.SpeciesAssets;
import net.magister.liberanimalium.platform.Services;
import net.magister.liberanimalium.render.FaunaProceduralAnimator;
import net.magister.liberanimalium.sound.FaunaSoundRegistry;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import org.slf4j.Logger;

public final class AnimationBindingLoader {
   private static final Logger LOGGER = LogUtils.getLogger();
   private static final Gson GSON = new Gson();
   private static final String BINDING_PATH = "animation_bindings/";
   private static final Map<String, AnimationBindingTable> CACHE = new ConcurrentHashMap<>();
   private static final Map<ResourceLocation, AnimationBindingTable> DEFAULTS = new ConcurrentHashMap<>();
   private static final Map<String, BindingReport> LAST_REPORTS = new ConcurrentHashMap<>();

   private AnimationBindingLoader() {
   }

   public static void invalidate() {
      CACHE.clear();
      LAST_REPORTS.clear();
      RigReport.invalidate();
      DEFAULTS.clear();
   }

   public static Map<String, BindingReport> reports() {
      return Map.copyOf(LAST_REPORTS);
   }

   private static Predicate<String> passCheck() {
      return !Services.platform().isClient() ? name -> true : FaunaProceduralAnimator.passNames()::contains;
   }

   private static void report(String species, JsonObject json) {
      BindingReport report = BindingValidator.validate(
         species, json, clipCheck(species), AnimSignals::isRegistered, voiceCheck(species), passCheck(), AnimSignals::isScalarKey, easingCheck()
      );
      LAST_REPORTS.put(species, report);
      if (!report.isClean()) {
         LOGGER.warn(
            "[AnimationBinding] {}: {} unresolved reference(s) in {} row(s) checked", new Object[]{species, report.problems().size(), report.rowsChecked()}
         );

         for (String problem : report.problems()) {
            LOGGER.warn("[AnimationBinding]   {}: {}", species, problem);
         }
      }
   }

   private static Predicate<String> easingCheck() {
      return name -> AzEasingTypes.LINEAR == null ? false : AzEasingTypeRegistry.getOrNull(LayerSpec.easingKey(name)) != null;
   }

   private static Predicate<String> voiceCheck(String species) {
      return FaunaSoundRegistry.species().isEmpty() ? name -> true : name -> FaunaSoundRegistry.of(species).voice(name) != null;
   }

   private static Predicate<String> clipCheck(String species) {
      Set<String> clips = clipsOf(species);
      return clips.isEmpty() ? name -> true : clips::contains;
   }

   private static Set<String> clipsOf(String species) {
      ResourceLocation loc = SpeciesAssets.locate("animations/" + species + ".animation.json");
      Optional<Resource> res = fetchResource(loc);
      if (res.isEmpty()) {
         return Set.of();
      }

      JsonObject json = parseJson(res.get(), loc);
      return json != null && json.has("animations") ? Set.copyOf(json.getAsJsonObject("animations").keySet()) : Set.of();
   }

   public static AnimationBindingTable get(String species) {
      return CACHE.computeIfAbsent(species, AnimationBindingLoader::load);
   }

   private static AnimationBindingTable load(String species) {
      ResourceLocation loc = SpeciesAssets.locate("animation_bindings/" + species + ".json");
      Optional<Resource> res = fetchResource(loc);
      JsonObject json = res.isEmpty() ? null : parseJson(res.get(), loc);
      if (json == null) {
         AnimationBindingTable base = getDefault(species);
         return new AnimationBindingTable(species, base.steps, base.events, base.v2, base.boneGroups, base.rig);
      } else {
         return parseTable(species, resolve(species, loc, json, () -> model(species)));
      }
   }

   private static JsonObject resolve(String species, ResourceLocation loc, JsonObject json, Supplier<BindingSource.Model> model) {
      List<String> problems = new ArrayList<>();
      JsonObject out = BindingSource.resolve(json, name -> {
         ResourceLocation at = SpeciesAssets.locateFor(loc.getNamespace(), "animation_bindings/templates/" + name + ".json");
         return fetchResource(at).map(r -> parseJson(r, at)).orElse(null);
      }, model, problems);

      for (String problem : problems) {
         LOGGER.warn("[AnimationBinding] {}: {}", species, problem);
      }

      return out;
   }

   @Nullable
   private static BindingSource.Model model(String species) {
      ResourceLocation loc = SpeciesAssets.locate("geo/" + species + ".geo.json");
      return fetchResource(loc).map(r -> BindingSource.Model.of(parseJson(r, loc))).orElse(null);
   }

   private static AnimationBindingTable getDefault(String species) {
      ResourceLocation loc = SpeciesAssets.locateFor(SpeciesAssets.hostOfSpecies(species), "animation_bindings/default.json");
      return DEFAULTS.computeIfAbsent(loc, AnimationBindingLoader::parseDefault);
   }

   private static AnimationBindingTable parseDefault(ResourceLocation loc) {
      Optional<Resource> res = fetchResource(loc);
      return res.isEmpty()
         ? new AnimationBindingTable("default", StepsSpec.EMPTY)
         : parseTable("default", resolve("default", loc, parseJson(res.get(), loc), () -> null));
   }

   private static AnimationBindingTable parseTable(String species, JsonObject json) {
      if (json == null) {
         return new AnimationBindingTable(species, StepsSpec.EMPTY);
      }

      StepsSpec steps = json.has("steps") ? StepsSpec.parse(json.getAsJsonObject("steps"), AnimSignals::isScalarKey) : StepsSpec.EMPTY;
      EventTable events = json.has("events") ? EventTable.parse(json.getAsJsonObject("events")) : EventTable.EMPTY;
      report(species, json);
      BindingsV2 v2 = BindingsV2.parse(json, AnimSignals::isScalarKey);
      Map<String, BoneMask> groups = json.has("bone_groups") ? BoneMask.parseGroups(json.getAsJsonObject("bone_groups")) : Map.of();
      RigSpec rig = json.has("rig") ? RigSpec.parse(json.getAsJsonObject("rig")) : RigSpec.EMPTY;
      return new AnimationBindingTable(species, steps, events, v2, groups, rig);
   }

   private static Optional<Resource> fetchResource(ResourceLocation loc) {
      try {
         return Minecraft.getInstance().getResourceManager().getResource(loc);
      } catch (Exception e) {
         return Optional.empty();
      }
   }

   private static JsonObject parseJson(Resource resource, ResourceLocation loc) {
      try (InputStreamReader reader = new InputStreamReader(resource.open())) {
         return (JsonObject)GSON.fromJson(reader, JsonObject.class);
      } catch (Exception e) {
         LOGGER.error("[AnimationBinding] Failed to parse {}: {}", loc, e.getMessage());
         return null;
      }
   }
}
