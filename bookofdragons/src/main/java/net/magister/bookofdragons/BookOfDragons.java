package net.magister.bookofdragons;

import com.mojang.logging.LogUtils;
import java.util.Arrays;
import net.magister.bookofdragons.advancement.ModAdvancementTriggers;
import net.magister.bookofdragons.client.event.ClientEvents;
import net.magister.bookofdragons.client.event.ClientForgeEvents;
import net.magister.bookofdragons.client.event.CommandWheelInputHandler;
import net.magister.bookofdragons.client.particle.BlastWaveParticle;
import net.magister.bookofdragons.client.particle.FireballParticle;
import net.magister.bookofdragons.client.render.dragon.DeadlyNadderRenderer;
import net.magister.bookofdragons.client.render.dragon.GronckleRenderer;
import net.magister.bookofdragons.client.render.dragon.HiddeousZipplebackRenderer;
import net.magister.bookofdragons.client.render.dragon.MonstrousNightmareRenderer;
import net.magister.bookofdragons.client.render.dragon.NightfuryRenderer;
import net.magister.bookofdragons.client.render.dragon.SkrillRenderer;
import net.magister.bookofdragons.client.render.dragon.SpeedStingerRenderer;
import net.magister.bookofdragons.client.render.dragon.TerribleTerrorRenderer;
import net.magister.bookofdragons.client.render.dragon.WhisperingDeathRenderer;
import net.magister.bookofdragons.client.render.misc.PlushRenderer;
import net.magister.bookofdragons.client.render.projectile.SimpleProjectileRenderer;
import net.magister.bookofdragons.config.DragonSettings;
import net.magister.bookofdragons.datagen.data.DragonDiscoveryProvider;
import net.magister.bookofdragons.entity.ModEntities;
import net.magister.bookofdragons.entity.base.dragon.AskReading;
import net.magister.bookofdragons.entity.base.dragon.DragonChecks;
import net.magister.bookofdragons.entity.custom.toy.PlushKind;
import net.magister.bookofdragons.entity.genetics.data.GeneticsSequenceData;
import net.magister.bookofdragons.event.DragonDiscoveryEventHandler;
import net.magister.bookofdragons.event.DrifterSpawnHandler;
import net.magister.bookofdragons.event.ModEvents;
import net.magister.bookofdragons.event.ModForgeEvents;
import net.magister.bookofdragons.event.ServerForgeEvents;
import net.magister.bookofdragons.item.custom.PlushItem;
import net.magister.bookofdragons.item.custom.WhistleStable;
import net.magister.bookofdragons.network.ControlNetwork;
import net.magister.bookofdragons.network.Strip;
import net.magister.bookofdragons.particle.ModParticles;
import net.magister.liberanimalium.body.SpeciesAssets;
import net.magister.liberanimalium.chassis.Notices;
import net.magister.liberanimalium.genetics.core.GeneticsSequenceBuffer;
import net.magister.liberanimalium.platform.Services;
import net.magister.liberanimalium.platform.client.ClientServices.ParticleSink;
import net.magister.liberanimalium.platform.client.ClientServices.RendererSink;
import net.magister.liberanimalium.progression.LockedRecipes;
import net.magister.liberanimalium.roost.Roosts;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.DispenserBlock;
import org.slf4j.Logger;

public final class BookOfDragons {
   public static final String MOD_ID = "bookofdragons";
   private static final Logger LOGGER = LogUtils.getLogger();

   private BookOfDragons() {
   }

   public static void init() {
      SpeciesAssets.host("bookofdragons", BookOfDragons.class);
      GeneticsSequenceBuffer.install("bookofdragons", GeneticsSequenceData.seed(), () -> BookOfDragons.class.getResourceAsStream("/bookofdragons.core"));
      Roosts.announce(ResourceKey.create(Registries.POINT_OF_INTEREST_TYPE, new ResourceLocation("bookofdragons", "roost")));
      DragonSettings.install();
      Notices.sink(Strip::say);
      LOGGER.debug("Configuration system initialized successfully!");
      DragonChecks.register();
      DragonDiscoveryProvider.init();
      DragonDiscoveryEventHandler.init();
      DrifterSpawnHandler.init();
      ModEvents.init();
      ModForgeEvents.init();
      ServerForgeEvents.init();

   }

   public static void clientInit() {
         BookOfDragons.ClientModEvents.clientInit();
         ClientEvents.ClientModBusEvents.clientInit();
         ClientForgeEvents.clientInit();
         CommandWheelInputHandler.clientInit();

   }

   public static void setup() {
      ControlNetwork.register();
      ModAdvancementTriggers.register();
      WhistleStable.init();
      AskReading.init();

      for (Item item : BuiltInRegistries.ITEM) {
         if (item instanceof PlushItem) {
            DispenserBlock.registerBehavior(item, PlushItem.DISPENSE);
         }
      }

      LockedRecipes.lock(id -> id.getNamespace().equals("bookofdragons") && Arrays.stream(PlushKind.values()).anyMatch(kind -> kind.id().equals(id.getPath())));
   }

   public static class ClientModEvents {
      public static void clientInit() {
         net.magister.liberanimalium.platform.event.ClientEvents.ENTITY_RENDERERS.register(BookOfDragons.ClientModEvents::onRegisterRenderers);
         net.magister.liberanimalium.platform.event.ClientEvents.PARTICLE_PROVIDERS.register(BookOfDragons.ClientModEvents::onRegisterParticleProviders);
      }

      public static void onRegisterRenderers(RendererSink event) {
         event.registerEntityRenderer((EntityType)ModEntities.NIGHTFURY.get(), NightfuryRenderer::new);
         event.registerEntityRenderer((EntityType)ModEntities.DEADLYNADDER.get(), DeadlyNadderRenderer::new);
         event.registerEntityRenderer((EntityType)ModEntities.MONSTROUSNIGHTMARE.get(), MonstrousNightmareRenderer::new);
         event.registerEntityRenderer((EntityType)ModEntities.GRONCKLE.get(), GronckleRenderer::new);
         event.registerEntityRenderer((EntityType)ModEntities.HIDDEOUS_ZIPPLEBACK.get(), HiddeousZipplebackRenderer::new);
         event.registerEntityRenderer((EntityType)ModEntities.SKRILL.get(), SkrillRenderer::new);
         event.registerEntityRenderer((EntityType)ModEntities.WHISPERING_DEATH.get(), WhisperingDeathRenderer::new);
         event.registerEntityRenderer((EntityType)ModEntities.TERRIBLE_TERROR.get(), TerribleTerrorRenderer::new);
         event.registerEntityRenderer((EntityType)ModEntities.SPEED_STINGER.get(), SpeedStingerRenderer::new);
         event.registerEntityRenderer((EntityType)ModEntities.SPIKE_PROJECTILE.get(), SimpleProjectileRenderer::new);
         event.registerEntityRenderer((EntityType)ModEntities.SHEEP_PLUSH.get(), context -> new PlushRenderer(context, PlushKind.SHEEP));
         event.registerEntityRenderer((EntityType)ModEntities.FURY_PLUSH.get(), context -> new PlushRenderer(context, PlushKind.FURY));
         event.registerEntityRenderer((EntityType)ModEntities.FISH_PLUSH.get(), context -> new PlushRenderer(context, PlushKind.FISH));
         event.registerEntityRenderer((EntityType)ModEntities.STEAK_PLUSH.get(), context -> new PlushRenderer(context, PlushKind.STEAK));
         event.registerEntityRenderer((EntityType)ModEntities.BALL_PLUSH.get(), context -> new PlushRenderer(context, PlushKind.BALL));
         event.registerEntityRenderer((EntityType)ModEntities.TERROR_PLUSH.get(), context -> new PlushRenderer(context, PlushKind.TERROR));
         event.registerEntityRenderer((EntityType)ModEntities.NIGHTMARE_PLUSH.get(), context -> new PlushRenderer(context, PlushKind.NIGHTMARE));
      }

      public static void onRegisterParticleProviders(ParticleSink event) {
         event.registerSpriteSet((ParticleType)ModParticles.BLASTWAVE.get(), BlastWaveParticle.Provider::new);
         event.registerSpriteSet((ParticleType)ModParticles.FIREBALL.get(), FireballParticle.Provider::new);
      }
   }
}
