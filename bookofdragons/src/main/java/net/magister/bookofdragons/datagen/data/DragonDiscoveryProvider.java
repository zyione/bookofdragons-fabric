package net.magister.bookofdragons.datagen.data;

import java.util.List;
import java.util.stream.Collectors;
import net.magister.bookofdragons.discovery.DiscoveryStore;
import net.magister.bookofdragons.network.ControlNetwork;
import net.magister.bookofdragons.network.message.SyncDiscoveryDataMessage;
import net.magister.bookofdragons.network.message.SyncTamedDragonsMessage;
import net.magister.liberanimalium.body.stats.BondLevel;
import net.magister.liberanimalium.platform.event.CommonEvents;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

public class DragonDiscoveryProvider {
   public static void init() {
      CommonEvents.PLAYER_RESPAWN.register(DragonDiscoveryProvider::onPlayerRespawn);
      CommonEvents.PLAYER_LOGIN.register(DragonDiscoveryProvider::onPlayerLogin);
   }

   public static void onPlayerRespawn(ServerPlayer serverPlayer) {
      DiscoveryStore.get(serverPlayer).ifPresent(data -> ControlNetwork.sendToPlayer(new SyncDiscoveryDataMessage(data), serverPlayer));
      syncTamedDragons(serverPlayer);
   }

   public static void onPlayerLogin(ServerPlayer serverPlayer) {
      DiscoveryStore.get(serverPlayer).ifPresent(data -> ControlNetwork.sendToPlayer(new SyncDiscoveryDataMessage(data), serverPlayer));
      syncTamedDragons(serverPlayer);
   }

   public static void syncTamedDragons(ServerPlayer serverPlayer) {
      MinecraftServer server = serverPlayer.getServer();
      if (server != null) {
         int BONDED_THRESHOLD = BondLevel.BONDED.getMinAffection();
         List<SyncTamedDragonsMessage.TamedDragonRecord> records = DragonRegistryData.get(server)
            .getDragonsByOwner(serverPlayer.getUUID())
            .stream()
            .map(
               e -> new SyncTamedDragonsMessage.TamedDragonRecord(
                  e.getDragonUUID(),
                  e.getCustomName(),
                  e.getAffection() >= BONDED_THRESHOLD,
                  e.getAffection() >= 1000,
                  e.getLastKnownPos().getX(),
                  e.getLastKnownPos().getZ(),
                  e.getType().name(),
                  e.getAffection(),
                  e.getTamedAt()
               )
            )
            .collect(Collectors.toList());
         ControlNetwork.sendToPlayer(new SyncTamedDragonsMessage(records), serverPlayer);
      }
   }
}
