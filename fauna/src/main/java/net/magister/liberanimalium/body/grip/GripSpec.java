package net.magister.liberanimalium.body.grip;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import org.jetbrains.annotations.Nullable;
import net.magister.liberanimalium.anim.rig.LimbDef;
import net.magister.liberanimalium.util.SpecReader;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;

public record GripSpec(
   List<GripSpec.Gripper> grippers,
   Vec3 hold,
   double bandLow,
   double bandHigh,
   double forward,
   double back,
   double side,
   Set<GripSpec.Medium> media,
   List<String> problems
) {
   private static final Set<GripSpec.Medium> DEFAULT_MEDIA = Set.of(GripSpec.Medium.AIR, GripSpec.Medium.WATER);
   public static final GripSpec NONE = new GripSpec(List.of(), Vec3.ZERO, 0.75, 0.95, 20.0, 35.0, 50.0, DEFAULT_MEDIA, List.of());

   public Vec3 middle() {
      if (this.grippers.isEmpty()) {
         return this.hold;
      }

      Vec3 sum = Vec3.ZERO;

      for (GripSpec.Gripper g : this.grippers) {
         sum = sum.add(g.at());
      }

      return sum.scale(1.0 / this.grippers.size());
   }

   public boolean grips() {
      return !this.grippers.isEmpty();
   }

   public double bandMid() {
      return (this.bandLow + this.bandHigh) * 0.5;
   }

   public double bandMid(@Nullable Entity held) {
      double mid = this.bandMid();
      if (held != null && !(held.getBbHeight() <= 0.0F)) {
         double height = held.getBbHeight();
         double eye = held.getEyeHeight();
         double chest = (eye - 1.5 * (height - eye)) / height;
         return chest > 0.0 ? Math.min(mid, chest) : mid;
      } else {
         return mid;
      }
   }

   public double shortestReach() {
      double shortest = Double.MAX_VALUE;

      for (GripSpec.Gripper g : this.grippers) {
         shortest = Math.min(shortest, g.reach());
      }

      return this.grippers.isEmpty() ? 0.0 : shortest;
   }

   public double longestReach() {
      double longest = 0.0;

      for (GripSpec.Gripper g : this.grippers) {
         longest = Math.max(longest, g.reach());
      }

      return longest;
   }

   public double span() {
      double apart = 0.0;

      for (int i = 0; i < this.grippers.size(); i++) {
         for (int j = i + 1; j < this.grippers.size(); j++) {
            apart = Math.max(apart, this.grippers.get(i).at().distanceTo(this.grippers.get(j).at()));
         }
      }

      return apart + this.shortestReach();
   }

   public static GripSpec parse(@Nullable JsonObject json) {
      if (json == null) {
         return NONE;
      }

      List<String> problems = new ArrayList<>();
      List<GripSpec.Gripper> grippers = new ArrayList<>();
      if (json.has("grippers") && json.get("grippers").isJsonArray()) {
         for (JsonElement element : json.getAsJsonArray("grippers")) {
            if (!element.isJsonObject()) {
               problems.add("grip.grippers holds something that is not an object");
            } else {
               GripSpec.Gripper g = gripper(element.getAsJsonObject(), problems);
               if (g != null) {
                  grippers.add(g);
               }
            }
         }
      } else {
         problems.add("grip has no grippers: the body grips nothing");
      }

      Vec3 hold = vec(json, "hold");
      if (hold == null) {
         problems.add("grip has no hold [x, y, z]: held bodies hang at the grippers");
         hold = Vec3.ZERO;
      }

      double low = NONE.bandLow;
      double high = NONE.bandHigh;
      if (json.has("band") && json.get("band").isJsonArray() && json.getAsJsonArray("band").size() == 2) {
         JsonArray band = json.getAsJsonArray("band");
         low = band.get(0).getAsDouble();
         high = band.get(1).getAsDouble();
      }

      if (!(low >= 0.0) || !(high <= 1.0) || !(low <= high)) {
         problems.add("grip.band must be two shares of height, low first, within [0, 1]");
         low = NONE.bandLow;
         high = NONE.bandHigh;
      }

      JsonObject limits = json.has("limits") && json.get("limits").isJsonObject() ? json.getAsJsonObject("limits") : new JsonObject();
      Set<GripSpec.Medium> media = DEFAULT_MEDIA;
      if (json.has("media") && json.get("media").isJsonArray()) {
         EnumSet<GripSpec.Medium> listed = EnumSet.noneOf(GripSpec.Medium.class);

         for (JsonElement element : json.getAsJsonArray("media")) {
            try {
               listed.add(GripSpec.Medium.valueOf(element.getAsString().toUpperCase(Locale.ROOT)));
            } catch (RuntimeException e) {
               problems.add("grip.media: " + element + " is not air, water or ground");
            }
         }

         media = Set.copyOf(listed);
      }

      return new GripSpec(
         List.copyOf(grippers),
         hold,
         low,
         high,
         Mth.clamp(SpecReader.num(limits, "forward", NONE.forward), 0.0, 170.0),
         Mth.clamp(SpecReader.num(limits, "back", NONE.back), 0.0, 170.0),
         Mth.clamp(SpecReader.num(limits, "side", NONE.side), 0.0, 170.0),
         media,
         List.copyOf(problems)
      );
   }

   @Nullable
   private static GripSpec.Gripper gripper(JsonObject json, List<String> problems) {
      Vec3 at = vec(json, "at");
      double reach = SpecReader.num(json, "reach", 0.0);
      List<String> limb = null;
      String bone = null;
      if (json.has("limb") && json.get("limb").isJsonArray()) {
         List<String> bones = new ArrayList<>();

         for (JsonElement element : json.getAsJsonArray("limb")) {
            bones.add(element.getAsString());
         }

         if (bones.size() != 3 && bones.size() != 4) {
            problems.add("grip.grippers entry " + json + ": a limb is three bones (root, mid, foot) or four (root, fold, mid, foot)");
            return null;
         }

         limb = List.copyOf(bones);
      } else if (json.has("bone") && json.get("bone").isJsonPrimitive()) {
         bone = json.get("bone").getAsString();
      }

      if ((limb != null || bone != null) && at != null && reach > 0.0) {
         return new GripSpec.Gripper(limb, bone, at, reach);
      }

      problems.add("grip.grippers entry " + json + " needs a limb or a bone, an at [x, y, z] and a reach above 0");
      return null;
   }

   @Nullable
   private static Vec3 vec(JsonObject json, String key) {
      if (json.has(key) && json.get(key).isJsonArray()) {
         JsonArray array = json.getAsJsonArray(key);
         return array.size() != 3 ? null : new Vec3(array.get(0).getAsDouble(), array.get(1).getAsDouble(), array.get(2).getAsDouble());
      } else {
         return null;
      }
   }

   public record Gripper(@Nullable List<String> limb, @Nullable String bone, Vec3 at, double reach) {
      public boolean closes() {
         return this.limb != null;
      }

      public String name() {
         return this.limb != null ? this.limb.get(this.limb.size() - 1) : this.bone;
      }

      @Nullable
      public LimbDef def() {
         if (this.limb == null) {
            return null;
         }

         boolean folds = this.limb.size() == 4;
         return new LimbDef(
            this.name(),
            this.limb.get(0),
            this.limb.get(folds ? 2 : 1),
            this.limb.get(this.limb.size() - 1),
            folds ? this.limb.get(1) : null,
            1.0F,
            -1.5F,
            false,
            false,
            0.0F,
            List.of(),
            Map.of(),
            Map.of()
         );
      }
   }

   public enum Medium {
      AIR,
      WATER,
      GROUND;
   }
}
