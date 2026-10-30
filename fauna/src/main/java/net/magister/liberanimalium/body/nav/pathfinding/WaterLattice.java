package net.magister.liberanimalium.body.nav.pathfinding;

import java.util.List;
import net.magister.liberanimalium.fauna.api.MoveMode;
import net.magister.liberanimalium.fauna.move.MoveTuning;
import net.magister.liberanimalium.fauna.move.NavNode;
import net.magister.liberanimalium.fauna.move.NavSolver;

public final class WaterLattice {
   private final BodyFit fit;
   private final int step;

   public WaterLattice(BodyFit fit, float bodyWidth) {
      this.fit = fit;
      this.step = MoveTuning.airLatticeStep(bodyWidth);
   }

   public int step() {
      return this.step;
   }

   public List<NavSolver.Neighbor> neighbors(NavNode node) {
      return Lattice.neighbors(node, this.step, MoveMode.WATER, this::segmentClear, (x, y, z) -> 0.0F, (x, y, z) -> false);
   }

   public boolean segmentClear(NavNode from, NavNode to) {
      return this.segmentClear(from.x(), from.y(), from.z(), to.x(), to.y(), to.z());
   }

   public boolean segmentClear(int x0, int y0, int z0, int x1, int y1, int z1) {
      if (!this.fit.fitsSwimming(x0, y0, z0)) {
         return false;
      }

      int samples = Math.max(Math.abs(x1 - x0), Math.max(Math.abs(y1 - y0), Math.abs(z1 - z0)));

      for (int i = 1; i <= samples; i++) {
         double t = (double)i / samples;
         int sx = (int)Math.round(x0 + (x1 - x0) * t);
         int sy = (int)Math.round(y0 + (y1 - y0) * t);
         int sz = (int)Math.round(z0 + (z1 - z0) * t);
         if (!this.fit.fitsSwimming(sx, sy, sz)) {
            return false;
         }
      }

      return true;
   }

   public boolean swims(int x, int y, int z) {
      return this.fit.fitsSwimming(x, y, z);
   }
}
