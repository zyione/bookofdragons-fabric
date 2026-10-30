package net.magister.liberanimalium.ride.render;

public final class RiderClock {
   static final long FRESH_GAP_NANOS = 250000000L;
   private boolean started;
   private double lastT;
   private long lastNanos;
   private double dtTicks;
   private double mountedTicks;

   public boolean frame(double t, long nanos) {
      boolean fresh = !this.started || nanos - this.lastNanos > 250000000L;
      if (fresh) {
         this.restart(t, nanos);
         return true;
      } else {
         double step = t - this.lastT;
         this.dtTicks = step > 0.0 ? Math.min(step, 5.0) : 0.0;
         this.mountedTicks = this.mountedTicks + this.dtTicks;
         this.lastT = t;
         this.lastNanos = nanos;
         return false;
      }
   }

   public void restart(double t, long nanos) {
      this.started = true;
      this.lastT = t;
      this.lastNanos = nanos;
      this.dtTicks = 0.0;
      this.mountedTicks = 0.0;
   }

   public double dtTicks() {
      return this.dtTicks;
   }

   public double mountedTicks() {
      return this.mountedTicks;
   }
}
