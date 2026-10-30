package net.magister.liberanimalium.network;

import net.magister.liberanimalium.chassis.FaunaBase;
import net.magister.liberanimalium.platform.net.PacketContext;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;

public class ControlMessageDismount {
   public static void encode(ControlMessageDismount message, FriendlyByteBuf buffer) {
   }

   public static ControlMessageDismount decode(FriendlyByteBuf buffer) {
      return new ControlMessageDismount();
   }

   public static void handle(ControlMessageDismount message, PacketContext context) {
      context.enqueueWork(() -> {
         ServerPlayer player = context.sender();
         if (player != null) {
            if (player.getVehicle() instanceof FaunaBase) {
               player.stopRiding();
            }
         }
      });
   }
}
