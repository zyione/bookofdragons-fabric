package net.magister.liberanimalium.anim.rig;

public final class MotionTrack {
   private double px;
   private double py;
   private double pz;
   private boolean anchored;
   private boolean moving;
   private double vx;
   private double vy;
   private double vz;
   private double ax;
   private double ay;
   private double az;

   public void reset() {
      this.anchored = false;
      this.moving = false;
      this.vx = this.vy = this.vz = 0.0;
      this.ax = this.ay = this.az = 0.0;
   }

   public void anchor(double x, double y, double z) {
      this.px = x;
      this.py = y;
      this.pz = z;
      this.anchored = true;
   }

   public void step(double x, double y, double z, double dt, double smoothTicks) {
      if (!this.anchored) {
         this.anchor(x, y, z);
      } else {
         double jx = x - this.px;
         double jy = y - this.py;
         double jz = z - this.pz;
         if (!this.moving) {
            this.vx = jx / dt;
            this.vy = jy / dt;
            this.vz = jz / dt;
            this.moving = true;
         } else {
            double tau = Math.max(0.001, Smoothing.on ? smoothTicks : 0.0);
            double alpha = Ease.lag(dt, tau);
            double nvx = this.vx + alpha * (jx / dt - this.vx);
            double nvy = this.vy + alpha * (jy / dt - this.vy);
            double nvz = this.vz + alpha * (jz / dt - this.vz);
            this.ax = this.ax + alpha * ((nvx - this.vx) / dt - this.ax);
            this.ay = this.ay + alpha * ((nvy - this.vy) / dt - this.ay);
            this.az = this.az + alpha * ((nvz - this.vz) / dt - this.az);
            this.vx = nvx;
            this.vy = nvy;
            this.vz = nvz;
         }

         this.anchor(x, y, z);
      }
   }

   public double x() {
      return this.px;
   }

   public double y() {
      return this.py;
   }

   public double z() {
      return this.pz;
   }

   public double vx() {
      return this.vx;
   }

   public double vy() {
      return this.vy;
   }

   public double vz() {
      return this.vz;
   }

   public double ax() {
      return this.ax;
   }

   public double ay() {
      return this.ay;
   }

   public double az() {
      return this.az;
   }

   public double accelAlong(double ux, double uy, double uz) {
      return this.ax * ux + this.ay * uy + this.az * uz;
   }
}
