package net.magister.liberanimalium.body.stats;

public enum BondLevel {
   WILD(0),
   TOLERANT(200),
   CURIOUS(400),
   FRIENDLY(600),
   BONDED(900);

   private final int minAffection;

   BondLevel(int minAffection) {
      this.minAffection = minAffection;
   }

   public int getMinAffection() {
      return this.minAffection;
   }
}
