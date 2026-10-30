package net.magister.liberanimalium.network;

import org.jetbrains.annotations.Nullable;
import net.magister.liberanimalium.body.grip.client.GripDeckReplay;
import net.magister.liberanimalium.platform.Services;
import net.magister.liberanimalium.platform.net.PacketContext;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.phys.AABB;

public class GripDeckPayload {
   private final int bodyId;
   @Nullable
   private final AABB part;

   public GripDeckPayload(int bodyId, @Nullable AABB part) {
      this.bodyId = bodyId;
      this.part = part;
   }

   public static void encode(GripDeckPayload message, FriendlyByteBuf buffer) {
      buffer.writeVarInt(message.bodyId);
      buffer.writeBoolean(message.part != null);
      if (message.part != null) {
         buffer.writeDouble(message.part.minX);
         buffer.writeDouble(message.part.minY);
         buffer.writeDouble(message.part.minZ);
         buffer.writeDouble(message.part.maxX);
         buffer.writeDouble(message.part.maxY);
         buffer.writeDouble(message.part.maxZ);
      }
   }

   public static GripDeckPayload decode(FriendlyByteBuf buffer) {
      int id = buffer.readVarInt();
      return !buffer.readBoolean()
         ? new GripDeckPayload(id, null)
         : new GripDeckPayload(
            id, new AABB(buffer.readDouble(), buffer.readDouble(), buffer.readDouble(), buffer.readDouble(), buffer.readDouble(), buffer.readDouble())
         );
   }

   public static void handle(GripDeckPayload message, PacketContext context) {
      context.enqueueWork(() -> {
         if (Services.platform().isClient()) {
            GripDeckReplay.apply(message.bodyId, message.part);
         }
      });
   }
}
