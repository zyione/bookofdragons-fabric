package net.magister.liberanimalium.fauna.api;

public record Armament(boolean present, boolean ready, int fuelTicks, boolean stream, int chargeTicks, double reach) {
   public static final Armament NONE = new Armament(false, false, 0, false, 0);

   public Armament(boolean present, boolean ready, int fuelTicks, boolean stream, int chargeTicks) {
      this(present, ready, fuelTicks, stream, chargeTicks, Double.POSITIVE_INFINITY);
   }
}
