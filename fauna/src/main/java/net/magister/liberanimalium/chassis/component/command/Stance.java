package net.magister.liberanimalium.chassis.component.command;

public enum Stance {
   STAY(Modifier.REST, Modifier.GUARD),
   FOLLOW(Modifier.TRAIL, Modifier.HEEL),
   FREE(Modifier.GROUND, Modifier.HOMEBOUND);

   private final Modifier defaultModifier;
   private final Modifier secondaryModifier;

   Stance(Modifier defaultModifier, Modifier secondaryModifier) {
      this.defaultModifier = defaultModifier;
      this.secondaryModifier = secondaryModifier;
   }

   public Modifier defaultModifier() {
      return this.defaultModifier;
   }

   public Modifier secondaryModifier() {
      return this.secondaryModifier;
   }

   public boolean accepts(Modifier modifier) {
      return modifier == this.defaultModifier || modifier == this.secondaryModifier;
   }

   public Stance next() {
      Stance[] values = values();
      return values[(this.ordinal() + 1) % values.length];
   }

   public static Stance fromOrdinal(int ordinal) {
      Stance[] values = values();
      return ordinal >= 0 && ordinal < values.length ? values[ordinal] : FREE;
   }
}
