package net.magister.liberanimalium.anim.rig;

import net.minecraft.world.phys.Vec3;

public final class FootPlant {
   private static final int LONG_AGO = 1048576;
   private static final int MAX_STEPS = 1;
   public Vec3 pin;
   public FootPlant.Release released = FootPlant.Release.NONE;
   public double share = Double.NaN;
   private double localForward;
   private double localRight;
   private boolean measured;
   private int steps;
   private double leftX;
   private double leftZ;
   private double leadX;
   private double leadZ;
   private double velX;
   private double velZ;
   private double leash;
   private int sinceRelease = 1048576;
   private double lagX;
   private double lagZ;

   public boolean planted() {
      return this.pin != null;
   }

   public void clear() {
      this.pin = null;
      this.released = FootPlant.Release.NONE;
      this.share = Double.NaN;
      this.measured = false;
      this.steps = 0;
      this.leftX = this.leftZ = 0.0;
      this.leadX = this.leadZ = 0.0;
      this.sinceRelease = 1048576;
   }

   public boolean begin(Vec3 clipFoot, Vec3 home, double x, double z, double xo, double zo, float yawDeg, boolean eligible, double leash, Vec3 homeVel) {
      return this.begin(clipFoot, home, x, z, xo, zo, yawDeg, eligible, leash, homeVel, 0.0, 0.0);
   }

   public boolean begin(
      Vec3 clipFoot, Vec3 home, double x, double z, double xo, double zo, float yawDeg, boolean eligible, double leash, Vec3 homeVel, double lagX, double lagZ
   ) {
      if (this.sinceRelease < 1048576) {
         this.sinceRelease++;
      }

      this.lagX = lagX;
      this.lagZ = lagZ;
      this.measure(clipFoot, x, z, xo, zo, yawDeg);
      this.velX = homeVel.x;
      this.velZ = homeVel.z;
      this.leash = leash;
      boolean swinging = !Double.isNaN(this.share) && this.share < 0.0;
      boolean firm = this.steps >= 1;
      if (swinging) {
         this.steps = 0;
         this.leadX = this.leadZ = 0.0;
      }

      if (this.pin == null) {
         return false;
      }

      if (!eligible) {
         this.release(FootPlant.Release.REGIME, home);
      } else if (swinging) {
         this.release(FootPlant.Release.SWING, home);
      } else if (!firm && FootStep.horizontal(this.pin, home) > leash) {
         this.release(FootPlant.Release.LEASH, home);
      }

      return this.pin != null;
   }

   public void settle(Vec3 home, boolean eligible, boolean terrain, double ground, boolean within) {
      boolean grounded = !Double.isNaN(ground);
      if (this.pin != null) {
         if (!grounded) {
            this.release(FootPlant.Release.GROUND, home);
         } else if (!within) {
            this.release(FootPlant.Release.REACH, home);
         } else {
            this.pin = new Vec3(this.pin.x, ground, this.pin.z);
         }
      } else {
         boolean down = !Double.isNaN(this.share) && this.share >= 0.5;
         if (!within && !this.bleeding()) {
            this.leadX = this.leadZ = 0.0;
         }

         if (eligible && !this.bleeding() && this.steps <= 1 && terrain && grounded && within && down) {
            this.pin = new Vec3(this.x(home) - this.lagX, ground, this.z(home) - this.lagZ);
            this.leftX = this.leftZ = 0.0;
            this.leadX = this.leadZ = 0.0;
            this.sinceRelease = 1048576;
            this.released = FootPlant.Release.NONE;
         }
      }
   }

   public double x(Vec3 home) {
      double left = this.left();
      return this.pin != null ? this.pin.x : home.x + this.leftX * left + this.leadX * (1.0 - left);
   }

   public double z(Vec3 home) {
      double left = this.left();
      return this.pin != null ? this.pin.z : home.z + this.leftZ * left + this.leadZ * (1.0 - left);
   }

   public double residual() {
      return Math.hypot(this.leftX, this.leftZ);
   }

   public double hop() {
      if (this.bleeding() && this.released != FootPlant.Release.SWING && this.released != FootPlant.Release.NONE && this.steps <= 1) {
         double k = this.sinceRelease / 3.0;
         return 4.0 * k * (1.0 - k);
      } else {
         return 0.0;
      }
   }

   private boolean bleeding() {
      return Smoothing.on && this.sinceRelease < 3;
   }

   private double left() {
      if (!this.bleeding()) {
         return 0.0;
      }

      double k = this.sinceRelease / 3.0;
      return 1.0 - k * k * (3.0 - 2.0 * k);
   }

   private void release(FootPlant.Release why, Vec3 home) {
      this.leftX = this.pin.x - (home.x - this.lagX);
      this.leftZ = this.pin.z - (home.z - this.lagZ);
      this.pin = null;
      this.sinceRelease = 0;
      this.released = why;
      double travel = Math.hypot(this.velX, this.velZ);
      boolean step = why != FootPlant.Release.SWING && this.steps < 1 && travel > 1.0E-6;
      double lead = step ? 0.6 * this.leash / travel : 0.0;
      this.leadX = this.velX * lead;
      this.leadZ = this.velZ * lead;
      if (why != FootPlant.Release.SWING) {
         this.steps++;
      }
   }

   private void measure(Vec3 clipFoot, double x, double z, double xo, double zo, float yawDeg) {
      double yaw = Math.toRadians(yawDeg);
      double fx = -Math.sin(yaw);
      double fz = Math.cos(yaw);
      double rx = -Math.cos(yaw);
      double rz = -Math.sin(yaw);
      double dx = clipFoot.x - x;
      double dz = clipFoot.z - z;
      double forward = dx * fx + dz * fz;
      double right = dx * rx + dz * rz;
      double tx = x - xo;
      double tz = z - zo;
      double travelForward = tx * fx + tz * fz;
      double travelRight = tx * rx + tz * rz;
      double speedSq = travelForward * travelForward + travelRight * travelRight;
      if (this.measured && speedSq > 1.0E-8) {
         double back = -((forward - this.localForward) * travelForward + (right - this.localRight) * travelRight);
         this.share = back / speedSq;
      } else {
         this.share = Double.NaN;
      }

      this.localForward = forward;
      this.localRight = right;
      this.measured = true;
   }

   public enum Release {
      NONE,
      SWING,
      LEASH,
      REACH,
      GROUND,
      REGIME;
   }
}
