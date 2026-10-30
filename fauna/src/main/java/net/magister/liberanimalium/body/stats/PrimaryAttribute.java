package net.magister.liberanimalium.body.stats;

public enum PrimaryAttribute {
   CON("Constitution"),
   ATH("Athleticism"),
   AGI("Agility"),
   PRW("Prowess"),
   POT("Potency"),
   VIG("Vigor");

   private final String displayName;

   PrimaryAttribute(String displayName) {
      this.displayName = displayName;
   }

   public String getDisplayName() {
      return this.displayName;
   }
}
