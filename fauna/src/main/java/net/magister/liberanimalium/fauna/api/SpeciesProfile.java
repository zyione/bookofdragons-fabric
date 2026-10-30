package net.magister.liberanimalium.fauna.api;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mojang.logging.LogUtils;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import org.jetbrains.annotations.Nullable;
import net.magister.liberanimalium.fauna.move.MoveTuning;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.level.pathfinder.BlockPathTypes;
import org.slf4j.Logger;

public record SpeciesProfile(
   SpeciesProfile.Senses senses,
   SpeciesProfile.Movement movement,
   SpeciesProfile.Social social,
   Map<String, float[]> traits,
   Map<String, Float> driveWeights,
   Map<String, SpeciesProfile.Smoothing> smoothing,
   List<SpeciesProfile.RelationRule> relations,
   SpeciesProfile.Taming taming,
   DayCycle.Habit habit,
   SpeciesProfile.Calls calls,
   boolean roosts,
   SpeciesProfile.DriveSet drives
) {
   public static final SpeciesProfile DEFAULT = new SpeciesProfile(
      SpeciesProfile.Senses.DEFAULT, SpeciesProfile.Movement.DEFAULT, SpeciesProfile.Social.DEFAULT, Map.of(), Map.of(), Map.of(), List.of()
   );
   private static final Logger LOGGER = LogUtils.getLogger();

   public SpeciesProfile(
      SpeciesProfile.Senses senses,
      SpeciesProfile.Movement movement,
      SpeciesProfile.Social social,
      Map<String, float[]> traits,
      Map<String, Float> driveWeights,
      Map<String, SpeciesProfile.Smoothing> smoothing,
      List<SpeciesProfile.RelationRule> relations,
      SpeciesProfile.Taming taming,
      DayCycle.Habit habit,
      SpeciesProfile.Calls calls,
      boolean roosts
   ) {
      this(senses, movement, social, traits, driveWeights, smoothing, relations, taming, habit, calls, roosts, SpeciesProfile.DriveSet.NONE);
   }

   public SpeciesProfile(
      SpeciesProfile.Senses senses,
      SpeciesProfile.Movement movement,
      SpeciesProfile.Social social,
      Map<String, float[]> traits,
      Map<String, Float> driveWeights,
      Map<String, SpeciesProfile.Smoothing> smoothing,
      List<SpeciesProfile.RelationRule> relations,
      SpeciesProfile.Taming taming,
      DayCycle.Habit habit,
      SpeciesProfile.Calls calls
   ) {
      this(senses, movement, social, traits, driveWeights, smoothing, relations, taming, habit, calls, false);
   }

   public SpeciesProfile(
      SpeciesProfile.Senses senses,
      SpeciesProfile.Movement movement,
      SpeciesProfile.Social social,
      Map<String, float[]> traits,
      Map<String, Float> driveWeights,
      Map<String, SpeciesProfile.Smoothing> smoothing,
      List<SpeciesProfile.RelationRule> relations,
      SpeciesProfile.Taming taming,
      DayCycle.Habit habit
   ) {
      this(senses, movement, social, traits, driveWeights, smoothing, relations, taming, habit, SpeciesProfile.Calls.NONE);
   }

   public SpeciesProfile(
      SpeciesProfile.Senses senses,
      SpeciesProfile.Movement movement,
      SpeciesProfile.Social social,
      Map<String, float[]> traits,
      Map<String, Float> driveWeights,
      Map<String, SpeciesProfile.Smoothing> smoothing,
      List<SpeciesProfile.RelationRule> relations
   ) {
      this(senses, movement, social, traits, driveWeights, smoothing, relations, SpeciesProfile.Taming.DEFAULT, DayCycle.Habit.DEFAULT);
   }

   public SpeciesProfile(
      SpeciesProfile.Senses senses,
      SpeciesProfile.Movement movement,
      SpeciesProfile.Social social,
      Map<String, float[]> traits,
      Map<String, Float> driveWeights,
      Map<String, SpeciesProfile.Smoothing> smoothing,
      List<SpeciesProfile.RelationRule> relations,
      SpeciesProfile.Taming taming
   ) {
      this(senses, movement, social, traits, driveWeights, smoothing, relations, taming, DayCycle.Habit.DEFAULT);
   }

   public static SpeciesProfile parse(JsonObject root) {
      return parse(root, "unnamed_species");
   }

   public static SpeciesProfile parse(JsonObject root, String speciesId) {
      return parse(root, speciesId, null);
   }

   public static SpeciesProfile parse(JsonObject root, String speciesId, @Nullable String namespace) {
      Map<String, float[]> traits = new HashMap<>();
      if (root.has("traits")) {
         JsonObject t = GsonHelper.getAsJsonObject(root, "traits");

         for (String key : t.keySet()) {
            if (!key.startsWith("__")) {
               JsonArray arr = t.getAsJsonArray(key);
               if (arr != null && arr.size() == 2) {
                  traits.put(key, new float[]{arr.get(0).getAsFloat(), arr.get(1).getAsFloat()});
               }
            }
         }
      }

      Map<String, Float> weights = new HashMap<>();
      Map<String, SpeciesProfile.Smoothing> smoothing = new HashMap<>();
      if (root.has("drives")) {
         JsonObject d = GsonHelper.getAsJsonObject(root, "drives");
         if (d.has("weights")) {
            JsonObject w = GsonHelper.getAsJsonObject(d, "weights");

            for (String key : w.keySet()) {
               if (!key.startsWith("__")) {
                  weights.put(key, w.get(key).getAsFloat());
               }
            }
         }

         if (d.has("smoothing")) {
            JsonObject s = GsonHelper.getAsJsonObject(d, "smoothing");

            for (String key : s.keySet()) {
               if (!key.startsWith("__")) {
                  JsonObject rates = GsonHelper.getAsJsonObject(s, key);
                  smoothing.put(
                     key, new SpeciesProfile.Smoothing(GsonHelper.getAsFloat(rates, "rise", Float.NaN), GsonHelper.getAsFloat(rates, "decay", Float.NaN))
                  );
               }
            }
         }
      }

      return new SpeciesProfile(
         SpeciesProfile.Senses.parse(root.has("senses") ? GsonHelper.getAsJsonObject(root, "senses") : null),
         SpeciesProfile.Movement.parse(root.has("movement") ? GsonHelper.getAsJsonObject(root, "movement") : null),
         SpeciesProfile.Social.parse(root.has("social") ? GsonHelper.getAsJsonObject(root, "social") : null),
         Map.copyOf(traits),
         Map.copyOf(weights),
         Map.copyOf(smoothing),
         parseRelations(root),
         SpeciesProfile.Taming.parse(root, speciesId, namespace),
         parseHabit(root, speciesId),
         SpeciesProfile.Calls.parse(root),
         GsonHelper.getAsBoolean(root, "roosts", false),
         SpeciesProfile.DriveSet.parse(root.has("drives") ? GsonHelper.getAsJsonObject(root, "drives") : null)
      );
   }

   private static DayCycle.Habit parseHabit(JsonObject root, String speciesId) {
      if (!root.has("habit")) {
         return DayCycle.Habit.DEFAULT;
      }

      JsonObject o = GsonHelper.getAsJsonObject(root, "habit");
      String active = GsonHelper.getAsString(o, "active", "");
      if (active.isEmpty()) {
         return DayCycle.Habit.DEFAULT;
      }

      try {
         return DayCycle.Habit.valueOf(active.toUpperCase(Locale.ROOT));
      } catch (IllegalArgumentException e) {
         LOGGER.warn("[habit] {}: unreadable active '{}' - using {}", new Object[]{speciesId, active, DayCycle.Habit.DEFAULT});
         return DayCycle.Habit.DEFAULT;
      }
   }

   private static List<SpeciesProfile.RelationRule> parseRelations(JsonObject root) {
      if (!root.has("relations")) {
         return List.of();
      }

      List<SpeciesProfile.RelationRule> out = new ArrayList<>();

      for (JsonElement element : GsonHelper.getAsJsonArray(root, "relations")) {
         if (element.isJsonObject()) {
            JsonObject o = element.getAsJsonObject();
            String targets = GsonHelper.getAsString(o, "targets", "");
            String typeName = GsonHelper.getAsString(o, "type", "");
            if (!targets.isEmpty() && !typeName.isEmpty()) {
               RelationType type = null;

               for (RelationType candidate : RelationType.values()) {
                  if (candidate.name().equalsIgnoreCase(typeName)) {
                     type = candidate;
                     break;
                  }
               }

               if (type != null) {
                  float intensity = GsonHelper.getAsFloat(o, "intensity", 1.0F);
                  out.add(new SpeciesProfile.RelationRule(targets, type, Math.max(0.0F, Math.min(1.0F, intensity))));
               }
            }
         }
      }

      return List.copyOf(out);
   }

   public Relation relationTo(Kind kind) {
      for (SpeciesProfile.RelationRule rule : this.relations) {
         if (kind.matches(rule.targets())) {
            return new Relation(rule.type(), rule.intensity());
         }
      }

      return Relation.NONE;
   }

   @Nullable
   public SpeciesProfile.Smoothing smoothingFor(String driveId) {
      return this.smoothing.get(driveId);
   }

   public float[] traitBand(String traitId) {
      return this.traits.getOrDefault(traitId, new float[]{0.0F, 1.0F});
   }

   public float driveWeight(String driveId) {
      return this.driveWeights.getOrDefault(driveId, 1.0F);
   }

   public record Calls(Set<String> keys, int hold) {
      public static final SpeciesProfile.Calls NONE = new SpeciesProfile.Calls(Set.of(), 0);

      public boolean has(String key) {
         return this.keys.contains(key);
      }

      static SpeciesProfile.Calls parse(JsonObject root) {
         if (root.has("calls") && root.get("calls").isJsonArray()) {
            Set<String> keys = new LinkedHashSet<>();

            for (JsonElement key : root.getAsJsonArray("calls")) {
               if (key.isJsonPrimitive()) {
                  keys.add(key.getAsString());
               }
            }

            return keys.isEmpty() ? NONE : new SpeciesProfile.Calls(Set.copyOf(keys), Math.max(0, GsonHelper.getAsInt(root, "callHoldTicks", 0)));
         } else {
            return NONE;
         }
      }
   }

   public record DriveSet(List<String> with, List<String> without) {
      public static final SpeciesProfile.DriveSet NONE = new SpeciesProfile.DriveSet(List.of(), List.of());

      public boolean isNone() {
         return this.with.isEmpty() && this.without.isEmpty();
      }

      static SpeciesProfile.DriveSet parse(@Nullable JsonObject drives) {
         return drives == null ? NONE : new SpeciesProfile.DriveSet(ids(drives, "with"), ids(drives, "without"));
      }

      private static List<String> ids(JsonObject drives, String key) {
         if (drives.has(key) && drives.get(key).isJsonArray()) {
            List<String> out = new ArrayList<>();

            for (JsonElement id : drives.getAsJsonArray(key)) {
               out.add(id.getAsString());
            }

            return List.copyOf(out);
         } else {
            return List.of();
         }
      }
   }

   public record Movement(Map<MoveMode, Float> cost, Map<BlockPathTypes, Float> hazards, Map<String, Float> styles, boolean canSprint) {
      public static final SpeciesProfile.Movement DEFAULT = new SpeciesProfile.Movement(
         new EnumMap<>(Map.of(MoveMode.GROUND, 1.0F, MoveMode.AIR, Float.POSITIVE_INFINITY, MoveMode.WATER, 4.0F, MoveMode.CLIMB, Float.POSITIVE_INFINITY)),
         Map.of(),
         Map.of(),
         true
      );

      public float styleAptitude(String id) {
         return this.styles.getOrDefault(id, 1.0F);
      }

      public boolean allows(MoveMode mode) {
         return Float.isFinite(this.cost.getOrDefault(mode, Float.POSITIVE_INFINITY));
      }

      static SpeciesProfile.Movement parse(JsonObject o) {
         if (o == null) {
            return DEFAULT;
         }

         EnumMap<MoveMode, Float> costs = new EnumMap<>(MoveMode.class);

         for (MoveMode m : MoveMode.values()) {
            String key = m.name();
            if (o.has(key) && o.get(key).isJsonNull()) {
               costs.put(m, Float.POSITIVE_INFINITY);
            } else {
               costs.put(m, GsonHelper.getAsFloat(o, key, DEFAULT.cost.getOrDefault(m, Float.POSITIVE_INFINITY)));
            }
         }

         MoveTuning.orderModeCosts(costs);
         boolean canSprint = GsonHelper.getAsBoolean(o, "canSprint", DEFAULT.canSprint);
         return new SpeciesProfile.Movement(costs, parseHazards(o), parseStyles(o), canSprint);
      }

      private static Map<String, Float> parseStyles(JsonObject o) {
         if (!o.has("styles")) {
            return Map.of();
         }

         JsonObject st = GsonHelper.getAsJsonObject(o, "styles");
         Map<String, Float> out = new HashMap<>();

         for (String key : st.keySet()) {
            if (!key.startsWith("__")) {
               out.put(key, Math.max(0.0F, st.get(key).getAsFloat()));
            }
         }

         return Map.copyOf(out);
      }

      private static Map<BlockPathTypes, Float> parseHazards(JsonObject o) {
         if (!o.has("hazards")) {
            return Map.of();
         }

         JsonObject h = GsonHelper.getAsJsonObject(o, "hazards");
         EnumMap<BlockPathTypes, Float> out = new EnumMap<>(BlockPathTypes.class);

         for (String key : h.keySet()) {
            if (!key.startsWith("__")) {
               for (BlockPathTypes type : BlockPathTypes.values()) {
                  if (type.name().equals(key)) {
                     out.put(type, MoveTuning.hazardMalus(h.get(key).getAsFloat()));
                     break;
                  }
               }
            }
         }

         return out;
      }
   }

   public record RelationRule(String targets, RelationType type, float intensity) {
   }

   public record Senses(float sightRadius, float perfectVisionDeg, float peripheralDeg, boolean nightVision, float hearingSkill, float blindSpotDeg) {
      public static final SpeciesProfile.Senses DEFAULT = new SpeciesProfile.Senses(24.0F, 30.0F, 110.0F, false, 1.0F);
      public static final float AERIAL_SIGHT_MULTIPLE = 3.0F;
      public static final float ASLEEP_SIGHT_SHARE = 0.25F;

      public Senses(float sightRadius, float perfectVisionDeg, float peripheralDeg, boolean nightVision, float hearingSkill) {
         this(sightRadius, perfectVisionDeg, peripheralDeg, nightVision, hearingSkill, 0.0F);
      }

      static SpeciesProfile.Senses parse(JsonObject o) {
         if (o == null) {
            return DEFAULT;
         }

         float[] cone = new float[]{DEFAULT.perfectVisionDeg, DEFAULT.peripheralDeg};
         if (o.has("viewCone")) {
            JsonArray arr = GsonHelper.getAsJsonArray(o, "viewCone");
            if (arr.size() == 2) {
               cone[0] = arr.get(0).getAsFloat();
               cone[1] = arr.get(1).getAsFloat();
            }
         }

         return new SpeciesProfile.Senses(
            GsonHelper.getAsFloat(o, "sightRadius", DEFAULT.sightRadius),
            cone[0],
            cone[1],
            GsonHelper.getAsBoolean(o, "nightVision", DEFAULT.nightVision),
            GsonHelper.getAsFloat(o, "hearingSkill", DEFAULT.hearingSkill),
            Math.max(0.0F, GsonHelper.getAsFloat(o, "blindSpot", 0.0F))
         );
      }

      public SpeciesProfile.Senses whileAsleep() {
         return new SpeciesProfile.Senses(
            this.sightRadius * 0.25F, this.perfectVisionDeg, this.peripheralDeg, this.nightVision, this.hearingSkill, this.blindSpotDeg
         );
      }

      public SpeciesProfile.Senses aloft() {
         return new SpeciesProfile.Senses(
            this.sightRadius * 3.0F, this.perfectVisionDeg, this.peripheralDeg, this.nightVision, this.hearingSkill, this.blindSpotDeg
         );
      }

      public SpeciesProfile.Senses effectiveFor(boolean asleep, boolean aloft) {
         SpeciesProfile.Senses senses = this;
         if (aloft) {
            senses = senses.aloft();
         }

         if (asleep) {
            senses = senses.whileAsleep();
         }

         return senses;
      }
   }

   public record Smoothing(float rise, float decay) {
   }

   public record Social(int trackerCapacity, int forgetTicks, float territoryRadius, boolean territorial, int bonds) {
      public static final SpeciesProfile.Social DEFAULT = new SpeciesProfile.Social(6, 1200, 32.0F, true, 0);

      public Social(int trackerCapacity, int forgetTicks, float territoryRadius) {
         this(trackerCapacity, forgetTicks, territoryRadius, true, 0);
      }

      public Social(int trackerCapacity, int forgetTicks, float territoryRadius, boolean territorial) {
         this(trackerCapacity, forgetTicks, territoryRadius, territorial, 0);
      }

      static SpeciesProfile.Social parse(JsonObject o) {
         return o == null
            ? DEFAULT
            : new SpeciesProfile.Social(
               GsonHelper.getAsInt(o, "trackerCapacity", DEFAULT.trackerCapacity),
               GsonHelper.getAsInt(o, "forgetTicks", DEFAULT.forgetTicks),
               GsonHelper.getAsFloat(o, "territoryRadius", DEFAULT.territoryRadius),
               GsonHelper.getAsBoolean(o, "territorial", DEFAULT.territorial),
               GsonHelper.getAsInt(o, "bonds", DEFAULT.bonds)
            );
      }
   }

   public record Taming(TamingMatrix matrix, Ladder pool) {
      public static final SpeciesProfile.Taming DEFAULT = new SpeciesProfile.Taming(TamingMatrix.EMPTY, Ladder.defaultLadder());

      static SpeciesProfile.Taming parse(JsonObject root, String speciesId, @Nullable String namespace) {
         if (root != null && root.has("taming")) {
            JsonObject taming = GsonHelper.getAsJsonObject(root, "taming");
            return new SpeciesProfile.Taming(
               TamingMatrix.parse(taming.has("matrix") ? GsonHelper.getAsJsonObject(taming, "matrix") : null), Ladder.parse(taming, speciesId, namespace)
            );
         } else {
            return DEFAULT;
         }
      }
   }
}
