package net.magister.bookofdragons.network;

import net.magister.bookofdragons.network.message.ClientPacketHandler;
import net.magister.bookofdragons.network.message.StripLineMessage;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;

public final class Strip {
   private Strip() {
   }

   public static void say(Player who, Component line) {
      if (who instanceof ServerPlayer player) {
         ControlNetwork.sendToPlayer(new StripLineMessage(StripLineMessage.Text.of(line)), player);
      } else if (who.level().isClientSide()) {
         ClientPacketHandler.handleLocalStripLine(who, StripLineMessage.Text.of(line));
      }
   }
}
