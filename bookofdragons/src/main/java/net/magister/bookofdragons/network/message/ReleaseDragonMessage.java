package net.magister.bookofdragons.network.message;

import java.util.UUID;
import org.jetbrains.annotations.Nullable;
import net.magister.bookofdragons.entity.base.dragon.DragonBase;
import net.magister.bookofdragons.network.Strip;
import net.magister.liberanimalium.platform.net.PacketContext;
import net.minecraft.ChatFormatting;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

public class ReleaseDragonMessage {
   static final double REACH_SQ = 144.0;
   private final UUID dragonUUID;

   public ReleaseDragonMessage(UUID dragonUUID) {
      this.dragonUUID = dragonUUID;
   }

   public static void encode(ReleaseDragonMessage message, FriendlyByteBuf buffer) {
      buffer.writeUUID(message.dragonUUID);
   }

   public static ReleaseDragonMessage decode(FriendlyByteBuf buffer) {
      return new ReleaseDragonMessage(buffer.readUUID());
   }

   public static void handle(ReleaseDragonMessage message, PacketContext context) {
      context.enqueueWork(() -> {
         ServerPlayer player = context.sender();
         DragonBase dragon = ownedNear(player, message.dragonUUID);
         if (dragon != null) {
            Component name = dragon.getDisplayName();
            dragon.releaseToWild();
            Strip.say(player, Component.translatable("book.bookofdragons.dragon.released", new Object[]{name}).withStyle(ChatFormatting.GOLD));
         }
      });
   }

   @Nullable
   static DragonBase ownedNear(@Nullable ServerPlayer player, UUID dragonUUID) {
      if (player == null) {
         return null;
      } else if (player.serverLevel().getEntity(dragonUUID) instanceof DragonBase dragon && dragon.isTame() && dragon.isOwnedBy(player)) {
         return dragon.distanceToSqr(player) <= 144.0 ? dragon : null;
      } else {
         return null;
      }
   }
}
