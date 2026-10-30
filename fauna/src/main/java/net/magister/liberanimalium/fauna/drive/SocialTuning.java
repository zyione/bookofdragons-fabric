package net.magister.liberanimalium.fauna.drive;

import net.magister.liberanimalium.fauna.know.Territory;
import net.minecraft.util.Mth;

public final class SocialTuning {
   public static final float TERRITORY_RADIUS_SPREAD = 0.25F;
   public static final float RIVAL_THETA_KEEN = 0.2F;
   public static final float RIVAL_THETA_RELUCTANT = 0.7F;
   public static final float FIGHT_GRADIENT_RELUCTANT = 0.75F;
   public static final float FIGHT_GRADIENT_KEEN = 0.25F;
   public static final float POSTURE_SHARE = 0.5F;
   public static final double POSTURE_STANDOFF_MULTIPLE = 2.0;
   public static final float CONTEST_URGENCY = 1.0F;
   public static final float BACKDOWN_GAP = 0.35F;
   public static final float DUEL_JITTER = 0.05F;
   public static final float YIELD_HP_TIMID = 0.65F;
   public static final float YIELD_HP_BOLD = 0.5F;
   public static final float DOM_YIELD_BONUS = 0.1F;
   public static final float ALOOF_LINE = 0.5F;
   public static final int DISPLAY_DWELL_TICKS = 60;
   public static final int DISPLAY_CRY_REST_TICKS = 6000;
   public static final int MAX_DUEL_TICKS = 6000;
   public static final int CONCEDE_GRACE_TICKS = 40;
   public static final long SETTLED_COOLDOWN_TICKS = 1200L;
   public static final long SESSION_STALE_TICKS = 12000L;
   public static final double HOLD_NEAR = 4.0;
   public static final double HOLD_FAR = 8.0;
   public static final float DOMINANCE_WEIGHT = 0.5F;
   public static final float SIZE_WEIGHT = 0.3F;
   public static final float CONDITION_WEIGHT = 0.19999999F;
   public static final float NEUTRAL_DOMINANCE = 0.5F;
   public static final float PACK_THETA_KEEN = 0.25F;
   public static final float PACK_THETA_RELUCTANT = 0.75F;
   public static final float COMPANY_URGENCY = 1.0F;
   public static final double INVITE_RANGE = 16.0;
   public static final double INVITE_NEAR_MULTIPLE = 1.25;
   public static final double INVITE_FAR_MULTIPLE = 2.5;
   public static final double TAG_REARM_MULTIPLE = 3.75;
   public static final double TAG_REACH = 1.25;
   public static final int INVITE_DWELL_TICKS = 60;
   public static final int MAX_INVITE_TICKS = 240;
   public static final int MAX_BOUT_TICKS = 300;
   public static final int TAG_IDLE_TICKS = 240;
   public static final int TAG_TEASE_HOPS = 2;
   public static final int TAG_TEASE_PAUSE_TICKS = 20;
   public static final int MIN_ROUNDS = 2;
   public static final int ROUND_SPREAD = 1;
   public static final long PLAYED_COOLDOWN_KEEN = 4800L;
   public static final long PLAYED_COOLDOWN_RELUCTANT = 48000L;
   public static final long SETTLED_KEEP_TICKS = Math.max(1200L, 48000L) + 8L;
   public static final float INVITE_URGENCY = 1.0F;
   public static final float CHASE_URGENCY = 1.0F;
   public static final float FLEE_SHARE = 1.0F;
   public static final float PLAY_THETA_KEEN = 0.25F;
   public static final float PLAY_THETA_RELUCTANT = 0.75F;
   public static final float PLAY_ENERGY_SHARE = 0.35F;
   public static final float BORED_THETA_KEEN = 0.3F;
   public static final float BORED_THETA_RELUCTANT = 1.5F;
   public static final float SPACE_THETA_KEEN = 0.1F;
   public static final float SPACE_THETA_RELUCTANT = 0.8F;
   public static final float BEG_WILD_CEILING = 0.15F;
   public static final float BEG_BONDED_CEILING = 0.35F;
   public static final float PERCH_HOLD = 0.1F;
   public static final float ASKED_BID = 0.35F;
   public static final int SPAR_IDLE_TICKS = 240;
   public static final int SPAR_DISPLAY_TICKS = 200;
   public static final int SPAR_MAX_TICKS = 1200;
   public static final long SPAR_COOLDOWN_TICKS = 600L;

   private SocialTuning() {
   }

   public static float territoryRadius(float speciesBase, float dominance) {
      float scale = Mth.lerp(Mth.clamp(dominance, 0.0F, 1.0F), 0.75F, 1.25F);
      return Math.max(0.0F, speciesBase) * scale;
   }

   public static float gradient(double distance, float radius) {
      return Territory.homeAdvantage(distance, radius);
   }

   public static float fightLine(float aggression) {
      return BidShape.threshold(aggression, 0.25F, 0.75F);
   }

   public static float postureLine(float aggression) {
      return fightLine(aggression) * 0.5F;
   }

   public static double postureStandoff() {
      return 6.0;
   }

   public static float dominanceScore(float dominance, float sizeRatio, float healthFraction) {
      float size = Mth.clamp((sizeRatio - 1.0F) * 0.5F + 0.5F, 0.0F, 1.0F);
      return 0.5F * Mth.clamp(dominance, 0.0F, 1.0F) + 0.3F * size + 0.19999999F * Mth.clamp(healthFraction, 0.0F, 1.0F);
   }

   public static double withdrawDistance() {
      return 48.0;
   }

   public static long playedCooldownTicks(float appetite) {
      return (long)Mth.lerp(Mth.clamp(appetite, 0.0F, 1.0F), 48000.0F, 4800.0F);
   }

   public static float crowdResponse(float aggression, float sympathy) {
      return Mth.clamp(Mth.clamp(aggression, 0.0F, 1.0F) * (1.0F - Mth.clamp(sympathy, 0.0F, 1.0F)), 0.0F, 1.0F);
   }

   public static float spaceThreshold(float aggression, float sympathy) {
      return BidShape.threshold(crowdResponse(aggression, sympathy), 0.1F, 0.8F);
   }
}
