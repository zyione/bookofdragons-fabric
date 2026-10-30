package net.magister.liberanimalium.anim.rig;

import java.util.List;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

public final class TargetLift {
   private TargetLift() {
   }

   public static Vec3 axis(Vec3[] world) {
      Vec3 forward = world[world.length - 1].subtract(world[0]);
      Vec3 flat = new Vec3(forward.x, 0.0, forward.z);
      return flat.lengthSqr() < 1.0E-8 ? null : flat.normalize().cross(new Vec3(0.0, 1.0, 0.0));
   }

   public static Vec3[] arced(Vec3[] world, Vec3 axis, double rad) {
      Vec3[] out = (Vec3[])world.clone();
      int segments = world.length - 1;
      if (segments < 1) {
         return out;
      }

      double reached = 0.0;

      for (int k = 1; k <= segments; k++) {
         double turn = rad * ease((double)k / segments) - reached;
         reached += turn;
         double c = Math.cos(turn);
         double s = Math.sin(turn);
         Vec3 pivot = out[k - 1];

         for (int i = k; i < out.length; i++) {
            Vec3 v = out[i].subtract(pivot);
            out[i] = pivot.add(v.scale(c).add(axis.cross(v).scale(s)).add(axis.scale(axis.dot(v) * (1.0 - c))));
         }
      }

      return out;
   }

   public static double ease(double t) {
      return 1.0 - Math.pow(1.0 - t, 5.0);
   }

   public static boolean clear(Vec3[] world, double[] radii, List<AABB> boxes, double clear, Vec3 lead) {
      for (int i = 1; i < world.length; i++) {
         double r = radii[Math.min(i, radii.length - 1)] + clear;
         if (touches(world[i], r, boxes) || touches(world[i].add(lead), r, boxes)) {
            return false;
         }
      }

      return true;
   }

   private static boolean touches(Vec3 c, double r, List<AABB> boxes) {
      for (AABB box : boxes) {
         double dx = Math.max(Math.max(box.minX - c.x, 0.0), c.x - box.maxX);
         double dy = Math.max(Math.max(box.minY - c.y, 0.0), c.y - box.maxY);
         double dz = Math.max(Math.max(box.minZ - c.z, 0.0), c.z - box.maxZ);
         if (dx * dx + dy * dy + dz * dz < r * r) {
            return true;
         }
      }

      return false;
   }

   public static double lift(Vec3[] world, double[] radii, List<AABB> boxes, Vec3 lead, double maxRad, double clear) {
      if (world.length >= 2 && !boxes.isEmpty() && !clear(world, radii, boxes, clear, lead)) {
         Vec3 axis = axis(world);
         if (axis == null) {
            return 0.0;
         }

         if (!clear(arced(world, axis, maxRad), radii, boxes, clear, lead)) {
            return maxRad;
         }

         double lo = 0.0;
         double hi = maxRad;

         for (int i = 0; i < 6; i++) {
            double mid = 0.5 * (lo + hi);
            if (clear(arced(world, axis, mid), radii, boxes, clear, lead)) {
               hi = mid;
            } else {
               lo = mid;
            }
         }

         return hi;
      } else {
         return 0.0;
      }
   }
}
