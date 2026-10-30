package net.magister.liberanimalium.ride.render;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.Map.Entry;
import java.util.function.Predicate;
import net.magister.liberanimalium.anim.molang.SpringChannelSpec;

public final class RiderPoseProfile {
   private static final Gson STRINGS = new GsonBuilder().disableHtmlEscaping().create();
   private static final Set<String> TOP_KEYS = Set.of("seat", "blend_in_ticks", "head_follow", "load_smoothing_ticks", "pose", "hold", "springs", "channels");
   private static final Set<String> HOLD_KEYS = Set.of("bone", "offset", "weight");
   private static final Set<String> SEAT_KEYS = Set.of("bone", "bareback_bone", "offset");
   public RiderPoseProfile.Seat seat = RiderPoseProfile.Seat.DEFAULT;
   public float blendInTicks = 10.0F;
   public float headFollow = 0.0F;
   public float loadSmoothingTicks = 1.5F;
   public final float[][] pose = new float[RiderBone.COUNT][RiderBone.Dof.COUNT];
   public final RiderPoseProfile.Hold[] hold = new RiderPoseProfile.Hold[2];
   public SpringChannelSpec motion = new SpringChannelSpec();
   private final List<String> structural = new ArrayList<>();
   private final List<String> report = new ArrayList<>();

   public static RiderPoseProfile parse(JsonObject json, Predicate<String> queryKnown) {
      RiderPoseProfile p = new RiderPoseProfile();

      for (String key : json.keySet()) {
         if (!TOP_KEYS.contains(key) && !key.startsWith("__")) {
            p.structural.add("unknown key '" + key + "' (ignored)");
         }
      }

      if (json.has("seat")) {
         p.seat = seat(p, json.getAsJsonObject("seat"));
      }

      p.blendInTicks = clamp(p, "blend_in_ticks", number(p, json, "blend_in_ticks", 10.0F), 0.0F, 200.0F);
      p.headFollow = clamp(p, "head_follow", number(p, json, "head_follow", 0.0F), 0.0F, 1.0F);
      p.loadSmoothingTicks = clamp(p, "load_smoothing_ticks", number(p, json, "load_smoothing_ticks", 1.5F), 0.05F, 20.0F);
      if (json.has("pose")) {
         JsonObject pose = json.getAsJsonObject("pose");

         for (Entry<String, JsonElement> e : pose.entrySet()) {
            RiderBone bone = RiderBone.byKey(e.getKey());
            if (bone == null) {
               p.structural.add("pose: unknown bone '" + e.getKey() + "' (bones: " + boneKeys() + ")");
            } else {
               JsonObject b = e.getValue().getAsJsonObject();

               for (String k : b.keySet()) {
                  if (!k.equals("rot") && !k.equals("pos")) {
                     p.structural.add("pose." + bone.key + ": unknown key '" + k + "' (rot, pos)");
                  }
               }

               triple(p, b, "rot", bone, 0);
               triple(p, b, "pos", bone, 3);
            }
         }
      }

      if (json.has("hold")) {
         for (Entry<String, JsonElement> e : json.getAsJsonObject("hold").entrySet()) {
            int arm = e.getKey().equals(RiderBone.RIGHT_ARM.key) ? 0 : (e.getKey().equals(RiderBone.LEFT_ARM.key) ? 1 : -1);
            if (arm < 0) {
               p.structural.add("hold: '" + e.getKey() + "' is not an arm (right_arm, left_arm)");
            } else {
               p.hold[arm] = hold(p, e.getKey(), e.getValue().getAsJsonObject());
            }
         }
      }

      p.motion = SpringChannelSpec.parse(
         json,
         new SpringChannelSpec.Names(
            bx -> RiderBone.byKey(bx) != null,
            boneKeys(),
            d -> RiderBone.Dof.byKey(d) != null,
            "rx ry rz tx ty tz",
            List.of(SpringChannelSpec.Vars.of("rider", RiderQueries.Var.values())),
            queryKnown
         )
      );
      p.validate();
      return p;
   }

   private static RiderPoseProfile.Seat seat(RiderPoseProfile p, JsonObject s) {
      for (String k : s.keySet()) {
         if (!SEAT_KEYS.contains(k)) {
            p.structural.add("seat: unknown key '" + k + "' (bone, bareback_bone, offset)");
         }
      }

      String bone = s.has("bone") ? s.get("bone").getAsString() : RiderPoseProfile.Seat.DEFAULT.bone();
      String bareback = s.has("bareback_bone") && !s.get("bareback_bone").isJsonNull() ? s.get("bareback_bone").getAsString() : null;
      float x = 0.0F;
      float y = 0.0F;
      float z = 0.0F;
      if (s.has("offset")) {
         JsonArray a = s.getAsJsonArray("offset");
         if (a.size() != 3) {
            p.structural.add("seat.offset needs three numbers");
         } else {
            x = a.get(0).getAsFloat();
            y = a.get(1).getAsFloat();
            z = a.get(2).getAsFloat();
         }
      }

      return new RiderPoseProfile.Seat(bone, bareback, x, y, z);
   }

   private static RiderPoseProfile.Hold hold(RiderPoseProfile p, String arm, JsonObject h) {
      for (String k : h.keySet()) {
         if (!HOLD_KEYS.contains(k)) {
            p.structural.add("hold." + arm + ": unknown key '" + k + "' (bone, offset, weight)");
         }
      }

      if (!h.has("bone")) {
         p.structural.add("hold." + arm + " needs a bone");
         return null;
      }

      float x = 0.0F;
      float y = 0.0F;
      float z = 0.0F;
      if (h.has("offset")) {
         JsonArray a = h.getAsJsonArray("offset");
         if (a.size() != 3) {
            p.structural.add("hold." + arm + ".offset needs three numbers");
         } else {
            x = a.get(0).getAsFloat();
            y = a.get(1).getAsFloat();
            z = a.get(2).getAsFloat();
         }
      }

      float weight = clamp(p, "hold." + arm + ".weight", number(p, h, "weight", 1.0F), 0.0F, 1.0F);
      return new RiderPoseProfile.Hold(h.get("bone").getAsString(), x, y, z, weight);
   }

   private static float number(RiderPoseProfile p, JsonObject o, String key, float fallback) {
      if (!o.has(key)) {
         return fallback;
      }

      try {
         return o.get(key).getAsFloat();
      } catch (RuntimeException e) {
         p.structural.add("'" + key + "' is not a number");
         return fallback;
      }
   }

   private static float clamp(RiderPoseProfile p, String what, float v, float lo, float hi) {
      if (!(v < lo) && !(v > hi) && Float.isFinite(v)) {
         return v;
      }

      float c = Float.isFinite(v) ? Math.max(lo, Math.min(hi, v)) : lo;
      p.structural.add(what + " " + v + " is outside [" + lo + ", " + hi + "] - using " + c);
      return c;
   }

   private static void triple(RiderPoseProfile p, JsonObject b, String key, RiderBone bone, int offset) {
      if (b.has(key)) {
         JsonArray a = b.getAsJsonArray(key);
         if (a.size() != 3) {
            p.structural.add("pose." + bone.key + "." + key + " needs three numbers");
         } else {
            for (int i = 0; i < 3; i++) {
               p.pose[bone.ordinal()][offset + i] = a.get(i).getAsFloat();
            }
         }
      }
   }

   private static String boneKeys() {
      StringBuilder sb = new StringBuilder();

      for (RiderBone b : RiderBone.values()) {
         sb.append(sb.length() == 0 ? "" : " ").append(b.key);
      }

      return sb.toString();
   }

   public List<String> report() {
      return this.report;
   }

   public void validate() {
      this.motion.validate();
      this.report.clear();
      this.report.addAll(this.structural);
      this.report.addAll(this.motion.report());
   }

   public String write() {
      StringBuilder sb = new StringBuilder();
      sb.append("{\n");
      sb.append("  \"seat\": { \"bone\": ").append(STRINGS.toJson(this.seat.bone()));
      if (this.seat.barebackBone() != null) {
         sb.append(", \"bareback_bone\": ").append(STRINGS.toJson(this.seat.barebackBone()));
      }

      sb.append(", \"offset\": [").append(num(this.seat.x())).append(", ").append(num(this.seat.y())).append(", ").append(num(this.seat.z())).append("] },\n");
      sb.append("  \"blend_in_ticks\": ").append(num(this.blendInTicks)).append(",\n");
      sb.append("  \"head_follow\": ").append(num(this.headFollow)).append(",\n");
      sb.append("  \"load_smoothing_ticks\": ").append(num(this.loadSmoothingTicks)).append(",\n");
      sb.append("  \"pose\": {\n");
      RiderBone[] bones = RiderBone.values();

      for (int i = 0; i < bones.length; i++) {
         float[] row = this.pose[i];
         sb.append("    \"")
            .append(bones[i].key)
            .append("\": { \"rot\": [")
            .append(num(row[0]))
            .append(", ")
            .append(num(row[1]))
            .append(", ")
            .append(num(row[2]))
            .append("], \"pos\": [")
            .append(num(row[3]))
            .append(", ")
            .append(num(row[4]))
            .append(", ")
            .append(num(row[5]))
            .append("] }")
            .append(i < bones.length - 1 ? ",\n" : "\n");
      }

      sb.append("  },\n");
      if (this.hold[0] != null || this.hold[1] != null) {
         sb.append("  \"hold\": {\n");
         boolean first = true;

         for (int i = 0; i < 2; i++) {
            RiderPoseProfile.Hold h = this.hold[i];
            if (h != null) {
               if (!first) {
                  sb.append(",\n");
               }

               first = false;
               sb.append("    \"")
                  .append(i == 0 ? RiderBone.RIGHT_ARM.key : RiderBone.LEFT_ARM.key)
                  .append("\": { \"bone\": ")
                  .append(STRINGS.toJson(h.bone()))
                  .append(", \"offset\": [")
                  .append(num(h.x()))
                  .append(", ")
                  .append(num(h.y()))
                  .append(", ")
                  .append(num(h.z()))
                  .append("], \"weight\": ")
                  .append(num(h.weight()))
                  .append(" }");
            }
         }

         sb.append("\n  },\n");
      }

      sb.append("  \"springs\": ").append(this.motion.springsJson("  ")).append(",\n");
      sb.append("  \"channels\": ").append(this.motion.channelsJson("  ")).append("\n");
      sb.append("}\n");
      return sb.toString();
   }

   static String num(float v) {
      return SpringChannelSpec.num(v);
   }

   public record Hold(String bone, float x, float y, float z, float weight) {
   }

   public record Seat(String bone, String barebackBone, float x, float y, float z) {
      public static final RiderPoseProfile.Seat DEFAULT = new RiderPoseProfile.Seat("saddle", null, 0.0F, 0.0F, 0.0F);

      public String attachBone(boolean saddled) {
         return !saddled && this.barebackBone != null ? this.barebackBone : this.bone;
      }
   }
}
