package net.magister.liberanimalium.body.grip;

public final class GripTuning {
   public static final int SPRING_STEPS = 4;
   public static final double TUCK_WHOLE = 0.5;
   public static final double BEAT_STEP = 0.05;
   public static final double BEAT_GAP = 2.0;
   public static final int ESCAPE_ROW_SHORTEST = 3;
   public static final int ESCAPE_ROW_PER_CLASS = 2;
   public static final int ESCAPE_GRACE = 60;
   public static final int STRUGGLE_QUEUE = 8;
   public static final double HELD_PLAYER_SLACK = 2.0;
   public static final double HELD_PLAYER_LAG_TICKS = 6.0;
   public static final double AIR = 0.91;
   public static final int THROWN_TICKS = 100;
   public static final double SPEED_EASE = 0.25;
   public static final double JUMP = 16.0;
   public static final int SWEEP_SAMPLES_MAX = 8;
   public static final float WEIGHT_RATE = 0.2F;
   public static final double HIPS_SETTLE = 0.1;
   public static final int HIPS_STALE = 2;
   public static final int CURL_SCAN = 8;
   public static final int CURL_STEPS = 8;
   public static final double FORCE_RADIUS = 8.0;
   public static final int STANCE_REPORT_TICKS = 5;
   public static final double STANCE_REPORT_MOVE = 0.05;
   public static final int STANCE_STALE = 40;
   public static final boolean DECK_TAKE = false;
   public static final double DECK_STIFFNESS = 400.0;
   public static final double DECK_DAMPING = 28.0;
   public static final double DECK_TURN_STIFFNESS = 100.0;
   public static final double DECK_TURN_DAMPING = 14.0;
   public static final double DECK_FORCE_HEADROOM = 1.5;
   public static final double DECK_TURN_ARM = 1.0;
   public static final double DECK_HOLD_SPAN = 1.0;
   public static final double DECK_LOAD_EASE = 0.2;
   public static final float DECK_LOAD_STEP = 0.05F;
   public static final double DECK_SLACK = 1.0;
   public static final double DECK_SLIP = 6.0;
   public static final int DECK_TELL_TICKS = 20;

   private GripTuning() {
   }
}
