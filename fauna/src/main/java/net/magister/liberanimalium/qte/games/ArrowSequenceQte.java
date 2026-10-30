package net.magister.liberanimalium.qte.games;

import net.magister.liberanimalium.qte.QteInput;
import net.magister.liberanimalium.qte.QteMinigame;
import net.magister.liberanimalium.qte.QteOutcome;
import net.magister.liberanimalium.qte.QteRenderState;
import net.magister.liberanimalium.qte.QteType;
import net.minecraft.util.RandomSource;

public final class ArrowSequenceQte implements QteMinigame {
   private static final float PRESS = 0.5F;
   private static final int NO_MISTAKE = 1000;
   private final int length;
   private final QteInput.Dir[] row;
   private int pressed;
   private int sinceMistake = 1000;
   private boolean won;

   public ArrowSequenceQte(int length) {
      this.length = Math.max(1, length);
      this.row = new QteInput.Dir[this.length];
   }

   @Override
   public QteType type() {
      return QteType.ARROW_SEQUENCE;
   }

   @Override
   public void start(RandomSource rng) {
      QteInput.Dir[] all = QteInput.Dir.values();

      for (int i = 0; i < this.length; i++) {
         this.row[i] = all[rng.nextInt(all.length)];
      }

      this.pressed = 0;
      this.sinceMistake = 1000;
      this.won = false;
   }

   @Override
   public QteOutcome tick(QteInput input) {
      if (this.won) {
         return QteOutcome.WON;
      }

      if (this.sinceMistake < 1000) {
         this.sinceMistake++;
      }

      QteInput.Dir press = input.direction(0.5F);
      if (press == null) {
         return QteOutcome.RUNNING;
      }

      if (press == this.row[this.pressed]) {
         this.pressed++;
         if (this.pressed == this.length) {
            this.won = true;
            return QteOutcome.WON;
         }
      } else {
         this.pressed = 0;
         this.sinceMistake = 0;
      }

      return QteOutcome.RUNNING;
   }

   @Override
   public float score() {
      return this.won ? 1.0F : (float)this.pressed / this.length;
   }

   @Override
   public QteRenderState renderState() {
      float[] data = new float[3 + this.length];
      data[0] = this.length;
      data[1] = this.pressed;
      data[2] = this.sinceMistake;

      for (int i = 0; i < this.length; i++) {
         data[3 + i] = this.row[i].ordinal();
      }

      return new QteRenderState(QteType.ARROW_SEQUENCE, data);
   }

   QteInput.Dir arrow(int index) {
      return this.row[index];
   }

   public static QteInput press(QteInput.Dir dir) {
      return switch (dir) {
         case UP -> new QteInput(0.0F, 1.0F, false, false);
         case DOWN -> new QteInput(0.0F, -1.0F, false, false);
         case LEFT -> new QteInput(1.0F, 0.0F, false, false);
         case RIGHT -> new QteInput(-1.0F, 0.0F, false, false);
      };
   }
}
