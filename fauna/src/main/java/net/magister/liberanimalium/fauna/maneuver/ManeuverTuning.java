package net.magister.liberanimalium.fauna.maneuver;

import net.minecraft.util.Mth;

public final class ManeuverTuning {
   public static final double ABSORB_EPS_SQ = 1.0;
   public static final int BLOCKED_TOLERANCE_TICKS = 20;
   public static final double FLIGHT_CLEARANCE = 4.0;
   public static final double ARRIVAL_SLACK = 1.5;
   public static final double ARRIVAL_BODIES = 1.5;
   public static final double REAIM = 2.0;
   public static final double TICKS_PER_BLOCK = 3.0;
   public static final int MAX_LEAD_TICKS = 20;
   public static final double ABORT_TICKS_PER_BLOCK = 6.0;
   public static final double PROGRESS_EPS = 0.35;
   public static final int MIN_BEAT_TICKS = 10;
   public static final int PREMISE_GRACE_TICKS = 10;
   public static final int AIR_BEATS_BEFORE_LANDING = 4;
   public static final int AIR_QUIET_TICKS = 160;
   public static final double ARC_LEG_DEGREES = 45.0;
   public static final double ARC_LEG_SPREAD = 30.0;
   public static final double ARC_RECENTRE = 2.5;
   public static final double ARC_RADIUS_EASE = 0.35;
   public static final double ARC_OPEN_EASE = 1.0;
   public static final double COMMITTED_CLOSURE = 0.45;
   public static final int FLANK_SAMPLES = 4;
   public static final double FLANK_SPREAD_DEGREES = 60.0;
   public static final double FLANK_REAIM_DEGREES = 35.0;
   public static final double FLANK_BEHIND_DOT = -0.15;
   public static final double WITHDRAW_SPREAD_DEGREES = 30.0;
   public static final int WITHDRAW_SAMPLES = 4;
   public static final double WITHDRAW_HOP_MAX = 16.0;
   public static final double WITHDRAW_RELEASE_SLACK = 1.5;
   public static final float CREEP_PACE = 0.35F;
   public static final double COVER_BAND_SLACK = 3.0;
   public static final int COVER_SAMPLES = 8;
   public static final double SPRING_MARGIN = 3.0;
   public static final double SPRING_BODIES = 1.5;
   public static final double SPRING_SCALE = 1.0;
   public static final double CREEP_ARRIVE_OF_UNSCALED_SPRING = 0.8;
   public static final double CREEP_ARRIVE_SHARE = 0.8;
   public static final double COVER_VANTAGE_MULTIPLE = 2.0;
   public static final double ARC_RETREAT_OPENING_THRESHOLD = 0.35;
   public static final double ARC_RETREAT_DISTANCE_EXTRA = 3.0;
   public static final double SPRING_MARGIN_MIN = 2.0;
   public static final double SPRING_MARGIN_MAX = 4.0;
   public static final double WITHDRAW_HOP_MIN = 10.0;
   public static final double WITHDRAW_HOP_MAX_CAP = 22.0;
   public static final int MIN_BEAT_TICKS_FAST = 7;
   public static final int MIN_BEAT_TICKS_SLOW = 13;
   public static final double ARC_RETREAT_OPENING_MIN = 0.45;
   public static final double ARC_RETREAT_OPENING_MAX = 0.25;
   public static final double ARC_RETREAT_DISTANCE_EXTRA_MIN = 4.5;
   public static final double ARC_RETREAT_DISTANCE_EXTRA_MAX = 1.5;
   public static final double STREAM_PASS_SLANT = Math.sqrt(2.0);

   private ManeuverTuning() {
   }

   public static double springMargin(float aggression) {
      return Mth.lerp(Mth.clamp(aggression, 0.0F, 1.0F), 2.0, 4.0);
   }

   public static double withdrawHopMax(float nervousness) {
      return Mth.lerp(Mth.clamp(nervousness, 0.0F, 1.0F), 10.0, 22.0);
   }

   public static int dwellTicks(float energy) {
      return Math.round(Mth.lerp(1.0F - Mth.clamp(energy, 0.0F, 1.0F), 7.0F, 13.0F));
   }

   public static double arcRetreatOpeningThreshold(float aggression) {
      return Mth.lerp(Mth.clamp(aggression, 0.0F, 1.0F), 0.45, 0.25);
   }

   public static double arcRetreatDistanceExtra(float aggression) {
      return Mth.lerp(Mth.clamp(aggression, 0.0F, 1.0F), 4.5, 1.5);
   }
}
