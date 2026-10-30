package net.magister.liberanimalium.anim.rig;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

public final class CascadeSpec {
   public static final CascadeSpec EMPTY = new CascadeSpec(List.of(), List.of());
   private final List<CascadeSpec.Group> groups;
   private final List<Entry<String, CascadeSpec.Limb>> limbs;

   public CascadeSpec(List<CascadeSpec.Group> groups, List<Entry<String, CascadeSpec.Limb>> limbs) {
      this.groups = List.copyOf(groups);
      this.limbs = List.copyOf(limbs);
   }

   public List<CascadeSpec.Group> groups() {
      return this.groups;
   }

   public List<Entry<String, CascadeSpec.Limb>> limbs() {
      return this.limbs;
   }

   public boolean isEmpty() {
      return this.groups.isEmpty() && this.limbs.isEmpty();
   }

   public static CascadeSpec parse(JsonObject json) {
      if (json == null) {
         return EMPTY;
      }

      List<CascadeSpec.Group> groups = new ArrayList<>();
      if (json.has("groups") && json.get("groups").isJsonArray()) {
         for (JsonElement element : json.getAsJsonArray("groups")) {
            if (element.isJsonObject()) {
               JsonObject g = element.getAsJsonObject();
               boolean lead = g.has("lead") && g.get("lead").isJsonPrimitive() && g.get("lead").getAsBoolean();
               List<String> bones = new ArrayList<>();
               if (g.has("bones") && g.get("bones").isJsonArray()) {
                  for (JsonElement bone : g.getAsJsonArray("bones")) {
                     if (bone.isJsonPrimitive()) {
                        bones.add(bone.getAsString());
                     }
                  }
               }

               Map<String, CascadeSpec.Row> rows = new LinkedHashMap<>();
               if (g.has("rows") && g.get("rows").isJsonObject()) {
                  for (Entry<String, JsonElement> row : g.getAsJsonObject("rows").entrySet()) {
                     double[] v = triple(row.getValue());
                     if (v != null) {
                        rows.put(row.getKey(), new CascadeSpec.Row(v[0], v[1], v[2]));
                     }
                  }
               }

               groups.add(new CascadeSpec.Group(lead, List.copyOf(bones), Map.copyOf(rows), curve(g)));
            }
         }
      }

      List<Entry<String, CascadeSpec.Limb>> limbs = new ArrayList<>();
      if (json.has("limbs") && json.get("limbs").isJsonObject()) {
         for (Entry<String, JsonElement> entry : json.getAsJsonObject("limbs").entrySet()) {
            if (entry.getValue().isJsonArray()) {
               JsonArray a = entry.getValue().getAsJsonArray();
               if (a.size() >= 4) {
                  boolean left = "left".equalsIgnoreCase(a.get(0).getAsString());
                  limbs.add(Map.entry(entry.getKey(), new CascadeSpec.Limb(left, a.get(1).getAsDouble(), a.get(2).getAsDouble(), a.get(3).getAsDouble())));
               }
            }
         }
      }

      return new CascadeSpec(groups, limbs);
   }

   private static CascadeSpec.Curve curve(JsonObject group) {
      double[] s = triple2(group, "strength");
      double[] d = triple2(group, "decay");
      double[] m = triple2(group, "max_bend");
      return s != null && d != null && m != null ? new CascadeSpec.Curve(s[0], s[1], d[0], d[1], m[0], m[1]) : null;
   }

   private static double[] triple2(JsonObject group, String key) {
      if (group.has(key) && group.get(key).isJsonArray()) {
         JsonArray a = group.getAsJsonArray(key);
         return a.size() < 2 ? null : new double[]{a.get(0).getAsDouble(), a.get(1).getAsDouble()};
      } else {
         return null;
      }
   }

   private static double[] triple(JsonElement element) {
      if (!element.isJsonArray()) {
         return null;
      }

      JsonArray a = element.getAsJsonArray();
      return a.size() < 3 ? null : new double[]{a.get(0).getAsDouble(), a.get(1).getAsDouble(), a.get(2).getAsDouble()};
   }

   public record Curve(double strengthBase, double strengthTip, double decayBase, double decayTip, double bendBase, double bendTip) {
      public CascadeSpec.Row at(int index, int count) {
         double t = count <= 1 ? 0.0 : (double)index / (count - 1);
         return new CascadeSpec.Row(lerp(this.strengthBase, this.strengthTip, t), lerp(this.decayBase, this.decayTip, t), lerp(this.bendBase, this.bendTip, t));
      }

      private static double lerp(double a, double b, double t) {
         return a + (b - a) * t;
      }
   }

   public record Group(boolean lead, List<String> bones, Map<String, CascadeSpec.Row> rows, CascadeSpec.Curve curve) {
      public CascadeSpec.Row rowFor(String bone) {
         CascadeSpec.Row explicit = this.rows.get(bone);
         if (explicit != null) {
            return explicit;
         }

         if (this.curve == null) {
            return null;
         }

         int index = this.bones.indexOf(bone);
         return index < 0 ? null : this.curve.at(index, this.bones.size());
      }
   }

   public record Limb(boolean left, double strength, double decay, double maxBend) {
   }

   public record Row(double strength, double decay, double maxBend) {
   }
}
