package net.magister.liberanimalium.fauna.reservoir;

public final class ReservoirTuning {
   public static final float ANNOYANCE_PER_STARTLE = 0.34F;
   public static final int ANNOYANCE_QUIET_TICKS = 6000;
   public static final float ANNOYANCE_DECAY_PER_REFRESH = 0.0033333334F;
   public static final float ANNOYANCE_EASE_RISE = 0.025F;
   public static final float ANNOYANCE_EASE_FALL_SHARE = 0.5F;
   public static final float REFRESHES_PER_DAY = 1200.0F;
   public static final float SLEEP_DEBT_PER_REFRESH = 8.3333335E-4F;
   public static final float SLEEP_RESTORE_MULTIPLE = 1.3333334F;
   public static final float SLEEP_RESTORE_PER_REFRESH = 0.0011111111F;
   public static final float COMPANY_PER_REFRESH = 0.0033333334F;
   public static final float COMPANY_DRAIN_PER_REFRESH = 0.016666668F;
   public static final float BOREDOM_PER_REFRESH = 0.0016666667F;
   public static final float BOREDOM_BUSY_DRAIN_PER_REFRESH = 0.0016666667F;
   public static final int BOREDOM_ROMP_EMPTIES_TICKS = 300;
   public static final int BOREDOM_ROMP_EMPTIES_TICKS_KEEN = 900;

   private ReservoirTuning() {
   }

   public static float boredomRompDrainPerRefresh(float appetite) {
      float span = 300.0F + 600.0F * Math.max(0.0F, Math.min(1.0F, appetite));
      return 20.0F / span;
   }
}
