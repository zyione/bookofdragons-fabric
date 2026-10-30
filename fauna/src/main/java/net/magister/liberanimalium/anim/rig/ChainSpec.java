package net.magister.liberanimalium.anim.rig;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import net.minecraft.util.GsonHelper;

public record ChainSpec(Map<String, PointChain.Params> physics, Map<String, PointChain.Water> water, List<String> problems) {
   public static final ChainSpec EMPTY = new ChainSpec(Map.of(), Map.of(), List.of());
   static final double DEFAULT_DAMPING = 0.25;
   static final double DEFAULT_STIFFNESS_BASE = 0.1;
   static final double DEFAULT_STIFFNESS_TIP = 0.01;
   static final double DEFAULT_BEND_BASE = 20.0;
   static final double DEFAULT_BEND_TIP = 45.0;
   static final double DEFAULT_RADIUS_BASE = 0.5;
   static final double DEFAULT_RADIUS_TIP = 0.1;
   static final double DEFAULT_ASLEEP_FACTOR = 0.2;
   static final double DEFAULT_FRICTION = 0.9;
   static final double DEFAULT_FLOW_STIFFNESS = 0.9;
   static final double DEFAULT_FLOW_SPEED = 0.0;

   public ChainSpec {
      physics = Collections.unmodifiableMap(new LinkedHashMap<>(physics));
      problems = List.copyOf(problems);
   }

   public static ChainSpec parse(JsonObject json) {
      if (json == null) {
         return EMPTY;
      }

      Map<String, PointChain.Params> physics = new LinkedHashMap<>();
      Map<String, PointChain.Water> water = new LinkedHashMap<>();
      List<String> problems = new ArrayList<>();

      for (Entry<String, JsonElement> entry : json.entrySet()) {
         if (!entry.getValue().isJsonObject()) {
            problems.add("chain physics " + entry.getKey() + " is not an object");
         } else {
            JsonObject chain = entry.getValue().getAsJsonObject();
            physics.put(entry.getKey(), chainParams(entry.getKey(), chain, problems));
            double[] drag = pair(chain, "water_drag", PointChain.Water.DEFAULT.dragAlong(), PointChain.Water.DEFAULT.dragAcross());
            water.put(
               entry.getKey(),
               new PointChain.Water(
                  GsonHelper.getAsDouble(chain, "buoyancy", PointChain.Water.DEFAULT.buoyancy()), drag[0], drag[1], rows(chain, "water_follow", 1.0, 1.0)
               )
            );
         }
      }

      return new ChainSpec(physics, water, problems);
   }

   private static PointChain.Params chainParams(String chain, JsonObject json, List<String> problems) {
      if (json.has("gravity")) {
         problems.add("chain " + chain + " authors gravity, which is the world's (RigTuning.CHAIN_GRAVITY)");
      }

      if (json.has("held_stiffness")) {
         problems.add("chain " + chain + " authors held_stiffness, replaced by flow");
      }

      if (json.has("contact")) {
         problems.add("chain " + chain + " authors contact; the hard cone is measured from the parent now and the contact cone is gone");
      }

      if (json.has("iterations")) {
         problems.add("chain " + chain + " authors iterations; the chain is solved in one sweep");
      }

      double[] radius = pair(json, "radius", 0.5, 0.1);
      double[] bend = pair(json, "bend", 20.0, 45.0);
      double[] bendRows = list(json, "bend");
      if (bendRows != null && bendRows.length >= 3) {
         bend = new double[]{bendRows[1], bendRows[bendRows.length - 1]};
      } else {
         bendRows = null;
      }

      double[] cone = list(json, "cone");
      double[] flow = pair(json, "flow", 0.9, 0.0);
      if (json.has("flow") && json.get("flow").isJsonPrimitive()) {
         flow[1] = 0.0;
      }

      return new PointChain.Params(
         GsonHelper.getAsDouble(json, "damping", 0.25),
         rows(json, "stiffness", 0.1, 0.01),
         bend[0],
         bend[1],
         radius[0],
         radius[1],
         GsonHelper.getAsDouble(json, "asleep_stiffness", 0.2),
         GsonHelper.getAsDouble(json, "friction", 0.9),
         flow[0],
         flow[1],
         GsonHelper.getAsDouble(json, "tip", 0.0),
         GsonHelper.getAsBoolean(json, "head_led", false),
         bendRows,
         cone
      );
   }

   private static double[] list(JsonObject json, String key) {
      if (!json.has(key)) {
         return null;
      }

      JsonElement e = json.get(key);
      if (e.isJsonPrimitive()) {
         return new double[]{e.getAsDouble()};
      }

      if (e.isJsonArray() && !e.getAsJsonArray().isEmpty()) {
         JsonArray array = e.getAsJsonArray();
         double[] out = new double[array.size()];

         for (int i = 0; i < out.length; i++) {
            out[i] = array.get(i).getAsDouble();
         }

         return out;
      } else {
         return null;
      }
   }

   private static double[] pair(JsonObject json, String key, double base, double tip) {
      if (json.has(key) && json.get(key).isJsonArray() && json.getAsJsonArray(key).size() == 2) {
         JsonArray array = json.getAsJsonArray(key);
         return new double[]{array.get(0).getAsDouble(), array.get(1).getAsDouble()};
      } else if (json.has(key) && json.get(key).isJsonPrimitive()) {
         double both = json.get(key).getAsDouble();
         return new double[]{both, both};
      } else {
         return new double[]{base, tip};
      }
   }

   private static double[] rows(JsonObject json, String key, double base, double tip) {
      if (json.has(key) && json.get(key).isJsonArray()) {
         JsonArray array = json.getAsJsonArray(key);
         if (array.size() >= 2) {
            double[] rows = new double[array.size()];

            for (int i = 0; i < rows.length; i++) {
               rows[i] = array.get(i).getAsDouble();
            }

            return rows;
         }
      }

      return pair(json, key, base, tip);
   }
}
