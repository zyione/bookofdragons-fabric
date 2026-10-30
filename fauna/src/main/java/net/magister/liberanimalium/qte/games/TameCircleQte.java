package net.magister.liberanimalium.qte.games;

import net.magister.liberanimalium.qte.QteInput;
import net.magister.liberanimalium.qte.QteMinigame;
import net.magister.liberanimalium.qte.QteOutcome;
import net.magister.liberanimalium.qte.QteRenderState;
import net.magister.liberanimalium.qte.QteType;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;

public class TameCircleQte implements QteMinigame {
   private static final float DEAD_ZONE = 0.5F;
   private static final float START_RADIUS = 0.3F;
   public static final float OUT_BIAS = 0.7F;
   public static final int LETTER_COUNT = "FGHJKL".length();
   private final TameCircleQte.Params p;
   private RandomSource rng;
   private float iconX;
   private float iconY;
   private float resistX;
   private float resistY;
   private int rerollIn;
   private float tame;
   private float soul;
   private int prevDirX;
   private int prevDirY;
   private int heldTicksX;
   private int heldTicksY;
   private float score;
   private int skewLetter = -1;
   private float skewEdgeX;
   private float skewEdgeY;
   private int skewHits;
   private int nextSkewIn;
   private int recoverLeft;

   public TameCircleQte(TameCircleQte.Params params) {
      this.p = params;
   }

   @Override
   public QteType type() {
      return QteType.TAME_CIRCLE;
   }

   @Override
   public void start(RandomSource rng) {
      this.rng = rng;
      float angle = rng.nextFloat() * (float) (Math.PI * 2);
      this.iconX = Mth.cos(angle) * 0.3F;
      this.iconY = Mth.sin(angle) * 0.3F;
      this.tame = 0.0F;
      this.soul = 1.0F;
      this.prevDirX = 0;
      this.prevDirY = 0;
      this.heldTicksX = 0;
      this.heldTicksY = 0;
      this.skewLetter = -1;
      this.skewHits = 0;
      this.recoverLeft = 0;
      this.score = 0.0F;
      this.rollResist();
      this.scheduleNextSkew();
   }

   @Override
   public QteOutcome tick(QteInput input) {
      boolean skewed = this.skewLetter >= 0;
      if (skewed) {
         float dx = this.skewEdgeX - this.iconX;
         float dy = this.skewEdgeY - this.iconY;
         float d = (float)Math.sqrt(dx * dx + dy * dy);
         if (d <= this.p.skewPull()) {
            this.iconX = this.skewEdgeX;
            this.iconY = this.skewEdgeY;
         } else {
            this.iconX = this.iconX + dx / d * this.p.skewPull();
            this.iconY = this.iconY + dy / d * this.p.skewPull();
         }
      } else if (this.recoverLeft > 0) {
         float share = 1.0F / this.recoverLeft;
         this.iconX = this.iconX - this.iconX * share;
         this.iconY = this.iconY - this.iconY * share;
         this.recoverLeft--;
      } else {
         if (--this.rerollIn <= 0) {
            this.rollResist();
         }

         this.iconX = this.iconX + this.resistX;
         this.iconY = this.iconY + this.resistY;
         float or = (float)Math.sqrt(this.iconX * this.iconX + this.iconY * this.iconY);
         if (or > 1.0E-4F) {
            float out = this.p.shove() * 0.7F / or;
            this.iconX = this.iconX + this.iconX * out;
            this.iconY = this.iconY + this.iconY * out;
         }
      }

      int dx = tapDir(input.axisX());
      if (this.fires(dx, dx == this.prevDirX, this.heldTicksX)) {
         this.iconX = this.iconX - this.p.tapImpulse() * dx;
      }

      this.heldTicksX = dx != 0 && dx == this.prevDirX ? this.heldTicksX + 1 : 0;
      this.prevDirX = dx;
      int dy = tapDir(input.axisY());
      if (this.fires(dy, dy == this.prevDirY, this.heldTicksY)) {
         this.iconY = this.iconY + this.p.tapImpulse() * dy;
      }

      this.heldTicksY = dy != 0 && dy == this.prevDirY ? this.heldTicksY + 1 : 0;
      this.prevDirY = dy;
      float r = (float)Math.sqrt(this.iconX * this.iconX + this.iconY * this.iconY);
      boolean pinned = r >= 1.0F;
      if (r > 1.0F) {
         this.iconX /= r;
         this.iconY /= r;
      }

      if (!pinned) {
         this.tame = this.tame + (r <= this.p.innerRadius() ? this.p.fillFast() : this.p.fillSlow());
      }

      if (skewed) {
         this.soul = this.soul - this.p.soulDrain() * this.p.skewDrainMultiple();
         if (input.letter() == this.skewLetter && input.letterPresses() > 0) {
            this.skewHits = this.skewHits + input.letterPresses();
            if (this.skewHits >= this.p.skewMashCount()) {
               this.recover();
            }
         }
      } else {
         this.soul = this.soul - this.p.soulDrain();
         if (pinned) {
            this.soul = this.soul - this.p.rimDrain();
         }

         if (this.recoverLeft == 0 && this.nextSkewIn > 0 && --this.nextSkewIn <= 0) {
            this.beginSkew();
         }
      }

      if (this.tame >= 1.0F) {
         this.score = Math.max(0.0F, this.soul);
         return QteOutcome.WON;
      } else if (this.soul <= 0.0F) {
         this.score = 0.0F;
         return QteOutcome.LOST;
      } else {
         return QteOutcome.RUNNING;
      }
   }

   private void beginSkew() {
      this.skewLetter = this.rng.nextInt(LETTER_COUNT);
      this.skewHits = 0;
      float a = this.rng.nextFloat() * (float) (Math.PI * 2);
      this.skewEdgeX = Mth.cos(a);
      this.skewEdgeY = Mth.sin(a);
   }

   private void recover() {
      this.skewLetter = -1;
      this.skewHits = 0;
      this.soul = Math.min(1.0F, this.soul + this.p.skewRefund());
      this.recoverLeft = Math.max(1, this.p.recoverTicks());
      this.rollResist();
      this.scheduleNextSkew();
   }

   private static int tapDir(float axis) {
      return axis > 0.5F ? 1 : (axis < -0.5F ? -1 : 0);
   }

   private boolean fires(int dir, boolean held, int heldTicks) {
      if (dir == 0) {
         return false;
      } else {
         return !held ? true : this.p.assistPeriod() > 0 && (heldTicks + 1) % this.p.assistPeriod() == 0;
      }
   }

   private void rollResist() {
      float angle = this.rng.nextFloat() * (float) (Math.PI * 2);
      float mag = this.p.shove() * (0.6F + this.rng.nextFloat() * 0.7F);
      this.resistX = Mth.cos(angle) * mag;
      this.resistY = Mth.sin(angle) * mag;
      this.rerollIn = this.p.rerollMin() + this.rng.nextInt(Math.max(1, this.p.rerollMax() - this.p.rerollMin() + 1));
   }

   private void scheduleNextSkew() {
      this.nextSkewIn = this.p.skewGapMin() > 0 ? this.p.skewGapMin() + this.rng.nextInt(Math.max(1, this.p.skewGapMax() - this.p.skewGapMin() + 1)) : 0;
   }

   public boolean skewed() {
      return this.skewLetter >= 0;
   }

   @Override
   public float score() {
      return this.score;
   }

   @Override
   public QteRenderState renderState() {
      float progress = this.skewLetter >= 0 && this.p.skewMashCount() > 0 ? Math.min(1.0F, (float)this.skewHits / this.p.skewMashCount()) : 0.0F;
      return new QteRenderState(QteType.TAME_CIRCLE, new float[]{this.iconX, this.iconY, this.tame, this.soul, this.p.innerRadius(), this.skewLetter, progress});
   }

   public record Params(
      float shove,
      int rerollMin,
      int rerollMax,
      float tapImpulse,
      float innerRadius,
      float fillFast,
      float fillSlow,
      float soulDrain,
      float rimDrain,
      int skewGapMin,
      int skewGapMax,
      float skewPull,
      float skewDrainMultiple,
      int skewMashCount,
      float skewRefund,
      int recoverTicks,
      int assistPeriod
   ) {
   }
}
