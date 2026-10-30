package net.magister.bookofdragons.discovery;

import net.magister.bookofdragons.advancement.DragonMilestones;
import net.magister.bookofdragons.datagen.data.DragonDiscoveryData;
import net.magister.bookofdragons.entity.data.DragonType;
import net.magister.bookofdragons.network.ControlNetwork;
import net.magister.bookofdragons.network.message.DiscoveryLevelUpdateMessage;
import net.magister.bookofdragons.network.message.SyncDiscoveryDataMessage;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;

public class DiscoveryManager {
   public static void markEncountered(Player player, DragonType dragonType) {
      if (!player.level().isClientSide()) {
         DiscoveryStore.get(player).ifPresent(data -> {
            if (!data.hasEncountered(dragonType)) {
               data.markEncountered(dragonType);
               syncToClient((ServerPlayer)player, dragonType, data);
               DragonMilestones.reach(player, "met", dragonType);
            }
         });
      }
   }

   public static void markKilled(Player player, DragonType dragonType) {
      if (!player.level().isClientSide()) {
         DiscoveryStore.get(player).ifPresent(data -> {
            if (!data.hasKilled(dragonType)) {
               data.markKilled(dragonType);
               syncToClient((ServerPlayer)player, dragonType, data);
            }
         });
      }
   }

   public static void markTamed(Player player, DragonType dragonType) {
      if (!player.level().isClientSide()) {
         DiscoveryStore.get(player).ifPresent(data -> {
            if (!data.hasTamed(dragonType)) {
               data.markTamed(dragonType);
               syncToClient((ServerPlayer)player, dragonType, data);
            }
         });
      }
   }

   public static void markBonded(Player player, DragonType dragonType) {
      if (!player.level().isClientSide()) {
         DiscoveryStore.get(player).ifPresent(data -> {
            if (!data.isBonded(dragonType) && data.hasTamed(dragonType)) {
               data.markBonded(dragonType);
               syncToClient((ServerPlayer)player, dragonType, data);
            }
         });
      }
   }

   public static void markVariantSeen(Player player, String variant) {
      if (!player.level().isClientSide()) {
         DiscoveryStore.get(player).ifPresent(data -> {
            if (data.markVariantSeen(variant)) {
               ControlNetwork.sendToPlayer(new SyncDiscoveryDataMessage(data), (ServerPlayer)player);
               DragonMilestones.reach(player, "variant", (DragonType)null);
            }
         });
      }
   }

   public static void learn(Player player, DragonType dragonType, String fact) {
      if (!player.level().isClientSide()) {
         if (Lore.all(dragonType.getSerializedName()).contains(fact)) {
            DiscoveryStore.get(player).ifPresent(data -> {
               if (data.learn(dragonType, fact)) {
                  ControlNetwork.sendToPlayer(new SyncDiscoveryDataMessage(data), (ServerPlayer)player);
                  DragonMilestones.wrote(player, data, dragonType);
               }
            });
         }
      }
   }

   private static void syncToClient(ServerPlayer player, DragonType dragonType, DragonDiscoveryData data) {
      DiscoveryLevelUpdateMessage message = new DiscoveryLevelUpdateMessage(
         dragonType, data.hasEncountered(dragonType), data.hasKilled(dragonType), data.hasTamed(dragonType), data.isBonded(dragonType)
      );
      ControlNetwork.sendToPlayer(message, player);
   }

   public static DiscoveryLevel getDiscoveryLevel(Player player, DragonType dragonType) {
      return DiscoveryStore.get(player).map(data -> data.getDiscoveryLevel(dragonType)).orElse(DiscoveryLevel.UNDISCOVERED);
   }

   public static PortraitVariant getDisplayPortrait(Player player, DragonType dragonType) {
      return DiscoveryStore.get(player).map(data -> data.getDisplayPortrait(dragonType)).orElse(PortraitVariant.UNDISCOVERED);
   }
}
