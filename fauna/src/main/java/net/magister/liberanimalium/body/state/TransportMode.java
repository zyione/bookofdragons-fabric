package net.magister.liberanimalium.body.state;

public enum TransportMode {
   GROUNDED,
   AIRBORNE,
   AQUATIC,
   CLIMBING;

   public static TransportMode fromOrdinal(int ordinal) {
      return ordinal >= 0 && ordinal < values().length ? values()[ordinal] : GROUNDED;
   }
}
