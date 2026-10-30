package net.magister.liberanimalium.ride.render;

public final class RiderLook {
   public static final int YAW = 0;
   public static final int PITCH = 1;
   private static final double UPRIGHT = Math.cos(Math.toRadians(45.0));

   private RiderLook() {
   }

   public static void inFrame(double worldYawDeg, double worldPitchDeg, double[] fwd, double[] right, double[] up, double[] out) {
      double yaw = Math.toRadians(worldYawDeg);
      double pitch = Math.toRadians(worldPitchDeg);
      double sy = Math.sin(yaw);
      double cy = Math.cos(yaw);
      double sp = Math.sin(pitch);
      double cp = Math.cos(pitch);
      double lx = -sy * cp;
      double ly = -sp;
      double lz = cy * cp;
      double rx = -cy;
      double rz = -sy;
      double lf = lx * fwd[0] + ly * fwd[1] + lz * fwd[2];
      double lr = lx * right[0] + ly * right[1] + lz * right[2];
      double lu = lx * up[0] + ly * up[1] + lz * up[2];
      double rf = rx * fwd[0] + rz * fwd[2];
      double rr = rx * right[0] + rz * right[2];
      double side = rr;
      double along = -rf;
      if (up[1] < UPRIGHT && lf * side + lr * along < 0.0) {
         side = -side;
         along = -along;
      }

      out[0] = Math.toDegrees(Math.atan2(lr + along, lf + side));
      out[1] = -Math.toDegrees(Math.asin(Math.max(-1.0, Math.min(1.0, lu))));
   }
}
