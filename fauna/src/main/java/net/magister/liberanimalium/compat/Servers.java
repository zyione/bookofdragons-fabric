package net.magister.liberanimalium.compat;

import net.magister.liberanimalium.compat.v20.ServersImpl;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

public final class Servers {
   private Servers() {
   }

   public static float averageTickMillis(MinecraftServer server) {
      return ServersImpl.averageTickMillis(server);
   }

   public static boolean hasAdvancement(ServerPlayer player, ResourceLocation id) {
      return ServersImpl.hasAdvancement(player, id);
   }
}
