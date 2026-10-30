package net.magister.bookofdragons.network.message;

import net.magister.bookofdragons.datagen.data.DragonDiscoveryData;
import net.magister.liberanimalium.platform.Services;
import net.magister.liberanimalium.platform.net.PacketContext;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;

public class SyncDiscoveryDataMessage {
   private final DragonDiscoveryData data;

   public SyncDiscoveryDataMessage(DragonDiscoveryData data) {
      this.data = data;
   }

   public static void encode(SyncDiscoveryDataMessage message, FriendlyByteBuf buffer) {
      CompoundTag tag = new CompoundTag();
      message.data.saveToNBT(tag);
      buffer.writeNbt(tag);
   }

   public static SyncDiscoveryDataMessage decode(FriendlyByteBuf buffer) {
      DragonDiscoveryData data = new DragonDiscoveryData();
      CompoundTag tag = buffer.readNbt();
      if (tag != null) {
         data.loadFromNBT(tag);
      }

      return new SyncDiscoveryDataMessage(data);
   }

   public static void handle(SyncDiscoveryDataMessage message, PacketContext context) {
      context.enqueueWork(() -> {
         if (Services.platform().isClient()) {
            ClientPacketHandler.handleSyncDiscoveryData(message.data);
         }
      });
   }
}
