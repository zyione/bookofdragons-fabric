package net.magister.liberanimalium.anim.rig;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;
import net.magister.liberanimalium.util.SpecReader;

public final class SwimRoles {
   public static final List<String> CHAINS = List.of("neck", "head", "spine", "tail", "tail_fork");
   public static final String TRUNK = "trunk";
   private static final List<String> AXES = List.of("rx", "ry", "rz", "px", "py", "pz");
   private static final Set<String> MIRRORED = Set.of("ry", "rz", "px");
   private static final Map<String, String> PART_OF = Map.of(
      "head", "head_parts", "jaw", "head_parts", "neck", "neck_parts", "spine", "body_parts", "tail", "tail_parts", "tail_fork", "tail_parts"
   );
   private static final double MIDDLE = 0.5;

   private SwimRoles() {
   }

   public static Map<String, SwimRoles.Role> parse(JsonObject roles) {
      Map<String, SwimRoles.Role> out = new LinkedHashMap<>();
      if (roles == null) {
         return out;
      }

      for (Entry<String, JsonElement> e : roles.entrySet()) {
         if (e.getValue().isJsonObject()) {
            JsonObject r = e.getValue().getAsJsonObject();
            Map<String, List<SwimRoles.Term>> axes = new LinkedHashMap<>();

            for (String axis : AXES) {
               if (r.has(axis) && r.get(axis).isJsonArray()) {
                  List<SwimRoles.Term> terms = new ArrayList<>();

                  for (JsonElement t : r.getAsJsonArray(axis)) {
                     SwimRoles.Term term = term(t);
                     if (term != null) {
                        terms.add(term);
                     }
                  }

                  if (!terms.isEmpty()) {
                     axes.put(axis, List.copyOf(terms));
                  }
               }
            }

            out.put(
               e.getKey(),
               new SwimRoles.Role(
                  r.has("reach") ? r.get("reach").getAsInt() : Integer.MAX_VALUE,
                  SpecReader.num(r, "side", 0.0),
                  !r.has("mirror") || r.get("mirror").getAsBoolean(),
                  SpecReader.num(r, "scatter", 0.0),
                  SpecReader.num(r, "scale", 1.0),
                  Map.copyOf(axes)
               )
            );
         }
      }

      return out;
   }

   public static List<String> problems(JsonObject roles, String where) {
      List<String> out = new ArrayList<>();
      if (roles == null) {
         return out;
      }

      for (Entry<String, JsonElement> e : roles.entrySet()) {
         if (!e.getValue().isJsonNull()) {
            if (!e.getValue().isJsonObject()) {
               out.add(where + "." + e.getKey() + ": a role is an object");
            } else {
               for (Entry<String, JsonElement> k : e.getValue().getAsJsonObject().entrySet()) {
                  String key = k.getKey();
                  if (AXES.contains(key)) {
                     if (!k.getValue().isJsonArray()) {
                        out.add(where + "." + e.getKey() + "." + key + ": a list of [k, amplitude, phase]");
                     } else {
                        for (JsonElement t : k.getValue().getAsJsonArray()) {
                           if (term(t) == null) {
                              out.add(where + "." + e.getKey() + "." + key + ": each wave is [k, amplitude, phase], amplitude and phase a number or rows");
                           }
                        }
                     }
                  } else if (!Set.of("reach", "side", "mirror", "scatter", "scale").contains(key)) {
                     out.add(where + "." + e.getKey() + ": no key " + key);
                  }
               }
            }
         }
      }

      return out;
   }

   private static SwimRoles.Term term(JsonElement t) {
      if (t.isJsonArray() && t.getAsJsonArray().size() == 3) {
         JsonArray a = t.getAsJsonArray();
         if (!a.get(0).isJsonPrimitive()) {
            return null;
         }

         double[] amplitude = rows(a.get(1));
         double[] phase = rows(a.get(2));
         return amplitude != null && phase != null ? new SwimRoles.Term(a.get(0).getAsInt(), amplitude, phase) : null;
      } else {
         return null;
      }
   }

   private static double[] rows(JsonElement e) {
      if (e.isJsonPrimitive()) {
         return new double[]{e.getAsDouble()};
      }

      if (e.isJsonArray() && !e.getAsJsonArray().isEmpty()) {
         JsonArray a = e.getAsJsonArray();
         double[] out = new double[a.size()];

         for (int i = 0; i < out.length; i++) {
            if (!a.get(i).isJsonPrimitive()) {
               return null;
            }

            out[i] = a.get(i).getAsDouble();
         }

         return out;
      } else {
         return null;
      }
   }

   public static Map<String, SwimRoles.Place> places(SwimRoles.Shape shape) {
      Map<String, SwimRoles.Place> out = new LinkedHashMap<>();
      Map<String, List<String>> kids = new HashMap<>();

      for (Entry<String, String> e : shape.parent().entrySet()) {
         if (e.getValue() != null) {
            kids.computeIfAbsent(e.getValue(), k -> new ArrayList<>()).add(e.getKey());
         }
      }

      List<Entry<String, List<String>>> groups = new ArrayList<>(shape.roots().entrySet());
      List<String> wingRoots = new ArrayList<>();

      for (String prefix : shape.prefixes()) {
         for (String chain : List.of("left_wing", "right_wing")) {
            List<String> bones = shape.chains().getOrDefault(chain, List.of());
            if (!bones.isEmpty()) {
               wingRoots.add(prefix + bones.get(0));
            }
         }
      }

      groups.add(Map.entry("wings", wingRoots));
      Set<String> wingBones = new HashSet<>();

      for (String root : wingRoots) {
         if (shape.parent().containsKey(root)) {
            wingBones.addAll(subtree(kids, root).keySet());
         }
      }

      List<String> front = new ArrayList<>();
      List<String> hind = new ArrayList<>();

      for (Entry<String, String> limb : shape.limbs().entrySet()) {
         String root = limb.getValue();
         if (root != null && shape.parent().containsKey(root) && !wingBones.contains(root)) {
            (limb.getKey().toLowerCase(Locale.ROOT).contains("front") ? front : hind).add(root);
         }
      }

      groups.add(Map.entry("front_legs", front));
      groups.add(Map.entry("hind_legs", hind));

      for (Entry<String, List<String>> group : groups) {
         for (String root : group.getValue()) {
            if (shape.parent().containsKey(root)) {
               Map<String, Integer> sub = subtree(kids, root);
               int depth = sub.values().stream().mapToInt(Integer::intValue).max().orElse(0);
               int side = side(shape, root);

               for (Entry<String, Integer> b : sub.entrySet()) {
                  out.putIfAbsent(b.getKey(), new SwimRoles.Place(group.getKey(), b.getValue(), depth, side));
               }
            }
         }
      }

      for (String chain : CHAINS) {
         for (String prefix : shape.prefixes()) {
            List<String> names = new ArrayList<>();

            for (String n : shape.chains().getOrDefault(chain, List.of())) {
               String bone = prefix + n;
               if (shape.parent().containsKey(bone) && !out.containsKey(bone) && !names.contains(bone)) {
                  names.add(bone);
               }
            }

            for (int i = 0; i < names.size(); i++) {
               out.put(names.get(i), new SwimRoles.Place(chain, i, names.size() - 1, 0));
            }
         }
      }

      for (Entry<String, String> e : shape.parent().entrySet()) {
         String bone = e.getKey();
         String up = e.getValue();
         if (!out.containsKey(bone) && up != null) {
            SwimRoles.Place p = out.get(up);
            if (p != null && p.role().equals("head") && bone.toLowerCase(Locale.ROOT).contains("jaw")) {
               out.put(bone, new SwimRoles.Place("jaw", 0, 0, side(shape, bone)));
            }
         }
      }

      Map<String, SwimRoles.Place> parts = new HashMap<>();
      Map<String, Integer> deepest = new HashMap<>();

      for (String bone : shape.parent().keySet()) {
         if (!out.containsKey(bone)) {
            String up = shape.parent().get(bone);

            int depth;
            for (depth = 1; up != null && !out.containsKey(up); depth++) {
               up = shape.parent().get(up);
            }

            if (up != null) {
               String role = PART_OF.get(out.get(up).role());
               if (role != null) {
                  parts.put(bone, new SwimRoles.Place(role, depth, 0, side(shape, bone)));
                  deepest.merge(role, depth, Math::max);
               }
            }
         }
      }

      for (Entry<String, SwimRoles.Place> e : parts.entrySet()) {
         SwimRoles.Place p = e.getValue();
         out.put(e.getKey(), new SwimRoles.Place(p.role(), p.index() - 1, Math.max(deepest.get(p.role()) - 1, 0), p.side()));
      }

      out.keySet().retainAll(shape.moved());
      out.keySet().removeIf(bonex -> {
         String n = bonex.toLowerCase(Locale.ROOT);
         return shape.still().stream().anyMatch(w -> n.contains(w.toLowerCase(Locale.ROOT)));
      });
      return out;
   }

   private static Map<String, Integer> subtree(Map<String, List<String>> kids, String root) {
      Map<String, Integer> out = new LinkedHashMap<>();
      ArrayDeque<Entry<String, Integer>> todo = new ArrayDeque<>();
      todo.push(Map.entry(root, 0));

      while (!todo.isEmpty()) {
         Entry<String, Integer> at = todo.pop();
         out.put(at.getKey(), at.getValue());

         for (String k : kids.getOrDefault(at.getKey(), List.of())) {
            todo.push(Map.entry(k, at.getValue() + 1));
         }
      }

      return out;
   }

   static int side(SwimRoles.Shape shape, String bone) {
      double[] p = shape.pivot().get(bone);
      if (p != null && Math.abs(p[0]) >= 0.5) {
         return p[0] < 0.0 ? 1 : -1;
      } else {
         return side(bone);
      }
   }

   static int side(String bone) {
      String n = bone.toLowerCase(Locale.ROOT);
      if (n.startsWith("l_") || n.startsWith("lh_") || n.contains("left")) {
         return 1;
      } else {
         return !n.startsWith("r_") && !n.startsWith("rh_") && !n.contains("right") ? 0 : -1;
      }
   }

   static double scatter(String bone) {
      long h = 0L;

      for (int i = 0; i < bone.length(); i++) {
         h = h * 31L + bone.charAt(i) & 4294967295L;
      }

      return h % 1000L / 1000.0;
   }

   public static Map<String, SwimSpec.Track> tracks(Map<String, SwimRoles.Role> roles, SwimRoles.Shape shape, Map<String, SwimRoles.Place> places, double scale) {
      Map<String, List<List<double[]>>> waves = new LinkedHashMap<>();

      for (Entry<String, SwimRoles.Place> e : places.entrySet()) {
         SwimRoles.Place p = e.getValue();
         SwimRoles.Role role = roles.get(p.role());
         if (role != null && p.index() < role.reach()) {
            int span = Math.min(p.span(), role.reach() - 1);
            double u = span > 0 ? (double)p.index() / span : 0.0;
            double offset = p.side() < 0 ? role.side() : 0.0;
            double scatter = role.scatter() * scatter(e.getKey());

            for (int a = 0; a < 6; a++) {
               String axis = AXES.get(a);

               for (SwimRoles.Term t : role.axes().getOrDefault(axis, List.of())) {
                  double amplitude = at(t.amplitude(), u) * role.scale() * scale;
                  double phase = at(t.phase(), u) + t.k() * (offset + scatter);
                  if (p.side() < 0 && role.mirror() && MIRRORED.contains(axis)) {
                     phase += 180.0;
                  }

                  add(waves, e.getKey(), a, t.k(), amplitude, phase);
               }
            }
         }
      }

      SwimRoles.Role trunk = roles.get("trunk");
      if (trunk != null) {
         Map<String, SwimRoles.Along> along = along(shape, places);

         for (Entry<String, SwimRoles.Along> e : along.entrySet()) {
            SwimRoles.Along up = e.getValue().above() == null ? null : along.get(e.getValue().above());

            for (int a = 0; a < 6; a++) {
               for (SwimRoles.Term t : trunk.axes().getOrDefault(AXES.get(a), List.of())) {
                  double size = trunk.scale() * scale;
                  double[] own = phasor(t, e.getValue().s(), size);
                  double[] parent = up == null ? new double[2] : phasor(t, up.s(), size);
                  double re = own[0] - parent[0];
                  double im = own[1] - parent[1];
                  add(waves, e.getKey(), a, t.k(), Math.hypot(re, im), Math.toDegrees(Math.atan2(im, re)));
               }
            }
         }
      }

      Map<String, SwimSpec.Track> out = new LinkedHashMap<>();

      for (Entry<String, List<List<double[]>>> e : waves.entrySet()) {
         SwimSpec.Wave[] axes = new SwimSpec.Wave[6];

         for (int a = 0; a < 6; a++) {
            List<double[]> list = e.getValue().get(a);
            if (!list.isEmpty()) {
               int[] k = new int[list.size()];
               double[] amp = new double[list.size()];
               double[] ph = new double[list.size()];

               for (int i = 0; i < k.length; i++) {
                  k[i] = (int)list.get(i)[0];
                  amp[i] = list.get(i)[1];
                  ph[i] = list.get(i)[2];
               }

               axes[a] = new SwimSpec.Wave(k, amp, ph);
            }
         }

         SwimSpec.Wave[] rot = axes[0] == null && axes[1] == null && axes[2] == null ? null : new SwimSpec.Wave[]{axes[0], axes[1], axes[2]};
         SwimSpec.Wave[] pos = axes[3] == null && axes[4] == null && axes[5] == null ? null : new SwimSpec.Wave[]{axes[3], axes[4], axes[5]};
         if (rot != null || pos != null) {
            out.put(e.getKey(), new SwimSpec.Track(rot, pos));
         }
      }

      return out;
   }

   private static void add(Map<String, List<List<double[]>>> waves, String bone, int axis, int k, double amplitude, double phase) {
      if (!(Math.abs(amplitude) < 1.0E-6)) {
         List<List<double[]>> axes = waves.computeIfAbsent(bone, b -> {
            List<List<double[]>> list = new ArrayList<>();

            for (int i = 0; i < 6; i++) {
               list.add(new ArrayList<>());
            }

            return list;
         });
         axes.get(axis).add(new double[]{k, amplitude, phase});
      }
   }

   private static double[] phasor(SwimRoles.Term t, double s, double size) {
      double amplitude = at(t.amplitude(), s) * size;
      double phase = Math.toRadians(at(t.phase(), s));
      return new double[]{amplitude * Math.cos(phase), amplitude * Math.sin(phase)};
   }

   public static Map<String, SwimRoles.Along> along(SwimRoles.Shape shape, Map<String, SwimRoles.Place> places) {
      Set<String> member = new HashSet<>();

      for (Entry<String, SwimRoles.Place> e : places.entrySet()) {
         if (CHAINS.contains(e.getValue().role()) && shape.pivot().containsKey(e.getKey())) {
            member.add(e.getKey());
         }
      }

      if (member.isEmpty()) {
         return Map.of();
      }

      List<double[]> line = new ArrayList<>();

      for (String chain : List.of("head", "neck", "spine", "tail", "tail_fork")) {
         List<double[]> pts = new ArrayList<>();

         for (String n : shape.chains().getOrDefault(chain, List.of())) {
            double[] p = point(shape, n, member);
            if (p != null) {
               pts.add(p);
            }
         }

         if (!pts.isEmpty()) {
            if (!line.isEmpty() && pts.size() > 1) {
               double[] end = line.get(line.size() - 1);
               if (dist(pts.get(pts.size() - 1), end) < dist(pts.get(0), end)) {
                  Collections.reverse(pts);
               }
            }

            for (double[] p : pts) {
               if (line.isEmpty() || dist(line.get(line.size() - 1), p) > 1.0E-6) {
                  line.add(p);
               }
            }
         }
      }

      double[] run = new double[line.size()];

      for (int i = 1; i < line.size(); i++) {
         run[i] = run[i - 1] + dist(line.get(i - 1), line.get(i));
      }

      double length = Math.max(run[run.length - 1], 1.0E-6);
      Map<String, String> above = new HashMap<>();
      Map<String, List<String>> below = new HashMap<>();

      for (String bone : member) {
         String up = shape.parent().get(bone);

         while (up != null && !member.contains(up)) {
            up = shape.parent().get(up);
         }

         above.put(bone, up);
         if (up != null) {
            below.computeIfAbsent(up, kx -> new ArrayList<>()).add(bone);
         }
      }

      Map<String, SwimRoles.Along> out = new LinkedHashMap<>();

      for (String bone : member) {
         double[] own = shape.pivot().get(bone);
         double[] mid = (double[])own.clone();
         List<String> kids = below.getOrDefault(bone, List.of());
         if (!kids.isEmpty()) {
            double[] far = new double[3];

            for (String k : kids) {
               for (int i = 0; i < 3; i++) {
                  far[i] += ((double[])shape.pivot().get(k))[i] / kids.size();
               }
            }

            for (int i = 0; i < 3; i++) {
               mid[i] = (own[i] + far[i]) / 2.0;
            }
         }

         out.put(bone, new SwimRoles.Along(project(line, run, mid) / length, above.get(bone)));
      }

      return out;
   }

   private static double project(List<double[]> line, double[] run, double[] p) {
      if (line.size() == 1) {
         return 0.0;
      }

      double best = Double.MAX_VALUE;
      double at = 0.0;

      for (int i = 1; i < line.size(); i++) {
         double[] a = line.get(i - 1);
         double[] b = line.get(i);
         double[] ab = new double[]{b[0] - a[0], b[1] - a[1], b[2] - a[2]};
         double len2 = ab[0] * ab[0] + ab[1] * ab[1] + ab[2] * ab[2];
         double t = len2 < 1.0E-12 ? 0.0 : ((p[0] - a[0]) * ab[0] + (p[1] - a[1]) * ab[1] + (p[2] - a[2]) * ab[2]) / len2;
         t = Math.max(0.0, Math.min(1.0, t));
         double[] q = new double[]{a[0] + ab[0] * t, a[1] + ab[1] * t, a[2] + ab[2] * t};
         double d = dist(p, q);
         if (d < best) {
            best = d;
            at = run[i - 1] + (run[i] - run[i - 1]) * t;
         }
      }

      return at;
   }

   private static double[] point(SwimRoles.Shape shape, String name, Set<String> member) {
      double[] sum = new double[3];
      int n = 0;

      for (String prefix : shape.prefixes()) {
         double[] p = shape.pivot().get(prefix + name);
         if (p != null && member.contains(prefix + name)) {
            for (int i = 0; i < 3; i++) {
               sum[i] += p[i];
            }

            n++;
         }
      }

      if (n == 0) {
         return null;
      }

      for (int i = 0; i < 3; i++) {
         sum[i] /= n;
      }

      return sum;
   }

   private static double dist(double[] a, double[] b) {
      return Math.sqrt((a[0] - b[0]) * (a[0] - b[0]) + (a[1] - b[1]) * (a[1] - b[1]) + (a[2] - b[2]) * (a[2] - b[2]));
   }

   public static Map<String, SwimSpec.Track> scaled(Map<String, SwimSpec.Track> tracks, Map<String, Double> scales, List<String> prefixes) {
      if (scales.isEmpty()) {
         return tracks;
      }

      Map<String, SwimSpec.Track> out = new LinkedHashMap<>(tracks);

      for (Entry<String, Double> e : scales.entrySet()) {
         for (String prefix : prefixes) {
            SwimSpec.Track t = out.get(prefix + e.getKey());
            if (t != null) {
               out.put(prefix + e.getKey(), new SwimSpec.Track(scaled(t.rot(), e.getValue()), scaled(t.pos(), e.getValue())));
            }
         }
      }

      return out;
   }

   private static SwimSpec.Wave[] scaled(SwimSpec.Wave[] axes, double by) {
      if (axes == null) {
         return null;
      }

      SwimSpec.Wave[] out = new SwimSpec.Wave[axes.length];

      for (int i = 0; i < axes.length; i++) {
         SwimSpec.Wave w = axes[i];
         if (w != null) {
            double[] amp = (double[])w.amplitude().clone();

            for (int k = 0; k < amp.length; k++) {
               amp[k] *= by;
            }

            out[i] = new SwimSpec.Wave(w.harmonic(), amp, w.phase());
         }
      }

      return out;
   }

   static double at(double[] rows, double u) {
      if (rows.length == 1) {
         return rows[0];
      }

      double x = u * (rows.length - 1);
      int i = Math.min((int)Math.floor(x), rows.length - 2);
      return rows[i] + (rows[i + 1] - rows[i]) * (x - i);
   }

   public record Along(double s, String above) {
   }

   public record Place(String role, int index, int span, int side) {
   }

   public record Role(int reach, double side, boolean mirror, double scatter, double scale, Map<String, List<SwimRoles.Term>> axes) {
   }

   public record Shape(
      Map<String, String> parent,
      Set<String> moved,
      Map<String, List<String>> chains,
      List<String> prefixes,
      Map<String, String> limbs,
      Map<String, List<String>> roots,
      List<String> still,
      Map<String, double[]> pivot
   ) {
   }

   public record Term(int k, double[] amplitude, double[] phase) {
   }
}
