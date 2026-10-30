package net.magister.bookofdragons.network.message;

import net.magister.liberanimalium.platform.Services;
import net.magister.liberanimalium.platform.net.PacketContext;
import net.minecraft.network.FriendlyByteBuf;

public class BookStripMessage {
   private final int dragonId;
   private final String kind;
   private final String act;

   public BookStripMessage(int dragonId, String kind, String act) {
      this.dragonId = dragonId;
      this.kind = kind;
      this.act = act;
   }

   public static void encode(BookStripMessage message, FriendlyByteBuf buffer) {
      buffer.writeVarInt(message.dragonId);
      buffer.writeUtf(message.kind);
      buffer.writeUtf(message.act);
   }

   public static BookStripMessage decode(FriendlyByteBuf buffer) {
      return new BookStripMessage(buffer.readVarInt(), buffer.readUtf(), buffer.readUtf());
   }

   public static void handle(BookStripMessage message, PacketContext context) {
      context.enqueueWork(() -> {
         if (Services.platform().isClient()) {
            ClientPacketHandler.handleBookStrip(message.dragonId, message.kind, message.act);
         }
      });
   }
}
