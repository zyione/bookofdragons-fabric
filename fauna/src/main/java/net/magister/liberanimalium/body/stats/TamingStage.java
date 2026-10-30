package net.magister.liberanimalium.body.stats;

public enum TamingStage {
   UNAWARE,
   WARY,
   CURIOUS,
   TOLERANT,
   COMPANION,
   OFFERING,
   TAMED,
   BONDED;

   public boolean atLeast(TamingStage other) {
      return this.ordinal() >= other.ordinal();
   }

   public static TamingStage of(int bond, boolean owned) {
      if (owned) {
         return bond >= BondLevel.BONDED.getMinAffection() ? BONDED : TAMED;
      } else if (bond >= BondLevel.FRIENDLY.getMinAffection()) {
         return OFFERING;
      } else if (bond >= BondLevel.CURIOUS.getMinAffection()) {
         return COMPANION;
      } else if (bond >= BondLevel.TOLERANT.getMinAffection()) {
         return TOLERANT;
      } else {
         return bond > 0 ? CURIOUS : UNAWARE;
      }
   }
}
