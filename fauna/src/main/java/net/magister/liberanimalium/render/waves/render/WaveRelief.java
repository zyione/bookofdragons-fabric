package net.magister.liberanimalium.render.waves.render;

public final class WaveRelief {
   public static final int SIDE = 65;
   public static final float FLAT = 0.012F;
   private static final float CELL = 0.25F;

   private WaveRelief() {
   }

   public static float lift(float height) {
      return height <= 0.0F ? 0.0F : Math.min(0.6F, 0.26F * (float)Math.log1p(height / 0.25F));
   }

   public static float steep(float slope) {
      return 0.2F + 0.8F * slope / (slope + 0.5F);
   }

   public static float skew(float rise) {
      return 0.35F * rise / (Math.abs(rise) + 0.12F);
   }

   public static float corners(float[] heights, float[] rises, boolean[] wet, float[] out) {
      int stride = 66;
      float tallest = 0.0F;

      for (int j = 0; j < 65; j++) {
         for (int i = 0; i < 65; i++) {
            int a = j * stride + i;
            float lifted = 0.0F;
            if (wet == null || wet[a] && wet[a + 1] && wet[a + stride] && wet[a + stride + 1]) {
               float nw = Math.max(heights[a], 0.0F);
               float ne = Math.max(heights[a + 1], 0.0F);
               float sw = Math.max(heights[a + stride], 0.0F);
               float se = Math.max(heights[a + stride + 1], 0.0F);
               float up = (nw + ne + sw + se) * 0.25F;
               if (up > 0.0F) {
                  float dx = (ne + se - nw - sw) * 0.5F;
                  float dz = (sw + se - nw - ne) * 0.5F;
                  float slope = (float)Math.sqrt(dx * dx + dz * dz);
                  float rise = (rises[a] + rises[a + 1] + rises[a + stride] + rises[a + stride + 1]) * 0.25F;
                  lifted = Math.min(0.6F, lift(up) * steep(slope / up) * (1.0F + skew(rise)));
               }
            }

            out[j * 65 + i] = lifted;
            if (lifted > tallest) {
               tallest = lifted;
            }
         }
      }

      return tallest;
   }

   public static float slopeX(float[] lifts, int i, int j) {
      int w = Math.max(i - 1, 0);
      int e = Math.min(i + 1, 64);
      return (lifts[j * 65 + e] - lifts[j * 65 + w]) / ((e - w) * 0.25F);
   }

   public static float slopeZ(float[] lifts, int i, int j) {
      int n = Math.max(j - 1, 0);
      int s = Math.min(j + 1, 64);
      return (lifts[s * 65 + i] - lifts[n * 65 + i]) / ((s - n) * 0.25F);
   }

   public static float shade(float dx, float dz) {
      float up = 1.0F / (1.0F + dx * dx + dz * dz);
      return up * (1.0F + dx * dx * 0.6F + dz * dz * 0.8F);
   }
}
