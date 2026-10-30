package net.magister.liberanimalium.network;

import net.magister.liberanimalium.fauna.debug.FaunaDebug;
import net.magister.liberanimalium.platform.Services;
import net.magister.liberanimalium.platform.net.PacketContext;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;

public class FaunaDebugToggleMessage {
   private final boolean enabled;

   public FaunaDebugToggleMessage(boolean enabled) {
      this.enabled = enabled;
   }

   public static void encode(FaunaDebugToggleMessage msg, FriendlyByteBuf buf) {
      buf.writeBoolean(msg.enabled);
   }

   public static FaunaDebugToggleMessage decode(FriendlyByteBuf buf) {
      return new FaunaDebugToggleMessage(buf.readBoolean());
   }

   public static void handle(FaunaDebugToggleMessage msg, PacketContext ctx) {
      if (!Services.platform().isProduction()) {
         ctx.enqueueWork(() -> {
            ServerPlayer player = ctx.sender();
            if (player != null) {
               FaunaDebug.setEnabled(player.getUUID(), msg.enabled);
            }
         });
      }
   }
}
