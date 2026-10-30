package net.magister.liberanimalium.ride.flight.rotation;

import net.magister.liberanimalium.body.physics.PhysicsSheet;

public final class RollDeadzone {
   private double held;
   private double rate;

   public double filter(double input, double dt, PhysicsSheet.Attitude attitude) {
      return this.filter(input, dt, attitude.rollDeadzone(), attitude.rollDeadzoneFull(), attitude.rollDeadzoneTime());
   }

   public double filter(double input, double dt, double width, double full, double time) {
      if (width <= 0.0) {
         double out = input + this.held;
         this.reset();
         return out;
      }

      if (dt <= 0.0) {
         this.held += input;
         return 0.0;
      }

      double fade = Math.exp(-dt / Math.max(0.001, time));
      this.rate = this.rate * fade + input / dt * (1.0 - fade);
      double share = full <= 0.0 ? 1.0 : Math.min(1.0, Math.abs(this.rate) / full);
      double band = width * (1.0 - share * share * (3.0 - 2.0 * share));
      this.held = this.held * fade + input;
      if (Math.abs(this.held) <= band) {
         return 0.0;
      }

      double out = this.held - Math.copySign(band, this.held);
      this.held = Math.copySign(band, this.held);
      return out;
   }

   public void reset() {
      this.held = 0.0;
      this.rate = 0.0;
   }
}
