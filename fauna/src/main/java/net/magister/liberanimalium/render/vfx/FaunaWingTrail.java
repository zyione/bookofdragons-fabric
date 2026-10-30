package net.magister.liberanimalium.render.vfx;

import net.minecraft.world.phys.Vec3;

public class FaunaWingTrail {
   public final float[] buffer;
   public final int capacity;
   private int headIndex;
   private int pointCount;

   public FaunaWingTrail(int length) {
      this.capacity = length;
      this.buffer = new float[4 * length];
      this.headIndex = 0;
      this.pointCount = 0;
   }

   public void add(Vec3 pos, float alpha) {
      int targetIndex;
      if (this.pointCount < this.capacity) {
         targetIndex = (this.headIndex + this.pointCount) % this.capacity;
         this.pointCount++;
      } else {
         targetIndex = this.headIndex;
         this.headIndex = (this.headIndex + 1) % this.capacity;
      }

      int offset = targetIndex * 4;
      this.buffer[offset] = (float)pos.x;
      this.buffer[offset + 1] = (float)pos.y;
      this.buffer[offset + 2] = (float)pos.z;
      this.buffer[offset + 3] = alpha;
   }

   public void decay() {
      if (this.pointCount > 0) {
         this.headIndex = (this.headIndex + 1) % this.capacity;
         this.pointCount--;
      }
   }

   public void clear() {
      this.pointCount = 0;
      this.headIndex = 0;
   }

   public int getPointCount() {
      return this.pointCount;
   }

   public int getHeadIndex() {
      return this.headIndex;
   }

   public int getCapacity() {
      return this.capacity;
   }
}
