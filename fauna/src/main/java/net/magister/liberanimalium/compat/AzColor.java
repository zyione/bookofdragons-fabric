package net.magister.liberanimalium.compat;

import mod.azure.azurelib.render.AzRendererPipelineContext;
import net.magister.liberanimalium.compat.v20.AzColorImpl;

public final class AzColor {
   private AzColor() {
   }

   public static float r(AzRendererPipelineContext<?, ?> context) {
      return AzColorImpl.r(context);
   }

   public static float g(AzRendererPipelineContext<?, ?> context) {
      return AzColorImpl.g(context);
   }

   public static float b(AzRendererPipelineContext<?, ?> context) {
      return AzColorImpl.b(context);
   }

   public static float a(AzRendererPipelineContext<?, ?> context) {
      return AzColorImpl.a(context);
   }

   public static void set(AzRendererPipelineContext<?, ?> context, float r, float g, float b, float a) {
      AzColorImpl.set(context, r, g, b, a);
   }
}
