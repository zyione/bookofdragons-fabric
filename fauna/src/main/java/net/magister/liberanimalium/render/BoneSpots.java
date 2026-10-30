package net.magister.liberanimalium.render;

import java.util.HashMap;
import java.util.Map;
import net.minecraft.world.phys.Vec3;

public final class BoneSpots {
   public static final int STALE_TICKS = 5;
   private static final Map<Integer, Map<String, BoneSpots.Spot>> SPOTS = new HashMap<>();

   private BoneSpots() {
   }

   public static void store(int entityId, String bone, Vec3 at, long tick) {
      SPOTS.computeIfAbsent(entityId, id -> new HashMap<>()).put(bone, new BoneSpots.Spot(at, tick));
   }

   public static Vec3 get(int entityId, String bone, long tick) {
      Map<String, BoneSpots.Spot> spots = SPOTS.get(entityId);
      if (spots == null) {
         return null;
      }

      BoneSpots.Spot spot = spots.get(bone);
      return spot != null && tick - spot.tick() <= 5L ? spot.at() : null;
   }

   public static void forget(int entityId) {
      SPOTS.remove(entityId);
   }

   private record Spot(Vec3 at, long tick) {
   }
}
