package net.magister.liberanimalium.util;

import net.magister.liberanimalium.compat.Entities;
import net.magister.liberanimalium.compat.Pathfinding;
import net.minecraft.core.BlockPos;
import net.minecraft.core.BlockPos.MutableBlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.pathfinder.PathComputationType;
import net.minecraft.world.phys.Vec3;

public class TerrainUtils {
   private static final int[][] NUDGE_DIRS = new int[][]{{1, 0}, {-1, 0}, {0, 1}, {0, -1}, {1, 1}, {1, -1}, {-1, 1}, {-1, -1}};

   public static Vec3 snapToGround(Level level, Vec3 target, Entity e) {
      double startY = Math.max(e.getY() + 1.0, target.y + 1.0);
      MutableBlockPos pos = new MutableBlockPos(target.x, startY, target.z);

      while (pos.getY() > level.getMinBuildHeight()) {
         boolean currentSolid = isSolid(level, pos);
         pos.move(0, -1, 0);
         boolean belowSolid = isSolid(level, pos);
         if (!currentSolid && belowSolid) {
            return new Vec3(target.x, pos.getY() + 1, target.z);
         }

         if (currentSolid && belowSolid) {
            while (pos.getY() < level.getMaxBuildHeight()) {
               pos.move(0, 1, 0);
               if (!isSolid(level, pos)) {
                  return new Vec3(target.x, pos.getY(), target.z);
               }
            }
            break;
         }
      }

      return target;
   }

   private static boolean isSolid(Level level, BlockPos pos) {
      return !level.getBlockState(pos).getCollisionShape(level, pos).isEmpty() || !level.getFluidState(pos).isEmpty();
   }

   public static Vec3 snapAndClear(Level level, Vec3 target, Entity e, double cornerMargin) {
      Vec3 snapped = snapToGround(level, target, e);
      return clearFootprint(snapped, e, cornerMargin);
   }

   public static Vec3 clearFootprint(Vec3 center, Entity e, double cornerMargin) {
      double halfW = e.getBbWidth() * 0.5;
      Vec3 spacious = searchClear(center, halfW + cornerMargin, halfW, e);
      if (spacious != null) {
         return spacious;
      }

      Vec3 fit = searchClear(center, halfW, halfW, e);
      return fit != null ? fit : center;
   }

   private static Vec3 searchClear(Vec3 center, double w, double maxNudge, Entity e) {
      if (footprintClear(center, w, e)) {
         return center;
      }

      for (double step = 0.5; step <= maxNudge; step += 0.5) {
         for (int[] d : NUDGE_DIRS) {
            Vec3 cand = center.add(d[0] * step, 0.0, d[1] * step);
            if (footprintClear(cand, w, e)) {
               return cand;
            }
         }
      }

      return null;
   }

   private static boolean footprintClear(Vec3 center, double w, Entity e) {
      int nodeY = Mth.floor(center.y);
      int minX = Mth.floor(center.x - w);
      int maxX = Mth.floor(center.x + w);
      int minZ = Mth.floor(center.z - w);
      int maxZ = Mth.floor(center.z + w);
      int bodyH = Math.max(1, Mth.ceil(e.getBbHeight()));
      int maxStep = Math.max(1, (int)Math.ceil(Entities.stepHeight(e)));

      for (int x = minX; x <= maxX; x++) {
         for (int z = minZ; z <= maxZ; z++) {
            if (!columnStandable(x, z, nodeY, bodyH, maxStep, e)) {
               return false;
            }
         }
      }

      return true;
   }

   private static boolean columnStandable(int x, int z, int nodeY, int bodyH, int maxStep, Entity e) {
      MutableBlockPos p = new MutableBlockPos();
      Level level = e.level();

      for (int y0 = nodeY + maxStep; y0 >= nodeY - maxStep; y0--) {
         p.set(x, y0 - 1, z);
         boolean floor = !level.getBlockState(p).getCollisionShape(level, p).isEmpty();
         if (floor) {
            boolean clear = true;

            for (int h = 0; h < bodyH; h++) {
               p.set(x, y0 + h, z);
               if (!Pathfinding.pathfindable(level.getBlockState(p), level, p, PathComputationType.LAND)) {
                  clear = false;
                  break;
               }
            }

            if (clear) {
               return true;
            }
         }
      }

      return false;
   }
}
