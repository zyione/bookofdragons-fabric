package net.magister.liberanimalium.fauna.core;

public final class FaunaTuning {
   public static final int SENSE_PERIOD = 20;
   public static final int THINK_PERIOD = 2;
   public static final int RESERVOIR_PERIOD = 20;
   public static final double FAR_THINK_DISTANCE = 48.0;
   public static final double FAR_THINK_FRACTION = 0.5;
   public static final int FAR_THINK_PERIOD = 8;
   public static final int LANDING_RETRY_TICKS = 10;
   public static final double ROUTE_PIN_RANGE = 192.0;
   public static final long FORCED_ROUTE_TICKS = 1536L;
   public static final long FORCED_POSE_TICKS = 1536L;
   public static final long SETTLED_GRACE_TICKS = 20L;
   public static final double FLY_ON_REACH = 0.5;
   public static final double DESCENT_GLIDE_RUN = 1.25;
   public static final double GLIDE_HOLD_SHARE = 0.5;
   public static final float REGARD_BRISTLE = 0.7F;
   public static final float REGARD_STALK = 0.8F;
   public static final float REGARD_PLAY = 0.8F;
   public static final float REGARD_KIN = 0.5F;

   private FaunaTuning() {
   }
}
