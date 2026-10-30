package net.magister.liberanimalium.chassis;

public final class LookFrame {
   public static final double[] IDENTITY = new double[]{1.0, 0.0, 0.0, 0.0, 1.0, 0.0, 0.0, 0.0, 1.0};

   private LookFrame() {
   }

   public static double[] rotX(double a) {
      double c = Math.cos(a);
      double s = Math.sin(a);
      return new double[]{1.0, 0.0, 0.0, 0.0, c, -s, 0.0, s, c};
   }

   public static double[] rotY(double b) {
      double c = Math.cos(b);
      double s = Math.sin(b);
      return new double[]{c, 0.0, s, 0.0, 1.0, 0.0, -s, 0.0, c};
   }

   public static double[] rotZ(double g) {
      double c = Math.cos(g);
      double s = Math.sin(g);
      return new double[]{c, -s, 0.0, s, c, 0.0, 0.0, 0.0, 1.0};
   }

   public static double[] of(double x, double y, double z) {
      return mul(mul(rotZ(z), rotY(y)), rotX(x));
   }

   public static double[] mul(double[] a, double[] b) {
      double[] m = new double[9];

      for (int r = 0; r < 3; r++) {
         for (int c = 0; c < 3; c++) {
            m[r * 3 + c] = a[r * 3] * b[c] + a[r * 3 + 1] * b[3 + c] + a[r * 3 + 2] * b[6 + c];
         }
      }

      return m;
   }

   public static double[] transpose(double[] m) {
      return new double[]{m[0], m[3], m[6], m[1], m[4], m[7], m[2], m[5], m[8]};
   }

   public static double[] apply(double[] m, double[] v) {
      return new double[]{m[0] * v[0] + m[1] * v[1] + m[2] * v[2], m[3] * v[0] + m[4] * v[1] + m[5] * v[2], m[6] * v[0] + m[7] * v[1] + m[8] * v[2]};
   }

   public static float[] euler(double[] m) {
      double sy = Math.max(-1.0, Math.min(1.0, -m[6]));
      double y = Math.asin(sy);
      double x;
      double z;
      if (Math.abs(sy) < 0.999999) {
         x = Math.atan2(m[7], m[8]);
         z = Math.atan2(m[3], m[0]);
      } else {
         z = 0.0;
         x = Math.atan2(-m[5], m[4]);
      }

      return new float[]{(float)x, (float)y, (float)z};
   }

   public static float[] look(double[] parent, double x, double y, double z, double yaw, double pitch) {
      return turn(parent, x, y, z, mul(rotY(yaw), rotX(pitch)));
   }

   public static float[] turn(double[] parent, double x, double y, double z, double[] delta) {
      double[] local = mul(mul(transpose(parent), delta), parent);
      return euler(mul(local, of(x, y, z)));
   }

   public static double[] share(double yaw, double pitch, double from, double to) {
      double[] before = mul(rotY(yaw * from), rotX(pitch * from));
      double[] after = mul(rotY(yaw * to), rotX(pitch * to));
      return mul(after, transpose(before));
   }
}
