package net.magister.liberanimalium.render.gpu;

public final class IrisQuad {
   private IrisQuad() {
   }

   public static void faceNormal(float[] pos, int quad, float[] out, int at) {
      int p = quad * 12;
      float ax = pos[p + 6] - pos[p];
      float ay = pos[p + 7] - pos[p + 1];
      float az = pos[p + 8] - pos[p + 2];
      float bx = pos[p + 9] - pos[p + 3];
      float by = pos[p + 10] - pos[p + 4];
      float bz = pos[p + 11] - pos[p + 5];
      float nx = ay * bz - az * by;
      float ny = az * bx - ax * bz;
      float nz = ax * by - ay * bx;
      float length = (float)Math.sqrt(nx * nx + ny * ny + nz * nz);
      if (length > 0.0F) {
         nx /= length;
         ny /= length;
         nz /= length;
      }

      out[at] = nx;
      out[at + 1] = ny;
      out[at + 2] = nz;
   }

   public static void tangent(float[] pos, float[] uv, int quad, float nx, float ny, float nz, float[] out, int at) {
      int c0 = quad * 4;
      float area1 = uvArea(uv, c0, c0 + 1, c0 + 2);
      float area2 = uvArea(uv, c0, c0 + 2, c0 + 3);
      boolean first = Math.abs(area1) >= Math.abs(area2);
      int i1 = first ? c0 + 1 : c0 + 2;
      int i2 = first ? c0 + 2 : c0 + 3;
      float area = first ? area1 : area2;
      float e1x = pos[i1 * 3] - pos[c0 * 3];
      float e1y = pos[i1 * 3 + 1] - pos[c0 * 3 + 1];
      float e1z = pos[i1 * 3 + 2] - pos[c0 * 3 + 2];
      float e2x = pos[i2 * 3] - pos[c0 * 3];
      float e2y = pos[i2 * 3 + 1] - pos[c0 * 3 + 1];
      float e2z = pos[i2 * 3 + 2] - pos[c0 * 3 + 2];
      float w = 1.0F;
      float tx;
      float ty;
      float tz;
      if (area != 0.0F) {
         float du1 = uv[i1 * 2] - uv[c0 * 2];
         float dv1 = uv[i1 * 2 + 1] - uv[c0 * 2 + 1];
         float du2 = uv[i2 * 2] - uv[c0 * 2];
         float dv2 = uv[i2 * 2 + 1] - uv[c0 * 2 + 1];
         float inv = 1.0F / area;
         tx = (dv2 * e1x - dv1 * e2x) * inv;
         ty = (dv2 * e1y - dv1 * e2y) * inv;
         tz = (dv2 * e1z - dv1 * e2z) * inv;
         float bx = (du1 * e2x - du2 * e1x) * inv;
         float by = (du1 * e2y - du2 * e1y) * inv;
         float bz = (du1 * e2z - du2 * e1z) * inv;
         float cx = ty * nz - tz * ny;
         float cy = tz * nx - tx * nz;
         float cz = tx * ny - ty * nx;
         w = cx * bx + cy * by + cz * bz < 0.0F ? -1.0F : 1.0F;
      } else {
         tx = e1x;
         ty = e1y;
         tz = e1z;
      }

      float along = tx * nx + ty * ny + tz * nz;
      tx -= nx * along;
      ty -= ny * along;
      tz -= nz * along;
      float length = (float)Math.sqrt(tx * tx + ty * ty + tz * tz);
      if (length > 0.0F) {
         tx /= length;
         ty /= length;
         tz /= length;
      }

      out[at] = tx;
      out[at + 1] = ty;
      out[at + 2] = tz;
      out[at + 3] = w;
   }

   public static void midTexCoord(float[] uv, int quad, float[] out, int at) {
      int t = quad * 8;
      out[at] = (uv[t] + uv[t + 2] + uv[t + 4] + uv[t + 6]) * 0.25F;
      out[at + 1] = (uv[t + 1] + uv[t + 3] + uv[t + 5] + uv[t + 7]) * 0.25F;
   }

   private static float uvArea(float[] uv, int a, int b, int c) {
      float du1 = uv[b * 2] - uv[a * 2];
      float dv1 = uv[b * 2 + 1] - uv[a * 2 + 1];
      float du2 = uv[c * 2] - uv[a * 2];
      float dv2 = uv[c * 2 + 1] - uv[a * 2 + 1];
      return du1 * dv2 - du2 * dv1;
   }
}
