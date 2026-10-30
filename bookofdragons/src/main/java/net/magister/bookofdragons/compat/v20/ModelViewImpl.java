package net.magister.bookofdragons.compat.v20;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;

public final class ModelViewImpl {
   private ModelViewImpl() {
   }

   public static void pushIdentity() {
      PoseStack stack = RenderSystem.getModelViewStack();
      stack.pushPose();
      stack.setIdentity();
      RenderSystem.applyModelViewMatrix();
   }

   public static void pop() {
      RenderSystem.getModelViewStack().popPose();
      RenderSystem.applyModelViewMatrix();
   }
}
