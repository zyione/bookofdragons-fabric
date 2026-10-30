package net.magister.liberanimalium.compat.v20;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.pathfinder.PathComputationType;

public final class PathfindingImpl {
   private PathfindingImpl() {
   }

   public static boolean pathfindable(BlockState state, BlockGetter level, BlockPos pos, PathComputationType type) {
      return state.isPathfindable(level, pos, type);
   }
}
