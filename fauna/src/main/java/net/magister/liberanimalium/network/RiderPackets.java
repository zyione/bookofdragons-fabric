package net.magister.liberanimalium.network;

import org.jetbrains.annotations.Nullable;
import net.magister.liberanimalium.chassis.FaunaBase;
import net.minecraft.server.level.ServerPlayer;

public final class RiderPackets {
   private RiderPackets() {
   }

   @Nullable
   public static FaunaBase driven(@Nullable ServerPlayer sender) {
      if (sender != null && sender.getVehicle() instanceof FaunaBase body) {
         return body.getControllingPassenger() == sender ? body : null;
      } else {
         return null;
      }
   }

   @Nullable
   public static FaunaBase ridden(@Nullable ServerPlayer sender) {
      if (sender != null && sender.getVehicle() instanceof FaunaBase body) {
         return body.ridingPlayer() == sender ? body : null;
      } else {
         return null;
      }
   }
}
