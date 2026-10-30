package net.magister.liberanimalium.fauna.behavior;

public final class EngageTuning {
   public static final double BREATH_MIN_REACHES = 2.0;
   public static final double BREATH_MAX_RANGE = 20.0;
   public static final double BREATH_STAND_MAX = 14.0;
   public static final int BURST_WORTH_STARTING = 25;
   public static final int BURST_SPENT = 4;
   public static final double MELEE_SLACK = 1.6;
   public static final int SENSE_LOSS_GRACE = 30;
   public static final float FIRING_PACE = 1.0F;
   public static final double AIR_FIRE_CLOSURE = 0.5;
   public static final int CLOSURE_WINDOW = 30;
   public static final double CLOSURE_EPSILON = 0.75;
   public static final double BREAK_CLEARANCE = 8.0;
   public static final double BREAK_CLEARANCE_COMMITTED_SHARE = 0.35;
   public static final double BREAK_BAND_WIDTH = 4.0;
   public static final float PRESS_CONTACT = 0.9F;
   public static final float PRESS_BLOW = 0.35F;
   public static final float BREAK_PATIENCE_MAX = 0.75F;
   public static final float BREAK_PATIENCE_SPREAD = 0.5F;
   public static final float BREAK_RESUME_SHARE = 0.55F;
   public static final int BREAK_ABSURDITY_TICKS = (int)Math.ceil(4.0 * Math.log(4.8484845682499875) / -Math.log(0.9874999998137355));
   public static final float SPENT_FLOOR = 0.35F;
   public static final float OBSERVED_WEIGHT = 1.4F;
   public static final float COMMIT_RISE_MIN = 0.02F;
   public static final float COMMIT_RISE_MAX = 0.12F;
   public static final float COMMIT_FALL_MULTIPLE = 2.0F;
   public static final int DWELL_MAX_TICKS = 30;
   public static final int DWELL_MIN_TICKS = 10;
   public static final float LEAP_WAIT_MIN_RHYTHMS = 3.0F;
   public static final float LEAP_WAIT_MAX_RHYTHMS = 8.0F;
   public static final float FRUSTRATION_RISE_PER_THINK = 0.02F;
   public static final float FRUSTRATION_RELIEF_PER_BLOW = 0.39999998F;
   public static final float FRUSTRATION_DECAY_PER_THINK = 0.005F;
   public static final int BLOW_MEMORY_TICKS = 45;
   public static final double MELEE_WORKING_STANDOFF = 7.0;
   public static final double MELEE_INNER_SHARE = 0.7;
   public static final long COMMITMENT_LAPSE_TICKS = 10L;

   private EngageTuning() {
   }

   public static double breathReach(double contact) {
      return Math.max(contact, 3.0);
   }

   public static double breathMin(double contact) {
      return breathReach(contact) * 2.0;
   }

   public static double breathStandMin(double contact) {
      return breathMin(contact) + breathReach(contact);
   }

   public static double breathStandMax(double contact) {
      return Math.max(14.0, breathStandMin(contact));
   }
}
