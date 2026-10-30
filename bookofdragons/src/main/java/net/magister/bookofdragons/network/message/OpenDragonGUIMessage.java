package net.magister.bookofdragons.network.message;

import net.magister.bookofdragons.entity.base.dragon.DragonBase;
import net.magister.liberanimalium.platform.net.PacketContext;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;

public class OpenDragonGUIMessage {
   private final int mountId;

   public OpenDragonGUIMessage(int mountId) {
      this.mountId = mountId;
   }

   public static void encode(OpenDragonGUIMessage message, FriendlyByteBuf buffer) {
      buffer.writeInt(message.mountId);
   }

   public static OpenDragonGUIMessage decode(FriendlyByteBuf buffer) {
      return new OpenDragonGUIMessage(buffer.readInt());
   }

   public static void handle(OpenDragonGUIMessage message, PacketContext context) {
      context.enqueueWork(() -> {
         ServerPlayer player = context.sender();
         if (player != null && player.level().getEntity(message.mountId) instanceof DragonBase mount && player.getVehicle() == mount) {
            mount.openGui(player);
         }
      });
   }
}
