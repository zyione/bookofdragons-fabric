package net.magister.liberanimalium.render.gpu;

import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.VertexSorting;
import com.mojang.blaze3d.vertex.VertexFormat.Mode;
import java.util.HashMap;
import java.util.Map;
import net.magister.liberanimalium.compat.Meshes;
import net.magister.liberanimalium.platform.event.ClientEvents;
import net.magister.liberanimalium.platform.event.RenderStage;
import net.magister.liberanimalium.platform.event.StageContext;
import net.magister.liberanimalium.render.FaunaRenderTypes;
import net.magister.liberanimalium.util.PerfClock;
import net.minecraft.client.renderer.RenderType;

public final class SpeciesBatches {
   private static final Map<RenderType, Meshes.Batch> BATCHES = new HashMap<>();
   private static boolean entityPass;
   private static int drawnLastFrame;
   public static final PerfClock.Section CLOCK = PerfClock.client("gpu.membranes");

   private SpeciesBatches() {
   }

   public static void clientInit() {
      ClientEvents.RENDER_STAGE.register(SpeciesBatches::onStage);
   }

   public static boolean deferring() {
      return entityPass;
   }

   public static VertexConsumer consumer(RenderType speciesType, int layer) {
      Meshes.Batch batch = BATCHES.computeIfAbsent(speciesType, t -> new Meshes.Batch(t, 65536));
      return new LayeredConsumer(batch.open(Mode.QUADS, LayeredEntityFormat.FORMAT), layer);
   }

   public static VertexConsumer membrane(String species, int layer) {
      return consumer(FaunaRenderTypes.speciesTranslucent(species), layer);
   }

   public static void onStage(StageContext event) {
      if (event.getStage() == RenderStage.AFTER_SKY) {
         discardAll();
         InstancedBatches.discard();
         entityPass = true;
      } else if (event.getStage() == RenderStage.AFTER_ENTITIES) {
         entityPass = false;
         InstancedBatches.flush();
         flush();
      }
   }

   private static void flush() {
      long t = CLOCK.start();

      try {
         int drawn = 0;

         for (Meshes.Batch batch : BATCHES.values()) {
            if (batch.isOpen()) {
               batch.draw(VertexSorting.DISTANCE_TO_ORIGIN);
               drawn++;
            }
         }

         drawnLastFrame = drawn;
      } finally {
         CLOCK.end(t);
      }
   }

   private static void discardAll() {
      for (Meshes.Batch batch : BATCHES.values()) {
         if (batch.isOpen()) {
            batch.abandon();
         }
      }
   }

   public static int drawnLastFrame() {
      return drawnLastFrame;
   }

   public static void release() {
      discardAll();

      for (Meshes.Batch batch : BATCHES.values()) {
         batch.close();
      }

      BATCHES.clear();
      InstancedBatches.release();
      entityPass = false;
   }
}
