package net.magister.liberanimalium.chassis;

import net.magister.liberanimalium.fauna.api.MoveIntent;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;

public final class Vehicles {
   private Vehicles() {
   }

   public static boolean mayBoard(FaunaBase body, Entity vehicle, int boardingCooldown) {
      return vehicle instanceof Player ? true : !body.isVehicle() && boardingCooldown <= 0;
   }

   public static void leaveAtWill(FaunaBase body, MoveIntent intent) {
      if (!body.level().isClientSide() && intent != null && intent.moving()) {
         body.stopRiding();
      }
   }
}
