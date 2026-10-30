package net.magister.liberanimalium.body;

import java.util.ArrayList;
import java.util.List;
import net.magister.liberanimalium.anim.rig.HeadTrackSpec;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

public final class RestChain {
   private RestChain() {
   }

   public static Vec3[] solve(BodyPlan plan, Vec3 body, float bodyYaw, float headYaw, float headPitch, HeadTrackSpec weights) {
      List<PartSpec> parts = plan.parts();
      Vec3[] out = new Vec3[parts.size()];

      for (int i = 0; i < parts.size(); i++) {
         out[i] = body.add(toWorld(parts.get(i).rest(), bodyYaw, 0.0F));
      }

      for (String prefix : plan.prefixes()) {
         List<Integer> neck = neckOrder(plan, prefix);
         if (neck.size() >= 2) {
            bend(out, parts, neck, body, bodyYaw, headYaw, headPitch, weights);
         }
      }

      return out;
   }

   static List<Integer> neckOrder(BodyPlan plan, String prefix) {
      List<Integer> order = new ArrayList<>();
      List<String> bones = new ArrayList<>(plan.chain("neck"));
      bones.addAll(plan.chain("head"));

      for (String bone : bones) {
         String full = prefix + bone;

         for (int i = 0; i < plan.parts().size(); i++) {
            if (plan.parts().get(i).bone().equals(full)) {
               order.add(i);
               break;
            }
         }
      }

      return order;
   }

   private static void bend(
      Vec3[] out, List<PartSpec> parts, List<Integer> neck, Vec3 body, float bodyYaw, float headYaw, float headPitch, HeadTrackSpec weights
   ) {
      int joints = neck.size();
      int slots = weights.neckSlots();
      float total = 0.0F;
      float[] cumulative = new float[joints];

      for (int j = 0; j < joints; j++) {
         boolean head = j == joints - 1;
         float w = head ? weights.weight(Math.min(joints - 1, slots)) : weights.weight(Math.min(j, slots));
         total += w;
         cumulative[j] = total;
      }

      if (!(total <= 0.0F)) {
         Vec3 at = out[neck.get(0)];

         for (int j = 0; j < joints - 1; j++) {
            float share = cumulative[j] / total;
            Vec3 segment = parts.get(neck.get(j + 1)).rest().subtract(parts.get(neck.get(j)).rest());
            at = at.add(toWorld(segment, bodyYaw + headYaw * share, headPitch * share));
            out[neck.get(j + 1)] = at;
         }
      }
   }

   static Vec3 toWorld(Vec3 local, float yaw, float pitch) {
      float p = pitch * (float) (Math.PI / 180.0);
      double up = local.y * Mth.cos(p) - local.z * Mth.sin(p);
      double forward = local.z * Mth.cos(p) + local.y * Mth.sin(p);
      Vec3 ahead = Vec3.directionFromRotation(0.0F, yaw);
      Vec3 right = Vec3.directionFromRotation(0.0F, yaw + 90.0F);
      return ahead.scale(forward).add(0.0, up, 0.0).add(right.scale(local.x));
   }
}
