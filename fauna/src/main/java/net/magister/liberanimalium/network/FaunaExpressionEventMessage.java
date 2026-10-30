package net.magister.liberanimalium.network;

import net.magister.liberanimalium.anim.ExpressionEventClient;
import net.magister.liberanimalium.platform.Services;
import net.magister.liberanimalium.platform.net.PacketContext;
import net.minecraft.network.FriendlyByteBuf;

public class FaunaExpressionEventMessage {
   private final int entityId;
   private final String key;

   public FaunaExpressionEventMessage(int entityId, String key) {
      this.entityId = entityId;
      this.key = key;
   }

   public int entityId() {
      return this.entityId;
   }

   public String key() {
      return this.key;
   }

   public static void encode(FaunaExpressionEventMessage msg, FriendlyByteBuf buf) {
      buf.writeVarInt(msg.entityId);
      buf.writeUtf(msg.key);
   }

   public static FaunaExpressionEventMessage decode(FriendlyByteBuf buf) {
      return new FaunaExpressionEventMessage(buf.readVarInt(), buf.readUtf());
   }

   public static void handle(FaunaExpressionEventMessage msg, PacketContext ctx) {
      ctx.enqueueWork(() -> {
         if (Services.platform().isClient()) {
            ExpressionEventClient.accept(msg);
         }
      });
   }
}
