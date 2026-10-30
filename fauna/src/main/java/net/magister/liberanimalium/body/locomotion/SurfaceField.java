package net.magister.liberanimalium.body.locomotion;

import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

public final class SurfaceField {
   public static final SurfaceField.Reading NONE = new SurfaceField.Reading(Vec3.ZERO, Double.POSITIVE_INFINITY);
   private static final double NO_DIRECTION = 1.0E-6;

   private SurfaceField() {
   }

   public static SurfaceField.Reading read(AABB box, Iterable<AABB> solids, double reach) {
      double cx = (box.minX + box.maxX) * 0.5;
      double cy = (box.minY + box.maxY) * 0.5;
      double cz = (box.minZ + box.maxZ) * 0.5;
      double nx = 0.0;
      double ny = 0.0;
      double nz = 0.0;
      double nearest = Double.POSITIVE_INFINITY;

      for (AABB solid : solids) {
         double gap = gap(box, solid);
         if (!(gap >= reach)) {
            double dx = cx - Math.max(solid.minX, Math.min(solid.maxX, cx));
            double dy = cy - Math.max(solid.minY, Math.min(solid.maxY, cy));
            double dz = cz - Math.max(solid.minZ, Math.min(solid.maxZ, cz));
            double length = Math.sqrt(dx * dx + dy * dy + dz * dz);
            if (!(length < 1.0E-6)) {
               double weight = 1.0 - gap / reach;
               weight *= weight;
               nx += dx / length * weight;
               ny += dy / length * weight;
               nz += dz / length * weight;
               nearest = Math.min(nearest, gap);
            }
         }
      }

      if (nearest == Double.POSITIVE_INFINITY) {
         return NONE;
      }

      double length = Math.sqrt(nx * nx + ny * ny + nz * nz);
      return length < 1.0E-6
         ? new SurfaceField.Reading(Vec3.ZERO, nearest)
         : new SurfaceField.Reading(new Vec3(nx / length, ny / length, nz / length), nearest);
   }

   static double gap(AABB a, AABB b) {
      double dx = Math.max(0.0, Math.max(b.minX - a.maxX, a.minX - b.maxX));
      double dy = Math.max(0.0, Math.max(b.minY - a.maxY, a.minY - b.maxY));
      double dz = Math.max(0.0, Math.max(b.minZ - a.maxZ, a.minZ - b.maxZ));
      return Math.sqrt(dx * dx + dy * dy + dz * dz);
   }

   public record Reading(Vec3 normal, double gap) {
      public boolean held() {
         return this.gap < Double.POSITIVE_INFINITY;
      }

      public boolean directed() {
         return this.normal.lengthSqr() > 0.0;
      }
   }
}
