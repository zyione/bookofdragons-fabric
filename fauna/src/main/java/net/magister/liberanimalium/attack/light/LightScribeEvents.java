package net.magister.liberanimalium.attack.light;

import net.magister.liberanimalium.platform.event.CommonEvents;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.chunk.ChunkAccess;

public final class LightScribeEvents {
   private LightScribeEvents() {
   }

   public static void init() {
      CommonEvents.LEVEL_TICK_END.register(LightScribeEvents::onLevelTick);
      CommonEvents.CHUNK_LOAD.register(LightScribeEvents::onChunkLoad);
      CommonEvents.CHUNK_UNLOAD.register(LightScribeEvents::onChunkUnload);
      CommonEvents.LEVEL_UNLOAD.register(LightScribeEvents::onLevelUnload);
      CommonEvents.SERVER_STOPPING.register(LightScribeEvents::onServerStopping);
   }

   public static void onLevelTick(Level ticked) {
      if (ticked instanceof ServerLevel level) {
         LightScribe.of(level).tick(level);
      }
   }

   public static void onChunkLoad(LevelAccessor accessor, ChunkAccess chunk) {
      if (accessor instanceof ServerLevel level) {
         LightScribe.of(level).reclaim(level, chunk.getPos());
      }
   }

   public static void onChunkUnload(LevelAccessor accessor, ChunkAccess chunk) {
      if (accessor instanceof ServerLevel level) {
         LightScribe.of(level).revertChunk(level, chunk);
      }
   }

   public static void onLevelUnload(LevelAccessor accessor) {
      if (accessor instanceof ServerLevel level) {
         LightScribe.of(level).revertAll(level);
         LightScribe.forget(level);
      }
   }

   public static void onServerStopping(MinecraftServer server) {
      for (ServerLevel level : server.getAllLevels()) {
         LightScribe.of(level).revertAll(level);
      }

      LightScribe.forgetAll();
   }
}
