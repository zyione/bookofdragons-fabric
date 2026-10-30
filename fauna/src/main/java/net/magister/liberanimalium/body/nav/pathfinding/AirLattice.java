package net.magister.liberanimalium.body.nav.pathfinding;

import it.unimi.dsi.fastutil.longs.Long2ByteOpenHashMap;
import java.util.List;
import net.magister.liberanimalium.fauna.api.MoveMode;
import net.magister.liberanimalium.fauna.move.MoveTuning;
import net.magister.liberanimalium.fauna.move.NavNode;
import net.magister.liberanimalium.fauna.move.NavSolver;
import net.minecraft.core.BlockPos;

public final class AirLattice {
   private final BodyFit fit;
   private final int step;
   private final int band;
   private static final byte UNKNOWN = 0;
   private static final byte HIGH = 1;
   private static final byte LOW = 2;
   private final Long2ByteOpenHashMap lowCache = new Long2ByteOpenHashMap();

   public AirLattice(BodyFit fit, float bodyWidth) {
      this.fit = fit;
      this.step = stepFor(bodyWidth);
      this.band = MoveTuning.skyClearance(bodyWidth);
      this.lowCache.defaultReturnValue((byte)0);
   }

   public static int stepFor(float bodyWidth) {
      return MoveTuning.airLatticeStep(bodyWidth);
   }

   public int step() {
      return this.step;
   }

   public int band() {
      return this.band;
   }

   public List<NavSolver.Neighbor> neighbors(NavNode node) {
      return Lattice.neighbors(node, this.step, MoveMode.AIR, this::segmentClear, this::skyMalus, this::low);
   }

   private float skyMalus(int x, int y, int z) {
      return this.low(x, y, z) ? 0.25F * this.step : 0.0F;
   }

   public boolean low(int x, int y, int z) {
      long key = BlockPos.asLong(x, y, z);
      byte cached = this.lowCache.get(key);
      if (cached != 0) {
         return cached == 2;
      }

      boolean low = !this.fit.clearBelow(x, y, z, this.band);
      this.lowCache.put(key, (byte)(low ? 2 : 1));
      return low;
   }

   public boolean besideDrop(int x, int y, int z) {
      for (int dx = -this.step; dx <= this.step; dx += this.step) {
         for (int dz = -this.step; dz <= this.step; dz += this.step) {
            if ((dx != 0 || dz != 0) && this.clear(x + dx, y, z + dz) && !this.low(x + dx, y, z + dz)) {
               return true;
            }
         }
      }

      return false;
   }

   public boolean segmentClear(NavNode from, NavNode to) {
      return this.segmentClear(from.x(), from.y(), from.z(), to.x(), to.y(), to.z());
   }

   public boolean segmentClear(int x0, int y0, int z0, int x1, int y1, int z1) {
      int samples = Math.max(Math.abs(x1 - x0), Math.max(Math.abs(y1 - y0), Math.abs(z1 - z0)));

      for (int i = 1; i <= samples; i++) {
         double t = (double)i / samples;
         int sx = (int)Math.round(x0 + (x1 - x0) * t);
         int sy = (int)Math.round(y0 + (y1 - y0) * t);
         int sz = (int)Math.round(z0 + (z1 - z0) * t);
         if (!this.clear(sx, sy, sz)) {
            return false;
         }
      }

      return true;
   }

   public boolean clear(int x, int y, int z) {
      return this.fit.fitsDry(x, y, z);
   }
}
