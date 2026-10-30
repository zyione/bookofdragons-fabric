package net.magister.liberanimalium.body.data;

public enum FaunaWeightClass {
   FEATHER,
   LIGHT,
   MEDIUM,
   HEAVY,
   TITANIC;

   public String getDisplayName() {
      return this.name().charAt(0) + this.name().substring(1).toLowerCase();
   }
}
