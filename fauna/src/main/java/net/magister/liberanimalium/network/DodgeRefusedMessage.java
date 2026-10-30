package net.magister.liberanimalium.network;

import net.magister.liberanimalium.platform.Services;
import net.magister.liberanimalium.platform.net.PacketContext;
import net.magister.liberanimalium.ride.client.RideClientEvents;
import net.minecraft.network.FriendlyByteBuf;

public class DodgeRefusedMessage {
   private final int bodyId;

   public DodgeRefusedMessage(int bodyId) {
      this.bodyId = bodyId;
   }

   public static void encode(DodgeRefusedMessage message, FriendlyByteBuf buffer) {
      buffer.writeVarInt(message.bodyId);
   }

   public static DodgeRefusedMessage decode(FriendlyByteBuf buffer) {
      return new DodgeRefusedMessage(buffer.readVarInt());
   }

   public static void handle(DodgeRefusedMessage message, PacketContext context) {
      context.enqueueWork(() -> {
         if (Services.platform().isClient()) {
            RideClientEvents.dodgeRefused(message.bodyId);
         }
      });
   }
}
