package net.magister.liberanimalium.qte;

import net.magister.liberanimalium.qte.games.TameCircleQte;

public final class QteTuning {
   public static final int TAP_ASSIST_PERIOD = 4;
   public static final float TAME_TAP_IMPULSE = 0.13F;
   public static final float TAME_INNER_RADIUS = 0.35F;
   public static final float TAME_FILL_FAST = 0.01F;
   public static final float TAME_FILL_SLOW = 0.004F;
   public static final float TAME_SOUL_DRAIN = 0.004F;
   public static final float TAME_RIM_DRAIN = 0.008F;
   public static final int TAME_SKEW_MASH_COUNT = 5;
   public static final float TAME_SKEW_PULL = 0.15F;
   public static final float TAME_SKEW_DRAIN_MULTIPLE = 4.0F;
   public static final int TAME_SKEW_MASH_REFERENCE_TICKS = 20;
   public static final float TAME_SKEW_REFUND = 0.32000002F;
   public static final int TAME_SKEW_RECOVER_TICKS = 4;
   public static final float TAME_DRUM_PER_TICK = 0.043333333F;
   public static final float TAME_SHOVE_CEILING = 0.025490195F;
   public static final float TAME_SHOVE_MILD = 0.0063725486F;
   public static final float TAME_SHOVE_FIERCE = 0.012745097F;
   public static final int TAME_REROLL_MILD = 16;
   public static final int TAME_REROLL_FIERCE = 6;
   public static final float TAME_SKEW_FIERCENESS_FLOOR = 0.33333334F;
   public static final int TAME_SKEW_GAP_MILD = 120;
   public static final int TAME_SKEW_GAP_FIERCE = 40;

   private QteTuning() {
   }

   public static TameCircleQte.Params rodeo(float fierceness, boolean tapAssist) {
      float t = Math.max(0.0F, Math.min(1.0F, fierceness));
      float shove = 0.0063725486F + 0.0063725486F * t;
      int reroll = Math.round(16.0F + -10.0F * t);
      int gap = 0;
      if (t >= 0.33333334F) {
         float u = (t - 0.33333334F) / 0.6666666F;
         gap = Math.round(120.0F + -80.0F * u);
      }

      return new TameCircleQte.Params(
         shove,
         Math.max(1, reroll / 2),
         Math.max(2, reroll),
         0.13F,
         0.35F,
         0.01F,
         0.004F,
         0.004F,
         0.008F,
         gap > 0 ? Math.max(1, gap / 2) : 0,
         Math.max(2, gap),
         0.15F,
         4.0F,
         5,
         0.32000002F,
         4,
         tapAssist ? 4 : 0
      );
   }
}
