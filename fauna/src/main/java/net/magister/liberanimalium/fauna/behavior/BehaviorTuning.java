package net.magister.liberanimalium.fauna.behavior;

public final class BehaviorTuning {
   public static final int WANDER_PAUSE_MIN = 40;
   public static final int FLIGHT_LEGS = 3;
   public static final float WANDER_URGENCY = 1.0F;
   public static final int WANDER_PAUSE_SPREAD = 120;
   public static final float HERD_SYNC = 0.75F;
   public static final int WANDER_RANGE_MIN = 6;
   public static final int WANDER_RANGE_MAX = 20;
   public static final int WANDER_RETRY_TICKS = 20;
   public static final int FULL_CIRCLE_DEGREES = 360;
   public static final int SALT_RANGE = 1;
   public static final int SALT_ANGLE = 2;
   public static final float OBEY_URGENCY = 1.0F;
   public static final double COMPANION_RING_MULTIPLE = 2.0;
   public static final double FOLLOW_SPRINT_BLOCKS = 16.0;
   public static final double FOLLOW_SIGHT_LIFT = 1.0;
   public static final long FOLLOW_HIDDEN_PATIENCE_TICKS = 200L;
   public static final double FOLLOW_SPRINT_EXIT_SHARE = 0.75;
   public static final double FOLLOW_SPRINT_RING_MARGIN = 4.0;
   public static final float FOLLOW_WALK_FLOOR = 0.3F;
   public static final float FOLLOW_CREEP_SHARE = 0.3F;
   public static final float REST_URGENCY = 1.0F;
   public static final int REST_SETTLE_TICKS = 300;
   public static final float INVESTIGATE_URGENCY = 1.0F;
   public static final int ROAM_ATTEMPTS = 10;
   public static final long ROAM_SALT_STEP = 31L;
   public static final int ROAM_POLAR_SPAN_DEG = 150;
   public static final int ROAM_POLAR_SPREAD_DEG = 75;
   public static final int SHOO_STEP_TICKS = 40;
   public static final int SHOO_WARN_TICKS = 80;
   public static final float SHOO_RELEASE_SHARE = 0.66F;
   public static final float SHOO_URGENCY = 0.4F;
   public static final float FORAGE_URGENCY = 1.0F;
   public static final int CURIOSITY_DWELL_TICKS = 150;
   public static final float SOLICIT_URGENCY = 0.3F;
   public static final double SOLICIT_RING_MULTIPLE = 2.0;
   public static final float BUCK_URGENCY = 1.0F;
   public static final int BUCK_RANGE_MIN = 10;
   public static final int BUCK_RANGE_MAX = 20;
   public static final int BUCK_LEG_MIN_TICKS = 40;
   public static final int BUCK_LIFT_BLOCKS = 12;
   public static final int BUCK_REAR_SLOW_TICKS = 200;
   public static final int BUCK_REAR_FAST_TICKS = 80;
   public static final double PILFER_LUNGE = 1.0;
   public static final float PILFER_SIDLE_URGENCY = 0.45F;
   public static final float PILFER_LUNGE_URGENCY = 1.0F;
   public static final double PILFER_ESCAPE_RANGE = 12.0;
   public static final float LOAF_URGENCY = 1.0F;
   public static final double LOAF_NEAR = 2.0;
   public static final double LOAF_SEAT_TOLERANCE = 0.25;
   public static final double PERCH_REACH = 1.5;
   public static final float PERCH_URGENCY = 1.0F;
   public static final int PERCH_REFUSALS = 40;

   private BehaviorTuning() {
   }

   public static double followSprintLine(double ring) {
      return Math.max(16.0, ring + 4.0);
   }
}
