package net.magister.liberanimalium.network;

import net.magister.liberanimalium.chassis.FaunaBase;
import net.magister.liberanimalium.chassis.component.FaunaMultipartComponent;
import net.magister.liberanimalium.platform.net.PacketContext;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;

public class PartSyncPayload {
   private final int bodyId;
   private final long tick;
   private final short[] offsets;

   public PartSyncPayload(int bodyId, long tick, short[] offsets) {
      this.bodyId = bodyId;
      this.tick = tick;
      this.offsets = offsets;
   }

   public static void encode(PartSyncPayload message, FriendlyByteBuf buffer) {
      buffer.writeVarInt(message.bodyId);
      buffer.writeVarLong(message.tick);
      buffer.writeVarInt(message.offsets.length);

      for (short offset : message.offsets) {
         buffer.writeShort(offset);
      }
   }

   public static PartSyncPayload decode(FriendlyByteBuf buffer) {
      int bodyId = buffer.readVarInt();
      long tick = buffer.readVarLong();
      int length = buffer.readVarInt();
      if (length < 0 || length % 3 != 0 || length > buffer.readableBytes() / Short.BYTES)
         throw new IllegalArgumentException("Invalid multipart report length");
      short[] offsets = new short[length];

      for (int i = 0; i < offsets.length; i++) {
         offsets[i] = buffer.readShort();
      }

      return new PartSyncPayload(bodyId, tick, offsets);
   }

   public static void handle(PartSyncPayload message, PacketContext context) {
      context.enqueueWork(() -> {
         ServerPlayer sender = context.sender();
         if (sender != null) {
            if (sender.level().getEntity(message.bodyId) instanceof FaunaBase body) {
               long now = body.level().getGameTime();
               if (message.tick < now - 20 || message.tick > now + 5 || sender.distanceToSqr(body) > 16384.0) return;
               FaunaMultipartComponent multipart = body.componentRegistry.get(FaunaMultipartComponent.class);
               if (multipart != null) {
                  multipart.accept(sender.getId(), message.tick, message.offsets);
               }
            }
         }
      });
   }
}
