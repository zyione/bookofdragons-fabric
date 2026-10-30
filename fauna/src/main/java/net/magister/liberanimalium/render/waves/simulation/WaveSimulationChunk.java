package net.magister.liberanimalium.render.waves.simulation;

public class WaveSimulationChunk {
   public final int x;
   public final int z;
   public final int y;
   public static final int SIZE = 64;
   public static final int STRIDE = 66;
   public static final int TOTAL_CELLS = 4356;
   public static final int MASK_SIDE = 18;
   private float[] hCurrent;
   private float[] hPrev;
   private float[] hNext;
   private boolean[] wet;
   public boolean isActive = true;
   public boolean isDormant = false;
   public boolean isDirty = true;
   public boolean needsTextureClear = false;
   public boolean dry = false;
   public long maskMark;
   public float peak;
   private int idleTicks = 0;
   public long lastActivityTime;
   public int biomeWaterColor = 4159204;

   public WaveSimulationChunk(int chunkX, int chunkZ, int chunkY) {
      this.x = chunkX;
      this.z = chunkZ;
      this.y = chunkY;
      this.hCurrent = new float[4356];
      this.hPrev = new float[4356];
      this.hNext = new float[4356];
      this.lastActivityTime = System.currentTimeMillis();
   }

   public void setWater(boolean[] blocks) {
      boolean all = true;
      boolean any = false;

      for (int bz = 0; bz < 18; bz++) {
         for (int bx = 0; bx < 18; bx++) {
            boolean open = blocks[bz * 18 + bx];
            all &= open;
            if (open && bx > 0 && bx < 17 && bz > 0 && bz < 17) {
               any = true;
            }
         }
      }

      if (all) {
         this.wet = null;
      } else {
         boolean[] cells = new boolean[4356];
         int perBlock = 4;

         for (int cz = 0; cz < 66; cz++) {
            int bz = Math.floorDiv(cz - 1, perBlock) + 1;

            for (int cx = 0; cx < 66; cx++) {
               int bx = Math.floorDiv(cx - 1, perBlock) + 1;
               boolean open = blocks[bz * 18 + bx];
               int c = cz * 66 + cx;
               cells[c] = open;
               if (!open) {
                  this.hCurrent[c] = 0.0F;
                  this.hPrev[c] = 0.0F;
                  this.hNext[c] = 0.0F;
               }
            }
         }

         this.wet = cells;
      }

      if (!any && !this.dry) {
         this.dry = true;
         this.isDormant = true;
         this.needsTextureClear = true;
         this.isDirty = true;
      } else if (any && this.dry) {
         this.dry = false;
         this.wakeUp();
      }
   }

   public void tick() {
      if (this.isDormant) {
         this.idleTicks++;
         if (this.idleTicks > 100) {
            this.isActive = false;
         }
      } else {
         float damping = 0.935F;
         float waveSpeed2 = 0.17639999F;
         boolean[] wet = this.wet;
         float maxEnergy = 0.0F;

         for (int j = 1; j <= 64; j++) {
            int rowAbove = (j - 1) * 66;
            int rowCurrent = j * 66;
            int rowBelow = (j + 1) * 66;

            for (int i = 1; i <= 64; i++) {
               int c = rowCurrent + i;
               if (wet != null && !wet[c]) {
                  this.hNext[c] = 0.0F;
               } else {
                  float val = this.hCurrent[c];
                  float n = this.hCurrent[rowAbove + i];
                  float s = this.hCurrent[rowBelow + i];
                  float w = this.hCurrent[c - 1];
                  float e = this.hCurrent[c + 1];
                  float nw = this.hCurrent[rowAbove + i - 1];
                  float ne = this.hCurrent[rowAbove + i + 1];
                  float sw = this.hCurrent[rowBelow + i - 1];
                  float se = this.hCurrent[rowBelow + i + 1];
                  if (wet != null) {
                     if (!wet[rowAbove + i]) {
                        n = val;
                     }

                     if (!wet[rowBelow + i]) {
                        s = val;
                     }

                     if (!wet[c - 1]) {
                        w = val;
                     }

                     if (!wet[c + 1]) {
                        e = val;
                     }

                     if (!wet[rowAbove + i - 1]) {
                        nw = val;
                     }

                     if (!wet[rowAbove + i + 1]) {
                        ne = val;
                     }

                     if (!wet[rowBelow + i - 1]) {
                        sw = val;
                     }

                     if (!wet[rowBelow + i + 1]) {
                        se = val;
                     }
                  }

                  float laplacian = 0.5F * (n + s + w + e) + 0.25F * (nw + ne + sw + se) - 3.0F * val;
                  float nextVal = (2.0F * val - this.hPrev[c] + waveSpeed2 * laplacian) * damping;
                  this.hNext[c] = nextVal;
                  float absVal = Math.abs(nextVal);
                  if (absVal > maxEnergy) {
                     maxEnergy = absVal;
                  }
               }
            }
         }

         float[] temp = this.hPrev;
         this.hPrev = this.hCurrent;
         this.hCurrent = this.hNext;
         this.hNext = temp;
         this.peak = maxEnergy;
         if (maxEnergy > 0.0015F) {
            this.idleTicks = 0;
            this.isDirty = true;
            this.lastActivityTime = System.currentTimeMillis();
         } else {
            this.idleTicks++;
            if (this.idleTicks == 1) {
               this.needsTextureClear = true;
               this.isDirty = true;
            } else if (this.idleTicks > 25) {
               this.isDormant = true;
            }

            if (this.idleTicks > 100) {
               this.isActive = false;
            }
         }
      }
   }

   public void wakeUp() {
      if (!this.dry) {
         this.isDormant = false;
         this.idleTicks = 0;
         this.isDirty = true;
         this.lastActivityTime = System.currentTimeMillis();
      }
   }

   public boolean isWater(int localX, int localZ) {
      return localX >= 1 && localX <= 64 && localZ >= 1 && localZ <= 64 ? this.wet == null || this.wet[localZ * 66 + localX] : false;
   }

   public void addImpulse(int localX, int localZ, float force) {
      if (this.isWater(localX, localZ)) {
         int idx = localZ * 66 + localX;
         this.hCurrent[idx] = this.hCurrent[idx] + force;
         this.hPrev[idx] = this.hPrev[idx] - force * 0.5F;
         if (Math.abs(this.hCurrent[idx]) > this.peak) {
            this.peak = Math.abs(this.hCurrent[idx]);
         }

         this.wakeUp();
      }
   }

   public void addRadialImpulse(int localX, int localZ, float radius, float force) {
      int r = (int)Math.ceil(radius);
      float rSq = radius * radius;
      int minX = Math.max(1, localX - r);
      int maxX = Math.min(64, localX + r);
      int minZ = Math.max(1, localZ - r);
      int maxZ = Math.min(64, localZ + r);

      for (int z = minZ; z <= maxZ; z++) {
         int row = z * 66;
         float dz = z - localZ;

         for (int x = minX; x <= maxX; x++) {
            float dx = x - localX;
            float dSq = dx * dx + dz * dz;
            if (dSq <= rSq && (this.wet == null || this.wet[row + x])) {
               float factor = 1.0F - (float)Math.sqrt(dSq) / radius;
               float f = force * factor;
               int idx = row + x;
               this.hCurrent[idx] = this.hCurrent[idx] + f;
               this.hPrev[idx] = this.hPrev[idx] - f * 0.5F;
               if (Math.abs(this.hCurrent[idx]) > this.peak) {
                  this.peak = Math.abs(this.hCurrent[idx]);
               }
            }
         }
      }

      this.wakeUp();
   }

   public float getHeight(int localX, int localZ) {
      return localX >= 1 && localX <= 64 && localZ >= 1 && localZ <= 64 ? this.hCurrent[localZ * 66 + localX] : 0.0F;
   }

   public float getSmoothedHeight(int localX, int localZ) {
      if (localX >= 1 && localX <= 64 && localZ >= 1 && localZ <= 64) {
         int idx = localZ * 66 + localX;
         return (this.hCurrent[idx] + this.hPrev[idx]) * 0.5F;
      } else {
         return 0.0F;
      }
   }

   public float getRise(int localX, int localZ) {
      if (localX >= 1 && localX <= 64 && localZ >= 1 && localZ <= 64) {
         int idx = localZ * 66 + localX;
         return this.hCurrent[idx] - this.hPrev[idx];
      } else {
         return 0.0F;
      }
   }

   public void setBoundary(int x, int z, float height) {
      x = Math.max(0, Math.min(65, x));
      z = Math.max(0, Math.min(65, z));
      int idx = z * 66 + x;
      if (this.wet == null || this.wet[idx]) {
         this.hCurrent[idx] = height;
      }
   }

   public void smoothedInto(float[] heights, float partial) {
      for (int c = 0; c < 4356; c++) {
         float before = this.hNext[c];
         float now = this.hCurrent[c];
         heights[c] = (this.hPrev[c] + before + (now - before) * partial) * 0.5F;
      }
   }

   public void risesInto(float[] rises) {
      for (int c = 0; c < 4356; c++) {
         rises[c] = (this.hCurrent[c] - this.hNext[c]) * 0.5F;
      }
   }

   public boolean[] waterCells() {
      return this.wet;
   }
}
