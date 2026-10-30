package net.magister.bookofdragons.network.message;

import net.magister.bookofdragons.entity.data.DragonType;
import net.magister.liberanimalium.platform.Services;
import net.magister.liberanimalium.platform.net.PacketContext;
import net.minecraft.network.FriendlyByteBuf;

public class DiscoveryLevelUpdateMessage {
   private final DragonType dragonType;
   private final boolean encountered;
   private final boolean killed;
   private final boolean tamed;
   private final boolean bonded;

   public DiscoveryLevelUpdateMessage(DragonType dragonType, boolean encountered, boolean killed, boolean tamed, boolean bonded) {
      this.dragonType = dragonType;
      this.encountered = encountered;
      this.killed = killed;
      this.tamed = tamed;
      this.bonded = bonded;
   }

   public static void encode(DiscoveryLevelUpdateMessage message, FriendlyByteBuf buffer) {
      buffer.writeUtf(message.dragonType.name());
      buffer.writeBoolean(message.encountered);
      buffer.writeBoolean(message.killed);
      buffer.writeBoolean(message.tamed);
      buffer.writeBoolean(message.bonded);
   }

   public static DiscoveryLevelUpdateMessage decode(FriendlyByteBuf buffer) {
      DragonType type = DragonType.valueOf(buffer.readUtf());
      boolean encountered = buffer.readBoolean();
      boolean killed = buffer.readBoolean();
      boolean tamed = buffer.readBoolean();
      boolean bonded = buffer.readBoolean();
      return new DiscoveryLevelUpdateMessage(type, encountered, killed, tamed, bonded);
   }

   public static void handle(DiscoveryLevelUpdateMessage message, PacketContext context) {
      context.enqueueWork(() -> {
         if (Services.platform().isClient()) {
            ClientPacketHandler.handleDiscoveryLevelUpdate(message.dragonType, message.encountered, message.killed, message.tamed, message.bonded);
         }
      });
   }
}
