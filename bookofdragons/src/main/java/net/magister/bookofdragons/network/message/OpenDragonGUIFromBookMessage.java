package net.magister.bookofdragons.network.message;

import java.util.UUID;
import net.magister.bookofdragons.entity.base.dragon.DragonBase;
import net.magister.liberanimalium.platform.net.PacketContext;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;

public class OpenDragonGUIFromBookMessage {
   private final UUID dragonUUID;

   public OpenDragonGUIFromBookMessage(UUID dragonUUID) {
      this.dragonUUID = dragonUUID;
   }

   public static void encode(OpenDragonGUIFromBookMessage message, FriendlyByteBuf buffer) {
      buffer.writeUUID(message.dragonUUID);
   }

   public static OpenDragonGUIFromBookMessage decode(FriendlyByteBuf buffer) {
      return new OpenDragonGUIFromBookMessage(buffer.readUUID());
   }

   public static void handle(OpenDragonGUIFromBookMessage message, PacketContext context) {
      context.enqueueWork(
         () -> {
            ServerPlayer player = context.sender();
            if (player != null) {
               for (Entity entity : player.serverLevel().getAllEntities()) {
                  if (entity instanceof DragonBase dragon
                     && dragon.getUUID().equals(message.dragonUUID)
                     && dragon.isTame()
                     && player.getUUID().equals(dragon.getOwnerUUID())) {
                     dragon.openGui(player);
                     break;
                  }
               }
            }
         }
      );
   }
}
