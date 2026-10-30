package net.magister.bookofdragons.event;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.magister.bookofdragons.advancement.DragonMilestones;
import net.magister.bookofdragons.discovery.DiscoveryManager;
import net.magister.bookofdragons.discovery.DiscoveryStore;
import net.magister.bookofdragons.discovery.EncounterDetectionSystem;
import net.magister.bookofdragons.discovery.Witness;
import net.magister.bookofdragons.entity.base.dragon.DragonBase;
import net.magister.bookofdragons.entity.data.DragonType;
import net.magister.liberanimalium.body.stats.BondLevel;
import net.magister.liberanimalium.chassis.FaunaTick;
import net.magister.liberanimalium.platform.event.CommonEvents;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

public class DragonDiscoveryEventHandler {
   private static final Map<UUID, Map<UUID, Long>> encounterCheckTimestamps = new HashMap<>();
   private static final long CHECK_COOLDOWN = 20L;

   public static void init() {
      FaunaTick.LIVING.register(DragonDiscoveryEventHandler::onLivingTick);
      CommonEvents.LIVING_DIED.register(DragonDiscoveryEventHandler::onLivingDeath);
      CommonEvents.ANIMAL_TAMED.register(DragonDiscoveryEventHandler::onAnimalTame);
      CommonEvents.ENTITY_LEAVE.register(DragonDiscoveryEventHandler::onEntityLeave);
      CommonEvents.PLAYER_LOGOUT.register(DragonDiscoveryEventHandler::onPlayerLeave);
   }

   public static void onLivingTick(LivingEntity entity) {
      if (!entity.level().isClientSide() && entity instanceof DragonBase dragon) {
         Level level = entity.level();

         for (Player player : level.players()) {
            if (player instanceof ServerPlayer serverPlayer) {
               checkEncounterForPlayer(serverPlayer, dragon);
            }
         }
      }
   }

   private static void checkEncounterForPlayer(ServerPlayer player, DragonBase dragon) {
      UUID playerUUID = player.getUUID();
      UUID dragonUUID = dragon.getUUID();
      long currentTime = player.level().getGameTime();
      Map<UUID, Long> playerChecks = encounterCheckTimestamps.computeIfAbsent(playerUUID, k -> new HashMap<>());
      Long lastCheck = playerChecks.get(dragonUUID);
      if (lastCheck == null || currentTime - lastCheck >= 20L) {
         playerChecks.put(dragonUUID, currentTime);
         if (dragon.isTame() && playerUUID.equals(dragon.getOwnerUUID()) && !DiscoveryStore.get(player).map(d -> d.hasTamed(dragon.getDragonType())).orElse(true)
            )
          {
            onDragonTamed(player, dragon);
         }

         if (dragon.isTame()
            && playerUUID.equals(dragon.getOwnerUUID())
            && dragon.getBond(playerUUID) >= BondLevel.BONDED.getMinAffection()
            && !DiscoveryStore.get(player).map(d -> d.isBonded(dragon.getDragonType())).orElse(true)) {
            onDragonBonded(player, dragon);
         }

         if (EncounterDetectionSystem.shouldMarkEncountered(player, dragon)) {
            DragonType dragonType = EncounterDetectionSystem.getDragonType(dragon);
            DiscoveryManager.markEncountered(player, dragonType);
            ResourceLocation variant = dragon.variantId();
            if (variant != null) {
               DiscoveryManager.markVariantSeen(player, variant.toString());
            }

            String seen = Witness.seen(dragon);
            if (seen != null && player.hasLineOfSight(dragon)) {
               DiscoveryManager.learn(player, dragonType, seen);
            }
         }
      }
   }

   public static void onLivingDeath(LivingEntity entity, DamageSource source) {
      if (!entity.level().isClientSide() && entity instanceof DragonBase dragon) {
         if (source.getEntity() instanceof ServerPlayer player) {
            DragonType dragonType = EncounterDetectionSystem.getDragonType(dragon);
            DiscoveryManager.markKilled(player, dragonType);
         }
      }
   }

   public static void onAnimalTame(Animal animal, Player player) {
      if (animal instanceof DragonBase dragon) {
         if (!player.level().isClientSide()) {
            onDragonTamed(player, dragon);
         }
      }
   }

   public static void onDragonTamed(Player player, DragonBase dragon) {
      if (!player.level().isClientSide()) {
         DragonType dragonType = EncounterDetectionSystem.getDragonType(dragon);
         DiscoveryManager.markTamed(player, dragonType);
         DragonMilestones.reach(player, "tamed", dragonType);
      }
   }

   public static void onDragonBonded(ServerPlayer player, DragonBase dragon) {
      DragonType dragonType = EncounterDetectionSystem.getDragonType(dragon);
      DiscoveryManager.markBonded(player, dragonType);
      DragonMilestones.reach(player, "bonded", dragonType);
   }

   public static void forgetDragon(UUID dragonUUID) {
      for (Map<UUID, Long> checks : encounterCheckTimestamps.values()) {
         checks.remove(dragonUUID);
      }
   }

   public static void onEntityLeave(Entity entity, Level level) {
      if (!level.isClientSide()) {
         if (entity instanceof DragonBase dragon) {
            forgetDragon(dragon.getUUID());
         }
      }
   }

   public static void onPlayerLeave(Player player) {
      cleanupPlayerData(player.getUUID());
   }

   public static void cleanupPlayerData(UUID playerUUID) {
      encounterCheckTimestamps.remove(playerUUID);
   }
}
