package net.magister.liberanimalium.spawn;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public record SpawnSettings(
   boolean enabled,
   int periodTicks,
   int roostlessCap,
   int perExtraPlayer,
   int densityCap,
   double densityRadius,
   double speciesShare,
   SpawnSettings.Abundance abundance,
   SpawnSettings.Despawn despawn,
   SpawnSettings.Guard guard
) {
   private static final Logger LOGGER = LoggerFactory.getLogger("SpawnSettings");
   public static final SpawnSettings DEFAULT = new SpawnSettings(
      true, 600, 30, 4, 10, 128.0, 0.35, SpawnSettings.Abundance.DEFAULT, SpawnSettings.Despawn.DEFAULT, SpawnSettings.Guard.DEFAULT
   );
   private static final Set<String> KEYS = Set.of(
      "enabled", "period_ticks", "roostless_cap", "per_extra_player", "density_cap", "density_radius", "species_share", "abundance", "despawn", "guard"
   );
   private static volatile Map<String, SpawnSettings> byHost = Map.of();

   public static SpawnSettings of(String host) {
      return byHost.getOrDefault(host, DEFAULT);
   }

   public static void installAll(Map<String, SpawnSettings> settings) {
      byHost = Map.copyOf(new LinkedHashMap<>(settings));
   }

   public int roostlessCap(int players) {
      return this.roostlessCap + this.perExtraPlayer * Math.max(0, players - 1);
   }

   public int speciesRoostlessCap(int players) {
      return scaled(this.roostlessCap(players), this.speciesShare);
   }

   public int speciesDensityCap() {
      return scaled(this.densityCap, this.speciesShare);
   }

   public static int scaled(int cap, double factor) {
      return (int)Math.ceil(cap * factor - 1.0E-9);
   }

   public static SpawnSettings parse(JsonObject json, String host) {
      warnUnknown(json, KEYS, host, "");
      return new SpawnSettings(
         bool(json, "enabled", DEFAULT.enabled(), host),
         atLeast(json, "period_ticks", DEFAULT.periodTicks(), 1, host),
         atLeast(json, "roostless_cap", DEFAULT.roostlessCap(), 0, host),
         atLeast(json, "per_extra_player", DEFAULT.perExtraPlayer(), 0, host),
         atLeast(json, "density_cap", DEFAULT.densityCap(), 0, host),
         atLeast(json, "density_radius", (int)DEFAULT.densityRadius(), 1, host),
         fraction(json, "species_share", DEFAULT.speciesShare(), false, host),
         abundance(json, host),
         despawn(json, host),
         guard(json, host)
      );
   }

   private static SpawnSettings.Guard guard(JsonObject json, String host) {
      JsonObject o = section(json, "guard", host);
      if (o == null) {
         return SpawnSettings.Guard.DEFAULT;
      }

      warnUnknown(o, SpawnSettings.Guard.KEYS, host, "guard.");
      SpawnSettings.Guard d = SpawnSettings.Guard.DEFAULT;
      return new SpawnSettings.Guard(atLeast(o, "sight_margin", (int)d.sightMargin(), 0, host), atLeast(o, "home_radius", (int)d.homeRadius(), 0, host));
   }

   private static SpawnSettings.Abundance abundance(JsonObject json, String host) {
      JsonObject o = section(json, "abundance", host);
      if (o == null) {
         return SpawnSettings.Abundance.DEFAULT;
      }

      warnUnknown(o, SpawnSettings.Abundance.KEYS, host, "abundance.");
      SpawnSettings.Abundance d = SpawnSettings.Abundance.DEFAULT;
      return new SpawnSettings.Abundance(
         atLeast(o, "region_blocks", d.regionBlocks(), 16, host),
         positive(o, "days", d.days(), host),
         fraction(o, "floor", d.floor(), true, host),
         fraction(o, "tide_floor", d.tideFloor(), true, host)
      );
   }

   private static SpawnSettings.Despawn despawn(JsonObject json, String host) {
      JsonObject o = section(json, "despawn", host);
      if (o == null) {
         return SpawnSettings.Despawn.DEFAULT;
      }

      warnUnknown(o, SpawnSettings.Despawn.KEYS, host, "despawn.");
      SpawnSettings.Despawn d = SpawnSettings.Despawn.DEFAULT;
      return new SpawnSettings.Despawn(
         atLeast(o, "safe_radius", (int)d.safeRadius(), 0, host),
         atLeast(o, "unwatched_ticks", d.unwatchedTicks(), 0, host),
         fraction(o, "chance_per_tick", d.chancePerTick(), true, host)
      );
   }

   private static JsonObject section(JsonObject json, String key, String host) {
      JsonElement value = json.get(key);
      if (value == null) {
         return null;
      }

      if (value.isJsonObject()) {
         return value.getAsJsonObject();
      }

      LOGGER.warn("[SpawnSettings] {}: '{}' must be an object; using the defaults", host, key);
      return null;
   }

   private static void warnUnknown(JsonObject json, Set<String> keys, String host, String prefix) {
      for (String key : json.keySet()) {
         if (!key.startsWith("__") && !keys.contains(key)) {
            LOGGER.warn("[SpawnSettings] {}: unknown key '{}{}' ignored (known: {})", new Object[]{host, prefix, key, keys});
         }
      }
   }

   private static boolean bool(JsonObject json, String key, boolean fallback, String host) {
      JsonElement value = json.get(key);
      if (value == null) {
         return fallback;
      } else if (value instanceof JsonPrimitive p && p.isBoolean()) {
         return p.getAsBoolean();
      } else {
         LOGGER.warn("[SpawnSettings] {}: '{}' must be true or false; using {}", new Object[]{host, key, fallback});
         return fallback;
      }
   }

   private static int atLeast(JsonObject json, String key, int fallback, int min, String host) {
      JsonElement value = json.get(key);
      if (value == null) {
         return fallback;
      } else if (value instanceof JsonPrimitive p && p.isNumber() && p.getAsInt() >= min) {
         return p.getAsInt();
      } else {
         LOGGER.warn("[SpawnSettings] {}: '{}' must be a whole number of at least {}; using {}", new Object[]{host, key, min, fallback});
         return fallback;
      }
   }

   private static double positive(JsonObject json, String key, double fallback, String host) {
      JsonElement value = json.get(key);
      if (value == null) {
         return fallback;
      } else if (value instanceof JsonPrimitive p && p.isNumber() && p.getAsDouble() > 0.0) {
         return p.getAsDouble();
      } else {
         LOGGER.warn("[SpawnSettings] {}: '{}' must be a number above 0; using {}", new Object[]{host, key, fallback});
         return fallback;
      }
   }

   private static double fraction(JsonObject json, String key, double fallback, boolean zeroOk, String host) {
      JsonElement value = json.get(key);
      if (value == null) {
         return fallback;
      }

      if (value instanceof JsonPrimitive p && p.isNumber()) {
         double v = p.getAsDouble();
         if ((zeroOk ? v >= 0.0 : v > 0.0) && v <= 1.0) {
            return v;
         }
      }

      LOGGER.warn("[SpawnSettings] {}: '{}' must be a number {} 0 and at most 1; using {}", new Object[]{host, key, zeroOk ? "of at least" : "above", fallback});
      return fallback;
   }

   public record Abundance(int regionBlocks, double days, double floor, double tideFloor) {
      public static final SpawnSettings.Abundance DEFAULT = new SpawnSettings.Abundance(512, 3.0, 0.0, 0.4);
      public static final SpawnSettings.Abundance STEADY = new SpawnSettings.Abundance(512, 3.0, 1.0, 1.0);
      private static final Set<String> KEYS = Set.of("region_blocks", "days", "floor", "tide_floor");
   }

   public record Despawn(double safeRadius, int unwatchedTicks, double chancePerTick) {
      public static final SpawnSettings.Despawn DEFAULT = new SpawnSettings.Despawn(64.0, 6000, 5.0E-4);
      private static final Set<String> KEYS = Set.of("safe_radius", "unwatched_ticks", "chance_per_tick");
   }

   public record Guard(double sightMargin, double homeRadius) {
      public static final SpawnSettings.Guard DEFAULT = new SpawnSettings.Guard(8.0, 64.0);
      private static final Set<String> KEYS = Set.of("sight_margin", "home_radius");

      public double playerDistance(double sightRadius) {
         return Math.max(24.0, sightRadius + this.sightMargin);
      }

      public boolean guards(double x, double z, double homeX, double homeZ) {
         double dx = x - homeX;
         double dz = z - homeZ;
         return dx * dx + dz * dz < this.homeRadius * this.homeRadius;
      }
   }
}
