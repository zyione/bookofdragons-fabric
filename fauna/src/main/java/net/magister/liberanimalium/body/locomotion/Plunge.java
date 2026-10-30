package net.magister.liberanimalium.body.locomotion;

import net.magister.liberanimalium.body.physics.PhysicsSheet;
import net.magister.liberanimalium.chassis.FaunaBase;

public final class Plunge {
   private Plunge() {
   }

   public static boolean flies(FaunaBase body) {
      if (!body.isUnderWater()) {
         return true;
      }

      int budget = PhysicsSheet.loaded() ? PhysicsSheet.of(body).water().plungeTicks() : 30;
      boolean fast = body.skimsWater() || body.steeredByRider() && !body.level().isClientSide();
      return fast && body.getMaxAirSupply() - body.getAirSupply() < budget;
   }
}
