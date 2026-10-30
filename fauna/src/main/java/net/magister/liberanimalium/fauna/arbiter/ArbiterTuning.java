package net.magister.liberanimalium.fauna.arbiter;

public final class ArbiterTuning {
   public static final float NOISE_FLOOR = 0.02F;
   public static final float RISE_RATE = 0.5F;
   public static final float DECAY_PER_TICK = 0.005F;
   public static final float PREEMPT_BAND = 0.3F;
   public static final float EMERGENCY_CEILING = 0.95F;
   public static final float RELEASE_LO = 0.35F;
   public static final float RELEASE_HI = 0.75F;
   public static final long RELEASE_DRAW_SALT = 98691678L;

   private ArbiterTuning() {
   }
}
