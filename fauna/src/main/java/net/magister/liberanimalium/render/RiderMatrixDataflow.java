package net.magister.liberanimalium.render;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4f;

public final class RiderMatrixDataflow {
   private static final Map<Integer, Matrix4f> matrices = new ConcurrentHashMap<>();
   private static final Map<Integer, Long> matrixTimestamps = new ConcurrentHashMap<>();
   private static final Map<Integer, Long> renderTimestamps = new ConcurrentHashMap<>();
   private static final Set<Integer> frameExtractedBodies = ConcurrentHashMap.newKeySet();
   private static long lastFrameNanoTime = 0L;
   private static final long FRAME_TIME_THRESHOLD_NS = 1000000L;

   private RiderMatrixDataflow() {
   }

   public static boolean tryLockForFrame(int entityId) {
      long currentNanoTime = System.nanoTime();
      if (currentNanoTime - lastFrameNanoTime > 1000000L) {
         frameExtractedBodies.clear();
         lastFrameNanoTime = currentNanoTime;
      }

      return frameExtractedBodies.add(entityId);
   }

   public static void store(int entityId, Matrix4f matrix) {
      matrices.put(entityId, new Matrix4f(matrix));
      matrixTimestamps.put(entityId, System.currentTimeMillis());
   }

   @Nullable
   public static Matrix4f get(int entityId) {
      return matrices.get(entityId);
   }

   public static long getTimestamp(int entityId) {
      return matrixTimestamps.getOrDefault(entityId, 0L);
   }

   public static void notifyRendered(int entityId) {
      renderTimestamps.put(entityId, System.currentTimeMillis());
   }

   public static long getLastRenderTime(int entityId) {
      return renderTimestamps.getOrDefault(entityId, 0L);
   }

   public static void remove(int entityId) {
      matrices.remove(entityId);
      matrixTimestamps.remove(entityId);
      renderTimestamps.remove(entityId);
   }

   public static void clearAll() {
      matrices.clear();
      matrixTimestamps.clear();
      renderTimestamps.clear();
      frameExtractedBodies.clear();
   }
}
