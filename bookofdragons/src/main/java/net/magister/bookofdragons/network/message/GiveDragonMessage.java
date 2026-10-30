package net.magister.bookofdragons.network.message;

import java.util.UUID;
import net.magister.bookofdragons.entity.base.dragon.DragonBase;
import net.magister.bookofdragons.network.Strip;
import net.magister.liberanimalium.platform.net.PacketContext;
import net.minecraft.ChatFormatting;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

public class GiveDragonMessage {
   public static final double GIVE_RANGE = 16.0;
   private final UUID dragonUUID;
   private final UUID recipientUUID;

   public GiveDragonMessage(UUID dragonUUID, UUID recipientUUID) {
      this.dragonUUID = dragonUUID;
      this.recipientUUID = recipientUUID;
   }

   public static void encode(GiveDragonMessage message, FriendlyByteBuf buffer) {
      buffer.writeUUID(message.dragonUUID);
      buffer.writeUUID(message.recipientUUID);
   }

   public static GiveDragonMessage decode(FriendlyByteBuf buffer) {
      return new GiveDragonMessage(buffer.readUUID(), buffer.readUUID());
   }

   public static void handle(GiveDragonMessage message, PacketContext context) {
      context.enqueueWork(
         () -> {
            ServerPlayer player = context.sender();
            DragonBase dragon = ReleaseDragonMessage.ownedNear(player, message.dragonUUID);
            if (dragon != null) {
               MinecraftServer server = player.getServer();
               ServerPlayer to = server == null ? null : server.getPlayerList().getPlayer(message.recipientUUID);
               double reach = 20.0;
               if (to != null && to != player && !to.isSpectator() && to.level() == player.level() && !(to.distanceToSqr(player) > reach * reach)) {
                  Component name = dragon.getDisplayName();
                  dragon.transferTo(to);
                  Strip.say(player, Component.translatable("book.bookofdragons.dragon.given", new Object[]{name, to.getDisplayName()}).withStyle(ChatFormatting.GOLD));
                  to.displayClientMessage(
                     Component.translatable("book.bookofdragons.dragon.received", new Object[]{player.getDisplayName(), name}).withStyle(ChatFormatting.GOLD), false
                  );
               } else {
                  Strip.say(player, Component.translatable("book.bookofdragons.dragon.give_gone").withStyle(ChatFormatting.RED));
               }
            }
         }
      );
   }
}
