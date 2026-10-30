package net.magister.liberanimalium.fauna.move;

import java.util.Map;
import net.magister.liberanimalium.fauna.api.MoveMode;
import net.minecraft.core.BlockPos;

public record SolveRequest(
   NavSolver.NeighborSource source, BlockPos goal, MoveMode goalMode, Map<MoveMode, Float> modeCost, float range, long generation, NavSolver.DangerField danger
) {
   public SolveRequest(NavSolver.NeighborSource source, BlockPos goal, MoveMode goalMode, Map<MoveMode, Float> modeCost, float range, long generation) {
      this(source, goal, goalMode, modeCost, range, generation, NavSolver.DangerField.NONE);
   }
}
