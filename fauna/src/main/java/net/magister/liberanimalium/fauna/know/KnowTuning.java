package net.magister.liberanimalium.fauna.know;

import net.minecraft.util.Mth;

public final class KnowTuning {
   public static final int FIND_FAST_WINDOW_TICKS = 80;
   public static final float FIND_TAU_TICKS = 40.0F;
   public static final long PREDICT_CAP_TICKS = 40L;
   public static final float SEARCH_FAIL_FACTOR = 0.25F;
   public static final float SEARCH_REACH_BODIES = 2.0F;
   public static final float NOISE_FIND_CEILING = 0.8F;
   public static final float ROAR_PRESS = 0.6666667F;
   public static final float ALARM_PER_CREATURE = 1.0F;
   public static final float ALARM_FIND_EXPONENT = 0.75F;
   public static final int ALARM_HALF_LIFE_TICKS = 400;
   public static final double ALARM_HALF_LIFE_SPREAD = 0.4;
   public static final double MARK_RADIUS = 6.0;
   public static final double MARK_MERGE_RADIUS = 3.0;
   public static final int MARK_LIFETIME_TICKS = 6000;
   public static final int MARK_CAPACITY = 8;
   public static final float MARK_PATH_COST = 8.0F;
   public static final float MARK_UNREACHABLE = 0.5F;
   public static final long FENCE_LIFETIME_TICKS = 3000L;
   public static final double FENCE_MARGIN = 6.0;
   public static final int NOISE_QUIET_TICKS = 100;
   public static final float NOISE_DECAY_PER_TICK = 0.025F;
   public static final float TERRITORY_MIN_RADIUS = 16.0F;
   public static final float TERRITORY_MAX_RADIUS = 64.0F;
   public static final float TERRITORY_DRIFT_EMA = 0.05F;
   public static final long EXILE_MIN_TICKS = 72000L;
   public static final long EXILE_MAX_TICKS = 168000L;
   public static final float FRESH_GROUND_CORE_MIN = 0.2F;
   public static final double TERRITORY_INWARD_BIAS_THRESHOLD = 0.7;
   public static final float OFFER_SHADE_MAX = 0.9F;
   public static final int GROUND_PROBE_PERIOD = 100;
   public static final int GROUND_SCAN_STEP = 2;
   public static final int GROUND_CANDIDATE_CAP = 24;
   public static final float SHELTER_MIN_SCORE = 1.5F;
   public static final double SHELTER_SEARCH_SHARE = 0.5;
   public static final int BED_SEARCH_PERIOD = 400;
   public static final double VOUCH_RADIUS = 6.0;
   public static final float VOUCH_MAX = 0.9F;
   public static final int CHEST_HALF_LIFE_TICKS = 1200;
   public static final int CHEST_FORGET_TICKS = 24000;
   public static final int CHESTS_KEPT = 4;
   public static final int PERCH_INVITE_TICKS = 200;
   public static final double HERD_WALK = 0.05;
   public static final double COURAGE_RANGE = 16.0;
   public static final float COURAGE_LIFT = 0.6F;
   public static final float COURAGE_HALF_KIN = 2.0F;
   public static final float COURAGE_CALM = 1.6F;

   private KnowTuning() {
   }

   public static double alarmRetentionPerTick(float nervousness) {
      double halfLife = 400.0 * Mth.lerp(Mth.clamp(nervousness, 0.0F, 1.0F), 0.6, 1.4);
      return Math.pow(0.5, 1.0 / halfLife);
   }
}
