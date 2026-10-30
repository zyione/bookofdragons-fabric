package net.magister.bookofdragons.event;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.function.Consumer;
import net.magister.bookofdragons.entity.ModEntities;
import net.magister.bookofdragons.entity.custom.HooliganDrifterEntity;
import net.magister.bookofdragons.entity.custom.dragon.DeadlynadderEntity;
import net.magister.bookofdragons.entity.custom.dragon.GronckleEntity;
import net.magister.bookofdragons.entity.custom.dragon.HiddeousZipplebackEntity;
import net.magister.bookofdragons.entity.custom.dragon.MonstrousnightmareEntity;
import net.magister.bookofdragons.entity.custom.dragon.NightfuryEntity;
import net.magister.bookofdragons.entity.custom.dragon.SkrillEntity;
import net.magister.bookofdragons.entity.custom.dragon.SpeedStingerEntity;
import net.magister.bookofdragons.entity.custom.dragon.TerribleTerrorEntity;
import net.magister.bookofdragons.entity.custom.dragon.WhisperingDeathEntity;
import net.magister.bookofdragons.entity.custom.toy.PlushSkins;
import net.magister.bookofdragons.entity.data.PartWounds;
import net.magister.liberanimalium.compat.Packs;
import net.magister.liberanimalium.genetics.core.GeneticsCoreProvider;
import net.magister.liberanimalium.network.DataSync;
import net.magister.liberanimalium.platform.Services;
import net.magister.liberanimalium.platform.event.CommonEvents;
import net.magister.liberanimalium.platform.event.CommonEvents.ReloadListenerSink;
import net.magister.liberanimalium.platform.world.GameRegistrar.AttributeSink;
import net.magister.liberanimalium.platform.world.GameRegistrar.PackSourceSink;
import net.magister.liberanimalium.platform.world.GameRegistrar.SpawnPlacementSink;
import net.minecraft.network.chat.Component;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.repository.Pack;
import net.minecraft.server.packs.repository.PackSource;
import net.minecraft.server.packs.repository.Pack.Position;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.levelgen.Heightmap.Types;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ModEvents {
   private static final Logger LOGGER = LoggerFactory.getLogger(ModEvents.class);
   private static final String GENETICS_PACKS_FOLDER = "BODGeneticsPacks";

   public static void init() {
      CommonEvents.PACK_SOURCES.register(ModEvents::onAddPackFinders);
      CommonEvents.RELOAD_LISTENERS.register((Consumer<ReloadListenerSink>)sink -> {
         sink.addListener(new PartWounds());
         sink.addListener(new PlushSkins());
      });
      DataSync.register("bookofdragons:plush_skins", PlushSkins::raw, PlushSkins::install);
      CommonEvents.ENTITY_ATTRIBUTES.register(ModEvents::entityAttributeEvent);
      CommonEvents.SPAWN_PLACEMENTS.register(ModEvents::registerSpawnPlacements);
   }

   public static void onAddPackFinders(PackSourceSink event) {
      Path packsDir = Services.platform().gameDir().resolve("BODGeneticsPacks");

      try {
         Files.createDirectories(packsDir);
      } catch (IOException e) {
         LOGGER.error("[BookOfDragons] Could not create {} folder: {}", "BODGeneticsPacks", e.getMessage());
         return;
      }

      PackType packType = event.getPackType();
      event.addRepositorySource(
         consumer -> {
            try {
               Packs.discover(
                  packsDir,
                  (packPath, supplier) -> {
                     String packId = "dragon_genetics:" + packPath.getFileName().toString();
                     Pack pack = Packs.create(
                        packId, Component.literal(packPath.getFileName().toString()), true, supplier, packType, Position.TOP, PackSource.DEFAULT
                     );
                     if (pack != null) {
                        consumer.accept(pack);
                        LOGGER.debug("[BookOfDragons] Registered genetics pack: {} ({})", packId, packType);
                     }
                  }
               );
            } catch (IOException e) {
               LOGGER.error("[BookOfDragons] Error scanning {} folder: {}", "BODGeneticsPacks", e.getMessage());
            }
         }
      );
      if (packType == PackType.CLIENT_RESOURCES) {
         event.addRepositorySource(consumer -> {
            Pack pack = GeneticsCoreProvider.pack("bookofdragons");
            if (pack != null) {
               consumer.accept(pack);
            }
         });
      }
   }

   public static void entityAttributeEvent(AttributeSink event) {
      event.put((EntityType)ModEntities.NIGHTFURY.get(), NightfuryEntity.setAttributes().build());
      event.put((EntityType)ModEntities.DEADLYNADDER.get(), DeadlynadderEntity.setAttributes().build());
      event.put((EntityType)ModEntities.MONSTROUSNIGHTMARE.get(), MonstrousnightmareEntity.setAttributes().build());
      event.put((EntityType)ModEntities.GRONCKLE.get(), GronckleEntity.setAttributes().build());
      event.put((EntityType)ModEntities.HIDDEOUS_ZIPPLEBACK.get(), HiddeousZipplebackEntity.setAttributes().build());
      event.put((EntityType)ModEntities.SKRILL.get(), SkrillEntity.setAttributes().build());
      event.put((EntityType)ModEntities.WHISPERING_DEATH.get(), WhisperingDeathEntity.setAttributes().build());
      event.put((EntityType)ModEntities.TERRIBLE_TERROR.get(), TerribleTerrorEntity.setAttributes().build());
      event.put((EntityType)ModEntities.SPEED_STINGER.get(), SpeedStingerEntity.setAttributes().build());
      event.put((EntityType)ModEntities.HOOLIGAN_DRIFTER.get(), HooliganDrifterEntity.createAttributes().build());
      event.put((EntityType)ModEntities.SHEEP_PLUSH.get(), LivingEntity.createLivingAttributes().build());
      event.put((EntityType)ModEntities.FURY_PLUSH.get(), LivingEntity.createLivingAttributes().build());
      event.put((EntityType)ModEntities.FISH_PLUSH.get(), LivingEntity.createLivingAttributes().build());
      event.put((EntityType)ModEntities.STEAK_PLUSH.get(), LivingEntity.createLivingAttributes().build());
      event.put((EntityType)ModEntities.BALL_PLUSH.get(), LivingEntity.createLivingAttributes().build());
      event.put((EntityType)ModEntities.TERROR_PLUSH.get(), LivingEntity.createLivingAttributes().build());
      event.put((EntityType)ModEntities.NIGHTMARE_PLUSH.get(), LivingEntity.createLivingAttributes().build());
   }

   public static void registerSpawnPlacements(SpawnPlacementSink event) {
      event.registerOnGround((EntityType)ModEntities.NIGHTFURY.get(), Types.MOTION_BLOCKING_NO_LEAVES, Mob::checkMobSpawnRules);
      event.registerOnGround((EntityType)ModEntities.DEADLYNADDER.get(), Types.MOTION_BLOCKING_NO_LEAVES, Mob::checkMobSpawnRules);
      event.registerOnGround((EntityType)ModEntities.MONSTROUSNIGHTMARE.get(), Types.MOTION_BLOCKING_NO_LEAVES, Mob::checkMobSpawnRules);
      event.registerOnGround((EntityType)ModEntities.GRONCKLE.get(), Types.MOTION_BLOCKING_NO_LEAVES, Mob::checkMobSpawnRules);
      event.registerOnGround((EntityType)ModEntities.HIDDEOUS_ZIPPLEBACK.get(), Types.MOTION_BLOCKING_NO_LEAVES, Mob::checkMobSpawnRules);
      event.registerOnGround((EntityType)ModEntities.WHISPERING_DEATH.get(), Types.MOTION_BLOCKING_NO_LEAVES, Mob::checkMobSpawnRules);
      event.registerOnGround((EntityType)ModEntities.TERRIBLE_TERROR.get(), Types.MOTION_BLOCKING_NO_LEAVES, Mob::checkMobSpawnRules);
      event.registerOnGround((EntityType)ModEntities.SPEED_STINGER.get(), Types.MOTION_BLOCKING_NO_LEAVES, Mob::checkMobSpawnRules);
   }
}
