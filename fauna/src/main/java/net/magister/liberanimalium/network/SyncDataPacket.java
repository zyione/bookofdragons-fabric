package net.magister.liberanimalium.network;

import io.netty.buffer.Unpooled;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;
import net.magister.liberanimalium.platform.net.PacketContext;
import net.minecraft.network.FriendlyByteBuf;

public final class SyncDataPacket {
   private static final int MAX_TEXT = 2097152;
   private final String id;
   private final Map<String, String> files;

   public SyncDataPacket(String id, Map<String, String> files) {
      this.id = id;
      this.files = files;
   }

   public static void encode(SyncDataPacket message, FriendlyByteBuf buffer) {
      buffer.writeUtf(message.id);
      buffer.writeVarInt(message.files.size());

      for (Entry<String, String> file : message.files.entrySet()) {
         buffer.writeUtf(file.getKey());
         buffer.writeUtf(file.getValue(), 2097152);
      }
   }

   public static SyncDataPacket decode(FriendlyByteBuf buffer) {
      String id = buffer.readUtf();
      int size = buffer.readVarInt();
      Map<String, String> files = new LinkedHashMap<>();

      for (int i = 0; i < size; i++) {
         String key = buffer.readUtf();
         files.put(key, buffer.readUtf(2097152));
      }

      return new SyncDataPacket(id, files);
   }

   public static void handle(SyncDataPacket message, PacketContext context) {
      context.enqueueWork(() -> DataSync.install(message.id, message.files));
   }

   static int size(SyncDataPacket packet) {
      FriendlyByteBuf buffer = new FriendlyByteBuf(Unpooled.buffer());

      try {
         encode(packet, buffer);
         return buffer.readableBytes();
      } finally {
         buffer.release();
      }
   }
}
