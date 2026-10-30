package net.magister.liberanimalium.body;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.logging.LogUtils;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.jetbrains.annotations.Nullable;
import net.magister.liberanimalium.body.anim.binding.BindingSource;
import net.magister.liberanimalium.body.grip.GripSpec;
import org.slf4j.Logger;

public final class BodyPlans {
   private static final Logger LOGGER = LogUtils.getLogger();
   private static final Map<String, BodyPlan> PLANS = new ConcurrentHashMap<>();
   private static final Map<String, GripSpec> GRIPS = new ConcurrentHashMap<>();
   private static final Map<String, Integer> PUSH_OFF = new ConcurrentHashMap<>();

   private BodyPlans() {
   }

   public static BodyPlan of(String species) {
      return PLANS.computeIfAbsent(species, BodyPlans::load);
   }

   public static void invalidate() {
      PLANS.clear();
      PUSH_OFF.clear();
      GRIPS.clear();
   }

   public static GripSpec grip(String species) {
      return GRIPS.computeIfAbsent(species, BodyPlans::loadGrip);
   }

   private static GripSpec loadGrip(String species) {
      String path = "animation_bindings/" + species + ".json";

      try {
         JsonObject root = read(species, null, species);
         if (root == null) {
            return GripSpec.NONE;
         }

         JsonObject rig = root.has("rig") && root.get("rig").isJsonObject() ? root.getAsJsonObject("rig") : null;
         JsonObject block = rig != null && rig.has("grip") && rig.get("grip").isJsonObject() ? rig.getAsJsonObject("grip") : null;
         GripSpec spec = GripSpec.parse(block);
         if (block != null) {
            for (String problem : spec.problems()) {
               LOGGER.warn("[BodyPlans] {} grip: {}", species, problem);
            }
         }

         return spec;
      } catch (Exception e) {
         LOGGER.error("[BodyPlans] {} - failed to read the grip of {}: {}", new Object[]{species, path, e.toString()});
         return GripSpec.NONE;
      }
   }

   public static int pushOffTicks(String species) {
      return PUSH_OFF.computeIfAbsent(species, s -> {
         Integer own = readPushOff(s, null, s);
         if (own != null) {
            return own;
         } else {
            Integer inherited = readPushOff(s, SpeciesAssets.hostOfSpecies(s), "default");
            if (inherited == null) {
               throw new IllegalStateException("animation_bindings/default.json authors no wall_hit.push_off_ticks");
            } else {
               return inherited;
            }
         }
      });
   }

   private static Integer readPushOff(String species, @Nullable String host, String file) {
      String path = "animation_bindings/" + file + ".json";

      try {
         JsonObject root = read(species, host, file);
         if (root == null) {
            return null;
         }

         if (root.has("wall_hit") && root.get("wall_hit").isJsonObject()) {
            JsonObject block = root.getAsJsonObject("wall_hit");
            if (!block.has("push_off_ticks")) {
               return null;
            }

            try {
               int ticks = block.get("push_off_ticks").getAsInt();
               if (ticks < 0) {
                  throw new IllegalArgumentException("negative");
               } else {
                  return ticks;
               }
            } catch (RuntimeException bad) {
               LOGGER.warn("[BodyPlans] {}: wall_hit.push_off_ticks must be a whole number of ticks, got {}", species, block.get("push_off_ticks"));
               return null;
            }
         } else {
            return null;
         }
      } catch (Exception e) {
         LOGGER.error("[BodyPlans] {} - failed to read {}: {}", new Object[]{species, path, e.toString()});
         return null;
      }
   }

   private static BodyPlan load(String species) {
      String path = "animation_bindings/" + species + ".json";

      try {
         JsonObject root = read(species, null, species);
         if (root == null) {
            LOGGER.warn("[BodyPlans] {} has no animation binding at {} - no parts, no chains", species, path);
            return BodyPlan.EMPTY;
         }

         JsonObject rig = root.has("rig") && root.get("rig").isJsonObject() ? root.getAsJsonObject("rig") : null;
         BodyPlan plan = BodyPlan.parse(rig);

         for (String problem : plan.problems()) {
            LOGGER.warn("[BodyPlans] {}: {}", species, problem);
         }

         return plan;
      } catch (Exception e) {
         LOGGER.error("[BodyPlans] {} - failed to read {}: {}", new Object[]{species, path, e.toString()});
         return BodyPlan.EMPTY;
      }
   }

   @Nullable
   public static JsonObject resolved(String species) {
      return read(species, null, species);
   }

   @Nullable
   private static JsonObject read(String species, @Nullable String host, String file) {
      JsonObject root = open(host, "animation_bindings/" + file + ".json");
      if (root == null) {
         return null;
      }

      String own = host != null ? host : SpeciesAssets.hostOfSpecies(species);
      List<String> problems = new ArrayList<>();
      JsonObject out = BindingSource.resolve(
         root,
         name -> open(own, "animation_bindings/templates/" + name + ".json"),
         () -> file.equals(species) ? BindingSource.Model.of(open(own, "geo/" + species + ".geo.json")) : null,
         problems
      );

      for (String problem : problems) {
         LOGGER.warn("[BodyPlans] {}: {}", species, problem);
      }

      return out;
   }

   @Nullable
   private static JsonObject open(@Nullable String host, String path) {
      try (InputStream in = SpeciesAssets.openFor(host, BodyPlans.class, path)) {
         return in == null ? null : JsonParser.parseReader(new InputStreamReader(in, StandardCharsets.UTF_8)).getAsJsonObject();
      } catch (IOException e) {
         throw new UncheckedIOException(e);
      }
   }
}
