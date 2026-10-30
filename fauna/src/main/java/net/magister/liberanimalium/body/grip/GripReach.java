package net.magister.liberanimalium.body.grip;

import java.util.List;
import java.util.function.Predicate;
import java.util.function.ToDoubleFunction;
import org.jetbrains.annotations.Nullable;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaternionfc;

public final class GripReach {
   private GripReach() {
   }

   public static Vec3[] hips(GripSpec spec, Vec3 position, double halfHeight, Quaternionfc attitude, double size) {
      Vec3[] hips = new Vec3[spec.grippers().size()];

      for (int i = 0; i < hips.length; i++) {
         hips[i] = GripGeometry.worldPoint(position, halfHeight, attitude, spec.grippers().get(i).at().scale(size));
      }

      return hips;
   }

   public static double distance(AABB box, Vec3 point) {
      double dx = Math.max(Math.max(box.minX - point.x, 0.0), point.x - box.maxX);
      double dy = Math.max(Math.max(box.minY - point.y, 0.0), point.y - box.maxY);
      double dz = Math.max(Math.max(box.minZ - point.z, 0.0), point.z - box.maxZ);
      return Math.sqrt(dx * dx + dy * dy + dz * dz);
   }

   public static double reach(GripSpec spec, AABB box, Vec3[] from, Vec3[] to, double size) {
      return reach(spec, hip -> distance(box, hip), from, to, size);
   }

   public static double reach(GripSpec spec, ToDoubleFunction<Vec3> distance, Vec3[] from, Vec3[] to, double size) {
      double shortest = spec.shortestReach() * size;
      double travel = 0.0;

      for (int i = 0; i < to.length; i++) {
         travel = Math.max(travel, from[i].distanceTo(to[i]));
      }

      int samples = shortest <= 0.0 ? 1 : Math.min(8, 1 + (int)Math.ceil(travel / (shortest * 0.5)));
      double best = Double.MAX_VALUE;

      for (int s = 0; s < samples; s++) {
         double t = samples == 1 ? 1.0 : (double)s / (samples - 1);
         double sum = 0.0;
         boolean all = true;

         for (int i = 0; i < to.length; i++) {
            Vec3 hip = from[i].lerp(to[i], t);
            double d = distance.applyAsDouble(hip);
            if (d > spec.grippers().get(i).reach() * size) {
               all = false;
               break;
            }

            sum += d;
         }

         if (all) {
            best = Math.min(best, sum);
         }
      }

      return best;
   }

   public static AABB sweep(GripSpec spec, Vec3[] from, Vec3[] to, double size) {
      AABB box = new AABB(to[0], to[0]);

      for (int i = 0; i < to.length; i++) {
         box = box.minmax(new AABB(from[i], to[i]));
      }

      return box.inflate(spec.longestReach() * size);
   }

   @Nullable
   public static Entity choose(GripSpec spec, List<Entity> candidates, Vec3[] from, Vec3[] to, double size, Predicate<Entity> fits) {
      Entity best = null;
      double bestScore = Double.MAX_VALUE;

      for (Entity candidate : candidates) {
         double score = reach(spec, candidate.getBoundingBox(), from, to, size);
         if (score < bestScore && fits.test(candidate)) {
            best = candidate;
            bestScore = score;
         }
      }

      return best;
   }
}
