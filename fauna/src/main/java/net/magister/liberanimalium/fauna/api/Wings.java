package net.magister.liberanimalium.fauna.api;

import org.jetbrains.annotations.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.Vec3;

public interface Wings {
   static Wings of(AnimalHandle handle) {
      return handle.endowed(Wings.class).orElseGet(() -> new Wings.Grounded(handle));
   }

   default boolean canTakeOff() {
      return true;
   }

   default boolean takingOff() {
      return false;
   }

   default double airGapBelow(Vec3 point) {
      return 0.0;
   }

   default boolean overWater(Vec3 point) {
      return false;
   }

   default double airUnder(Vec3 point, double upTo) {
      return upTo;
   }

   default double landHeightUnder(Vec3 point) {
      return point.y;
   }

   @Nullable
   BlockPos landingSiteNear(BlockPos var1, int var2);

   default float stallFraction() {
      return 0.0F;
   }

   record Grounded(AnimalHandle handle) implements Wings {
      @Override
      public boolean canTakeOff() {
         return false;
      }

      @Nullable
      @Override
      public BlockPos landingSiteNear(BlockPos approx, int radius) {
         return this.handle.findStandableNear(approx);
      }
   }
}
