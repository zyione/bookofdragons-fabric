package net.magister.liberanimalium.platform.event;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Camera;
import net.minecraft.client.renderer.culling.Frustum;
import org.joml.Matrix4f;

public final class StageContext {
   private final RenderStage stage;
   private final PoseStack poseStack;
   private final Matrix4f projectionMatrix;
   private final float partialTick;
   private final Camera camera;
   private final Frustum frustum;

   public StageContext(RenderStage stage, PoseStack poseStack, Matrix4f projectionMatrix, float partialTick, Camera camera, Frustum frustum) {
      this.stage = stage;
      this.poseStack = poseStack;
      this.projectionMatrix = projectionMatrix;
      this.partialTick = partialTick;
      this.camera = camera;
      this.frustum = frustum;
   }

   public RenderStage getStage() {
      return this.stage;
   }

   public PoseStack getPoseStack() {
      return this.poseStack;
   }

   public Matrix4f getProjectionMatrix() {
      return this.projectionMatrix;
   }

   public float getPartialTick() {
      return this.partialTick;
   }

   public Camera getCamera() {
      return this.camera;
   }

   public Frustum getFrustum() {
      return this.frustum;
   }
}
