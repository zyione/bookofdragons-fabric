package net.magister.liberanimalium.ride.render;

import java.util.Arrays;
import net.magister.liberanimalium.anim.rig.Ease;
import net.magister.liberanimalium.anim.rig.MotionTrack;

public final class RiderLoad {
   private boolean primed;
   private double lastT;
   private final MotionTrack body = new MotionTrack();
   private final MotionTrack offset = new MotionTrack();
   private double ax;
   private double ay;
   private double az;
   private double speed;
   private double mx;
   private double my;
   private double mz;
   private double bob;
   private final double[] load = new double[3];
   private final double[] accel = new double[3];
   private final double[] gravity = new double[3];

   public void reset() {
      this.primed = false;
      this.body.reset();
      this.offset.reset();
      this.ax = this.ay = this.az = 0.0;
      this.speed = 0.0;
      this.bob = 0.0;
      Arrays.fill(this.load, 0.0);
      Arrays.fill(this.accel, 0.0);
      Arrays.fill(this.gravity, 0.0);
   }

   public void update(double t, double x, double y, double z, double[] fwd, double[] right, double[] up, double smoothTicks) {
      this.update(t, x, y, z, 0.0, 0.0, 0.0, fwd, right, up, smoothTicks);
   }

   public void update(double t, double x, double y, double z, double ox, double oy, double oz, double[] fwd, double[] right, double[] up, double smoothTicks) {
      if (this.primed) {
         double dt = t - this.lastT;
         if (dt < -1.0E-6) {
            this.lastT = t;
            this.body.anchor(x, y, z);
            this.offset.anchor(ox, oy, oz);
            this.project(fwd, right, up);
            return;
         }

         if (!(dt > 1.0E-6)) {
            this.project(fwd, right, up);
            return;
         }

         double jx = x + ox - (this.body.x() + this.offset.x());
         double jy = y + oy - (this.body.y() + this.offset.y());
         double jz = z + oz - (this.body.z() + this.offset.z());
         double jump2 = jx * jx + jy * jy + jz * jz;
         if (!(dt > 5.0) && !(jump2 > 64.0)) {
            this.body.step(x, y, z, dt, smoothTicks);
            this.offset.step(ox, oy, oz, dt, 0.25);
            this.ax = this.body.ax() + this.offset.ax();
            this.ay = this.body.ay() + this.offset.ay();
            this.az = this.body.az() + this.offset.az();
            double vx = this.body.vx() + this.offset.vx();
            double vy = this.body.vy() + this.offset.vy();
            double vz = this.body.vz() + this.offset.vz();
            this.speed = Math.sqrt(vx * vx + vy * vy + vz * vz);
            double beta = Ease.lag(dt, 20.0);
            this.mx = this.mx + beta * (ox - this.mx);
            this.my = this.my + beta * (oy - this.my);
            this.mz = this.mz + beta * (oz - this.mz);
         } else {
            this.reset();
         }
      }

      if (!this.primed) {
         this.mx = ox;
         this.my = oy;
         this.mz = oz;
      }

      this.primed = true;
      this.lastT = t;
      this.body.anchor(x, y, z);
      this.offset.anchor(ox, oy, oz);
      this.project(fwd, right, up);
   }

   private void project(double[] fwd, double[] right, double[] up) {
      double g = 0.08;
      this.bob = (this.offset.x() - this.mx) * up[0] + (this.offset.y() - this.my) * up[1] + (this.offset.z() - this.mz) * up[2];
      this.accel[0] = -dot(this.ax, this.ay, this.az, fwd) / g;
      this.accel[1] = -dot(this.ax, this.ay, this.az, right) / g;
      this.accel[2] = -dot(this.ax, this.ay, this.az, up) / g;
      this.gravity[0] = -fwd[1];
      this.gravity[1] = -right[1];
      this.gravity[2] = 1.0 - up[1];

      for (int i = 0; i < 3; i++) {
         this.load[i] = this.accel[i] + this.gravity[i];
      }
   }

   private static double dot(double x, double y, double z, double[] v) {
      return x * v[0] + y * v[1] + z * v[2];
   }

   public double loadForward() {
      return this.load[0];
   }

   public double loadRight() {
      return this.load[1];
   }

   public double loadUp() {
      return this.load[2];
   }

   public double accelForward() {
      return this.accel[0];
   }

   public double accelRight() {
      return this.accel[1];
   }

   public double accelUp() {
      return this.accel[2];
   }

   public double gravityForward() {
      return this.gravity[0];
   }

   public double gravityRight() {
      return this.gravity[1];
   }

   public double gravityUp() {
      return this.gravity[2];
   }

   public double bob() {
      return this.bob;
   }

   public double speed() {
      return this.speed;
   }
}
