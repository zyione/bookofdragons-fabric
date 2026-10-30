package net.magister.liberanimalium;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.logging.LogUtils;
import net.magister.liberanimalium.attack.AttackManager;
import net.magister.liberanimalium.attack.client.particle.AttackParticle;
import net.magister.liberanimalium.attack.client.particle.BreathPuffParticle;
import net.magister.liberanimalium.attack.client.particle.FaunaParticles;
import net.magister.liberanimalium.attack.client.render.ProjectileRenderer;
import net.magister.liberanimalium.attack.light.LightScribeEvents;
import net.magister.liberanimalium.body.BodyPlans;
import net.magister.liberanimalium.body.event.PartIndexEvents;
import net.magister.liberanimalium.body.gameplay.SpeciesGameplayLoader;
import net.magister.liberanimalium.body.grip.GripRules;
import net.magister.liberanimalium.body.physics.PhysicsSheetLoader;
import net.magister.liberanimalium.body.registry.FaunaEntities;
import net.magister.liberanimalium.chassis.PerchMemory;
import net.magister.liberanimalium.chassis.component.ranged.data.FaunaAttackManager;
import net.magister.liberanimalium.client.FaunaResourceReloadListener;
import net.magister.liberanimalium.client.FrameStages;
import net.magister.liberanimalium.client.LibraryClientEvents;
import net.magister.liberanimalium.debug.client.LegReachDebugRenderer;
import net.magister.liberanimalium.event.FaunaWorldEvents;
import net.magister.liberanimalium.event.PlayBow;
import net.magister.liberanimalium.fauna.api.SpeciesProfiles;
import net.magister.liberanimalium.fauna.core.PairSession;
import net.magister.liberanimalium.fauna.debug.FaunaPoseCommand;
import net.magister.liberanimalium.fauna.move.AsyncSolver;
import net.magister.liberanimalium.genetics.config.SpeciesGeneticsJsonLoader;
import net.magister.liberanimalium.network.FaunaNetwork;
import net.magister.liberanimalium.platform.Services;
import net.magister.liberanimalium.platform.client.ClientServices;
import net.magister.liberanimalium.platform.event.ClientEvents;
import net.magister.liberanimalium.platform.event.CommonEvents;
import net.magister.liberanimalium.render.BodyWatch;
import net.magister.liberanimalium.render.ChainWatch;
import net.magister.liberanimalium.render.gpu.FaunaShaders;
import net.magister.liberanimalium.render.gpu.SpeciesBatches;
import net.magister.liberanimalium.render.membrane.MembraneUvLoader;
import net.magister.liberanimalium.render.nametag.NametagSkinManager;
import net.magister.liberanimalium.render.vfx.SleepBubbleParticle;
import net.magister.liberanimalium.render.vfx.WaterSprayParticle;
import net.magister.liberanimalium.render.vfx.WingWashParticle;
import net.magister.liberanimalium.ride.client.RideClientEvents;
import net.magister.liberanimalium.ride.render.RideHandRenderer;
import net.magister.liberanimalium.ride.render.RiderPoseLoader;
import net.magister.liberanimalium.ride.touch.TouchHandRenderer;
import net.magister.liberanimalium.sound.FaunaSoundLoader;
import net.magister.liberanimalium.spawn.FaunaSpawner;
import net.magister.liberanimalium.spawn.SpawnLog;
import net.magister.liberanimalium.spawn.SpawnRules;
import net.magister.liberanimalium.spawn.SpawnSettingsLoader;
import net.magister.liberanimalium.util.NavStats;
import net.magister.liberanimalium.util.PerfClock;
import net.magister.liberanimalium.variant.CreatureVariants;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.packs.resources.ResourceManagerReloadListener;
import org.slf4j.Logger;

public final class LiberAnimalium {
   public static final String MOD_ID = "liberanimalium";
   public static final Logger LOGGER = LogUtils.getLogger();

   private LiberAnimalium() {
   }

   public static void init() {
      CommonEvents.SERVER_TICK_END.register(LiberAnimalium::onServerTick);
      CommonEvents.COMMANDS.register(LiberAnimalium::onRegisterCommands);
      CommonEvents.RELOAD_LISTENERS.register(LiberAnimalium::onAddReloadListeners);
      CommonEvents.SERVER_STOPPING.register(server -> {
         PairSession.clearAll();
         SpawnLog.clearAll();
      });
      CommonEvents.LEVEL_TICK_END.register(FaunaSpawner::onLevelTick);
      CommonEvents.LEVEL_TICK_END.register(PlayBow::onLevelTick);
      CommonEvents.PLAYER_LOGOUT.register(PlayBow::forget);
      boolean dev = !Services.platform().isProduction();
      LightScribeEvents.init();
      PartIndexEvents.init();
      FaunaWorldEvents.init();
      PerchMemory.init();

   }

   public static void clientInit() {      boolean dev = !Services.platform().isProduction();

         FrameStages.clientInit();
         LibraryClientEvents.clientInit();
         if (dev) {
            LegReachDebugRenderer.clientInit();
         }

         LiberAnimalium.ClientModEvents.clientInit();
         LiberAnimalium.ClientPerf.clientInit();
         BodyWatch.clientInit();
         ChainWatch.clientInit();
         FaunaShaders.clientInit();
         SpeciesBatches.clientInit();
         RideClientEvents.clientInit();
         TouchHandRenderer.clientInit();
         RideHandRenderer.clientInit();

   }

   public static void setup() {
      FaunaNetwork.register();
   }

   public static void onServerTick(MinecraftServer server) {
      AsyncSolver.drain();
      PerfClock.rollServer();
      NavStats.roll();
   }

   public static void onRegisterCommands(CommandDispatcher<CommandSourceStack> dispatcher, CommandBuildContext context) {
      FaunaPoseCommand.register(dispatcher);
   }

   public static void onAddReloadListeners(CommonEvents.ReloadListenerSink listeners) {
      listeners.addListener(new SpeciesGeneticsJsonLoader());
      listeners.addListener(new FaunaAttackManager());
      listeners.addListener(new AttackManager());
      listeners.addListener(new SpeciesProfiles());
      listeners.addListener(new PhysicsSheetLoader());
      listeners.addListener(new GripRules.Loader());
      listeners.addListener(new SpeciesGameplayLoader());
      listeners.addListener((ResourceManagerReloadListener)manager -> BodyPlans.invalidate());
      listeners.addListener(new FaunaSoundLoader());
      listeners.addListener(new SpawnSettingsLoader());
      listeners.addListener(new SpawnRules());
      listeners.addListener(new CreatureVariants());
   }

   public static final class ClientModEvents {
      private ClientModEvents() {
      }

      public static void clientInit() {
         ClientEvents.CLIENT_RELOAD_LISTENERS.register(LiberAnimalium.ClientModEvents::onRegisterClientReloadListeners);
         ClientEvents.PARTICLE_PROVIDERS.register(LiberAnimalium.ClientModEvents::onRegisterParticleProviders);
         ClientEvents.ENTITY_RENDERERS.register(LiberAnimalium.ClientModEvents::onRegisterRenderers);
      }

      public static void onRegisterClientReloadListeners(ClientServices.ReloadListenerSink event) {
         event.registerReloadListener(new FaunaResourceReloadListener());
         event.registerReloadListener(new NametagSkinManager());
         event.registerReloadListener(new MembraneUvLoader());
         event.registerReloadListener(new RiderPoseLoader());
      }

      public static void onRegisterParticleProviders(ClientServices.ParticleSink event) {
         event.registerSpriteSet((ParticleType)FaunaParticles.BREATH_PUFF.get(), BreathPuffParticle.Provider::new);
         event.registerSpriteSet(FaunaParticles.ATTACK_PARTICLE_TYPE.get(), AttackParticle.Provider::new);
         event.registerSpriteSet((ParticleType)FaunaParticles.WING_DUST.get(), WingWashParticle.DustProvider::new);
         event.registerSpriteSet((ParticleType)FaunaParticles.WING_DEBRIS.get(), WingWashParticle.DebrisProvider::new);
         event.registerSpriteSet((ParticleType)FaunaParticles.SLEEP_BUBBLE.get(), SleepBubbleParticle.Provider::new);
         event.registerSpriteSet((ParticleType)FaunaParticles.WATER_SPRAY.get(), WaterSprayParticle.Provider::new);
      }

      public static void onRegisterRenderers(ClientServices.RendererSink event) {
         event.registerEntityRenderer(FaunaEntities.ATTACK_PROJECTILE.get(), ProjectileRenderer::new);
      }
   }

   public static final class ClientPerf {
      private ClientPerf() {
      }

      public static void clientInit() {
         ClientEvents.CLIENT_TICK_END.register(LiberAnimalium.ClientPerf::onClientTick);
         ClientEvents.FRAME_START.register(LiberAnimalium.ClientPerf::onRenderTick);
      }

      public static void onClientTick() {
         PerfClock.rollClient();
      }

      public static void onRenderTick() {
         PerfClock.frame();
      }
   }
}
