package net.magister.liberanimalium.chassis.component.command;

public enum Modifier {
   REST,
   GUARD,
   TRAIL,
   HEEL,
   GROUND,
   HOMEBOUND;

   public static Modifier fromOrdinal(int ordinal) {
      Modifier[] values = values();
      return ordinal >= 0 && ordinal < values.length ? values[ordinal] : GROUND;
   }
}
