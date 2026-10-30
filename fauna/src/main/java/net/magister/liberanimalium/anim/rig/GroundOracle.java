package net.magister.liberanimalium.anim.rig;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;

public interface GroundOracle {
   double PROBE_UNDER = 0.01;
   double EDGE_RESOLUTION = 0.0625;

   Vec3 along(Entity var1, Vec3 var2, Vec3 var3);

   default double column(Entity entity, double x, double z, double top, double down) {
      Vec3 hit = this.along(entity, new Vec3(x, top, z), new Vec3(x, top - down, z));
      return hit == null ? Double.NaN : hit.y;
   }

   boolean inside(Entity var1, double var2, double var4, double var6);

   double surfaceAbove(Entity var1, double var2, double var4, double var6, double var8);

   default double probe(Entity entity, double x, double z, double startY, double upMax, double down) {
      double under = startY - 0.01;
      if (this.inside(entity, x, under, z)) {
         return upMax > 0.0 ? this.surfaceAbove(entity, x, z, under, upMax) : Double.NaN;
      } else {
         return this.column(entity, x, z, startY, down);
      }
   }

   default boolean missedHigh(Entity entity, double x, double z, double startY) {
      return this.inside(entity, x, startY - 0.01, z);
   }

   default double edge(Entity entity, double x0, double z0, double x1, double z1, double surfaceY) {
      double y = surfaceY - 0.01;
      if (this.inside(entity, x0, y, z0) && !this.inside(entity, x1, y, z1)) {
         double length = Math.hypot(x1 - x0, z1 - z0);
         double lo = 0.0;
         double hi = 1.0;
         double resolution = length > 0.0625 ? 0.0625 / length : 1.0;

         while (hi - lo > resolution) {
            double mid = 0.5 * (lo + hi);
            if (this.inside(entity, x0 + mid * (x1 - x0), y, z0 + mid * (z1 - z0))) {
               lo = mid;
            } else {
               hi = mid;
            }
         }

         return 0.5 * (lo + hi);
      } else {
         return Double.NaN;
      }
   }

   default double overhead(Entity entity, double x, double z, double fromY, double up) {
      if (up <= 0.0) {
         return Double.NaN;
      }

      Vec3 hit = this.along(entity, new Vec3(x, fromY, z), new Vec3(x, fromY + up, z));
      return hit == null ? Double.NaN : hit.y;
   }

   default double chainSupport(Entity entity, double x, double z, double y, double down) {
      return this.column(entity, x, z, y, down);
   }
}
