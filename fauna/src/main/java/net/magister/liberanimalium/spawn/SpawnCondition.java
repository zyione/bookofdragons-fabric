package net.magister.liberanimalium.spawn;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.TreeSet;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import net.magister.liberanimalium.compat.Servers;
import net.magister.liberanimalium.fauna.api.DayCycle;
import net.magister.liberanimalium.roost.RoostBlockEntity;
import net.magister.liberanimalium.roost.Roosts;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.FluidTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.Difficulty;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.village.poi.PoiType;
import net.minecraft.world.entity.ai.village.poi.PoiManager.Occupancy;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap.Types;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

public interface SpawnCondition {
   String name();

   boolean test(SpawnContext var1);

   static SpawnCondition parse(JsonElement element) {
      if (element.isJsonObject() && element.getAsJsonObject().size() == 1) {
         Entry<String, JsonElement> only = (Entry<String, JsonElement>)element.getAsJsonObject().entrySet().iterator().next();
         Function<JsonElement, SpawnCondition> reader = SpawnCondition.Readers.ALL.get(only.getKey());
         if (reader == null) {
            reader = SpawnCondition.Readers.HOSTS.get(only.getKey());
         }

         if (reader == null) {
            Set<String> known = new TreeSet<>(SpawnCondition.Readers.ALL.keySet());
            known.addAll(SpawnCondition.Readers.HOSTS.keySet());
            throw new IllegalArgumentException("unknown condition '" + only.getKey() + "' (known: " + known + ")");
         } else {
            return reader.apply(only.getValue());
         }
      } else {
         throw new IllegalArgumentException("a condition is an object with one key, its name: " + element);
      }
   }

   static void register(String name, Function<JsonElement, SpawnCondition> reader) {
      if (SpawnCondition.Readers.ALL.containsKey(name) || SpawnCondition.Readers.HOSTS.putIfAbsent(name, reader) != null) {
         throw new IllegalArgumentException("spawn condition '" + name + "' is already read");
      }
   }

   private static JsonObject object(JsonElement element, String what) {
      if (!element.isJsonObject()) {
         throw new IllegalArgumentException(what + ": expects an object");
      } else {
         return element.getAsJsonObject();
      }
   }

   private static JsonElement refs(JsonElement element, String what) {
      if (element.isJsonObject()) {
         JsonObject o = element.getAsJsonObject();
         if (!o.has("is")) {
            throw new IllegalArgumentException(what + ": expects {\"is\": ids}, an id or a list of ids");
         } else {
            return o.get("is");
         }
      } else {
         return element;
      }
   }

   private static int reach(JsonObject o, String key, int fallback) {
      int value = o.has(key) ? o.get(key).getAsInt() : fallback;
      return Math.max(0, Math.min(8, value));
   }

   record AboveGround(SpawnCondition.Range range) implements SpawnCondition {
      @Override
      public String name() {
         return "above_ground";
      }

      @Override
      public boolean test(SpawnContext c) {
         return this.range.holds(c.pos().getY() - c.surface());
      }
   }

   record Advancement(ResourceLocation id) implements SpawnCondition {
      @Override
      public String name() {
         return "advancement";
      }

      @Override
      public boolean test(SpawnContext c) {
         return c.nearest() != null && Servers.hasAdvancement(c.nearest(), this.id);
      }
   }

   record BelowSurface(SpawnCondition.Range range) implements SpawnCondition {
      @Override
      public String name() {
         return "below_surface";
      }

      @Override
      public boolean test(SpawnContext c) {
         return this.range.holds(c.surface() - c.pos().getY());
      }
   }

   record BiomeIs(List<SpawnCondition.Ref> refs) implements SpawnCondition {
      @Override
      public String name() {
         return "biome";
      }

      @Override
      public boolean test(SpawnContext c) {
         Holder<Biome> biome = c.level().getBiome(c.pos());

         for (SpawnCondition.Ref ref : this.refs) {
            if (ref.biome(biome)) {
               return true;
            }
         }

         return false;
      }
   }

   record BlockBelow(List<SpawnCondition.Ref> refs) implements SpawnCondition {
      @Override
      public String name() {
         return "block_below";
      }

      @Override
      public boolean test(SpawnContext c) {
         BlockState below = c.level().getBlockState(c.pos().below());

         for (SpawnCondition.Ref ref : this.refs) {
            if (ref.block(below)) {
               return true;
            }
         }

         return false;
      }
   }

   record CaveSize(int minAir) implements SpawnCondition {
      @Override
      public String name() {
         return "cave_size";
      }

      @Override
      public boolean test(SpawnContext c) {
         Set<BlockPos> seen = new HashSet<>();
         Queue<BlockPos> queue = new ArrayDeque<>();
         queue.add(c.pos());
         int limit = Math.min(this.minAir, 512);

         while (!queue.isEmpty() && seen.size() < limit) {
            BlockPos at = queue.poll();
            if (at.distManhattan(c.pos()) <= 24 && seen.add(at)) {
               if (!c.level().getBlockState(at).isAir()) {
                  seen.remove(at);
               } else {
                  for (Direction d : Direction.values()) {
                     queue.add(at.relative(d));
                  }
               }
            }
         }

         return seen.size() >= limit;
      }
   }

   record CreaturesNearby(double radius, int max) implements SpawnCondition {
      @Override
      public String name() {
         return "creatures_nearby";
      }

      @Override
      public boolean test(SpawnContext c) {
         return c.census().near(c.rule().host(), Vec3.atBottomCenterOf(c.pos()), this.radius) <= this.max;
      }
   }

   record DifficultyAtLeast(Difficulty min) implements SpawnCondition {
      @Override
      public String name() {
         return "difficulty";
      }

      @Override
      public boolean test(SpawnContext c) {
         return c.level().getDifficulty().getId() >= this.min.getId();
      }
   }

   record DimensionIs(List<SpawnCondition.Ref> refs) implements SpawnCondition {
      @Override
      public String name() {
         return "dimension";
      }

      @Override
      public boolean test(SpawnContext c) {
         ResourceLocation here = c.level().dimension().location();

         for (SpawnCondition.Ref ref : this.refs) {
            if (ref.id().equals(here)) {
               return true;
            }
         }

         return false;
      }
   }

   record Fluid(String kind) implements SpawnCondition {
      @Override
      public String name() {
         return "fluid";
      }

      @Override
      public boolean test(SpawnContext c) {
         FluidState fluid = c.level().getFluidState(c.pos());

         return switch (this.kind) {
            case "water" -> fluid.is(FluidTags.WATER);
            case "lava" -> fluid.is(FluidTags.LAVA);
            default -> fluid.isEmpty();
         };
      }
   }

   record FromWorldSpawn(double min) implements SpawnCondition {
      @Override
      public String name() {
         return "from_world_spawn";
      }

      @Override
      public boolean test(SpawnContext c) {
         return Math.sqrt(c.level().getSharedSpawnPos().distSqr(c.pos())) >= this.min;
      }
   }

   record KinNearby(double radius, SpawnCondition.Range count) implements SpawnCondition {
      @Override
      public String name() {
         return "kin_nearby";
      }

      @Override
      public boolean test(SpawnContext c) {
         AABB box = new AABB(c.pos()).inflate(this.radius);
         int n = c.level().getEntitiesOfClass(Mob.class, box, m -> m.getType() == c.rule().type()).size();
         return this.count.holds(n);
      }
   }

   record Light(SpawnCondition.Range block, SpawnCondition.Range sky) implements SpawnCondition {
      @Override
      public String name() {
         return "light";
      }

      @Override
      public boolean test(SpawnContext c) {
         return this.block.holds(c.level().getBrightness(LightLayer.BLOCK, c.pos())) && this.sky.holds(c.level().getBrightness(LightLayer.SKY, c.pos()));
      }
   }

   record Moon(Set<Integer> phases) implements SpawnCondition {
      @Override
      public String name() {
         return "moon";
      }

      @Override
      public boolean test(SpawnContext c) {
         return this.phases.contains(c.level().getMoonPhase());
      }
   }

   record NearRoost(int within, boolean empty, String kind) implements SpawnCondition {
      @Override
      public String name() {
         return "near_roost";
      }

      @Override
      public boolean test(SpawnContext c) {
         for (BlockPos at : Roosts.nearest(c.level(), c.pos(), this.within, Integer.MAX_VALUE)) {
            if (c.level().getBlockEntity(at) instanceof RoostBlockEntity roost
               && (!this.empty || roost.occupants().isEmpty())
               && (this.kind.isEmpty() || roost.describedAs(this.kind))) {
               return true;
            }
         }

         return false;
      }
   }

   record NearbyBlock(List<SpawnCondition.Ref> refs, int radius, int min) implements SpawnCondition {
      @Override
      public String name() {
         return "nearby_block";
      }

      @Override
      public boolean test(SpawnContext c) {
         int found = 0;

         for (BlockPos at : BlockPos.betweenClosed(
            c.pos().offset(-this.radius, -this.radius, -this.radius), c.pos().offset(this.radius, this.radius, this.radius)
         )) {
            BlockState state = c.level().getBlockState(at);

            for (SpawnCondition.Ref ref : this.refs) {
               if (ref.block(state)) {
                  if (++found >= this.min) {
                     return true;
                  }
                  break;
               }
            }
         }

         return false;
      }
   }

   record PlayerDistance(SpawnCondition.Range range) implements SpawnCondition {
      @Override
      public String name() {
         return "player_distance";
      }

      @Override
      public boolean test(SpawnContext c) {
         return this.range.holds(c.nearestDistance());
      }
   }

   record Poi(List<SpawnCondition.Ref> refs, int within) implements SpawnCondition {
      @Override
      public String name() {
         return "poi";
      }

      @Override
      public boolean test(SpawnContext c) {
         return c.level().getPoiManager().findClosest(type -> {
            for (SpawnCondition.Ref ref : this.refs) {
               if (ref.poi(type)) {
                  return true;
               }
            }

            return false;
         }, c.pos(), this.within, Occupancy.ANY).isPresent();
      }
   }

   record Range(double min, double max) {
      static SpawnCondition.Range of(JsonElement element, String what) {
         if (!element.isJsonObject()) {
            throw new IllegalArgumentException(what + ": expects {\"min\": .., \"max\": ..}");
         } else {
            JsonObject o = element.getAsJsonObject();
            double min = o.has("min") ? o.get("min").getAsDouble() : Double.NEGATIVE_INFINITY;
            double max = o.has("max") ? o.get("max").getAsDouble() : Double.POSITIVE_INFINITY;
            if (min > max) {
               throw new IllegalArgumentException(what + ": min above max");
            } else {
               return new SpawnCondition.Range(min, max);
            }
         }
      }

      boolean holds(double value) {
         return value >= this.min && value <= this.max;
      }
   }

   final class Readers {
      static final Map<String, Function<JsonElement, SpawnCondition>> HOSTS = new ConcurrentHashMap<>();
      static final Map<String, Function<JsonElement, SpawnCondition>> ALL = Map.ofEntries(
         Map.entry("biome", e -> new SpawnCondition.BiomeIs(SpawnCondition.Ref.list(SpawnCondition.refs(e, "biome"), "biome"))),
         Map.entry("dimension", e -> new SpawnCondition.DimensionIs(SpawnCondition.Ref.list(SpawnCondition.refs(e, "dimension"), "dimension"))),
         Map.entry("y", e -> new SpawnCondition.Y(SpawnCondition.Range.of(e, "y"))),
         Map.entry("above_ground", e -> new SpawnCondition.AboveGround(SpawnCondition.Range.of(e, "above_ground"))),
         Map.entry("below_surface", e -> new SpawnCondition.BelowSurface(SpawnCondition.Range.of(e, "below_surface"))),
         Map.entry("sky", e -> {
            String v = e.getAsString();
            if (!v.equals("exposed") && !v.equals("sheltered")) {
               throw new IllegalArgumentException("sky: exposed or sheltered, not " + v);
            } else {
               return new SpawnCondition.Sky(v.equals("exposed"));
            }
         }),
         Map.entry(
            "light",
            e -> {
               JsonObject o = SpawnCondition.object(e, "light");
               SpawnCondition.Range open = new SpawnCondition.Range(Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY);
               return new SpawnCondition.Light(
                  o.has("block") ? SpawnCondition.Range.of(o.get("block"), "light.block") : open,
                  o.has("sky") ? SpawnCondition.Range.of(o.get("sky"), "light.sky") : open
               );
            }
         ),
         Map.entry("time", e -> {
            if (e.isJsonPrimitive()) {
               String word = e.getAsString();

               return switch (word) {
                  case "active", "inactive" -> new SpawnCondition.Time(List.of(), word);
                  case "day" -> new SpawnCondition.Time(List.of(new long[]{0L, 12999L}), "");
                  case "night" -> new SpawnCondition.Time(List.of(new long[]{13000L, 23999L}), "");
                  default -> throw new IllegalArgumentException("time: active, inactive, day or night, not " + word);
               };
            } else {
               JsonObject o = SpawnCondition.object(e, "time");
               if (o.has("habit")) {
                  String habit = o.get("habit").getAsString();
                  if (!habit.equals("active") && !habit.equals("inactive")) {
                     throw new IllegalArgumentException("time.habit: active or inactive, not " + habit);
                  } else {
                     return new SpawnCondition.Time(List.of(), habit);
                  }
               } else {
                  List<long[]> windows = new ArrayList<>();

                  for (JsonElement w : o.getAsJsonArray("windows")) {
                     JsonArray pair = w.getAsJsonArray();
                     windows.add(new long[]{pair.get(0).getAsLong(), pair.get(1).getAsLong()});
                  }

                  if (windows.isEmpty()) {
                     throw new IllegalArgumentException("time: no windows and no habit");
                  } else {
                     return new SpawnCondition.Time(List.copyOf(windows), "");
                  }
               }
            }
         }),
         Map.entry("moon", e -> {
            if (e.isJsonPrimitive()) {
               String word = e.getAsString();
               if (word.equals("full")) {
                  return new SpawnCondition.Moon(Set.of(0));
               } else if (word.equals("new")) {
                  return new SpawnCondition.Moon(Set.of(4));
               } else {
                  throw new IllegalArgumentException("moon: full, new or {\"phases\": [0..7]}, not " + word);
               }
            } else {
               Set<Integer> phases = new HashSet<>();

               for (JsonElement p : SpawnCondition.object(e, "moon").getAsJsonArray("phases")) {
                  phases.add(p.getAsInt());
               }

               return new SpawnCondition.Moon(Set.copyOf(phases));
            }
         }),
         Map.entry("weather", e -> {
            String v = e.getAsString();
            if (!Set.of("clear", "rain", "thunder").contains(v)) {
               throw new IllegalArgumentException("weather: clear, rain or thunder, not " + v);
            } else {
               return new SpawnCondition.Weather(v);
            }
         }),
         Map.entry(
            "temperature",
            e -> {
               JsonObject o = SpawnCondition.object(e, "temperature");
               SpawnCondition.Range range = new SpawnCondition.Range(
                  o.has("min") ? o.get("min").getAsDouble() : Double.NEGATIVE_INFINITY, o.has("max") ? o.get("max").getAsDouble() : Double.POSITIVE_INFINITY
               );
               return new SpawnCondition.Temperature(range, o.has("precipitation") ? o.get("precipitation").getAsString().toLowerCase(Locale.ROOT) : "");
            }
         ),
         Map.entry("block_below", e -> new SpawnCondition.BlockBelow(SpawnCondition.Ref.list(SpawnCondition.refs(e, "block_below"), "block_below"))),
         Map.entry(
            "nearby_block",
            e -> {
               JsonObject o = SpawnCondition.object(e, "nearby_block");
               return new SpawnCondition.NearbyBlock(
                  SpawnCondition.Ref.list(o.get("is"), "nearby_block"),
                  SpawnCondition.reach(o, "radius", 4),
                  Math.max(1, o.has("min_count") ? o.get("min_count").getAsInt() : 1)
               );
            }
         ),
         Map.entry("fluid", e -> {
            String v = e.getAsString();
            if (!Set.of("none", "water", "lava").contains(v)) {
               throw new IllegalArgumentException("fluid: none, water or lava, not " + v);
            } else {
               return new SpawnCondition.Fluid(v);
            }
         }),
         Map.entry("structure", e -> new SpawnCondition.StructureAt(SpawnCondition.Ref.list(SpawnCondition.refs(e, "structure"), "structure"))),
         Map.entry("poi", e -> {
            JsonObject o = SpawnCondition.object(e, "poi");
            return new SpawnCondition.Poi(SpawnCondition.Ref.list(o.get("type"), "poi"), o.has("within") ? o.get("within").getAsInt() : 32);
         }),
         Map.entry(
            "near_roost",
            e -> {
               JsonObject o = SpawnCondition.object(e, "near_roost");
               return new SpawnCondition.NearRoost(
                  o.has("within") ? o.get("within").getAsInt() : 96,
                  o.has("empty") && o.get("empty").getAsBoolean(),
                  o.has("kind") ? o.get("kind").getAsString() : ""
               );
            }
         ),
         Map.entry("kin_nearby", e -> {
            JsonObject o = SpawnCondition.object(e, "kin_nearby");
            return new SpawnCondition.KinNearby(o.has("radius") ? o.get("radius").getAsDouble() : 32.0, SpawnCondition.Range.of(e, "kin_nearby"));
         }),
         Map.entry("creatures_nearby", e -> {
            JsonObject o = SpawnCondition.object(e, "creatures_nearby");
            return new SpawnCondition.CreaturesNearby(o.has("radius") ? o.get("radius").getAsDouble() : 64.0, o.get("max").getAsInt());
         }),
         Map.entry("player_distance", e -> new SpawnCondition.PlayerDistance(SpawnCondition.Range.of(e, "player_distance"))),
         Map.entry("from_world_spawn", e -> new SpawnCondition.FromWorldSpawn(SpawnCondition.object(e, "from_world_spawn").get("min").getAsDouble())),
         Map.entry("world_age", e -> new SpawnCondition.WorldAge(SpawnCondition.object(e, "world_age").get("min_days").getAsLong())),
         Map.entry("difficulty", e -> {
            String v = SpawnCondition.object(e, "difficulty").get("min").getAsString();

            try {
               return new SpawnCondition.DifficultyAtLeast(Difficulty.valueOf(v.toUpperCase(Locale.ROOT)));
            } catch (IllegalArgumentException bad) {
               throw new IllegalArgumentException("difficulty.min: peaceful, easy, normal or hard, not " + v);
            }
         }),
         Map.entry("advancement", e -> {
            ResourceLocation id = ResourceLocation.tryParse(SpawnCondition.object(e, "advancement").get("id").getAsString());
            if (id == null) {
               throw new IllegalArgumentException("advancement.id: not an id");
            } else {
               return new SpawnCondition.Advancement(id);
            }
         }),
         Map.entry("slope", e -> {
            JsonObject o = SpawnCondition.object(e, "slope");
            return new SpawnCondition.Slope(o.get("min_rise").getAsInt(), Math.max(1, SpawnCondition.reach(o, "radius", 4)));
         }),
         Map.entry("cave_size", e -> new SpawnCondition.CaveSize(SpawnCondition.object(e, "cave_size").get("min_air").getAsInt()))
      );

      private Readers() {
      }
   }

   record Ref(ResourceLocation id, boolean tag) {
      static List<SpawnCondition.Ref> list(JsonElement element, String what) {
         List<SpawnCondition.Ref> out = new ArrayList<>();
         if (element.isJsonArray()) {
            for (JsonElement e : element.getAsJsonArray()) {
               out.add(one(e.getAsString(), what));
            }
         } else {
            out.add(one(element.getAsString(), what));
         }

         if (out.isEmpty()) {
            throw new IllegalArgumentException(what + ": an empty list");
         } else {
            return List.copyOf(out);
         }
      }

      private static SpawnCondition.Ref one(String text, String what) {
         boolean tag = text.startsWith("#");
         ResourceLocation id = ResourceLocation.tryParse(tag ? text.substring(1) : text);
         if (id == null) {
            throw new IllegalArgumentException(what + ": not an id: " + text);
         } else {
            return new SpawnCondition.Ref(id, tag);
         }
      }

      boolean biome(Holder<Biome> biome) {
         return this.tag ? biome.is(TagKey.create(Registries.BIOME, this.id)) : biome.is(this.id);
      }

      boolean block(BlockState state) {
         return this.tag
            ? state.is(TagKey.create(Registries.BLOCK, this.id))
            : BuiltInRegistries.BLOCK.getKey(state.getBlock()).equals(this.id);
      }

      boolean poi(Holder<PoiType> type) {
         return this.tag ? type.is(TagKey.create(Registries.POINT_OF_INTEREST_TYPE, this.id)) : type.is(this.id);
      }

      boolean structureAt(SpawnContext c) {
         StructureManager structures = c.level().structureManager();
         if (this.tag) {
            return structures.getStructureWithPieceAt(c.pos(), TagKey.create(Registries.STRUCTURE, this.id)).isValid();
         }

         Structure structure = (Structure)c.level().registryAccess().registryOrThrow(Registries.STRUCTURE).get(this.id);
         return structure != null && structures.getStructureWithPieceAt(c.pos(), structure).isValid();
      }
   }

   record Sky(boolean exposed) implements SpawnCondition {
      @Override
      public String name() {
         return "sky";
      }

      @Override
      public boolean test(SpawnContext c) {
         return c.level().canSeeSky(c.pos()) == this.exposed;
      }
   }

   record Slope(int minRise, int radius) implements SpawnCondition {
      @Override
      public String name() {
         return "slope";
      }

      @Override
      public boolean test(SpawnContext c) {
         int low = Integer.MAX_VALUE;
         int high = Integer.MIN_VALUE;

         for (int dx = -this.radius; dx <= this.radius; dx += 2) {
            for (int dz = -this.radius; dz <= this.radius; dz += 2) {
               int h = c.level().getHeight(Types.MOTION_BLOCKING_NO_LEAVES, c.pos().getX() + dx, c.pos().getZ() + dz);
               low = Math.min(low, h);
               high = Math.max(high, h);
            }
         }

         return high - low >= this.minRise;
      }
   }

   record StructureAt(List<SpawnCondition.Ref> refs) implements SpawnCondition {
      @Override
      public String name() {
         return "structure";
      }

      @Override
      public boolean test(SpawnContext c) {
         for (SpawnCondition.Ref ref : this.refs) {
            if (ref.structureAt(c)) {
               return true;
            }
         }

         return false;
      }
   }

   record Temperature(SpawnCondition.Range range, String precipitation) implements SpawnCondition {
      @Override
      public String name() {
         return "temperature";
      }

      @Override
      public boolean test(SpawnContext c) {
         Biome biome = (Biome)c.level().getBiome(c.pos()).value();
         return !this.range.holds(biome.getBaseTemperature())
            ? false
            : this.precipitation.isEmpty() || biome.getPrecipitationAt(c.pos()).name().toLowerCase(Locale.ROOT).equals(this.precipitation);
      }
   }

   record Time(List<long[]> windows, String habit) implements SpawnCondition {
      @Override
      public String name() {
         return "time";
      }

      @Override
      public boolean test(SpawnContext c) {
         long dayTime = c.level().getDayTime();
         if (!this.habit.isEmpty()) {
            if (c.species() == null) {
               return false;
            }

            boolean asleep = DayCycle.inactive(DayCycle.fraction(dayTime), c.species().habit());
            return this.habit.equals("active") != asleep;
         } else {
            long t = Math.floorMod(dayTime, 24000L);

            for (long[] w : this.windows) {
               boolean in = w[0] <= w[1] ? t >= w[0] && t <= w[1] : t >= w[0] || t <= w[1];
               if (in) {
                  return true;
               }
            }

            return false;
         }
      }
   }

   record Weather(String kind) implements SpawnCondition {
      @Override
      public String name() {
         return "weather";
      }

      @Override
      public boolean test(SpawnContext c) {
         return switch (this.kind) {
            case "clear" -> !c.level().isRaining();
            case "rain" -> c.level().isRaining();
            default -> c.level().isThundering();
         };
      }
   }

   record WorldAge(long minDays) implements SpawnCondition {
      @Override
      public String name() {
         return "world_age";
      }

      @Override
      public boolean test(SpawnContext c) {
         return c.level().getGameTime() / 24000L >= this.minDays;
      }
   }

   record Y(SpawnCondition.Range range) implements SpawnCondition {
      @Override
      public String name() {
         return "y";
      }

      @Override
      public boolean test(SpawnContext c) {
         return this.range.holds(c.pos().getY());
      }
   }
}
