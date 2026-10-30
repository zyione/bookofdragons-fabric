package net.magister.liberanimalium.body.nav.pathfinding;

import java.util.ArrayList;
import java.util.List;
import net.magister.liberanimalium.fauna.api.MoveMode;
import net.magister.liberanimalium.fauna.move.NavNode;
import net.magister.liberanimalium.fauna.move.NavSolver;

final class Lattice {
   private Lattice() {
   }

   static List<NavSolver.Neighbor> neighbors(NavNode node, int step, MoveMode mode, Lattice.Edge edge, Lattice.Cost cost, Lattice.Low low) {
      List<NavSolver.Neighbor> out = new ArrayList<>(26);

      for (int dx = -1; dx <= 1; dx++) {
         for (int dy = -1; dy <= 1; dy++) {
            for (int dz = -1; dz <= 1; dz++) {
               if (dx != 0 || dy != 0 || dz != 0) {
                  int nx = node.x() + dx * step;
                  int ny = node.y() + dy * step;
                  int nz = node.z() + dz * step;
                  if (edge.clear(node.x(), node.y(), node.z(), nx, ny, nz)) {
                     out.add(new NavSolver.Neighbor(new NavNode(nx, ny, nz, mode), cost.at(nx, ny, nz), low.at(nx, ny, nz)));
                  }
               }
            }
         }
      }

      return out;
   }

   @FunctionalInterface
   interface Cost {
      float at(int var1, int var2, int var3);
   }

   @FunctionalInterface
   interface Edge {
      boolean clear(int var1, int var2, int var3, int var4, int var5, int var6);
   }

   @FunctionalInterface
   interface Low {
      boolean at(int var1, int var2, int var3);
   }
}
