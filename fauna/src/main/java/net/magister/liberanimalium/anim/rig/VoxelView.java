package net.magister.liberanimalium.anim.rig;

import java.util.Arrays;
import java.util.List;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.AABB;

public final class VoxelView implements Penetration.Solids {
   private final int x0;
   private final int y0;
   private final int z0;
   private final int w;
   private final int h;
   private final int d;
   private final AABB[] cells;
   private int[] hits = new int[64];
   private int count;
   private AABB seen;
   private int seenX0;
   private int seenX1;
   private int seenY0;
   private int seenY1;
   private int seenZ0;
   private int seenZ1 = -1;

   private VoxelView(int x0, int y0, int z0, int w, int h, int d, AABB[] cells) {
      this.x0 = x0;
      this.y0 = y0;
      this.z0 = z0;
      this.w = w;
      this.h = h;
      this.d = d;
      this.cells = cells;
   }

   public static VoxelView of(List<AABB> held) {
      if (held.isEmpty()) {
         return new VoxelView(0, 0, 0, 0, 0, 0, new AABB[0]);
      }

      int ax = Integer.MAX_VALUE;
      int ay = Integer.MAX_VALUE;
      int az = Integer.MAX_VALUE;
      int bx = Integer.MIN_VALUE;
      int by = Integer.MIN_VALUE;
      int bz = Integer.MIN_VALUE;

      for (AABB box : held) {
         int x = (int)box.minX;
         int y = (int)box.minY;
         int z = (int)box.minZ;
         if (box.minX != x || box.minY != y || box.minZ != z || box.maxX != x + 1.0 || box.maxY != y + 1.0 || box.maxZ != z + 1.0) {
            return null;
         }

         ax = Math.min(ax, x);
         ay = Math.min(ay, y);
         az = Math.min(az, z);
         bx = Math.max(bx, x);
         by = Math.max(by, y);
         bz = Math.max(bz, z);
      }

      int w = bx - ax + 1;
      int h = by - ay + 1;
      int d = bz - az + 1;
      long size = (long)w * h * d;
      if (size > 262144L) {
         return null;
      }

      AABB[] cells = new AABB[(int)size];

      for (AABB box : held) {
         int cell = (((int)box.minZ - az) * h + ((int)box.minY - ay)) * w + ((int)box.minX - ax);
         if (cells[cell] != null) {
            return null;
         }

         cells[cell] = box;
      }

      return new VoxelView(ax, ay, az, w, h, d, cells);
   }

   public VoxelView over(AABB seen) {
      this.seen = seen;
      this.count = 0;
      this.seenX0 = 0;
      this.seenX1 = -1;
      if (this.w == 0) {
         return this;
      }

      int ax = Math.max(0, Mth.floor(seen.minX) - this.x0);
      int bx = Math.min(this.w - 1, Mth.ceil(seen.maxX) - 1 - this.x0);
      int ay = Math.max(0, Mth.floor(seen.minY) - this.y0);
      int by = Math.min(this.h - 1, Mth.ceil(seen.maxY) - 1 - this.y0);
      int az = Math.max(0, Mth.floor(seen.minZ) - this.z0);
      int bz = Math.min(this.d - 1, Mth.ceil(seen.maxZ) - 1 - this.z0);
      this.seenX0 = ax;
      this.seenX1 = bx;
      this.seenY0 = ay;
      this.seenY1 = by;
      this.seenZ0 = az;
      this.seenZ1 = bz;

      for (int z = az; z <= bz; z++) {
         for (int y = ay; y <= by; y++) {
            int row = (z * this.h + y) * this.w;

            for (int x = ax; x <= bx; x++) {
               AABB box = this.cells[row + x];
               if (box != null) {
                  if (this.count == this.hits.length) {
                     this.hits = Arrays.copyOf(this.hits, this.count * 2);
                  }

                  this.hits[this.count++] = row + x;
               }
            }
         }
      }

      return this;
   }

   @Override
   public int count() {
      return this.count;
   }

   @Override
   public AABB box(int k) {
      return this.cells[this.hits[k]];
   }

   @Override
   public boolean holds(AABB box, double qx, double qy, double qz) {
      int vx = Mth.floor(qx);
      int vy = Mth.floor(qy);
      int vz = Mth.floor(qz);
      if (qx != vx && qy != vy && qz != vz) {
         int cx = vx - this.x0;
         int cy = vy - this.y0;
         int cz = vz - this.z0;
         if (cx < 0 || cy < 0 || cz < 0 || cx >= this.w || cy >= this.h || cz >= this.d) {
            return false;
         } else if (cx >= this.seenX0 && cx <= this.seenX1 && cy >= this.seenY0 && cy <= this.seenY1 && cz >= this.seenZ0 && cz <= this.seenZ1) {
            AABB other = this.cells[(cz * this.h + cy) * this.w + cx];
            return other != null && other != box;
         } else {
            return false;
         }
      } else {
         return false;
      }
   }
}
