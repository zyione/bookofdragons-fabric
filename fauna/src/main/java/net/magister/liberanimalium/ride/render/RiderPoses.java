package net.magister.liberanimalium.ride.render;

import java.util.HashMap;
import java.util.Map;

public final class RiderPoses {
   private static volatile Map<String, RiderPoseProfile> bySpecies = Map.of();

   private RiderPoses() {
   }

   public static RiderPoseProfile get(String species) {
      return bySpecies.get(species);
   }

   public static RiderPoseProfile.Seat seat(String species) {
      RiderPoseProfile p = bySpecies.get(species);
      return p == null ? RiderPoseProfile.Seat.DEFAULT : p.seat;
   }

   public static Map<String, RiderPoseProfile> all() {
      return bySpecies;
   }

   static void set(Map<String, RiderPoseProfile> loaded) {
      bySpecies = Map.copyOf(loaded);
   }

   public static void putLive(String species, RiderPoseProfile profile) {
      Map<String, RiderPoseProfile> next = new HashMap<>(bySpecies);
      next.put(species, profile);
      bySpecies = Map.copyOf(next);
   }
}
