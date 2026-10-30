package net.magister.liberanimalium.body.anim.data;

public final class GaitPhase {
   public static final double TELEPORT_CLAMP = 3.0;
   private static final double SOURCE_EPSILON = 0.001;

   private GaitPhase() {
   }

   public static double distance(double deltaMovementHoriz, float syncedSpeed, double positionDelta) {
      double dist = deltaMovementHoriz;
      if (dist <= 0.001) {
         dist = syncedSpeed > 0.001 ? syncedSpeed : positionDelta;
      }

      return dist > 3.0 ? 0.0 : dist;
   }

   public static boolean advance(BoneAccessCache cache, int tickCount, double distance) {
      if (cache.getLastGaitTick() == tickCount) {
         return false;
      }

      cache.setLastGaitTick(tickCount);
      cache.setGaitPhasePrev(cache.getGaitPhase());
      cache.setGaitPhase(cache.getGaitPhase() + distance);
      return true;
   }
}
