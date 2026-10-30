package net.magister.liberanimalium.fauna.move;

import net.magister.liberanimalium.fauna.api.MoveMode;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.Vec3;

public record NavNode(int x, int y, int z, MoveMode mode) {
   public static NavNode of(BlockPos pos, MoveMode mode) {
      return new NavNode(pos.getX(), pos.getY(), pos.getZ(), mode);
   }

   public BlockPos pos() {
      return new BlockPos(this.x, this.y, this.z);
   }

   public Vec3 center() {
      return new Vec3(this.x + 0.5, this.y, this.z + 0.5);
   }
}
