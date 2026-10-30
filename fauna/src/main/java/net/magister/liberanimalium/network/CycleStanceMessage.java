package net.magister.liberanimalium.network;

import java.util.UUID;
import net.magister.liberanimalium.chassis.FaunaBase;
import net.magister.liberanimalium.chassis.component.CommandComponent;
import net.magister.liberanimalium.platform.net.PacketContext;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;

public class CycleStanceMessage {
   private static final double MAX_RANGE_SQ = 4096.0;
   private final UUID bodyUUID;

   public CycleStanceMessage(UUID bodyUUID) {
      this.bodyUUID = bodyUUID;
   }

   public static void encode(CycleStanceMessage msg, FriendlyByteBuf buf) {
      buf.writeUUID(msg.bodyUUID);
   }

   public static CycleStanceMessage decode(FriendlyByteBuf buf) {
      return new CycleStanceMessage(buf.readUUID());
   }

   public static void handle(CycleStanceMessage msg, PacketContext ctx) {
      ctx.enqueueWork(() -> {
         ServerPlayer player = ctx.sender();
         if (player != null) {
            if (player.serverLevel().getEntity(msg.bodyUUID) instanceof FaunaBase body) {
               if (!(player.distanceToSqr(body) > 4096.0)) {
                  if (body.isTame() && body.isOwnedBy(player)) {
                     CommandComponent command = body.componentRegistry.get(CommandComponent.class);
                     if (command != null) {
                        command.cycleStance(player);
                     }
                  }
               }
            }
         }
      });
   }
}
