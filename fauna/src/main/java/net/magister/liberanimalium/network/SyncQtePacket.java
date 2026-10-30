package net.magister.liberanimalium.network;

import net.magister.liberanimalium.platform.Services;
import net.magister.liberanimalium.platform.net.PacketContext;
import net.magister.liberanimalium.qte.QteRenderState;
import net.magister.liberanimalium.qte.QteType;
import net.magister.liberanimalium.qte.client.ClientQteState;
import net.minecraft.network.FriendlyByteBuf;

public class SyncQtePacket {
   private final int entityId;
   private final boolean active;
   private final int typeOrdinal;
   private final float[] data;

   public SyncQtePacket(int entityId, boolean active, QteRenderState state) {
      this.entityId = entityId;
      this.active = active;
      this.typeOrdinal = state != null ? state.type().ordinal() : 0;
      this.data = state != null ? state.data() : new float[0];
   }

   private SyncQtePacket(int entityId, boolean active, int typeOrdinal, float[] data) {
      this.entityId = entityId;
      this.active = active;
      this.typeOrdinal = typeOrdinal;
      this.data = data;
   }

   public static void encode(SyncQtePacket message, FriendlyByteBuf buffer) {
      buffer.writeVarInt(message.entityId);
      buffer.writeBoolean(message.active);
      buffer.writeByte(message.typeOrdinal);
      buffer.writeVarInt(message.data.length);

      for (float f : message.data) {
         buffer.writeFloat(f);
      }
   }

   public static SyncQtePacket decode(FriendlyByteBuf buffer) {
      int entityId = buffer.readVarInt();
      boolean active = buffer.readBoolean();
      int typeOrdinal = buffer.readByte();
      int len = buffer.readVarInt();
      float[] data = new float[len];

      for (int i = 0; i < len; i++) {
         data[i] = buffer.readFloat();
      }

      return new SyncQtePacket(entityId, active, typeOrdinal, data);
   }

   public static void handle(SyncQtePacket message, PacketContext context) {
      context.enqueueWork(() -> {
         if (Services.platform().isClient()) {
            handleClient(message);
         }
      });
   }

   private static void handleClient(SyncQtePacket message) {
      if (!message.active) {
         ClientQteState.clear(message.entityId);
      } else {
         QteType type = QteType.values()[message.typeOrdinal];
         ClientQteState.set(message.entityId, new QteRenderState(type, message.data));
      }
   }
}
