package net.magister.liberanimalium.body.locomotion;

import net.minecraft.core.BlockPos.MutableBlockPos;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.material.FluidState;

public final class WaterDepth {
   public static final int SCAN_ABOVE = 2;

   private WaterDepth() {
   }

   public static double depth(Entity body) {
      Level level = body.level();
      double bottom = body.getY();
      MutableBlockPos at = new MutableBlockPos(body.getX(), bottom, body.getZ());
      if (!level.getFluidState(at).is(FluidTags.WATER)) {
         return 0.0;
      }

      int top = at.getY();

      for (int cap = at.getY() + (int)Math.ceil(body.getBbHeight()) + 2; top < cap; top++) {
         at.setY(top + 1);
         if (!level.getFluidState(at).is(FluidTags.WATER)) {
            break;
         }
      }

      at.setY(top);
      FluidState surface = level.getFluidState(at);
      return Math.max(0.0, top + surface.getHeight(level, at) - bottom);
   }

   public static int below(Entity body, int reach) {
      Level level = body.level();
      MutableBlockPos at = new MutableBlockPos(body.getX(), body.getY(), body.getZ());

      int n;
      for (n = 0; n < reach; n++) {
         at.setY(at.getY() - 1);
         if (!level.getFluidState(at).is(FluidTags.WATER)) {
            break;
         }
      }

      return n;
   }

   public static double share(Entity body) {
      double height = Math.max(0.1, body.getBbHeight());
      return Math.min(1.0, depth(body) / height);
   }

   public static double surface(Level level, double x, double y, double z, int reach) {
      MutableBlockPos at = new MutableBlockPos(x, y, z);
      int start = at.getY();
      if (!level.getFluidState(at).is(FluidTags.WATER)) {
         for (int down = 1; down <= reach; down++) {
            at.setY(start - down);
            FluidState fluid = level.getFluidState(at);
            if (fluid.is(FluidTags.WATER)) {
               return at.getY() + fluid.getHeight(level, at);
            }

            if (!level.getBlockState(at).isAir()) {
               return Double.NaN;
            }
         }

         return Double.NaN;
      } else {
         int top;
         for (top = start; top < start + reach; top++) {
            at.setY(top + 1);
            if (!level.getFluidState(at).is(FluidTags.WATER)) {
               break;
            }
         }

         at.setY(top);
         return top >= start + reach && level.getFluidState(at.above()).is(FluidTags.WATER) ? Double.NaN : top + level.getFluidState(at).getHeight(level, at);
      }
   }
}
