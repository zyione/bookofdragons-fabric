package net.magister.liberanimalium.render;

import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4f;

public final class RenderPassContext {
   private static int extractingBodyId = -1;
   private static final Matrix4f ENTRY_INVERSE = new Matrix4f();
   private static boolean entryValid;

   private RenderPassContext() {
   }

   public static void beginExtraction(int bodyId) {
      extractingBodyId = bodyId;
   }

   public static void endExtraction() {
      extractingBodyId = -1;
      entryValid = false;
   }

   public static void setEntryPose(Matrix4f pose) {
      entryValid = Float.isFinite(ENTRY_INVERSE.set(pose).invert().m00());
   }

   @Nullable
   public static Matrix4f entryInverse() {
      return entryValid ? ENTRY_INVERSE : null;
   }

   public static boolean isExtractionAllowed(int bodyId) {
      return extractingBodyId == bodyId;
   }
}
