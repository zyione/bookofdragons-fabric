package net.magister.bookofdragons.network.message;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import net.magister.bookofdragons.client.gui.book.ClientDragonRegistry;
import net.magister.liberanimalium.platform.Services;
import net.magister.liberanimalium.platform.net.PacketContext;
import net.minecraft.network.FriendlyByteBuf;

public class SyncTamedDragonsMessage {
   private final List<SyncTamedDragonsMessage.TamedDragonRecord> dragons;

   public SyncTamedDragonsMessage(List<SyncTamedDragonsMessage.TamedDragonRecord> dragons) {
      this.dragons = dragons;
   }

   public static void encode(SyncTamedDragonsMessage message, FriendlyByteBuf buffer) {
      buffer.writeInt(message.dragons.size());

      for (SyncTamedDragonsMessage.TamedDragonRecord r : message.dragons) {
         buffer.writeUUID(r.uuid());
         buffer.writeUtf(r.displayName());
         buffer.writeBoolean(r.bonded());
         buffer.writeBoolean(r.maxAffection());
         buffer.writeInt(r.lastKnownX());
         buffer.writeInt(r.lastKnownZ());
         buffer.writeUtf(r.type(), 64);
         buffer.writeVarInt(r.affection());
         buffer.writeLong(r.tamedAt());
      }
   }

   public static SyncTamedDragonsMessage decode(FriendlyByteBuf buffer) {
      int count = buffer.readInt();
      List<SyncTamedDragonsMessage.TamedDragonRecord> list = new ArrayList<>(count);

      for (int i = 0; i < count; i++) {
         UUID uuid = buffer.readUUID();
         String name = buffer.readUtf();
         boolean bonded = buffer.readBoolean();
         boolean maxAffection = buffer.readBoolean();
         int x = buffer.readInt();
         int z = buffer.readInt();
         String type = buffer.readUtf(64);
         int affection = buffer.readVarInt();
         long tamedAt = buffer.readLong();
         list.add(new SyncTamedDragonsMessage.TamedDragonRecord(uuid, name, bonded, maxAffection, x, z, type, affection, tamedAt));
      }

      return new SyncTamedDragonsMessage(list);
   }

   public static void handle(SyncTamedDragonsMessage message, PacketContext context) {
      context.enqueueWork(() -> {
         if (Services.platform().isClient()) {
            ClientDragonRegistry.update(message.dragons);
         }
      });
   }

   public record TamedDragonRecord(
      UUID uuid, String displayName, boolean bonded, boolean maxAffection, int lastKnownX, int lastKnownZ, String type, int affection, long tamedAt
   ) {
   }
}
