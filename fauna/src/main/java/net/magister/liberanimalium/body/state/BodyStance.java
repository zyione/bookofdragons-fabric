package net.magister.liberanimalium.body.state;

import net.magister.liberanimalium.chassis.FaunaBase;

public final class BodyStance {
   private static final double REST_MAX_SPEED_SQ = 0.0025;

   private BodyStance() {
   }

   private static boolean resting(FaunaBase body) {
      return body.getDeltaMovement().horizontalDistanceSqr() <= 0.0025;
   }

   public static boolean sitting(FaunaBase body) {
      return resting(body) && !body.isVehicle() && body.getSyncedIsOrderedToSit() && !incapacitated(body);
   }

   public static boolean incapacitated(FaunaBase body) {
      return body.isTame() && body.getSyncedIsIncapacitated();
   }

   public static boolean chargingTakeoff(FaunaBase body) {
      return body.supportsFlying() && body.isBoosting() && !body.isFlying() && (body.onGround() || body.isInWater()) && !body.isUnderWater();
   }
}
