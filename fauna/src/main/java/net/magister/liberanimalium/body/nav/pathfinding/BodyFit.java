package net.magister.liberanimalium.body.nav.pathfinding;

import it.unimi.dsi.fastutil.longs.Long2ByteOpenHashMap;
import org.jetbrains.annotations.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.BlockPos.MutableBlockPos;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.Mth;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.PathNavigationRegion;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.VoxelShape;

public final class BodyFit {
   private final PathNavigationRegion region;
   private final int radius;
   private final int height;
   private static final byte UNKNOWN = 0;
   private static final byte NO = 1;
   private static final byte YES = 2;
   private final Long2ByteOpenHashMap clearCache = new Long2ByteOpenHashMap();
   private final Long2ByteOpenHashMap swimCache = new Long2ByteOpenHashMap();
   private final MutableBlockPos scan = new MutableBlockPos();
   public static final float CELL_OCCUPANCY = 0.25F;
   public static final double DOOR_PANEL = 0.1875;
   public static final double DOOR_CLEAR_WIDTH = 0.625;

   public BodyFit(PathNavigationRegion region, float bodyWidth, float bodyHeight) {
      this.region = region;
      this.radius = radiusCells(bodyWidth);
      this.height = Math.max(1, Mth.ceil(bodyHeight));
      this.clearCache.defaultReturnValue((byte)0);
      this.swimCache.defaultReturnValue((byte)0);
   }

   public static int radiusCells(float bodyWidth) {
      return Math.max(0, Mth.ceil(bodyWidth * 0.5F - 0.5F - 0.25F));
   }

   public static boolean passesDoorway(double width) {
      return width <= 0.625;
   }

   @Nullable
   public static Vec3 doorPassage(BlockState openDoor, BlockGetter level, BlockPos cell) {
      VoxelShape shape = openDoor.getCollisionShape(level, cell);
      if (shape.isEmpty()) {
         return null;
      }

      AABB panel = shape.bounds();
      double x = 0.5;
      double z = 0.5;
      if (panel.getXsize() < 0.5 && panel.getZsize() >= 0.5) {
         x = panel.minX < 0.5 ? (panel.maxX + 1.0) * 0.5 : panel.minX * 0.5;
      } else {
         if (!(panel.getZsize() < 0.5) || !(panel.getXsize() >= 0.5)) {
            return null;
         }

         z = panel.minZ < 0.5 ? (panel.maxZ + 1.0) * 0.5 : panel.minZ * 0.5;
      }

      return new Vec3(cell.getX() + x, cell.getY(), cell.getZ() + z);
   }

   public int radius() {
      return this.radius;
   }

   public int height() {
      return this.height;
   }

   public boolean fitsDry(int x, int y, int z) {
      long key = BlockPos.asLong(x, y, z);
      byte cached = this.clearCache.get(key);
      if (cached != 0) {
         return cached == 2;
      }

      boolean ok = true;

      label48:
      for (int dx = -this.radius; dx <= this.radius; dx++) {
         for (int dz = -this.radius; dz <= this.radius; dz++) {
            for (int dy = 0; dy < this.height; dy++) {
               this.scan.set(x + dx, y + dy, z + dz);
               if (!this.region.getBlockState(this.scan).getCollisionShape(this.region, this.scan).isEmpty() || !this.region.getFluidState(this.scan).isEmpty()) {
                  ok = false;
                  break label48;
               }
            }
         }
      }

      this.clearCache.put(key, (byte)(ok ? 2 : 1));
      return ok;
   }

   public boolean fitsSwimming(int x, int y, int z) {
      long key = BlockPos.asLong(x, y, z);
      byte cached = this.swimCache.get(key);
      if (cached != 0) {
         return cached == 2;
      }

      this.scan.set(x, y, z);
      boolean ok = this.region.getFluidState(this.scan).is(FluidTags.WATER);

      label48:
      for (int dx = -this.radius; ok && dx <= this.radius; dx++) {
         for (int dz = -this.radius; dz <= this.radius; dz++) {
            for (int dy = 0; dy < this.height; dy++) {
               this.scan.set(x + dx, y + dy, z + dz);
               if (!this.region.getBlockState(this.scan).getCollisionShape(this.region, this.scan).isEmpty()) {
                  ok = false;
                  break label48;
               }
            }
         }
      }

      this.swimCache.put(key, (byte)(ok ? 2 : 1));
      return ok;
   }

   public boolean fitsWet(int x, int y, int z) {
      this.scan.set(x, y, z);
      if (this.region.getFluidState(this.scan).isEmpty()) {
         return false;
      }

      for (int dx = -this.radius; dx <= this.radius; dx++) {
         for (int dz = -this.radius; dz <= this.radius; dz++) {
            for (int dy = 0; dy < this.height; dy++) {
               this.scan.set(x + dx, y + dy, z + dz);
               if (!this.region.getBlockState(this.scan).getCollisionShape(this.region, this.scan).isEmpty()) {
                  return false;
               }
            }
         }
      }

      return true;
   }

   public boolean columnClear(int x, int yFrom, int yTo, int z) {
      return columnClear(this.region, this.scan, this.radius, x, yFrom, yTo, z);
   }

   public static boolean columnClear(BlockGetter level, MutableBlockPos scan, int radius, int x, int yFrom, int yTo, int z) {
      for (int y = yFrom; y <= yTo; y++) {
         for (int dx = -radius; dx <= radius; dx++) {
            for (int dz = -radius; dz <= radius; dz++) {
               scan.set(x + dx, y, z + dz);
               if (!level.getBlockState(scan).getCollisionShape(level, scan).isEmpty()) {
                  return false;
               }
            }
         }
      }

      return true;
   }

   public boolean clearBelow(int x, int y, int z, int depth) {
      for (int dy = 1; dy <= depth; dy++) {
         for (int dx = -this.radius; dx <= this.radius; dx++) {
            for (int dz = -this.radius; dz <= this.radius; dz++) {
               this.scan.set(x + dx, y - dy, z + dz);
               if (!this.region.getBlockState(this.scan).getCollisionShape(this.region, this.scan).isEmpty() || !this.region.getFluidState(this.scan).isEmpty()) {
                  return false;
               }
            }
         }
      }

      return true;
   }

   public boolean floorAt(int x, int y, int z) {
      this.scan.set(x, y - 1, z);
      return !this.region.getBlockState(this.scan).getCollisionShape(this.region, this.scan).isEmpty();
   }

   public static float impossible() {
      return Float.POSITIVE_INFINITY;
   }

   public static boolean isImpossible(float cost) {
      return !Float.isFinite(cost);
   }
}
