package net.magister.liberanimalium.fauna.drive;

public final class SurvivalTuning {
   public static final float THREAT_THETA_BOLD = maxThreatTheta() * 0.9F;
   public static final float THREAT_THETA_TIMID = 0.25F;
   public static final float BUCK_THETA = 0.25F;
   public static final int FLEE_DISTANCE = 24;
   public static final float FLEE_URGENCY = 1.0F;
   public static final double FLEE_PANIC_GAP = 6.0;
   public static final float RETREAT_REAIM_COS = (float)Math.cos(Math.PI / 8);
   public static final int FLEE_REAIM_MIN_TICKS = 20;
   public static final float PREY_THETA_KEEN = 0.3F;
   public static final float PREY_THETA_RELUCTANT = 0.7F;
   public static final float PREY_DISTANCE_HALF = 0.5F;
   public static final float PREY_DISTANCE_EXPONENT = 1.5F;
   public static final float PREY_UNREACHABLE_GATE = 0.25F;
   public static final int PREY_PATIENCE_TICKS = 200;
   public static final float CONTRACT_FAR_FLOOR = 0.25F;
   public static final double WAR_SPAN = 48.0;
   public static final float ENMITY_THETA_BOLD = 0.12F;
   public static final float ENMITY_THETA_TIMID = 0.45F;
   public static final float ENMITY_URGENCY = 1.0F;
   public static final float PREY_SWITCH_BIAS = 2.0F;
   public static final float FORAGE_GIFT_FLOOR = 0.35F;
   public static final float APPETITE_ABSORB_PLACID = 0.4F;
   public static final float APPETITE_ABSORB_FIERCE = 0.95F;
   public static final float APPETITE_ABSORB_AXES = 2.0F;
   public static final float WARN_LEAD_SHARE = 0.6F;
   public static final float WARN_CEILING = 0.2F;
   public static final float WARN_OFFER_RELIEF = 0.75F;
   public static final float WARN_OFFER_SHADE_SHARE = 0.5F;
   public static final float HUNT_URGENCY = 1.0F;
   public static final double STRIKE_RANGE = 3.0;
   public static final double FLEE_VANTAGE_MULTIPLE = 2.0;
   public static final float FOOD_PER_MAX_HEALTH = 2.5F;
   public static final int PATROL_MIN_INTERVAL_TICKS = 40;
   public static final int PATROL_IDLE_MULTIPLE = 4;
   public static final int PATROL_RANGE_MIN = 10;
   public static final int PATROL_RANGE_MAX = 40;
   public static final float PATROL_URGENCY = 1.0F;
   public static final float STUCK_THETA = 0.5F;
   public static final float STUCK_DECAY_PER_THINK = 0.97F;
   public static final int STUCK_FAILURES_TO_ACT = 5;
   public static final float STUCK_PRESSURE_PER_FAILURE = 0.2F;
   public static final int STUCK_RUNG_TICKS = 40;
   public static final int UNSTICK_RANGE_MIN = 8;
   public static final int UNSTICK_RANGE_MAX = 24;
   public static final float UNSTICK_URGENCY = 1.0F;
   public static final float PANIC_FRACTION = 0.34F;
   public static final int PANIC_MEMORY_TICKS = 30;
   public static final float STARTLE_SPIKE = 0.5F;
   public static final float PILFER_THETA_BOLD = 0.15F;
   public static final float PILFER_THETA_TIMID = 0.6F;

   private SurvivalTuning() {
   }

   private static float maxThreatTheta() {
      double ceiling = 0.7F;
      return (float)Math.pow((1.0 - ceiling) / ceiling, 0.25);
   }
}
