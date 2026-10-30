package net.magister.liberanimalium.fauna.know;

public final class ObedienceTuning {
   public static final float C_EQ_BASE = 0.15F;
   public static final float C_EQ_BOND_WEIGHT = 0.6F;
   public static final float C_EQ_STEADINESS_WEIGHT = 0.15F;
   public static final float C_EQ_SYMPATHY_WEIGHT = 0.1F;
   public static final float C_MIN = 0.05F;
   public static final float C_MAX = 0.98F;
   public static final float DRIFT_MIN = 5.0E-4F;
   public static final float DRIFT_MAX = 0.003F;
   public static final int MAX_INTEGRATE_TICKS = 8;
   public static final float EVENT_FED = 0.15F;
   public static final float EVENT_OBEYED = 0.05F;
   public static final float EVENT_OWNER_BLOW = -0.4F;
   public static final float NEEDS_EROSION = -0.001F;
   public static final float D_STAY_BASE = 0.1F;
   public static final float D_STAY_THREAT = 0.4F;
   public static final float D_FOLLOW_BASE = 0.1F;
   public static final float D_FOLLOW_DANGER = 0.5F;
   public static final float D_FREE = 0.0F;

   private ObedienceTuning() {
   }
}
