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
import net.magister.liberanimalium.util.SpecReader;

public record SwimSpec(
   double[] rate,
   double[] amplitude,
   SwimSpec.Legs legs,
   SwimSpec.Wings wings,
   Map<String, SwimSpec.Track> bones,
   Map<String, SwimSpec.Track> under,
   List<SwimSpec.Floater> floats,
   Map<String, SwimRoles.Role> roles,
   Map<String, SwimRoles.Role> underRoles,
   Map<String, List<String>> roleRoots,
   double scale,
   List<String> still,
   Map<String, Double> boneScale
) {
   public static final SwimSpec EMPTY = new SwimSpec(
      new double[]{0.3, 0.8},
      new double[]{0.35, 1.0},
      SwimSpec.Legs.NONE,
      SwimSpec.Wings.NONE,
      Map.of(),
      Map.of(),
      List.of(),
      Map.of(),
      Map.of(),
      Map.of(),
      1.0,
      List.of(),
      Map.of()
   );

   public boolean isEmpty() {
      return this.bones.isEmpty() && this.under.isEmpty() && this.floats.isEmpty() && this.roles.isEmpty() && this.underRoles.isEmpty();
   }

   public static SwimSpec parse(JsonObject json) {
      if (json == null) {
         return EMPTY;
      }

      double[] rate = pair(json, "rate", EMPTY.rate);
      double[] amplitude = pair(json, "amplitude", EMPTY.amplitude);
      SwimSpec.Legs legs = SwimSpec.Legs.NONE;
      if (json.has("legs") && json.get("legs").isJsonObject()) {
         JsonObject l = json.getAsJsonObject("legs");
         Map<String, double[]> splay = new LinkedHashMap<>();
         if (l.has("splay") && l.get("splay").isJsonObject()) {
            for (Entry<String, JsonElement> e : l.getAsJsonObject("splay").entrySet()) {
               double[] v = numbers(e.getValue());
               if (v != null && v.length == 3) {
                  splay.put(e.getKey(), v);
               }
            }
         }

         legs = new SwimSpec.Legs(
            Set.copyOf(SpecReader.strings(l, "bones")),
            Map.copyOf(splay),
            SpecReader.num(l, "paddle", SwimSpec.Legs.NONE.paddle),
            pair(l, "tuck", SwimSpec.Legs.NONE.tuck)
         );
      }

      SwimSpec.Wings wings = SwimSpec.Wings.NONE;
      if (json.has("wings") && json.get("wings").isJsonObject()) {
         JsonObject w = json.getAsJsonObject("wings");
         wings = new SwimSpec.Wings(
            Set.copyOf(SpecReader.strings(w, "left")),
            Set.copyOf(SpecReader.strings(w, "right")),
            SpecReader.num(w, "open", 0.0),
            SpecReader.num(w, "turn", 0.0),
            SpecReader.num(w, "fold", SwimSpec.Wings.NONE.fold)
         );
      }

      Map<String, SwimSpec.Track> bones = tracks(json);
      Map<String, SwimSpec.Track> under = json.has("under") && json.get("under").isJsonObject() ? tracks(json.getAsJsonObject("under")) : Map.of();
      List<SwimSpec.Floater> floats = new ArrayList<>();
      if (json.has("float") && json.get("float").isJsonArray()) {
         for (JsonElement e : json.getAsJsonArray("float")) {
            if (e.isJsonObject()) {
               JsonObject f = e.getAsJsonObject();
               if (f.has("bone") && f.has("point")) {
                  boolean move = f.has("move") && f.get("move").getAsBoolean();
                  String axis = f.has("axis") ? f.get("axis").getAsString() : "x";
                  int a = "y".equals(axis) ? 1 : ("z".equals(axis) ? 2 : 0);
                  String wing = f.has("wing") ? f.get("wing").getAsString() : null;
                  floats.add(
                     new SwimSpec.Floater(
                        f.get("bone").getAsString(),
                        f.get("point").getAsString(),
                        SpecReader.num(f, "depth", 0.0),
                        move,
                        a,
                        wing,
                        SpecReader.num(f, "max", move ? 48.0 : 40.0)
                     )
                  );
               }
            }
         }
      }

      JsonObject deep = json.has("under") && json.get("under").isJsonObject() ? json.getAsJsonObject("under") : null;
      Map<String, List<String>> roots = new LinkedHashMap<>();
      if (json.has("role_roots") && json.get("role_roots").isJsonObject()) {
         for (String role : json.getAsJsonObject("role_roots").keySet()) {
            roots.put(role, List.copyOf(SpecReader.strings(json.getAsJsonObject("role_roots"), role)));
         }
      }

      return new SwimSpec(
         rate,
         amplitude,
         legs,
         wings,
         bones,
         under,
         List.copyOf(floats),
         Map.copyOf(SwimRoles.parse(SpecReader.object(json, "roles"))),
         Map.copyOf(SwimRoles.parse(deep == null ? null : SpecReader.object(deep, "roles"))),
         Map.copyOf(roots),
         SpecReader.num(json, "scale", 1.0),
         List.copyOf(SpecReader.strings(json, "still")),
         boneScale(json)
      );
   }

   private static Map<String, Double> boneScale(JsonObject json) {
      Map<String, Double> out = new LinkedHashMap<>();
      JsonObject scales = SpecReader.object(json, "bone_scale");
      if (scales == null) {
         return Map.of();
      }

      for (Entry<String, JsonElement> e : scales.entrySet()) {
         if (e.getValue().isJsonPrimitive()) {
            out.put(e.getKey(), e.getValue().getAsDouble());
         }
      }

      return Map.copyOf(out);
   }

   private static Map<String, SwimSpec.Track> tracks(JsonObject json) {
      Map<String, SwimSpec.Track> bones = new LinkedHashMap<>();
      if (json.has("bones") && json.get("bones").isJsonObject()) {
         for (Entry<String, JsonElement> e : json.getAsJsonObject("bones").entrySet()) {
            if (e.getValue().isJsonObject()) {
               JsonObject b = e.getValue().getAsJsonObject();
               SwimSpec.Wave[] rot = axes(b.get("rot"));
               SwimSpec.Wave[] pos = axes(b.get("pos"));
               if (rot != null || pos != null) {
                  bones.put(e.getKey(), new SwimSpec.Track(rot, pos));
               }
            }
         }
      }

      return Map.copyOf(bones);
   }

   private static SwimSpec.Wave[] axes(JsonElement e) {
      if (e != null && e.isJsonArray() && e.getAsJsonArray().size() == 3) {
         SwimSpec.Wave[] out = new SwimSpec.Wave[3];

         for (int i = 0; i < 3; i++) {
            out[i] = wave(e.getAsJsonArray().get(i));
         }

         return out;
      } else {
         return null;
      }
   }

   private static SwimSpec.Wave wave(JsonElement e) {
      double[] v = numbers(e);
      if (v != null && v.length != 0 && v.length % 3 == 0) {
         int n = v.length / 3;
         int[] k = new int[n];
         double[] a = new double[n];
         double[] p = new double[n];

         for (int i = 0; i < n; i++) {
            k[i] = (int)Math.round(v[3 * i]);
            a[i] = v[1 + 3 * i];
            p[i] = v[2 + 3 * i];
         }

         return new SwimSpec.Wave(k, a, p);
      } else {
         return null;
      }
   }

   public static List<String> problems(JsonObject json) {
      List<String> out = new ArrayList<>();
      if (json == null) {
         return out;
      }

      problems(json, "bones", out);
      if (json.has("under") && json.get("under").isJsonObject()) {
         problems(json.getAsJsonObject("under"), "under", out);
      }

      out.addAll(SwimRoles.problems(SpecReader.object(json, "roles"), "roles"));
      if (json.has("under") && json.get("under").isJsonObject()) {
         out.addAll(SwimRoles.problems(SpecReader.object(json.getAsJsonObject("under"), "roles"), "under.roles"));
      }

      return out;
   }

   private static void problems(JsonObject json, String where, List<String> out) {
      if (json.has("bones") && json.get("bones").isJsonObject()) {
         for (Entry<String, JsonElement> e : json.getAsJsonObject("bones").entrySet()) {
            if (e.getValue().isJsonObject()) {
               for (String key : new String[]{"rot", "pos"}) {
                  JsonElement axes = e.getValue().getAsJsonObject().get(key);
                  if (axes != null) {
                     if (axes.isJsonArray() && axes.getAsJsonArray().size() == 3) {
                        for (JsonElement axis : axes.getAsJsonArray()) {
                           if (!axis.isJsonNull()) {
                              double[] v = numbers(axis);
                              if (v == null || v.length == 0 || v.length % 3 != 0) {
                                 out.add(
                                    where + " " + e.getKey() + "." + key + ": an axis is [k, a, p, ...] waves; the held pose belongs in the swim_stance clip"
                                 );
                              }
                           }
                        }
                     } else {
                        out.add(where + " " + e.getKey() + "." + key + ": three axes, each waves or null");
                     }
                  }
               }
            }
         }
      }
   }

   private static double[] numbers(JsonElement e) {
      if (e != null && e.isJsonArray()) {
         JsonArray array = e.getAsJsonArray();
         double[] out = new double[array.size()];

         for (int i = 0; i < out.length; i++) {
            if (!array.get(i).isJsonPrimitive()) {
               return null;
            }

            out[i] = array.get(i).getAsDouble();
         }

         return out;
      } else {
         return null;
      }
   }

   private static double[] pair(JsonObject json, String key, double[] fallback) {
      double[] v = json.has(key) ? numbers(json.get(key)) : null;
      return v != null && v.length == 2 ? v : fallback;
   }

   public record Floater(String bone, String point, double depth, boolean move, int axis, String wing, double max) {
   }

   public record Legs(Set<String> bones, Map<String, double[]> splay, double paddle, double[] tuck) {
      public static final SwimSpec.Legs NONE = new SwimSpec.Legs(Set.of(), Map.of(), 0.15, new double[]{0.5, 0.8});
   }

   public record Track(SwimSpec.Wave[] rot, SwimSpec.Wave[] pos) {
   }

   public record Wave(int[] harmonic, double[] amplitude, double[] phase) {
      public double at(double phaseDeg, double share) {
         double sum = 0.0;

         for (int i = 0; i < this.harmonic.length; i++) {
            sum += this.amplitude[i] * Math.sin(Math.toRadians(this.harmonic[i] * phaseDeg + this.phase[i]));
         }

         return share * sum;
      }
   }

   public record Wings(Set<String> left, Set<String> right, double open, double turn, double fold) {
      public static final SwimSpec.Wings NONE = new SwimSpec.Wings(Set.of(), Set.of(), 0.0, 0.0, 0.5);
   }
}
