package net.magister.liberanimalium.anim.rig;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Function;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.AABB;

public final class GatherCache {
   public static final double PAD = 0.25;
   private final AABB[] region;
   private final List<AABB>[] boxes;
   private final List<AABB>[] view;
   private final int[] tick;
   private final GatherCache.Index[] index;
   private int[] found = new int[64];
   private final int[] served;
   private final long[] mark;
   private final boolean[] fleeting;
   private final boolean[] contextual;
   private final VoxelView[] voxel;
   private final boolean[] voxelTried;
   private int gathers;
   private int hits;
   private static final int INDEX_AFTER = 3;
   public static final double SLACK = 0.5;

   public GatherCache(int particles) {
      this.region = new AABB[particles];
      this.boxes = new List[particles];
      this.view = new List[particles];
      this.index = new GatherCache.Index[particles];

      for (int i = 0; i < particles; i++) {
         this.view[i] = new ArrayList<>();
         this.index[i] = new GatherCache.Index();
      }

      this.tick = new int[particles];
      this.served = new int[particles];
      this.mark = new long[particles];
      this.fleeting = new boolean[particles];
      this.contextual = new boolean[particles];
      this.voxel = new VoxelView[particles];
      this.voxelTried = new boolean[particles];
      Arrays.fill(this.tick, Integer.MIN_VALUE);
   }

   public List<AABB> boxes(int i, int now, AABB query, Function<AABB, List<AABB>> gather) {
      AABB seen = query.inflate(0.5);
      AABB have = this.region[i];
      if (this.tick[i] == now && have != null && !this.fleeting[i] && contains(have, seen)) {
         this.hits++;
      } else {
         this.hold(i, seen, gather.apply(seen), 0L, false);
         this.tick[i] = now;
      }

      return this.slice(i, seen);
   }

   public List<AABB> boxes(int i, AABB query, GatherCache.Source source) {
      AABB seen = query.inflate(0.5);
      this.reach(i, seen, source);
      return this.slice(i, seen);
   }

   public void reach(int i, AABB seen, GatherCache.Source source) {
      AABB have = this.region[i];
      if (have != null && !this.fleeting[i] && contains(have, seen) && !source.changedSince(have, this.mark[i])) {
         this.mark[i] = source.mark();
         this.hits++;
      } else {
         AABB read = new AABB(
            Math.floor(seen.minX - 0.5),
            Math.floor(seen.minY - 0.5),
            Math.floor(seen.minZ - 0.5),
            Math.ceil(seen.maxX + 0.5),
            Math.ceil(seen.maxY + 0.5),
            Math.ceil(seen.maxZ + 0.5)
         );
         long at = source.mark();
         List<AABB> list = source.gather(read);
         this.hold(i, read, list, at, source.lastGatherVolatile());
         this.contextual[i] = source.lastGatherContextual();
      }
   }

   public VoxelView voxels(int i, AABB query, GatherCache.Source source) {
      AABB seen = query.inflate(0.5);
      this.reach(i, seen, source);
      if (!this.voxelTried[i]) {
         this.voxelTried[i] = true;
         this.voxel[i] = VoxelView.of(this.boxes[i]);
      }

      VoxelView v = this.voxel[i];
      return v == null ? null : v.over(seen);
   }

   public void invalidate() {
      Arrays.fill(this.region, null);
   }

   public void invalidateContextual() {
      for (int i = 0; i < this.region.length; i++) {
         if (this.contextual[i]) {
            this.region[i] = null;
         }
      }
   }

   private void hold(int i, AABB read, List<AABB> list, long at, boolean moving) {
      this.region[i] = read;
      this.boxes[i] = list;
      this.served[i] = 0;
      this.voxelTried[i] = false;
      this.voxel[i] = null;
      this.contextual[i] = false;
      this.mark[i] = at;
      this.fleeting[i] = moving;
      this.gathers++;
   }

   private List<AABB> slice(int i, AABB seen) {
      List<AABB> slice = this.view[i];
      slice.clear();
      List<AABB> held = this.boxes[i];
      if (this.served[i]++ < 3) {
         for (AABB box : held) {
            if (box.intersects(seen)) {
               slice.add(box);
            }
         }

         return slice;
      } else {
         GatherCache.Index grid = this.index[i];
         if (this.served[i] == 4) {
            grid.build(held);
         }

         if (grid.w == 0) {
            return slice;
         }

         int ax = Math.max(0, Mth.floor(seen.minX - grid.extentX - 1.0E-6) - grid.x0);
         int bx = Math.min(grid.w - 1, Mth.floor(seen.maxX) - grid.x0);
         int ay = Math.max(0, Mth.floor(seen.minY - grid.extentY - 1.0E-6) - grid.y0);
         int by = Math.min(grid.h - 1, Mth.floor(seen.maxY) - grid.y0);
         int az = Math.max(0, Mth.floor(seen.minZ - grid.extentZ - 1.0E-6) - grid.z0);
         int bz = Math.min(grid.d - 1, Mth.floor(seen.maxZ) - grid.z0);
         int count = 0;

         for (int z = az; z <= bz; z++) {
            for (int y = ay; y <= by; y++) {
               int row = (z * grid.h + y) * grid.w;

               for (int x = ax; x <= bx; x++) {
                  for (int k = grid.head[row + x]; k >= 0; k = grid.next[k]) {
                     if (held.get(k).intersects(seen)) {
                        if (count == this.found.length) {
                           this.found = Arrays.copyOf(this.found, count * 2);
                        }

                        this.found[count++] = k;
                     }
                  }
               }
            }
         }

         for (int a = 1; a < count; a++) {
            int k = this.found[a];

            int b;
            for (b = a - 1; b >= 0 && this.found[b] > k; b--) {
               this.found[b + 1] = this.found[b];
            }

            this.found[b + 1] = k;
         }

         for (int c = 0; c < count; c++) {
            slice.add(held.get(this.found[c]));
         }

         return slice;
      }
   }

   public static AABB view(AABB query) {
      return query.inflate(0.25);
   }

   public static List<AABB> blocks(Entity entity, AABB region) {
      return new BlockSampler().begin(entity, null).gather(region);
   }

   private static boolean contains(AABB outer, AABB inner) {
      return inner.minX >= outer.minX
         && inner.maxX <= outer.maxX
         && inner.minY >= outer.minY
         && inner.maxY <= outer.maxY
         && inner.minZ >= outer.minZ
         && inner.maxZ <= outer.maxZ;
   }

   public int gathers() {
      return this.gathers;
   }

   public int hits() {
      return this.hits;
   }

   private static final class Index {
      int x0;
      int y0;
      int z0;
      int w;
      int h;
      int d;
      double extentX;
      double extentY;
      double extentZ;
      int[] head = new int[0];
      int[] next = new int[0];

      void build(List<AABB> boxes) {
         int n = boxes.size();
         if (n == 0) {
            this.w = this.h = this.d = 0;
         } else {
            int minX = Integer.MAX_VALUE;
            int minY = Integer.MAX_VALUE;
            int minZ = Integer.MAX_VALUE;
            int maxX = Integer.MIN_VALUE;
            int maxY = Integer.MIN_VALUE;
            int maxZ = Integer.MIN_VALUE;
            this.extentX = this.extentY = this.extentZ = 0.0;

            for (AABB box : boxes) {
               int cx = Mth.floor(box.minX);
               int cy = Mth.floor(box.minY);
               int cz = Mth.floor(box.minZ);
               minX = Math.min(minX, cx);
               minY = Math.min(minY, cy);
               minZ = Math.min(minZ, cz);
               maxX = Math.max(maxX, cx);
               maxY = Math.max(maxY, cy);
               maxZ = Math.max(maxZ, cz);
               this.extentX = Math.max(this.extentX, box.maxX - box.minX);
               this.extentY = Math.max(this.extentY, box.maxY - box.minY);
               this.extentZ = Math.max(this.extentZ, box.maxZ - box.minZ);
            }

            this.x0 = minX;
            this.y0 = minY;
            this.z0 = minZ;
            this.w = maxX - minX + 1;
            this.h = maxY - minY + 1;
            this.d = maxZ - minZ + 1;
            int cells = this.w * this.h * this.d;
            if (this.head.length < cells) {
               this.head = new int[cells];
            }

            Arrays.fill(this.head, 0, cells, -1);
            if (this.next.length < n) {
               this.next = new int[n];
            }

            int k = n - 1;

            while (k >= 0) {
               AABB box = boxes.get(k);
               int cell = ((Mth.floor(box.minZ) - this.z0) * this.h + (Mth.floor(box.minY) - this.y0)) * this.w
                  + (Mth.floor(box.minX) - this.x0);
               this.next[k] = this.head[cell];
               this.head[cell] = k--;
            }
         }
      }
   }

   public interface Source {
      List<AABB> gather(AABB var1);

      long mark();

      boolean changedSince(AABB var1, long var2);

      default boolean lastGatherVolatile() {
         return false;
      }

      default boolean lastGatherContextual() {
         return false;
      }
   }
}
