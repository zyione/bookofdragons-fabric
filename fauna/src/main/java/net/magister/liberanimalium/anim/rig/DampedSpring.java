package net.magister.liberanimalium.anim.rig;

public final class DampedSpring {
   private double value;
   private double velocity;

   public double value() {
      return this.value;
   }

   public double velocity() {
      return this.velocity;
   }

   public void reset(double to) {
      this.value = to;
      this.velocity = 0.0;
   }

   public void kick(double unitsPerSecond) {
      if (Double.isFinite(unitsPerSecond)) {
         this.velocity += unitsPerSecond;
      }
   }

   public void step(double target, double seconds, double frequencyHz, double damping, double limit) {
      if (seconds > 0.0 && frequencyHz > 0.0) {
         if (!Double.isFinite(target)) {
            target = 0.0;
         }

         double w = (Math.PI * 2) * frequencyHz;
         double z = Math.max(0.0, damping);
         double y0 = this.value - target;
         double v0 = this.velocity;
         double t = seconds;
         double y;
         double v;
         if (Math.abs(z - 1.0) < 1.0E-4) {
            double e = Math.exp(-w * t);
            double c = v0 + w * y0;
            y = (y0 + c * t) * e;
            v = (v0 - w * c * t) * e;
         } else if (z < 1.0) {
            double wd = w * Math.sqrt(1.0 - z * z);
            double e = Math.exp(-z * w * t);
            double cos = Math.cos(wd * t);
            double sin = Math.sin(wd * t);
            y = e * (y0 * cos + (v0 + z * w * y0) / wd * sin);
            v = e * (v0 * cos - (w * w * y0 + z * w * v0) / wd * sin);
         } else {
            double s = Math.sqrt(z * z - 1.0);
            double r1 = -w * (z - s);
            double r2 = -w * (z + s);
            double c1 = (v0 - r2 * y0) / (r1 - r2);
            double c2 = y0 - c1;
            double e1 = Math.exp(r1 * t);
            double e2 = Math.exp(r2 * t);
            y = c1 * e1 + c2 * e2;
            v = c1 * r1 * e1 + c2 * r2 * e2;
         }

         this.value = target + y;
         this.velocity = v;
         if (limit > 0.0) {
            if (this.value > limit) {
               this.value = limit;
               if (this.velocity > 0.0) {
                  this.velocity = 0.0;
               }
            } else if (this.value < -limit) {
               this.value = -limit;
               if (this.velocity < 0.0) {
                  this.velocity = 0.0;
               }
            }
         }
      }
   }
}
