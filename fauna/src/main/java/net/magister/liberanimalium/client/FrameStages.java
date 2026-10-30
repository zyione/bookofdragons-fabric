package net.magister.liberanimalium.client;

import net.magister.liberanimalium.platform.event.ClientEvents;
import net.magister.liberanimalium.platform.event.Event;
import net.magister.liberanimalium.platform.event.RenderStage;
import net.magister.liberanimalium.platform.event.StageContext;
import net.magister.liberanimalium.util.PerfClock;

public final class FrameStages {
   private static final PerfClock.Section FRAME = PerfClock.client("frame");
   private static final PerfClock.Section WORST = PerfClock.client("frame.worst");
   private static final PerfClock.Section PRE_SKY = PerfClock.client("frame.pre_sky");
   private static final PerfClock.Section TERRAIN = PerfClock.client("frame.terrain");
   private static final PerfClock.Section ENTITIES = PerfClock.client("frame.entities");
   private static final PerfClock.Section BLOCK_ENTITIES = PerfClock.client("frame.block_entities");
   private static final PerfClock.Section TRANSLUCENT = PerfClock.client("frame.translucent");
   private static final PerfClock.Section PARTICLES = PerfClock.client("frame.particles");
   private static final PerfClock.Section WEATHER = PerfClock.client("frame.weather");
   private static final PerfClock.Section LEVEL_END = PerfClock.client("frame.level_end");
   private static final PerfClock.Section AFTER_LEVEL = PerfClock.client("frame.after_level");
   private static long frameStart;
   private static long last;
   private static long worstNs;

   private FrameStages() {
   }

   public static void clientInit() {
      ClientEvents.FRAME_START.register(Event.Priority.HIGHEST, FrameStages::onRenderTickStart);
      ClientEvents.FRAME_END.register(Event.Priority.LOWEST, FrameStages::onRenderTickEnd);
      ClientEvents.RENDER_STAGE.register(Event.Priority.LOWEST, FrameStages::onStage);
      ClientEvents.CLIENT_TICK_END.register(FrameStages::onClientTick);
   }

   public static void onRenderTickStart() {
      frameStart = System.nanoTime();
      last = frameStart;
   }

   public static void onRenderTickEnd() {
      if (frameStart != 0L) {
         AFTER_LEVEL.end(last);
         FRAME.end(frameStart);
         worstNs = Math.max(worstNs, System.nanoTime() - frameStart);
         frameStart = 0L;
      }
   }

   public static void onStage(StageContext event) {
      if (frameStart != 0L) {
         PerfClock.Section section = sectionFor(event.getStage());
         if (section != null) {
            section.end(last);
            last = System.nanoTime();
         }
      }
   }

   public static void onClientTick() {
      if (worstNs != 0L) {
         WORST.end(System.nanoTime() - worstNs);
         worstNs = 0L;
      }
   }

   private static PerfClock.Section sectionFor(RenderStage stage) {
      if (stage == RenderStage.AFTER_SKY) {
         return PRE_SKY;
      } else if (stage == RenderStage.AFTER_SOLID_BLOCKS || stage == RenderStage.AFTER_CUTOUT_MIPPED_BLOCKS_BLOCKS || stage == RenderStage.AFTER_CUTOUT_BLOCKS) {
         return TERRAIN;
      } else if (stage == RenderStage.AFTER_ENTITIES) {
         return ENTITIES;
      } else if (stage == RenderStage.AFTER_BLOCK_ENTITIES) {
         return BLOCK_ENTITIES;
      } else if (stage == RenderStage.AFTER_TRANSLUCENT_BLOCKS || stage == RenderStage.AFTER_TRIPWIRE_BLOCKS) {
         return TRANSLUCENT;
      } else if (stage == RenderStage.AFTER_PARTICLES) {
         return PARTICLES;
      } else if (stage == RenderStage.AFTER_WEATHER) {
         return WEATHER;
      } else {
         return stage == RenderStage.AFTER_LEVEL ? LEVEL_END : null;
      }
   }
}
