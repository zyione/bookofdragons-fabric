package net.magister.liberanimalium.util;

public final class Quantum {
   public static final float SPEED = 0.005F;
   public static final float DEGREES = 0.1F;
   public static final float UNIT = 0.005F;
   public static final float STORE = 0.5F;
   public static final int GROWTH_SYNC_TICKS = 20;

   private Quantum() {
   }

   public static float step(float v, float quantum) {
      return Math.round(v / quantum) * quantum;
   }
}
