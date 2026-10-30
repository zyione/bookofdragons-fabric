package net.magister.liberanimalium.fauna.drive;

public final class ComfortTuning {
   public static final float REST_THETA_INACTIVE_LANGUID = 0.2F;
   public static final float REST_THETA_INACTIVE_TIRELESS = 0.35F;
   public static final float REST_THETA_ACTIVE_LANGUID = 0.9F;
   public static final float REST_THETA_ACTIVE_TIRELESS = 1.0F;
   public static final float REST_DOZE_BID = 0.85F;
   public static final float REST_WAKE_SHARE = 0.5F;
   public static final float NOISE_SATURATION = 20.0F;
   public static final float NOISE_CEILING = 0.2F;
   public static final double NOISE_CLEARED_RANGE = 3.0;
   public static final double NOISE_SEEN_MULTIPLE = 3.0;
   public static final float EXPLORE_BASE = 0.12F;
   public static final float EXPLORE_URGENCY = 1.0F;
   public static final int EXPLORE_PAUSE_TICKS = 60;
   public static final int EXPLORE_RETRY_TICKS = 20;
   public static final float LOAF_CEILING = 0.15F;
   public static final float LOAF_THETA_FOND = 0.25F;
   public static final float LOAF_THETA_ALOOF = 0.75F;

   private ComfortTuning() {
   }
}
