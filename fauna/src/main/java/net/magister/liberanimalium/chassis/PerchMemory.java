package net.magister.liberanimalium.chassis;

import net.magister.liberanimalium.platform.event.CommonEvents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;

public final class PerchMemory {
   private static final String KEY = "liberanimalium:perched";
   private static final int EVERY = 100;

   private PerchMemory() {
   }

   public static void init() {
      CommonEvents.SERVER_TICK_END.register(PerchMemory::tick);
      CommonEvents.PLAYER_LOGOUT.register(PerchMemory::forget);
      CommonEvents.PLAYER_LOGIN.register(PerchMemory::restore);
   }

   private static void tick(MinecraftServer server) {
      if (server.getTickCount() % 100 == 0) {
         for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            CompoundTag data = net.magister.liberanimalium.platform.fabric.EntityData.get(player);
            CompoundTag pet = new CompoundTag();
            if (player.getFirstPassenger() instanceof FaunaBase perched && perched.saveAsPassenger(pet)) {
               data.put("liberanimalium:perched", pet);
            } else {
               data.remove("liberanimalium:perched");
            }
         }
      }
   }

   private static void forget(ServerPlayer player) {
      net.magister.liberanimalium.platform.fabric.EntityData.get(player).remove("liberanimalium:perched");
   }

   private static void restore(ServerPlayer player) {
      CompoundTag data = net.magister.liberanimalium.platform.fabric.EntityData.get(player);
      if (data.contains("liberanimalium:perched")) {
         CompoundTag pet = data.getCompound("liberanimalium:perched");
         data.remove("liberanimalium:perched");
         ServerLevel level = player.serverLevel();
         Entity body = EntityType.loadEntityRecursive(pet, level, loaded -> {
            loaded.moveTo(player.getX(), player.getY(), player.getZ(), player.getYRot(), 0.0F);
            return loaded;
         });
         if (body != null && level.getEntity(body.getUUID()) == null) {
            level.addFreshEntity(body);
         }
      }
   }
}
