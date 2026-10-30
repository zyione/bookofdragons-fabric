package net.magister.liberanimalium.compat.v20;

import mod.azure.azurelib.render.AzRendererPipelineContext;

public final class AzColorImpl {
   private AzColorImpl() {
   }

   public static float r(AzRendererPipelineContext<?, ?> context) {
      return context.red();
   }

   public static float g(AzRendererPipelineContext<?, ?> context) {
      return context.green();
   }

   public static float b(AzRendererPipelineContext<?, ?> context) {
      return context.blue();
   }

   public static float a(AzRendererPipelineContext<?, ?> context) {
      return context.alpha();
   }

   public static void set(AzRendererPipelineContext<?, ?> context, float r, float g, float b, float a) {
      context.setRed(r);
      context.setGreen(g);
      context.setBlue(b);
      context.setAlpha(a);
   }
}
