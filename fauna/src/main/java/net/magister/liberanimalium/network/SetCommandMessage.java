package net.magister.liberanimalium.network;

import java.util.UUID;
import net.magister.liberanimalium.chassis.FaunaBase;
import net.magister.liberanimalium.chassis.component.CommandComponent;
import net.magister.liberanimalium.chassis.component.command.Modifier;
import net.magister.liberanimalium.chassis.component.command.Stance;
import net.magister.liberanimalium.platform.net.PacketContext;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;

public class SetCommandMessage {
   private static final double MAX_RANGE_SQ = 4096.0;
   private final UUID bodyUUID;
   private final byte stanceOrdinal;
   private final byte modifierOrdinal;

   public SetCommandMessage(UUID bodyUUID, Stance stance, Modifier modifier) {
      this.bodyUUID = bodyUUID;
      this.stanceOrdinal = (byte)stance.ordinal();
      this.modifierOrdinal = (byte)modifier.ordinal();
   }

   private SetCommandMessage(UUID bodyUUID, byte stanceOrdinal, byte modifierOrdinal) {
      this.bodyUUID = bodyUUID;
      this.stanceOrdinal = stanceOrdinal;
      this.modifierOrdinal = modifierOrdinal;
   }

   public static void encode(SetCommandMessage msg, FriendlyByteBuf buf) {
      buf.writeUUID(msg.bodyUUID);
      buf.writeByte(msg.stanceOrdinal);
      buf.writeByte(msg.modifierOrdinal);
   }

   public static SetCommandMessage decode(FriendlyByteBuf buf) {
      return new SetCommandMessage(buf.readUUID(), buf.readByte(), buf.readByte());
   }

   public static void handle(SetCommandMessage msg, PacketContext ctx) {
      ctx.enqueueWork(() -> {
         ServerPlayer player = ctx.sender();
         if (player != null) {
            FaunaBase body = resolveBody(player, msg.bodyUUID);
            if (body != null) {
               if (!(player.distanceToSqr(body) > 4096.0)) {
                  if (body.isTame() && body.isOwnedBy(player)) {
                     Stance stance = Stance.fromOrdinal(msg.stanceOrdinal);
                     Modifier modifier = Modifier.fromOrdinal(msg.modifierOrdinal);
                     CommandComponent command = body.componentRegistry.get(CommandComponent.class);
                     if (command != null) {
                        command.setCommandFromPlayer(stance, modifier, player);
                     }
                  }
               }
            }
         }
      });
   }

   private static FaunaBase resolveBody(ServerPlayer player, UUID uuid) {
      return player.serverLevel().getEntity(uuid) instanceof FaunaBase d ? d : null;
   }
}
