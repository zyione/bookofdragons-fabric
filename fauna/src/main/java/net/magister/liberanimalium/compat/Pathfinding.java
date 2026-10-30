package net.magister.liberanimalium.compat;

import net.magister.liberanimalium.compat.v20.PathfindingImpl;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.pathfinder.PathComputationType;

public final class Pathfinding {
   private Pathfinding() {
   }

   public static boolean pathfindable(BlockState state, BlockGetter level, BlockPos pos, PathComputationType type) {
      return PathfindingImpl.pathfindable(state, level, pos, type);
   }
}
