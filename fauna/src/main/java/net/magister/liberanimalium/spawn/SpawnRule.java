package net.magister.liberanimalium.spawn;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.Map.Entry;
import java.util.function.Consumer;
import org.jetbrains.annotations.Nullable;
import net.magister.liberanimalium.body.SpeciesAssets;
import net.magister.liberanimalium.genetics.GeneticsPreset;
import net.magister.liberanimalium.platform.registry.Regs;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public record SpawnRule(
   ResourceLocation id,
   EntityType<?> type,
   String host,
   int weight,
   int groupMin,
   int groupMax,
   int worldCap,
   int radiusCap,
   int radiusMax,
   Placement placement,
   List<SpawnCondition> conditions,
   List<SpawnRule.Special> specials,
   GeneticsPreset genetics,
   @Nullable ResourceLocation variant,
   @Nullable SpawnRule.Beacon beacon
) {
   private static final Logger LOGGER = LoggerFactory.getLogger("SpawnRules");
   private static final Set<String> SPAWN_KEYS = Set.of(
      "entity", "weight", "group", "world_cap", "radius_cap", "placement", "genetics", "variant", "conditions"
   );
   private static final Set<String> ROOT_KEYS = Set.of("where", "when", "special", "deviants", "beacon");
   private static final Set<String> DEVIANT_KEYS = Set.of("where", "when", "special");

   public String typeId() {
      return Regs.entityTypeKey(this.type).toString();
   }

   public float chance() {
      return Math.min(1.0F, this.weight / 100.0F);
   }

   public List<SpawnRule.Special> held(SpawnContext context) {
      List<SpawnRule.Special> out = new ArrayList<>();

      for (SpawnRule.Special special : this.specials) {
         if (special.condition().test(context)) {
            out.add(special);
         }
      }

      return out;
   }

   public SpawnRule resolve(List<SpawnRule.Special> held) {
      if (held.isEmpty()) {
         return this;
      }

      int weight = this.weight;
      int groupMin = this.groupMin;
      int groupMax = this.groupMax;
      int worldCap = this.worldCap;
      int radiusCap = this.radiusCap;
      int radiusMax = this.radiusMax;
      GeneticsPreset genetics = this.genetics;
      ResourceLocation variant = this.variant;

      for (SpawnRule.Special special : held) {
         weight += special.weight();
         SpawnRule.Overrides set = special.set();
         if (set.groupMin() != null) {
            groupMin = set.groupMin();
         }

         if (set.groupMax() != null) {
            groupMax = set.groupMax();
         }

         if (set.worldCap() != null) {
            worldCap = set.worldCap();
         }

         if (set.radiusCap() != null) {
            radiusCap = set.radiusCap();
         }

         if (set.radiusMax() != null) {
            radiusMax = set.radiusMax();
         }

         if (set.genetics() != null) {
            genetics = set.genetics();
         }

         if (set.variant() != null) {
            variant = set.variant();
         }
      }

      return new SpawnRule(
         this.id,
         this.type,
         this.host,
         weight,
         groupMin,
         groupMax,
         worldCap,
         radiusCap,
         radiusMax,
         this.placement,
         this.conditions,
         this.specials,
         genetics,
         variant,
         this.beacon
      );
   }

   public static List<SpawnRule> parseFile(ResourceLocation id, JsonObject json, Consumer<String> problem) {
      for (String key : json.keySet()) {
         if (!key.startsWith("__") && !SPAWN_KEYS.contains(key) && !ROOT_KEYS.contains(key)) {
            if (key.equals("rules") || key.equals("habitats") || key.equals("chance")) {
               throw new IllegalArgumentException("\"" + key + "\" is not a key: a species is where, when, special and deviants, and weight is its one number");
            }

            LOGGER.warn("[SpawnRules] {}: unknown key '{}' ignored", id, key);
         }
      }

      if (!json.has("entity")) {
         throw new IllegalArgumentException("no \"entity\"");
      }

      List<SpawnRule> out = new ArrayList<>();
      List<JsonElement> when = list(json, "when");
      List<SpawnRule.Special> specials = specials(id, json, problem, "special");
      readPlaces(id, json, json, when, specials, out, problem, "");
      if (json.has("beacon")) {
         JsonObject b = json.getAsJsonObject("beacon");

         try {
            out.add(parseBeacon(idOf(id, "beacon"), json, b, when, specials));
         } catch (RuntimeException e) {
            problem.accept("beacon: " + e.getMessage());
         }
      }

      if (json.has("deviants")) {
         if (!json.get("deviants").isJsonObject()) {
            throw new IllegalArgumentException("\"deviants\" must be an object of named deviants");
         }

         for (Entry<String, JsonElement> entry : json.getAsJsonObject("deviants").entrySet()) {
            String name = entry.getKey();
            if (!name.startsWith("__")) {
               if (entry.getValue().isJsonObject() && entry.getValue().getAsJsonObject().has("variant")) {
                  JsonObject deviant = entry.getValue().getAsJsonObject();

                  for (String key : deviant.keySet()) {
                     if (!key.startsWith("__") && !SPAWN_KEYS.contains(key) && !DEVIANT_KEYS.contains(key)) {
                        LOGGER.warn("[SpawnRules] {}: deviants.{}: unknown key '{}' ignored", new Object[]{id, name, key});
                     }
                  }

                  JsonObject base = withDefaults(json, deviant);
                  readPlaces(
                     id, base, deviant, list(deviant, "when"), specials(id, deviant, problem, "deviants." + name + ".special"), out, problem, name + "/"
                  );
               } else {
                  problem.accept("deviants." + name + ": an object that names its \"variant\"");
               }
            }
         }
      }

      if (out.isEmpty()) {
         throw new IllegalArgumentException("no spawn of the file reads");
      } else {
         return out;
      }
   }

   private static void readPlaces(
      ResourceLocation id,
      JsonObject defaults,
      JsonObject owner,
      List<JsonElement> when,
      List<SpawnRule.Special> specials,
      List<SpawnRule> out,
      Consumer<String> problem,
      String prefix
   ) {
      if (!owner.has("where")) {
         String name = prefix.isEmpty() ? "" : prefix.substring(0, prefix.length() - 1);

         try {
            out.add(parse(name.isEmpty() ? id : idOf(id, name), merged(defaults, null, when), specials));
         } catch (RuntimeException e) {
            problem.accept((name.isEmpty() ? "the spawn" : "deviants." + name) + ": " + e.getMessage());
         }
      } else if (!owner.get("where").isJsonObject()) {
         problem.accept(prefix + "where: an object of named places");
      } else {
         for (Entry<String, JsonElement> entry : owner.getAsJsonObject("where").entrySet()) {
            String name = entry.getKey();
            if (!name.startsWith("__")) {
               if (!entry.getValue().isJsonObject()) {
                  problem.accept("where." + prefix + name + ": an object");
               } else {
                  try {
                     out.add(parse(idOf(id, prefix + name), merged(defaults, entry.getValue().getAsJsonObject(), when), specials));
                  } catch (RuntimeException e) {
                     problem.accept((prefix.isEmpty() ? "where." : "deviants." + prefix + "where.") + name + ": " + e.getMessage());
                  }
               }
            }
         }
      }
   }

   private static SpawnRule parseBeacon(ResourceLocation beaconId, JsonObject root, JsonObject b, List<JsonElement> when, List<SpawnRule.Special> specials) {
      SpawnRule.Beacon beacon = new SpawnRule.Beacon(
         b.has("radius") ? b.get("radius").getAsInt() : 24,
         b.has("every_ticks") ? b.get("every_ticks").getAsInt() : 6000,
         b.has("max_kin") ? b.get("max_kin").getAsInt() : 6
      );
      if (beacon.radius() >= 1 && beacon.radius() <= 64 && beacon.everyTicks() >= 1 && beacon.maxKin() >= 1) {
         JsonObject own = new JsonObject();

         for (Entry<String, JsonElement> e : b.entrySet()) {
            if (SPAWN_KEYS.contains(e.getKey())) {
               own.add(e.getKey(), e.getValue());
            }
         }

         SpawnRule spawn = parse(beaconId, merged(root, own, when), specials);
         return new SpawnRule(
            spawn.id(),
            spawn.type(),
            spawn.host(),
            spawn.weight(),
            spawn.groupMin(),
            spawn.groupMax(),
            spawn.worldCap(),
            spawn.radiusCap(),
            spawn.radiusMax(),
            spawn.placement(),
            spawn.conditions(),
            spawn.specials(),
            spawn.genetics(),
            spawn.variant(),
            beacon
         );
      } else {
         throw new IllegalArgumentException("1 <= radius <= 64, every_ticks >= 1, max_kin >= 1");
      }
   }

   private static List<SpawnRule.Special> specials(ResourceLocation id, JsonObject owner, Consumer<String> problem, String what) {
      List<SpawnRule.Special> out = new ArrayList<>();
      if (!owner.has("special")) {
         return out;
      }

      if (!owner.get("special").isJsonArray()) {
         throw new IllegalArgumentException("\"special\" must be a list");
      }

      int i = 0;

      for (JsonElement e : owner.getAsJsonArray("special")) {
         try {
            out.add(SpawnRule.Special.parse(id, e));
         } catch (RuntimeException bad) {
            problem.accept(what + "[" + i + "]: " + bad.getMessage());
         }

         i++;
      }

      return List.copyOf(out);
   }

   private static List<JsonElement> list(JsonObject owner, String key) {
      List<JsonElement> out = new ArrayList<>();
      if (!owner.has(key)) {
         return out;
      }

      if (!owner.get(key).isJsonArray()) {
         throw new IllegalArgumentException("\"" + key + "\" must be a list of conditions");
      }

      for (JsonElement e : owner.getAsJsonArray(key)) {
         out.add(e);
      }

      return out;
   }

   private static ResourceLocation idOf(ResourceLocation file, String name) {
      ResourceLocation out = ResourceLocation.tryParse(file.getNamespace() + ":" + file.getPath() + "/" + name);
      if (out == null) {
         throw new IllegalArgumentException("'" + name + "' is not a name of [a-z0-9_.-/]");
      } else {
         return out;
      }
   }

   private static JsonObject withDefaults(JsonObject root, JsonObject deviant) {
      JsonObject out = new JsonObject();

      for (Entry<String, JsonElement> e : root.entrySet()) {
         if (SPAWN_KEYS.contains(e.getKey())) {
            out.add(e.getKey(), e.getValue());
         }
      }

      for (Entry<String, JsonElement> e : deviant.entrySet()) {
         if (SPAWN_KEYS.contains(e.getKey())) {
            out.add(e.getKey(), e.getValue());
         }
      }

      return out;
   }

   static JsonObject merged(JsonObject defaults, @Nullable JsonObject place, List<JsonElement> when) {
      JsonObject out = new JsonObject();

      for (Entry<String, JsonElement> e : defaults.entrySet()) {
         if (SPAWN_KEYS.contains(e.getKey()) && !e.getKey().equals("conditions")) {
            out.add(e.getKey(), e.getValue());
         }
      }

      if (place != null) {
         for (Entry<String, JsonElement> e : place.entrySet()) {
            if (!e.getKey().startsWith("__") && !e.getKey().equals("conditions")) {
               if (!SPAWN_KEYS.contains(e.getKey())) {
                  throw new IllegalArgumentException("unknown key '" + e.getKey() + "' (a place takes " + SPAWN_KEYS + ")");
               }

               out.add(e.getKey(), e.getValue());
            }
         }
      }

      JsonArray conditions = new JsonArray();

      for (JsonElement c : when) {
         conditions.add(c);
      }

      if (place != null && place.has("conditions")) {
         conditions.addAll(place.getAsJsonArray("conditions"));
      }

      out.add("conditions", conditions);
      return out;
   }

   private static int[] groupOf(JsonObject group) {
      int min = group.has("min") ? group.get("min").getAsInt() : 1;
      int max = group.has("max") ? group.get("max").getAsInt() : min;
      if (min >= 1 && max >= min) {
         return new int[]{min, max};
      } else {
         throw new IllegalArgumentException("group: 1 <= min <= max");
      }
   }

   private static int[] radiusCapOf(JsonObject cap) {
      int radius = cap.get("radius").getAsInt();
      int max = cap.get("max").getAsInt();
      if (radius >= 1 && max >= 0) {
         return new int[]{radius, max};
      } else {
         throw new IllegalArgumentException("radius_cap: radius >= 1, max >= 0");
      }
   }

   private static ResourceLocation variantOf(ResourceLocation id, String name) {
      ResourceLocation out = ResourceLocation.tryParse(name.contains(":") ? name : id.getNamespace() + ":" + name);
      if (out == null) {
         throw new IllegalArgumentException("variant: not an id: " + name);
      } else {
         return out;
      }
   }

   public static SpawnRule parse(ResourceLocation id, JsonObject json, List<SpawnRule.Special> specials) {
      if (!json.has("entity")) {
         throw new IllegalArgumentException("no \"entity\"");
      }

      String word = json.get("entity").getAsString();
      ResourceLocation typeId = ResourceLocation.tryParse(word.contains(":") ? word : id.getNamespace() + ":" + word);
      if (typeId != null && Regs.hasEntityType(typeId)) {
         EntityType<?> type = Regs.entityType(typeId);
         int weight = json.has("weight") ? json.get("weight").getAsInt() : 10;
         if (weight < 1) {
            throw new IllegalArgumentException("weight below 1");
         }

         int[] group = json.has("group") ? groupOf(json.getAsJsonObject("group")) : new int[]{1, 1};
         int worldCap = json.has("world_cap") ? json.get("world_cap").getAsInt() : -1;
         int[] cap = json.has("radius_cap") ? radiusCapOf(json.getAsJsonObject("radius_cap")) : new int[]{0, 0};
         Placement placement = Placement.GROUND;
         if (json.has("placement")) {
            placement = Placement.parse(json.get("placement").getAsString());
            if (placement == null) {
               throw new IllegalArgumentException("placement: ground, air, cave or fluid");
            }
         }

         List<SpawnCondition> conditions = new ArrayList<>();
         if (json.has("conditions")) {
            for (JsonElement c : json.getAsJsonArray("conditions")) {
               conditions.add(SpawnCondition.parse(c));
            }
         }

         GeneticsPreset genetics = json.has("genetics") ? GeneticsPreset.parse(json.getAsJsonObject("genetics")) : GeneticsPreset.NONE;
         ResourceLocation variant = json.has("variant") ? variantOf(id, json.get("variant").getAsString()) : null;
         return new SpawnRule(
            id,
            type,
            SpeciesAssets.hostOf(type),
            weight,
            group[0],
            group[1],
            worldCap,
            cap[0],
            cap[1],
            placement,
            List.copyOf(conditions),
            List.copyOf(specials),
            genetics,
            variant,
            null
         );
      } else {
         throw new IllegalArgumentException("no entity type " + word);
      }
   }

   public static SpawnRule parse(ResourceLocation id, JsonObject json) {
      return parse(id, json, List.of());
   }

   public record Beacon(int radius, int everyTicks, int maxKin) {
   }

   public record Overrides(
      @Nullable Integer groupMin,
      @Nullable Integer groupMax,
      @Nullable Integer worldCap,
      @Nullable Integer radiusCap,
      @Nullable Integer radiusMax,
      @Nullable GeneticsPreset genetics,
      @Nullable ResourceLocation variant
   ) {
      public static final SpawnRule.Overrides NONE = new SpawnRule.Overrides(null, null, null, null, null, null, null);
      private static final Set<String> KEYS = Set.of("group", "world_cap", "radius_cap", "genetics", "variant");

      static SpawnRule.Overrides parse(ResourceLocation id, JsonObject set) {
         for (String key : set.keySet()) {
            if (!key.startsWith("__") && !KEYS.contains(key)) {
               throw new IllegalArgumentException("set." + key + ": a special sets " + KEYS + " only");
            }
         }

         Integer groupMin = null;
         Integer groupMax = null;
         if (set.has("group")) {
            int[] group = SpawnRule.groupOf(set.getAsJsonObject("group"));
            groupMin = group[0];
            groupMax = group[1];
         }

         Integer radiusCap = null;
         Integer radiusMax = null;
         if (set.has("radius_cap")) {
            int[] cap = SpawnRule.radiusCapOf(set.getAsJsonObject("radius_cap"));
            radiusCap = cap[0];
            radiusMax = cap[1];
         }

         return new SpawnRule.Overrides(
            groupMin,
            groupMax,
            set.has("world_cap") ? set.get("world_cap").getAsInt() : null,
            radiusCap,
            radiusMax,
            set.has("genetics") ? GeneticsPreset.parse(set.getAsJsonObject("genetics")) : null,
            set.has("variant") ? SpawnRule.variantOf(id, set.get("variant").getAsString()) : null
         );
      }
   }

   public record Special(SpawnCondition condition, int weight, SpawnRule.Overrides set) {
      static SpawnRule.Special parse(ResourceLocation id, JsonElement element) {
         if (!element.isJsonObject()) {
            throw new IllegalArgumentException("a special is an object: " + element);
         }

         JsonObject object = element.getAsJsonObject();
         JsonObject bare = new JsonObject();

         for (Entry<String, JsonElement> e : object.entrySet()) {
            String key = e.getKey();
            if (!key.equals("weight") && !key.equals("set") && !key.startsWith("__")) {
               bare.add(key, e.getValue());
            }
         }

         SpawnCondition condition = SpawnCondition.parse(bare);
         int weight = object.has("weight") ? object.get("weight").getAsInt() : 0;
         if (weight < 0) {
            throw new IllegalArgumentException(condition.name() + ": a special's weight is 0 or more");
         } else {
            SpawnRule.Overrides set = object.has("set") ? SpawnRule.Overrides.parse(id, object.getAsJsonObject("set")) : SpawnRule.Overrides.NONE;
            if (weight == 0 && set.equals(SpawnRule.Overrides.NONE)) {
               throw new IllegalArgumentException(condition.name() + ": a special adds weight, or sets something, or both");
            } else {
               return new SpawnRule.Special(condition, weight, set);
            }
         }
      }
   }
}
