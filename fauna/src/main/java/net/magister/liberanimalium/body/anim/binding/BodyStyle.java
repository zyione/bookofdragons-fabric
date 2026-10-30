package net.magister.liberanimalium.body.anim.binding;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

final class BodyStyle {
   private static final double FOLLOW = 0.8;
   private static final double AT_WALK = 1.525;
   private static final double STRIDE_SHARE = 0.74;
   private static final double SWAY_SHARE = 0.52;
   private static final double LEAN_AT_WALK = 5.25;
   private static final double SHARE_MAX = 1.6;
   private static final double DEFAULT_WALK = 0.0875;
   private static final Pattern WALK = Pattern.compile("horiz_speed\\s*/\\s*([\\d.]+)");

   private BodyStyle() {
   }

   static JsonObject build(JsonObject body, JsonObject file, BindingSource.Model model) {
      if (body.has("style") && body.get("style").isJsonObject()) {
         JsonObject style = body.getAsJsonObject("style");
         JsonObject written = body.has("springs") && body.get("springs").isJsonObject() ? body.getAsJsonObject("springs") : new JsonObject();
         JsonObject writtenChannels = body.has("channels") && body.get("channels").isJsonObject() ? body.getAsJsonObject("channels") : new JsonObject();
         JsonObject rig = file.has("rig") ? file.getAsJsonObject("rig") : new JsonObject();
         JsonObject chains = rig.has("chains") ? rig.getAsJsonObject("chains") : new JsonObject();
         List<String> prefixes = strings(rig, "prefixes");
         if (prefixes.isEmpty()) {
            prefixes = List.of("");
         }

         List<String> spine = strings(chains, "spine");
         String chest = spine.isEmpty() ? "" : spine.get(0);
         String hips = spine.isEmpty() ? "" : spine.get(spine.size() - 1);
         String middle = spine.size() > 1 ? spine.get(1) : chest;
         String root = strings(chains, "root").isEmpty() ? "" : strings(chains, "root").get(0);
         double walk = walkSpeed(file);
         Map<String, JsonObject> springs = new LinkedHashMap<>();
         if (style.has("chest") && style.has("hips")) {
            double cy = at(style, "chest", 0);
            double cr = at(style, "chest", 1);
            double hy = at(style, "hips", 0);
            double hr = at(style, "hips", 1);
            JsonElement f = style.get("frequency");
            double fv = f.getAsDouble();
            JsonElement dChest = style.getAsJsonArray("damping").get(0);
            JsonElement dHips = style.getAsJsonArray("damping").get(1);
            String scale = "(1 + gait.speed * " + num(0.5249999999999999 / walk) + ")";
            spring(
               springs,
               "hips_yaw",
               "(gait.rear_stride * " + num(0.74 * base(hy)) + " - gait.rear_sway * " + num(0.52 * base(hy)) + ") * " + scale + " - gait.yaw_rate * 1.5",
               f,
               dHips,
               round(hy * 1.4 + 2.0, 1)
            );
            spring(
               springs,
               "chest_yaw",
               "(gait.front_stride * " + num(0.74 * base(cy)) + " + gait.front_sway * " + num(0.52 * base(cy)) + ") * " + scale + " - gait.yaw_rate",
               f,
               dChest,
               round(cy * 1.4 + 2.0, 1)
            );
            spring(springs, "hips_roll", "-gait.rear_sway * " + num(2.0 * base(hr)) + " * " + scale, f, dHips, round(hr * 1.4 + 2.0, 1));
            spring(springs, "chest_roll", "-gait.front_sway * " + num(2.0 * base(cr)) + " * " + scale, f, dChest, round(cr * 1.4 + 2.0, 1));
            double bob = style.get("bob").getAsDouble();
            double nod = style.get("nod").getAsDouble();
            spring(springs, "bob", "gait.lift * " + num(bob), number(round(Math.min(10.0, fv + 0.4), 2)), number(0.6), bob);
            spring(springs, "nod", "(gait.front_lift - gait.rear_lift) * " + num(nod), f, number(0.55), nod);
            spring(
               springs,
               "lean",
               "-gait.lateral_speed * " + num(5.25 / walk) + " - gait.accel_right * 12",
               number(round(Math.max(1.0, fv * 0.55), 2)),
               number(0.6),
               8.0
            );
            spring(springs, "pitch", "gait.accel_forward * 10", number(round(Math.max(1.0, fv * 0.6), 2)), number(0.6), 6.0);
         }

         for (Entry<String, JsonElement> e : written.entrySet()) {
            springs.put(e.getKey(), e.getValue().getAsJsonObject());
         }

         double slither = number(style, "slither");
         double whip = number(style, "tail_whip");
         double hold = number(style, "neck_hold");
         double centre = number(style, "neck_centre");
         List<BodyStyle.Line> necks = lines(model, prefixes, strings(chains, "neck"));
         Map<String, List<String>> heads = new LinkedHashMap<>();

         for (BodyStyle.Line l : lines(model, prefixes, strings(chains, "head"))) {
            heads.put(l.prefix, l.bones);
         }

         List<String> tailChain = strings(chains, "tail");
         List<BodyStyle.Line> tails = lines(model, prefixes, tailChain.isEmpty() ? List.of() : tailChain.subList(0, 1));
         boolean longNeck = necks.stream().anyMatch(l -> l.bones.size() >= 2);
         if (slither > 0.0 && longNeck) {
            JsonElement limit = springs.get("chest_yaw").get("limit");
            JsonArray lag = style.getAsJsonArray("slither_lag");
            generate(springs, "neck_lag", "spring.chest_yaw", lag.get(0), number(0.5), limit);
            generate(springs, "neck_lag2", "spring.neck_lag", lag.get(1), number(0.5), limit);
         }

         if (whip > 0.0 && !tails.isEmpty()) {
            generate(springs, "tail_lag", "spring.hips_yaw", style.get("tail_lag"), number(0.4), springs.get("hips_yaw").get("limit"));
         }

         Map<String, String> channels = new LinkedHashMap<>();
         channels.put(hips + ".ry", "spring.hips_yaw");
         channels.put(hips + ".rz", "spring.hips_roll");
         channels.put(chest + ".ry", "spring.chest_yaw");
         channels.put(chest + ".rz", "spring.chest_roll");
         channels.put(carrier(model, middle) + ".rx", "spring.nod");
         channels.put(root + ".ty", "spring.bob");
         channels.put(root + ".rz", "spring.lean");
         if (style.has("side")) {
            channels.put(root + ".tx", num(style.get("side").getAsDouble()) + " * spring.lean");
         }

         channels.put(root + ".rx", "spring.pitch");

         for (BodyStyle.Line neck : necks) {
            String head = heads.containsKey(neck.prefix)
               ? heads.get(neck.prefix).get(0)
               : (heads.containsKey("") ? heads.get("").get(0) : neck.bones.get(neck.bones.size() - 1));
            double[] shares = neckShares(model, chest, neck.bones, head, hold, centre);
            int n = neck.bones.size();
            int pairs = slither > 0.0 ? n / 2 : 0;
            int first = n - 2 * pairs;
            String[] lags = new String[]{"(spring.neck_lag - spring.chest_yaw)", "(spring.neck_lag2 - spring.neck_lag)"};
            Map<String, String> yaw = new LinkedHashMap<>();
            Map<String, String> roll = new LinkedHashMap<>();
            Map<String, String> pitch = new LinkedHashMap<>();

            for (int i = 0; i < n; i++) {
               String bone = carrier(model, neck.bones.get(i));
               String term = num(shares[i]) + " * spring.chest_yaw";
               int j = i - first;
               if (j >= 0 && j < pairs) {
                  term = term + " + " + num(slither) + " * " + lags[j % 2];
               } else if (j >= pairs && j < 2 * pairs) {
                  term = term + " - " + num(slither) + " * " + lags[(j - pairs) % 2];
               }

               yaw.put(bone + ".ry", term);
               roll.put(bone + ".rz", num(-hold / n) + " * (spring.chest_roll + spring.lean)");
               pitch.put(bone + ".rx", num(-hold / n) + " * (spring.pitch + spring.nod)");
            }

            channels.putAll(yaw);
            channels.putAll(roll);
            channels.putAll(pitch);
         }

         if (whip > 0.0) {
            for (BodyStyle.Line tail : tails) {
               channels.put(carrier(model, tail.bones.get(0)) + ".ry", num(whip) + " * (spring.tail_lag - spring.hips_yaw)");
            }
         }

         JsonObject out = new JsonObject();
         JsonObject springsOut = new JsonObject();
         springs.forEach(springsOut::add);
         JsonObject channelsOut = new JsonObject();
         channels.forEach(channelsOut::addProperty);

         for (Entry<String, JsonElement> e : writtenChannels.entrySet()) {
            channelsOut.add(e.getKey(), e.getValue());
         }

         out.add("springs", springsOut);
         out.add("channels", channelsOut);
         return out;
      } else {
         return body;
      }
   }

   private static double base(double amplitude) {
      return amplitude / 1.22;
   }

   private static void spring(Map<String, JsonObject> springs, String name, String target, JsonElement frequency, JsonElement damping, double limit) {
      JsonObject s = new JsonObject();
      s.addProperty("target", target);
      s.add("frequency", frequency.deepCopy());
      s.add("damping", damping.deepCopy());
      s.addProperty("limit", limit);
      springs.put(name, s);
   }

   private static void generate(Map<String, JsonObject> springs, String name, String target, JsonElement frequency, JsonElement damping, JsonElement limit) {
      if (!springs.containsKey(name)) {
         JsonObject s = new JsonObject();
         s.addProperty("target", target);
         s.add("frequency", frequency.deepCopy());
         s.add("damping", damping.deepCopy());
         s.add("limit", limit.deepCopy());
         springs.put(name, s);
      }
   }

   private static JsonPrimitive number(double v) {
      return new JsonPrimitive(v);
   }

   private static double number(JsonObject o, String key) {
      return o.has(key) ? o.get(key).getAsDouble() : 0.0;
   }

   private static double at(JsonObject o, String key, int i) {
      return o.getAsJsonArray(key).get(i).getAsDouble();
   }

   private static double walkSpeed(JsonObject file) {
      try {
         String speed = file.getAsJsonObject("controllers")
            .getAsJsonObject("ground_motion")
            .getAsJsonObject("states")
            .getAsJsonObject("walk")
            .get("anim_speed")
            .getAsString();
         Matcher m = WALK.matcher(speed);
         return m.find() ? Double.parseDouble(m.group(1)) : 0.0875;
      } catch (RuntimeException e) {
         return 0.0875;
      }
   }

   private static List<String> strings(JsonObject o, String key) {
      List<String> out = new ArrayList<>();
      if (o.has(key) && o.get(key).isJsonArray()) {
         for (JsonElement e : o.getAsJsonArray(key)) {
            out.add(e.getAsString());
         }
      }

      return out;
   }

   private static String carrier(BindingSource.Model model, String name) {
      for (String rot : new String[]{name + "_rot", name + "rot"}) {
         if (model.parent().containsKey(rot) || model.bones().contains(rot)) {
            return rot;
         }
      }

      return name;
   }

   private static List<BodyStyle.Line> lines(BindingSource.Model model, List<String> prefixes, List<String> chain) {
      List<BodyStyle.Line> out = new ArrayList<>();

      for (String p : prefixes) {
         List<String> names = new ArrayList<>();

         for (String b : chain) {
            names.add(p + b);
         }

         if (!names.isEmpty() && model.bones().containsAll(names)) {
            out.add(new BodyStyle.Line(p, names));
         }
      }

      return out;
   }

   private static double[] neckShares(BindingSource.Model model, String chest, List<String> neck, String head, double hold, double centre) {
      int n = neck.size();
      double[] even = new double[n];
      Arrays.fill(even, -1.0 / n);
      if (n >= 2 && !(centre <= 0.0)) {
         List<String> line = new ArrayList<>();
         line.add(chest);
         line.addAll(neck);
         line.add(head);
         double[] z = new double[line.size()];

         for (int k = 0; k < z.length; k++) {
            double[] pivot = model.pivot().get(line.get(k));
            z[k] = pivot == null ? 0.0 : -pivot[2];
         }

         double[] seg = new double[line.size() - 1];

         for (int k = 0; k < seg.length; k++) {
            seg[k] = z[k + 1] - z[k];
         }

         double[] w = new double[n];

         for (int i = 1; i <= n; i++) {
            double sum = 0.0;

            for (int k = i; k < n; k++) {
               sum += seg[k];
            }

            w[i - 1] = sum;
         }

         double tail = 0.0;

         for (int k = 1; k < n; k++) {
            tail += seg[k];
         }

         double b1 = -1.0;
         double b2 = -(seg[0] + tail);
         double s11 = n;
         double s12 = 0.0;
         double s22 = 0.0;

         for (double x : w) {
            s12 += x;
            s22 += x * x;
         }

         double det = s11 * s22 - s12 * s12;
         if (Math.abs(det) < 1.0E-9) {
            return scaled(even, hold);
         }

         double l1 = (s22 * b1 - s12 * b2) / det;
         double l2 = (-s12 * b1 + s11 * b2) / det;
         double[] centred = new double[n];

         for (int i = 0; i < n; i++) {
            centred[i] = l1 + l2 * w[i];
         }

         for (double c = centre; c > 0.0; c -= 0.05) {
            double[] shares = new double[n];
            double max = 0.0;

            for (int i = 0; i < n; i++) {
               shares[i] = hold * ((1.0 - c) * even[i] + c * centred[i]);
               max = Math.max(max, Math.abs(shares[i]));
            }

            if (max <= 1.6) {
               return shares;
            }
         }

         return scaled(even, hold);
      } else {
         return scaled(even, hold);
      }
   }

   private static double[] scaled(double[] v, double k) {
      double[] out = new double[v.length];

      for (int i = 0; i < v.length; i++) {
         out[i] = k * v[i];
      }

      return out;
   }

   static double round(double value, int places) {
      return new BigDecimal(value).setScale(places, RoundingMode.HALF_EVEN).doubleValue();
   }

   static String num(double v) {
      if (v == 0.0) {
         return "0";
      } else {
         BigDecimal three = new BigDecimal(v).round(new MathContext(3, RoundingMode.HALF_EVEN));
         int exponent = three.precision() - three.scale() - 1;
         if (exponent >= -4 && exponent < 3) {
            BigDecimal s = three.stripTrailingZeros();
            return s.scale() < 0 ? s.setScale(0).toPlainString() : s.toPlainString();
         } else {
            BigDecimal fixed = new BigDecimal(v).setScale(3, RoundingMode.HALF_EVEN).stripTrailingZeros();
            return fixed.scale() < 0 ? fixed.setScale(0).toPlainString() : fixed.toPlainString();
         }
      }
   }

   private record Line(String prefix, List<String> bones) {
   }
}
