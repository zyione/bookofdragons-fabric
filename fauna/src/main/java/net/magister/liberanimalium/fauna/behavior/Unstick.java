package net.magister.liberanimalium.fauna.behavior;

import net.magister.liberanimalium.fauna.api.AnimalHandle;
import net.minecraft.core.BlockPos;

public final class Unstick implements Behavior {
   private int rung;
   private boolean rungTried;
   private long rungStartedAt;
   private boolean markHere;
   private boolean failedAgain;

   public boolean takeMark() {
      if (!this.markHere) {
         return false;
      }

      this.markHere = false;
      return true;
   }

   public int rung() {
      return this.rung;
   }

   @Override
   public void start(AnimalHandle handle, MoveVerbs move, long now) {
      this.rung = 0;
      this.rungStartedAt = now;
      this.rungTried = false;
      this.markHere = false;
   }

   public void noteFailure(boolean failed) {
      this.failedAgain = failed;
   }

   @Override
   public void tick(AnimalHandle handle, MoveVerbs move, long now) {
      boolean failedAgain = this.failedAgain;
      if (this.rungTried) {
         if (now - this.rungStartedAt < 40L && !failedAgain) {
            return;
         }

         this.rung++;
      }

      this.rungTried = true;
      this.rungStartedAt = now;
      switch (this.rung) {
         case 0:
            move.unstick();
            break;
         case 1:
            move.changeMode();
            break;
         default:
            this.markHere = true;
            BlockPos elsewhere = Roam.pick(handle, now, 8, 24);
            if (elsewhere != null) {
               move.goTo(elsewhere, 1.0F);
            }

            this.rung = 2;
      }
   }

   @Override
   public void stop(AnimalHandle handle, MoveVerbs move, long now) {
      this.rung = 0;
      this.rungTried = false;
      this.markHere = false;
   }
}
