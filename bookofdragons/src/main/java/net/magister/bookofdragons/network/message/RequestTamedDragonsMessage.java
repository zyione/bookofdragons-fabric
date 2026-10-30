package net.magister.bookofdragons.network.message;

import net.magister.bookofdragons.datagen.data.DragonDiscoveryProvider;
import net.magister.liberanimalium.platform.net.PacketContext;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;

public class RequestTamedDragonsMessage {
   public static void encode(RequestTamedDragonsMessage message, FriendlyByteBuf buffer) {
   }

   public static RequestTamedDragonsMessage decode(FriendlyByteBuf buffer) {
      return new RequestTamedDragonsMessage();
   }

   public static void handle(RequestTamedDragonsMessage message, PacketContext context) {
      context.enqueueWork(() -> {
         ServerPlayer player = context.sender();
         if (player != null) {
            DragonDiscoveryProvider.syncTamedDragons(player);
         }
      });
   }
}
