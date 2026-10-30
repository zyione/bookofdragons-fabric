package net.magister.liberanimalium.fauna.know;

import net.minecraft.util.Mth;

public final class SocialBookTuning {
   public static final int CAPACITY = 16;
   public static final float STICKINESS = 0.9F;
   public static final float FAMILIARITY_PER_NOTE = 0.05F;
   public static final float DEFEAT_LIKE_SHARE = 0.5F;
   public static final float KINDNESS_LIKE = 0.08F;
   public static final float KINDNESS_FEAR_SHARE = 0.5F;
   public static final float FORGIVE_LIKE_PER_TICK = 3.3333334E-5F;
   public static final float FEED_FLOOR_SHARE = 0.25F;
   public static final long DAY_TICKS = 24000L;
   public static final long HABITUATE_PERIOD_TICKS = 1200L;
   public static final float DAY_RETENTION = 0.85F;
   public static final float PERMANENT_BAND = 0.5F;
   public static final float FORGET_BELOW = 0.25F;
   public static final float KEEP_FAMILIAR = 0.1F;
   public static final float FRIEND_LINE = 0.5F;
   public static final float PLAYED_LIKE_SHARE = 0.5F;
   public static final float GRIEVOUS_DAMAGE = 8.0F;
   public static final float GRIEVOUS_COST = 0.25F;
   public static final float HOUSEHOLD_PERIODS_TO_FRIEND = 10.0F;
   public static final float WILD_KIN_PERIOD_MULTIPLE = 2.0F;
   public static final float SETTLED_LIKE = 0.026666665F;
   public static final float HOUSEHOLD_LIKE_PER_PERIOD = 0.05F;
   public static final float PLAY_LIKE_PER_PERIOD = 0.1F;

   private SocialBookTuning() {
   }

   public static float betrayalCost(float amount, float maxHealth) {
      if (maxHealth <= 0.0F) {
         return 1.0F;
      }

      float wound = Mth.clamp(amount / maxHealth, 0.0F, 1.0F);
      float woundTerm = wound * Math.min(1.0F, wound / 0.25F);
      float grade = Math.min(1.0F, Math.max(0.0F, amount) / 8.0F);
      float weaponTerm = grade * grade * 0.25F;
      return Math.max(woundTerm, weaponTerm);
   }
}
