package net.magister.liberanimalium.fauna.maneuver;

public final class MoveStylesTuning {
   public static final Posture CLOSE = new Posture(0.9F, 0.05F, 0.05F, 0.62F);
   public static final Posture ARC = new Posture(0.3F, 0.8F, 0.1F, 0.65F);
   public static final Posture FLANK = new Posture(0.75F, 0.7F, 0.15F, 0.75F);
   public static final Posture WITHDRAW = new Posture(0.2F, 0.15F, 0.1F, 0.15F);
   public static final Posture COVER = new Posture(0.35F, 0.25F, 0.6F, 0.35F);
   public static final Posture CREEP = new Posture(0.85F, 0.05F, 0.55F, 0.4F);
   public static final Posture FREEZE = new Posture(0.1F, 0.0F, 0.75F, 0.2F);
   public static final Posture HOLD = new Posture(0.35F, 0.05F, 0.05F, 0.95F);
   public static final int POSTURE_AXES = 4;
   public static final float HOLD_BONUS = 1.08F;
   public static final float PRIOR_MIN = 0.6F;
   public static final float PRIOR_MAX = 1.4F;
   public static final float PRESS_BASE = 0.7F;
   public static final float PRESS_BRAVERY = 0.4F;
   public static final float PRESS_TENACITY = 0.2F;
   public static final float STAND_BRAVERY = 0.3F;
   public static final float STAND_TENACITY = 0.3F;
   public static final float ANGLE_BASE = 1.1F;
   public static final float ANGLE_BRAVERY = 0.2F;
   public static final float ANGLE_CONCERN = 0.2F;
   public static final float HIDE_BASE = 0.8F;
   public static final float HIDE_CONCERN = 0.5F;
   public static final float HIDE_BRAVERY = 0.2F;
   public static final float LETGO_BASE = 1.2F;
   public static final float LETGO_TENACITY = 0.4F;
   public static final float ARC_PACE = 1.0F;

   private MoveStylesTuning() {
   }
}
