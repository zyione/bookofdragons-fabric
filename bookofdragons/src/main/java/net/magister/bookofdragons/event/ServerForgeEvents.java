package net.magister.bookofdragons.event;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.WeakHashMap;
import net.magister.bookofdragons.entity.ModEntities;
import net.magister.bookofdragons.entity.custom.dragon.SkrillEntity;
import net.magister.liberanimalium.attack.light.LightScribeData;
import net.magister.liberanimalium.compat.Entities;
import net.magister.liberanimalium.platform.event.CommonEvents;
import net.magister.liberanimalium.platform.event.Event.Priority;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.chunk.ChunkAccess;

public class ServerForgeEvents {
   private static final Set<ServerLevel> LIGHT_BOOKS_ABSORBED = Collections.synchronizedSet(Collections.newSetFromMap(new WeakHashMap<>()));
   private static final double SKRILL_LIGHTNING_SPAWN_CHANCE = 0.0025;
   private static final Set<UUID> processedLightning = new HashSet<>();

   public static void init() {
      CommonEvents.ENTITY_JOIN.register(ServerForgeEvents::onEntityJoinLevel);
      CommonEvents.CHUNK_LOAD.register(Priority.HIGHEST, ServerForgeEvents::absorbFormerLightBook);
   }

   private static void absorbFormerLightBook(LevelAccessor accessor, ChunkAccess chunk) {
      if (accessor instanceof ServerLevel level && LIGHT_BOOKS_ABSORBED.add(level)) {
         LightScribeData.absorb(level, "bookofdragons_lights");
      }
   }

   public static void onEntityJoinLevel(Entity entity, Level level, boolean loadedFromDisk) {
      if (entity instanceof LightningBolt lightning && level instanceof ServerLevel serverLevel) {
         if (serverLevel.isThundering()) {
            UUID lightningId = lightning.getUUID();
            if (!processedLightning.contains(lightningId)) {
               processedLightning.add(lightningId);
               if (processedLightning.size() > 100) {
                  processedLightning.clear();
               }

               if (serverLevel.random.nextDouble() < 0.0025) {
                  SkrillEntity skrill = (SkrillEntity)((EntityType)ModEntities.SKRILL.get()).create(serverLevel);
                  if (skrill != null) {
                     skrill.moveTo(lightning.getX(), lightning.getY(), lightning.getZ(), serverLevel.random.nextFloat() * 360.0F, 0.0F);
                     Entities.finalizeSpawn(skrill, serverLevel, serverLevel.getCurrentDifficultyAt(skrill.blockPosition()), MobSpawnType.EVENT);
                     skrill.setGrowthStage(2);
                     serverLevel.addFreshEntity(skrill);
                  }
               }
            }
         }
      }
   }
}
