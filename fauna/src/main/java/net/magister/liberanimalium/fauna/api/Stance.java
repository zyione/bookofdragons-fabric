package net.magister.liberanimalium.fauna.api;

public enum Stance {
   NONE,
   SOFT_REPEL,
   POSTURE,
   ADVANCE,
   RETREAT;

   public boolean outranks(Stance other) {
      return this.rank() > other.rank();
   }

   private int rank() {
      return switch (this) {
         case RETREAT -> 4;
         case ADVANCE -> 3;
         case POSTURE -> 2;
         case SOFT_REPEL -> 1;
         case NONE -> 0;
      };
   }
}
