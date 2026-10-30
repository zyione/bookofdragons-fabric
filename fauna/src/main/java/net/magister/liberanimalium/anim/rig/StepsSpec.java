package net.magister.liberanimalium.anim.rig;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;
import java.util.function.Predicate;
import net.magister.liberanimalium.anim.molang.SpringChannelSpec;
import net.magister.liberanimalium.util.SpecReader;

public final class StepsSpec {
   public static final StepsSpec EMPTY = new StepsSpec(true, List.of(), null, GaitSpec.DEFAULT, SpringChannelSpec.EMPTY, List.of());
   private static final Set<String> KEYS = Set.of("enabled", "limbs", "attitude", "gait", "body");
   private static final Set<String> LIMB_KEYS = Set.of(
      "root", "mid", "foot", "knee", "reach_up", "reach_down", "splay", "reach", "zone", "anchors", "plant_high", "plant_low"
   );
   public final boolean enabled;
   public final List<LimbDef> limbs;
   public final JsonObject attitude;
   public final GaitSpec gait;
   public final SpringChannelSpec body;
   private final List<String> problems;

   public StepsSpec(boolean enabled, List<LimbDef> limbs, JsonObject attitude, GaitSpec gait, SpringChannelSpec body, List<String> problems) {
      this.enabled = enabled;
      this.limbs = List.copyOf(limbs);
      this.attitude = attitude;
      this.gait = gait;
      this.body = body;
      this.problems = List.copyOf(problems);
   }

   public boolean isEmpty() {
      return this.limbs.isEmpty();
   }

   public List<String> problems() {
      return this.problems;
   }

   public LimbDef limb(String name) {
      for (LimbDef l : this.limbs) {
         if (l.name().equals(name)) {
            return l;
         }
      }

      return null;
   }

   public static StepsSpec parse(JsonObject json, Predicate<String> queryKnown) {
      if (json == null) {
         return EMPTY;
      }

      List<String> problems = new ArrayList<>();

      for (String k : json.keySet()) {
         if (!KEYS.contains(k)) {
            problems.add("unknown key '" + k + "' (enabled, limbs, attitude, gait, body)");
         }
      }

      boolean enabled = !json.has("enabled") || json.get("enabled").getAsBoolean();
      GaitSpec gait = GaitSpec.parse(json.has("gait") && json.get("gait").isJsonObject() ? json.getAsJsonObject("gait") : null);
      List<LimbDef> limbs = new ArrayList<>();
      if (json.has("limbs") && json.get("limbs").isJsonObject()) {
         for (Entry<String, JsonElement> e : json.getAsJsonObject("limbs").entrySet()) {
            if (!e.getValue().isJsonObject()) {
               problems.add("limbs." + e.getKey() + " is not an object");
            } else {
               LimbDef limb = limb(e.getKey(), e.getValue().getAsJsonObject(), gait, problems);
               if (limb != null) {
                  limbs.add(limb);
               }
            }
         }
      }

      JsonObject attitude = json.has("attitude") && json.get("attitude").isJsonObject() ? json.getAsJsonObject("attitude") : null;
      SpringChannelSpec body = SpringChannelSpec.parse(
         json.has("body") && json.get("body").isJsonObject() ? json.getAsJsonObject("body") : null,
         new SpringChannelSpec.Names(
            b -> true,
            "any bone of the model",
            d -> BoneAxis.byKey(d) != null,
            "rx ry rz tx ty tz sx sy sz",
            List.of(SpringChannelSpec.Vars.of("gait", GaitQueries.Var.values())),
            queryKnown
         )
      );

      for (String line : body.report()) {
         problems.add("body: " + line);
      }

      return new StepsSpec(enabled, limbs, attitude, gait, body, problems);
   }

   private static LimbDef limb(String name, JsonObject lj, GaitSpec gait, List<String> problems) {
      for (String k : lj.keySet()) {
         if (!LIMB_KEYS.contains(k)) {
            problems.add("limbs." + name + ": unknown key '" + k + "'");
         }
      }

      String root = SpecReader.string(lj, "root");
      String mid = SpecReader.string(lj, "mid");
      String foot = SpecReader.string(lj, "foot");
      if (root != null && mid != null && foot != null) {
         List<String> anchors = new ArrayList<>();
         if (lj.has("anchors")) {
            for (JsonElement a : lj.getAsJsonArray("anchors")) {
               anchors.add(a.getAsString());
            }
         }

         return new LimbDef(
            name,
            root,
            mid,
            foot,
            SpecReader.string(lj, "knee"),
            SpecReader.numF(lj, "reach_up", 1.0F),
            SpecReader.numF(lj, "reach_down", -1.5F),
            SpecReader.flag(lj, "splay", false),
            SpecReader.flag(lj, "reach", true),
            SpecReader.numF(lj, "zone", (float)gait.zone()),
            List.copyOf(anchors),
            pose(lj, "plant_high"),
            pose(lj, "plant_low")
         );
      } else {
         problems.add("limbs." + name + " needs root, mid and foot (skipped)");
         return null;
      }
   }

   private static Map<String, float[]> pose(JsonObject limb, String key) {
      Map<String, float[]> pose = new LinkedHashMap<>();
      if (!limb.has(key)) {
         return pose;
      }

      for (Entry<String, JsonElement> e : limb.getAsJsonObject(key).entrySet()) {
         JsonArray v = e.getValue().getAsJsonArray();
         pose.put(e.getKey(), new float[]{v.get(0).getAsFloat(), v.get(1).getAsFloat(), v.get(2).getAsFloat()});
      }

      return pose;
   }
}
