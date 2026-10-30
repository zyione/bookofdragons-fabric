package net.magister.liberanimalium.anim.sample;

public final class PoseCadence {
   public static final int CHANNELS = 9;
   private float[] previous = new float[0];
   private float[] current = new float[0];
   private int bones = -1;
   private int lastEvaluatedTick = Integer.MIN_VALUE;
   private boolean primed;

   public int bones() {
      return this.bones;
   }

   public boolean primed() {
      return this.primed;
   }

   public boolean due(int tickCount, int boneCount, boolean forced) {
      return forced || boneCount != this.bones || tickCount != this.lastEvaluatedTick;
   }

   public void capture(float[] pose, int boneCount, int tickCount) {
      if (boneCount != this.bones) {
         this.bones = boneCount;
         this.previous = new float[boneCount * 9];
         this.current = new float[boneCount * 9];
         this.primed = false;
      } else {
         float[] swap = this.previous;
         this.previous = this.current;
         this.current = swap;
         this.primed = true;
      }

      System.arraycopy(pose, 0, this.current, 0, Math.min(pose.length, this.current.length));
      if (!this.primed) {
         System.arraycopy(this.current, 0, this.previous, 0, this.current.length);
      }

      this.lastEvaluatedTick = tickCount;
   }

   public void authoritative(float[] out) {
      System.arraycopy(this.current, 0, out, 0, Math.min(out.length, this.current.length));
   }

   public void blend(float alpha, float[] out) {
      float a = alpha < 0.0F ? 0.0F : (alpha > 1.0F ? 1.0F : alpha);
      int n = Math.min(out.length, this.current.length);

      for (int i = 0; i < n; i++) {
         float p = this.previous[i];
         out[i] = p + (this.current[i] - p) * a;
      }
   }

   public void reset() {
      this.bones = -1;
      this.primed = false;
      this.lastEvaluatedTick = Integer.MIN_VALUE;
   }
}
