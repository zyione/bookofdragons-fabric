package net.magister.liberanimalium.body.nav.pathfinding;

import org.jetbrains.annotations.Nullable;
import net.minecraft.core.BlockPos.MutableBlockPos;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.PathNavigationRegion;
import net.minecraft.world.level.pathfinder.BlockPathTypes;
import net.minecraft.world.level.pathfinder.Node;
import net.minecraft.world.level.pathfinder.NodeEvaluator;
import net.minecraft.world.level.pathfinder.Target;
import net.minecraft.world.phys.Vec3;

public class SwimNodeEvaluator extends NodeEvaluator {
   private static final int SURFACE_SEARCH_RANGE = 2;
   private static final int[][] HORIZONTAL = new int[][]{{1, 0}, {-1, 0}, {0, 1}, {0, -1}};
   private final MutableBlockPos scan = new MutableBlockPos();
   @Nullable
   private Vec3 startAt;

   public void prepare(PathNavigationRegion level, Mob mob) {
      super.prepare(level, mob);
      this.entityWidth = Mth.ceil(mob.getBbWidth());
      this.entityHeight = Mth.ceil(mob.getBbHeight());
      this.entityDepth = Mth.ceil(mob.getBbWidth());
   }

   public void startAt(@Nullable Vec3 at) {
      this.startAt = at;
   }

   private Vec3 from() {
      return this.startAt != null ? this.startAt : this.mob.position();
   }

   public Node getStart() {
      Vec3 at = this.from();
      int x = Mth.floor(at.x);
      int z = Mth.floor(at.z);
      int y = this.findSurfaceY(x, Mth.floor(at.y), z);
      if (y == Integer.MIN_VALUE) {
         y = Mth.floor(at.y);
      }

      return this.getNode(x, y, z);
   }

   public Target getGoal(double x, double y, double z) {
      int gx = Mth.floor(x);
      int gz = Mth.floor(z);
      int gy = this.findSurfaceY(gx, Mth.floor(y), gz);
      if (gy == Integer.MIN_VALUE) {
         gy = Mth.floor(y);
      }

      return new Target(this.getNode(gx, gy, gz));
   }

   public int getNeighbors(Node[] out, Node node) {
      int count = 0;

      for (int[] d : HORIZONTAL) {
         if (count >= out.length) {
            break;
         }

         int nx = node.x + d[0];
         int nz = node.z + d[1];
         int ny = this.findSurfaceY(nx, node.y, nz);
         if (ny != Integer.MIN_VALUE && Math.abs(ny - node.y) <= 2 && this.hasClearance(nx, ny, nz)) {
            out[count++] = this.getNode(nx, ny, nz);
         }
      }

      return count;
   }

   private int findSurfaceY(int x, int aroundY, int z) {
      for (int dy = 0; dy <= 2; dy++) {
         int[] signs = dy == 0 ? new int[]{0} : new int[]{1, -1};

         for (int sign : signs) {
            int y = aroundY + sign * dy;
            this.scan.set(x, y, z);
            if (this.level.getFluidState(this.scan).is(FluidTags.WATER)) {
               this.scan.set(x, y + 1, z);
               if (!this.level.getFluidState(this.scan).is(FluidTags.WATER)) {
                  return y;
               }
            }
         }
      }

      return Integer.MIN_VALUE;
   }

   private boolean hasClearance(int x, int y, int z) {
      int radius = BodyFit.radiusCells(this.mob.getBbWidth());

      for (int dx = -radius; dx <= radius; dx++) {
         for (int dz = -radius; dz <= radius; dz++) {
            for (int i = 1; i < this.entityHeight; i++) {
               this.scan.set(x + dx, y + i, z + dz);
               if (!this.level.getBlockState(this.scan).getCollisionShape(this.level, this.scan).isEmpty()) {
                  return false;
               }
            }
         }
      }

      return true;
   }

   public BlockPathTypes getBlockPathType(BlockGetter level, int x, int y, int z, Mob mob) {
      return this.getBlockPathType(level, x, y, z);
   }

   public BlockPathTypes getBlockPathType(BlockGetter level, int x, int y, int z) {
      this.scan.set(x, y, z);
      if (level.getFluidState(this.scan).is(FluidTags.WATER)) {
         return BlockPathTypes.WATER;
      } else {
         return level.getBlockState(this.scan).getCollisionShape(level, this.scan).isEmpty() ? BlockPathTypes.OPEN : BlockPathTypes.BLOCKED;
      }
   }

   public Node nodeAt(int x, int y, int z) {
      return this.getNode(x, y, z);
   }
}
