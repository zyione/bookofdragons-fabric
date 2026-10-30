package net.magister.liberanimalium.anim.rig;

import net.magister.liberanimalium.interop.SubLevels;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.ClipContext.Block;
import net.minecraft.world.level.ClipContext.Fluid;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.HitResult.Type;
import net.minecraft.world.phys.shapes.VoxelShape;

public final class LevelGroundOracle implements GroundOracle {
   public static final LevelGroundOracle INSTANCE = new LevelGroundOracle();

   private LevelGroundOracle() {
   }

   @Override
   public Vec3 along(Entity entity, Vec3 from, Vec3 to) {
      BlockHitResult hit = entity.level().clip(new ClipContext(from, to, Block.COLLIDER, Fluid.NONE, entity));
      return hit.getType() != Type.MISS && !hit.isInside() ? SubLevels.toWorld(entity.level(), hit.getLocation()) : null;
   }

   private static AABB solid(Entity entity, BlockPos pos) {
      VoxelShape shape = entity.level().getBlockState(pos).getCollisionShape(entity.level(), pos);
      return shape.isEmpty() ? null : shape.bounds().move(pos);
   }

   @Override
   public boolean inside(Entity entity, double x, double y, double z) {
      AABB box = solid(entity, BlockPos.containing(x, y, z));
      return box != null && box.contains(x, y, z) ? true : SubLevels.present() && insideSubLevel(entity, x, y, z);
   }

   private static boolean insideSubLevel(Entity entity, double x, double y, double z) {
      BlockHitResult hit = entity.level().clip(new ClipContext(new Vec3(x, y, z), new Vec3(x, y - 0.01, z), Block.COLLIDER, Fluid.NONE, entity));
      return hit.getType() != Type.MISS && hit.isInside();
   }

   public boolean held(Entity entity, double drop) {
      AABB below = entity.getBoundingBox().move(0.0, -drop, 0.0);

      for (VoxelShape shape : entity.level().getBlockCollisions(entity, below)) {
         if (!shape.isEmpty()) {
            return true;
         }
      }

      if (!SubLevels.present()) {
         return false;
      }

      AABB box = entity.getBoundingBox();
      Vec3 from = box.getCenter();
      return this.along(entity, from, new Vec3(from.x, box.minY - drop, from.z)) != null;
   }

   @Override
   public double surfaceAbove(Entity entity, double x, double z, double fromY, double upMax) {
      double y = fromY;
      double top = Double.NaN;

      for (double limit = fromY + upMax; y <= limit; y = top) {
         AABB box = solid(entity, BlockPos.containing(x, y + 0.001, z));
         if (box == null || !box.contains(x, y + 0.001, z)) {
            return Double.isNaN(top) ? this.subLevelSurfaceAbove(entity, x, z, fromY, upMax) : top;
         }

         top = box.maxY;
      }

      return Double.NaN;
   }

   private double subLevelSurfaceAbove(Entity entity, double x, double z, double fromY, double upMax) {
      if (SubLevels.present() && insideSubLevel(entity, x, fromY + 0.001, z)) {
         Vec3 hit = this.along(entity, new Vec3(x, fromY + upMax, z), new Vec3(x, fromY, z));
         return hit == null ? Double.NaN : hit.y;
      } else {
         return Double.NaN;
      }
   }
}
