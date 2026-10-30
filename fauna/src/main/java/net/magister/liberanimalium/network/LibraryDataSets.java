package net.magister.liberanimalium.network;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.logging.LogUtils;
import com.mojang.serialization.JsonOps;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;
import net.magister.liberanimalium.body.gameplay.SpeciesGameplayLoader;
import net.magister.liberanimalium.body.gameplay.SpeciesGameplayProfile;
import net.magister.liberanimalium.body.gameplay.SpeciesGameplayRegistry;
import net.magister.liberanimalium.body.grip.GripRules;
import net.magister.liberanimalium.body.physics.PhysicsSheet;
import net.magister.liberanimalium.body.physics.PhysicsSheetLoader;
import net.magister.liberanimalium.chassis.component.melee.data.MeleeAttackDefinition;
import net.magister.liberanimalium.chassis.component.ranged.data.FaunaAttackManager;
import net.magister.liberanimalium.fauna.api.SpeciesProfiles;
import net.magister.liberanimalium.genetics.LinkRuleRegistry;
import net.magister.liberanimalium.genetics.PalettePoolRegistry;
import net.magister.liberanimalium.genetics.PaletteTaxonomyRegistry;
import net.magister.liberanimalium.genetics.config.SpeciesGeneticsConfig;
import net.magister.liberanimalium.genetics.config.SpeciesGeneticsConfigRegistry;
import net.magister.liberanimalium.genetics.config.SpeciesGeneticsJsonLoader;
import net.magister.liberanimalium.render.nametag.NametagLooks;
import net.magister.liberanimalium.sound.FaunaSoundLoader;
import net.magister.liberanimalium.sound.FaunaSoundRegistry;
import net.magister.liberanimalium.sound.FaunaSoundTable;
import net.magister.liberanimalium.spawn.SpawnRules;
import net.magister.liberanimalium.variant.CreatureVariants;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.GsonHelper;
import org.slf4j.Logger;

public final class LibraryDataSets {
   private static final Logger LOGGER = LogUtils.getLogger();

   private LibraryDataSets() {
   }

   public static void register() {
      DataSync.register("genetics", LibraryDataSets::geneticsRaw, LibraryDataSets::installGenetics);
      DataSync.register("physics", () -> PhysicsSheetLoader.RAW_JSON, LibraryDataSets::installPhysics);
      DataSync.register("gameplay", () -> SpeciesGameplayLoader.RAW_JSON, LibraryDataSets::installGameplay);
      DataSync.register("sounds", () -> FaunaSoundLoader.RAW_JSON, LibraryDataSets::installSounds);
      DataSync.register("melee", () -> FaunaAttackManager.RAW_JSON, LibraryDataSets::installMelee);
      DataSync.register("species", () -> byName(SpeciesProfiles.rawJson()), files -> SpeciesProfiles.install(parse(files, "species file")));
      DataSync.register("variants", () -> byName(CreatureVariants.rawJson()), files -> CreatureVariants.install(parse(files, "variant file")));
      DataSync.register("spawns", () -> byName(SpawnRules.rawJson()), files -> SpawnRules.install(parse(files, "spawn file")));
      DataSync.register("grip_rules", GripRules::raw, LibraryDataSets::installGripRules);
   }

   static Map<String, String> geneticsRaw() {
      Map<String, String> files = new LinkedHashMap<>();

      for (Entry<String, SpeciesGeneticsJsonLoader.RawLayers> species : SpeciesGeneticsJsonLoader.RAW_JSON_LAYERS.entrySet()) {
         files.put(species.getValue().namespace() + ":" + species.getKey(), "[" + String.join(",", species.getValue().json()) + "]");
      }

      return files;
   }

   private static void installGenetics(Map<String, String> files) {
      NametagLooks.clear();
      SpeciesGeneticsConfigRegistry.clear();
      LinkRuleRegistry.clear();
      PaletteTaxonomyRegistry.clear();
      PalettePoolRegistry.clear();

      for (Entry<String, String> file : files.entrySet()) {
         int colon = file.getKey().indexOf(58);
         String namespace = file.getKey().substring(0, colon);
         String speciesId = file.getKey().substring(colon + 1);
         SpeciesGeneticsConfig.Builder builder = SpeciesGeneticsConfig.builder(speciesId);

         JsonArray layers;
         try {
            layers = JsonParser.parseString(file.getValue()).getAsJsonArray();
         } catch (RuntimeException e) {
            layers = new JsonArray();
         }

         for (JsonElement layer : layers) {
            try {
               JsonObject root = GsonHelper.convertToJsonObject(layer, "species config");
               SpeciesGeneticsJsonLoader.parseSpeciesConfigInto(namespace, speciesId, root, builder);
               SpeciesGeneticsJsonLoader.registerLinkRules(namespace, speciesId, root);
               if (root.has("palette_taxonomy")) {
                  JsonObject taxonomy = GsonHelper.getAsJsonObject(root, "palette_taxonomy");
                  SpeciesGeneticsJsonLoader.parsePaletteTaxonomy(namespace, speciesId, taxonomy);
               }
            } catch (Exception var12) {
            }
         }

         SpeciesGeneticsConfigRegistry.register(speciesId, builder.build());
      }
   }

   private static void installPhysics(Map<String, String> files) {
      Map<String, PhysicsSheet> sheets = new LinkedHashMap<>();

      for (Entry<String, String> sheet : files.entrySet()) {
         try {
            sheets.put(sheet.getKey(), PhysicsSheet.parse(JsonParser.parseString(sheet.getValue()).getAsJsonObject()));
         } catch (Exception e) {
            LOGGER.error("[SpeciesGameplay] {}'s physics sheet did not parse on this client: {}", sheet.getKey(), e.getMessage());
         }
      }

      PhysicsSheet.installAll(sheets);
   }

   private static void installGameplay(Map<String, String> raw) {
      Map<String, JsonObject> files = new HashMap<>();

      for (Entry<String, String> entry : raw.entrySet()) {
         try {
            files.put(entry.getKey(), JsonParser.parseString(entry.getValue()).getAsJsonObject());
         } catch (Exception e) {
            LOGGER.error("[SpeciesGameplay] gameplay file {} did not parse on this client: {}", entry.getKey(), e.getMessage());
         }
      }

      try {
         SpeciesGameplayRegistry.installAll(SpeciesGameplayProfile.parseAll(files), files);
      } catch (Exception e) {
         LOGGER.error("[SpeciesGameplay] the server's gameplay data did not parse on this client: {}", e.getMessage());
      }
   }

   private static void installSounds(Map<String, String> files) {
      FaunaSoundRegistry.clear();

      for (Entry<String, String> entry : files.entrySet()) {
         try {
            JsonObject root = GsonHelper.convertToJsonObject(JsonParser.parseString(entry.getValue()), "body sounds");
            FaunaSoundRegistry.register(entry.getKey(), FaunaSoundTable.parse(root));
         } catch (Exception var4) {
         }
      }
   }

   private static void installMelee(Map<String, String> files) {
      Map<ResourceLocation, MeleeAttackDefinition> parsed = new HashMap<>();

      for (Entry<String, String> entry : files.entrySet()) {
         try {
            MeleeAttackDefinition.CODEC
               .parse(JsonOps.INSTANCE, JsonParser.parseString(entry.getValue()))
               .result()
               .ifPresent(def -> parsed.put(ResourceLocation.tryParse(entry.getKey()), def));
         } catch (Exception var5) {
         }
      }

      FaunaAttackManager.getInstance().acceptSynced(parsed);
   }

   private static void installGripRules(Map<String, String> files) {
      Map<String, JsonObject> parsed = new LinkedHashMap<>();

      for (Entry<String, String> file : files.entrySet()) {
         try {
            parsed.put(file.getKey(), JsonParser.parseString(file.getValue()).getAsJsonObject());
         } catch (RuntimeException e) {
            LOGGER.error("[Grip] {}'s grip rules did not parse on this client: {}", file.getKey(), e.getMessage());
         }
      }

      GripRules.install(parsed);
   }

   private static Map<String, String> byName(Map<ResourceLocation, String> files) {
      Map<String, String> named = new LinkedHashMap<>();

      for (Entry<ResourceLocation, String> file : files.entrySet()) {
         named.put(file.getKey().toString(), file.getValue());
      }

      return named;
   }

   private static Map<ResourceLocation, JsonElement> parse(Map<String, String> files, String what) {
      Map<ResourceLocation, JsonElement> parsed = new LinkedHashMap<>();

      for (Entry<String, String> file : files.entrySet()) {
         try {
            parsed.put(ResourceLocation.tryParse(file.getKey()), JsonParser.parseString(file.getValue()));
         } catch (RuntimeException e) {
            LOGGER.error("[Sync] {} {} did not parse on this client: {}", new Object[]{what, file.getKey(), e.getMessage()});
         }
      }

      return parsed;
   }
}
