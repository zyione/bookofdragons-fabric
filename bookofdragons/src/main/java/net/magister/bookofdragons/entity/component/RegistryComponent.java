package net.magister.bookofdragons.entity.component;

import java.util.UUID;
import org.jetbrains.annotations.Nullable;
import net.magister.bookofdragons.datagen.data.DragonDiscoveryProvider;
import net.magister.bookofdragons.datagen.data.DragonRegistryData;
import net.magister.bookofdragons.datagen.data.DragonRegistryEntry;
import net.magister.bookofdragons.entity.base.dragon.DragonBase;
import net.magister.bookofdragons.entity.base.dragon.GrowthStage;
import net.magister.bookofdragons.entity.data.DragonNames;
import net.magister.liberanimalium.chassis.FaunaBase;
import net.magister.liberanimalium.chassis.component.IFaunaComponent;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

public class RegistryComponent implements IFaunaComponent {
   private static final int REGISTRY_UPDATE_INTERVAL = 200;
   protected DragonBase dragon;

   public void onAttach(FaunaBase host) {
      this.dragon = (DragonBase)host;
   }

   public void serverTick() {
      if (!this.dragon.level().isClientSide() && this.dragon.isTame() && (this.dragon.tickCount + this.dragon.getId()) % 200 == 0) {
         this.updateRegistryPosition();
      }
   }

   public void registerInRegistry() {
      if (!this.dragon.level().isClientSide() && this.dragon.isTame() && this.dragon.getOwnerUUID() != null) {
         MinecraftServer server = this.dragon.level().getServer();
         if (server != null) {
            DragonRegistryData registry = DragonRegistryData.get(server);
            DragonRegistryEntry entry = new DragonRegistryEntry(
               this.dragon.getUUID(),
               this.dragon.getOwnerUUID(),
               DragonNames.of(this.dragon),
               this.dragon.getDragonType(),
               GrowthStage.fromStageId(this.dragon.getGrowthStage()),
               this.dragon.blockPosition(),
               this.dragon.level().dimension(),
               this.dragon.getHealth(),
               this.dragon.getMaxHealth(),
               this.dragon.getBond(this.dragon.getOwnerUUID()),
               this.dragon.isSaddled(),
               this.dragon.hasChest()
            );
            entry.setGenetics((CompoundTag)this.dragon.getEntityData().get(FaunaBase.getGenotypeData()));
            entry.setTamedAt(this.dragon.level().getGameTime());
            registry.registerDragon(entry);
            ServerPlayer ownerPlayer = server.getPlayerList().getPlayer(this.dragon.getOwnerUUID());
            if (ownerPlayer != null) {
               DragonDiscoveryProvider.syncTamedDragons(ownerPlayer);
            }
         }
      }
   }

   public void updateRegistryPosition() {
      if (!this.dragon.level().isClientSide() && this.dragon.isTame() && this.dragon.getOwnerUUID() != null) {
         MinecraftServer server = this.dragon.level().getServer();
         if (server != null) {
            DragonRegistryData registry = DragonRegistryData.get(server);
            if (!registry.hasDragon(this.dragon.getUUID())) {
               this.registerInRegistry();
            } else {
               DragonRegistryEntry updatedEntry = new DragonRegistryEntry(
                  this.dragon.getUUID(),
                  this.dragon.getOwnerUUID(),
                  DragonNames.of(this.dragon),
                  this.dragon.getDragonType(),
                  GrowthStage.fromStageId(this.dragon.getGrowthStage()),
                  this.dragon.blockPosition(),
                  this.dragon.level().dimension(),
                  this.dragon.getHealth(),
                  this.dragon.getMaxHealth(),
                  this.dragon.getBond(this.dragon.getOwnerUUID()),
                  this.dragon.isSaddled(),
                  this.dragon.hasChest()
               );
               updatedEntry.setGenetics((CompoundTag)this.dragon.getEntityData().get(FaunaBase.getGenotypeData()));
               registry.updateDragon(this.dragon.getUUID(), updatedEntry);
            }
         }
      }
   }

   public void removeFromRegistry() {
      if (!this.dragon.level().isClientSide()) {
         MinecraftServer server = this.dragon.level().getServer();
         if (server != null) {
            DragonRegistryData registry = DragonRegistryData.get(server);
            registry.removeDragon(this.dragon.getUUID());
            if (this.dragon.isTame() && this.dragon.getOwnerUUID() != null) {
               ServerPlayer ownerPlayer = server.getPlayerList().getPlayer(this.dragon.getOwnerUUID());
               if (ownerPlayer != null) {
                  DragonDiscoveryProvider.syncTamedDragons(ownerPlayer);
               }
            }
         }
      }
   }

   public void removeFromRegistry(UUID formerOwner) {
      if (!this.dragon.level().isClientSide()) {
         MinecraftServer server = this.dragon.level().getServer();
         if (server != null) {
            DragonRegistryData.get(server).removeDragon(this.dragon.getUUID());
            syncTo(server, formerOwner);
         }
      }
   }

   public void handedOver(UUID formerOwner) {
      if (!this.dragon.level().isClientSide()) {
         MinecraftServer server = this.dragon.level().getServer();
         if (server != null) {
            this.updateRegistryPosition();
            syncTo(server, formerOwner);
            syncTo(server, this.dragon.getOwnerUUID());
         }
      }
   }

   private static void syncTo(MinecraftServer server, @Nullable UUID player) {
      if (player != null) {
         ServerPlayer online = server.getPlayerList().getPlayer(player);
         if (online != null) {
            DragonDiscoveryProvider.syncTamedDragons(online);
         }
      }
   }

   public void save(CompoundTag tag) {
   }

   public void load(CompoundTag tag) {
   }
}
