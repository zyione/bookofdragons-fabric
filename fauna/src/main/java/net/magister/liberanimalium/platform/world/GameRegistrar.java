package net.magister.liberanimalium.platform.world;

import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.repository.RepositorySource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.SpawnPlacements.SpawnPredicate;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.npc.VillagerProfession;
import net.minecraft.world.entity.npc.VillagerTrades.ItemListing;
import net.minecraft.world.level.levelgen.Heightmap.Types;

public final class GameRegistrar {
   private GameRegistrar() {
   }

   @FunctionalInterface
   public interface AttributeSink {
      void put(EntityType<? extends LivingEntity> var1, AttributeSupplier var2);
   }

   public interface PackSourceSink {
      PackType getPackType();

      void addRepositorySource(RepositorySource var1);
   }

   public interface SpawnPlacementSink {
      <T extends Mob> void registerOnGround(EntityType<T> var1, Types var2, SpawnPredicate<T> var3);
   }

   @FunctionalInterface
   public interface TradeSink {
      void add(VillagerProfession var1, int var2, ItemListing var3);
   }
}
