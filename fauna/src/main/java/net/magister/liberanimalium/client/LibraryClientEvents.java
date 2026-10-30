package net.magister.liberanimalium.client;

import net.magister.liberanimalium.anim.rig.BlockChanges;
import net.magister.liberanimalium.anim.rig.RiggedBody;
import net.magister.liberanimalium.chassis.FaunaBase;
import net.magister.liberanimalium.chassis.FaunaTick;
import net.magister.liberanimalium.genetics.client.GeneticsTextureCache;
import net.magister.liberanimalium.interop.ForeignWakes;
import net.magister.liberanimalium.platform.event.ClientEvents;
import net.magister.liberanimalium.platform.event.CommonEvents;
import net.magister.liberanimalium.platform.event.RenderStage;
import net.magister.liberanimalium.platform.event.StageContext;
import net.magister.liberanimalium.render.FaunaProceduralAnimator;
import net.magister.liberanimalium.render.FaunaRendererAz;
import net.magister.liberanimalium.render.RigLod;
import net.magister.liberanimalium.render.vfx.SwimWake;
import net.magister.liberanimalium.render.waves.FaunaWaveManager;
import net.magister.liberanimalium.render.waves.render.WaveRenderer;
import net.magister.liberanimalium.ride.client.SpeedLines;
import net.magister.liberanimalium.sound.client.SoundClientHelper;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.chunk.ChunkAccess;

public final class LibraryClientEvents {
   private static WaveRenderer waveRenderer;

   private LibraryClientEvents() {
   }

   public static void clientInit() {
      ClientEvents.CLIENT_TICK_END.register(LibraryClientEvents::onClientTick);
      FaunaTick.LIVING.register(LibraryClientEvents::onLivingTick);
      ClientEvents.RENDER_STAGE.register(LibraryClientEvents::onRenderLevelStage);
      CommonEvents.ENTITY_LEAVE.register(LibraryClientEvents::onEntityLeaveLevel);
      CommonEvents.CHUNK_LOAD.register(LibraryClientEvents::onChunkLoad);
      CommonEvents.CHUNK_UNLOAD.register(LibraryClientEvents::onChunkUnload);
      CommonEvents.LEVEL_UNLOAD.register(LibraryClientEvents::onLevelUnload);
   }

   public static void onClientTick() {
      FaunaWaveManager.getInstance().tick();
      ForeignWakes.look();
   }

   public static void onLivingTick(LivingEntity entity) {
      if (entity.level().isClientSide()) {
         if (entity instanceof FaunaBase body) {
            SoundClientHelper.tickAbilityLoopSound(body);
            SoundClientHelper.tickBreathSound(body);
         }
      }
   }

   public static void onRenderLevelStage(StageContext event) {
      if (event.getStage() == RenderStage.AFTER_TRANSLUCENT_BLOCKS) {
         if (waveRenderer == null) {
            waveRenderer = new WaveRenderer();
         }

         waveRenderer.render(event.getPoseStack(), event.getCamera());
      }

      if (event.getStage() == RenderStage.AFTER_PARTICLES) {
         SpeedLines.render(event);
      }
   }

   public static void onEntityLeaveLevel(Entity entity, Level level) {
      if (level.isClientSide() && entity instanceof FaunaBase body) {
         FaunaRendererAz.onBodyRemoved(body);
         SwimWake.forget(body.getId());
         SoundClientHelper.onBodyRemoved(body);
      } else if (level.isClientSide() && entity instanceof RiggedBody) {
         FaunaProceduralAnimator.forget(entity.getId());
         RigLod.forget(entity.getId());
      }
   }

   public static void onChunkLoad(LevelAccessor level, ChunkAccess chunk) {
      if (level != null && level.isClientSide()) {
         BlockChanges.reset();
      }
   }

   public static void onChunkUnload(LevelAccessor level, ChunkAccess chunk) {
      if (level != null && level.isClientSide()) {
         BlockChanges.reset();
      }
   }

   public static void onLevelUnload(LevelAccessor level) {
      if (level.isClientSide()) {
         BlockChanges.reset();
         FaunaWaveManager.getInstance().reset();
         if (waveRenderer != null) {
            waveRenderer.cleanup();
            waveRenderer = null;
         }

         GeneticsTextureCache.INSTANCE.clear();
         FaunaProceduralAnimator.forgetAll();
      }
   }
}
