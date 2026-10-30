package net.magister.liberanimalium.render;

import java.util.UUID;
import mod.azure.azurelib.model.AzBakedModel;
import mod.azure.azurelib.model.AzBone;
import mod.azure.azurelib.render.AzLayerRenderer;
import mod.azure.azurelib.render.AzModelRenderer;
import mod.azure.azurelib.render.AzRendererPipelineContext;
import mod.azure.azurelib.render.entity.AzEntityModelRenderer;
import mod.azure.azurelib.render.entity.AzEntityRenderer;
import mod.azure.azurelib.render.entity.AzEntityRendererConfig;
import mod.azure.azurelib.render.entity.AzEntityRendererPipeline;
import net.magister.liberanimalium.anim.rig.RiggedBody;
import net.minecraft.client.renderer.entity.EntityRendererProvider.Context;
import net.minecraft.world.entity.Entity;
import org.joml.Matrix4f;

public class RiggedRendererAz<T extends Entity & RiggedBody> extends AzEntityRenderer<T> {
   protected RiggedRendererAz(AzEntityRendererConfig<T> config, Context context) {
      super(config, context);
   }

   public AzEntityRendererPipeline<T> createPipeline(AzEntityRendererConfig<T> config) {
      return new RiggedRendererAz.Pipeline<>(config, this);
   }

   private static final class ModelRenderer<T extends Entity & RiggedBody> extends AzEntityModelRenderer<T> {
      private final RiggedRendererAz.Pipeline<T> pipeline;
      private final Matrix4f modelToWorld = new Matrix4f();

      ModelRenderer(RiggedRendererAz.Pipeline<T> pipeline, AzLayerRenderer<UUID, T> layerRenderer) {
         super(pipeline, layerRenderer);
         this.pipeline = pipeline;
      }

      public void renderRecursively(AzRendererPipelineContext<UUID, T> context, AzBone bone, boolean isReRender) {
         AzBakedModel model = context.bakedModel();
         if (!isReRender && model.getTopLevelBones().iterator().next() == bone) {
            this.modelToWorld.set(this.pipeline.entityTranslations()).invert().mul(context.poseStack().last().pose());
            FaunaProceduralAnimator.poseRigOnly((RiggedBody)context.animatable(), model, context.partialTick(), this.modelToWorld);
         }

         super.renderRecursively(context, bone, isReRender);
      }
   }

   private static final class Pipeline<T extends Entity & RiggedBody> extends AzEntityRendererPipeline<T> {
      Pipeline(AzEntityRendererConfig<T> config, AzEntityRenderer<T> renderer) {
         super(config, renderer);
      }

      protected AzModelRenderer<UUID, T> createModelRenderer(AzLayerRenderer<UUID, T> layerRenderer) {
         return new RiggedRendererAz.ModelRenderer<T>(this, layerRenderer);
      }

      Matrix4f entityTranslations() {
         return this.entityRenderTranslations;
      }
   }
}
