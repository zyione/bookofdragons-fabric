package net.magister.liberanimalium.compat.v20;

import net.minecraft.advancements.Advancement;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

public final class ServersImpl {
   private ServersImpl() {
   }

   public static float averageTickMillis(MinecraftServer server) {
      return server.getAverageTickTime();
   }

   public static boolean hasAdvancement(ServerPlayer player, ResourceLocation id) {
      Advancement advancement = player.server.getAdvancements().getAdvancement(id);
      return advancement != null && player.getAdvancements().getOrStartProgress(advancement).isDone();
   }
}
