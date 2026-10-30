package net.magister.liberanimalium.anim.rig;

public final class Ease {
   private Ease() {
   }

   public static double step(double state, double target, double rate, double maxStep) {
      return step(state, target, rate, maxStep, maxStep);
   }

   public static double step(double state, double target, double rate, double maxRise, double maxFall) {
      if (Double.isNaN(target)) {
         return state;
      }

      if (Double.isNaN(state)) {
         return target;
      }

      if (!Smoothing.on) {
         return target;
      }

      double move = (target - state) * rate;
      if (move > maxRise) {
         move = maxRise;
      } else if (move < -maxFall) {
         move = -maxFall;
      }

      return state + move;
   }

   public static double lag(double dt, double tau) {
      return 1.0 - Math.exp(-dt / tau);
   }

   public static float lag(float dt, float tau) {
      return 1.0F - (float)Math.exp(-dt / tau);
   }
}
