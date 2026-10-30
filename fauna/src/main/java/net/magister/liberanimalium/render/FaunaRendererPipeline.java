package net.magister.liberanimalium.render;

import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import java.util.Collection;
import java.util.UUID;
import java.util.function.Supplier;
import mod.azure.azurelib.model.AzBakedModel;
import mod.azure.azurelib.model.AzBone;
import mod.azure.azurelib.render.AzLayerRenderer;
import mod.azure.azurelib.render.AzModelRenderer;
import mod.azure.azurelib.render.AzRendererConfig;
import mod.azure.azurelib.render.AzRendererPipeline;
import mod.azure.azurelib.render.AzRendererPipelineContext;
import mod.azure.azurelib.render.entity.AzEntityLayerRenderer;
import mod.azure.azurelib.render.entity.AzEntityRenderer;
import mod.azure.azurelib.render.entity.AzEntityRendererConfig;
import mod.azure.azurelib.render.entity.AzEntityRendererPipeline;
import mod.azure.azurelib.render.entity.AzEntityRendererPipelineContext;
import mod.azure.azurelib.render.layer.AzRenderLayer;
import net.magister.liberanimalium.chassis.FaunaBase;
import net.magister.liberanimalium.render.gpu.FaunaShaders;
import net.magister.liberanimalium.render.gpu.GpuSkin;
import net.magister.liberanimalium.render.gpu.LayeredConsumer;
import net.magister.liberanimalium.render.gpu.SpeciesBatches;
import net.magister.liberanimalium.render.gpu.SpeciesTextureArrays;
import net.magister.liberanimalium.util.PerfClock;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.OutlineBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import org.joml.Matrix4f;

public class FaunaRendererPipeline<T extends FaunaBase> extends AzEntityRendererPipeline<T> {
   private static final PerfClock.Section LAYERS = PerfClock.client("azurelib.layers");
   private static final PerfClock.Section PRE = PerfClock.client("azurelib.pre");
   private static final PerfClock.Section POST = PerfClock.client("azurelib.post");
   private static final PerfClock.Section FINAL = PerfClock.client("azurelib.final");
   private static final PerfClock.Section LAYERS_RENDER = PerfClock.client("azurelib.layers.render");
   private static final PerfClock.Section FLUSH = PerfClock.client("azurelib.flush");

   public FaunaRendererPipeline(AzEntityRendererConfig<T> config, AzEntityRenderer<T> renderer) {
      super(config, renderer);
   }

   public Matrix4f entityRenderTranslations() {
      return this.entityRenderTranslations;
   }

   public Matrix4f modelRenderTranslations() {
      return this.modelRenderTranslations;
   }

   protected AzRendererPipelineContext<UUID, T> createContext(AzRendererPipeline<UUID, T> rendererPipeline) {
      return new FaunaRendererPipeline.LayeredContext(rendererPipeline);
   }

   public void preRender(AzRendererPipelineContext<UUID, T> context, boolean isReRender) {
      long start = PRE.start();
      super.preRender(context, isReRender);
      PRE.end(start);
   }

   public void postRender(AzRendererPipelineContext<UUID, T> context, boolean isReRender) {
      long start = POST.start();
      super.postRender(context, isReRender);
      POST.end(start);
   }

   public void renderFinal(AzRendererPipelineContext<UUID, T> context) {
      long start = FINAL.start();
      super.renderFinal(context);
      FINAL.end(start);
   }

   protected AzModelRenderer<UUID, T> createModelRenderer(AzLayerRenderer<UUID, T> layerRenderer) {
      return new FaunaModelRenderer(this, layerRenderer);
   }

   protected AzLayerRenderer<UUID, T> createLayerRenderer(AzRendererConfig<UUID, T> config) {
      return new AzEntityLayerRenderer<T>(config::renderLayers) {
         public void applyRenderLayersForBone(AzRendererPipelineContext<UUID, T> context, AzBone bone) {
            long start = FaunaRendererPipeline.LAYERS.start();
            super.applyRenderLayersForBone(context, bone);
            FaunaRendererPipeline.LAYERS.end(start);
         }

         public void applyRenderLayers(AzRendererPipelineContext<UUID, T> context) {
            long start = FaunaRendererPipeline.LAYERS_RENDER.start();
            super.applyRenderLayers(context);
            FaunaRendererPipeline.LAYERS_RENDER.end(start);
         }
      };
   }

   public static final class LayeredContext<T extends FaunaBase> extends AzEntityRendererPipelineContext<T> {
      private int layer = -1;
      private RenderType vanillaChoice;

      LayeredContext(AzRendererPipeline<UUID, T> rendererPipeline) {
         super(rendererPipeline);
      }

      public int layer() {
         return this.layer;
      }

      public void populate(
         T animatable,
         AzBakedModel bakedModel,
         MultiBufferSource multiBufferSource,
         int packedLight,
         float partialTick,
         PoseStack poseStack,
         RenderType renderType,
         VertexConsumer vertexConsumer
      ) {
         if (vertexConsumer == null && FaunaRenderTypes.isSpecies(renderType) && SpeciesBatches.deferring()) {
            vertexConsumer = SpeciesBatches.consumer(renderType, this.layer);
         }

         long start = FaunaRendererPipeline.FLUSH.start();
         super.populate(animatable, bakedModel, multiBufferSource, packedLight, partialTick, poseStack, renderType, vertexConsumer);
         FaunaRendererPipeline.FLUSH.end(start);
         if (FaunaRenderTypes.isSpecies(this.renderType()) && !(this.vertexConsumer() instanceof LayeredConsumer)) {
            if (this.vertexConsumer() instanceof BufferBuilder builder) {
               this.setVertexConsumer(new LayeredConsumer(builder, this.layer));
            } else {
               this.layer = -1;
               this.setRenderType(this.vanillaChoice);
               this.setVertexConsumer(multiBufferSource.getBuffer(this.vanillaChoice));
            }
         }
      }

      public RenderType getDefaultRenderType(
         T animatable, ResourceLocation texture, MultiBufferSource bufferSource, float partialTick, RenderType defaultRenderType, float alpha
      ) {
         this.layer = -1;
         this.vanillaChoice = null;
         RenderType chosen = super.getDefaultRenderType(animatable, texture, bufferSource, partialTick, defaultRenderType, alpha);
         boolean plain = chosen == RenderType.entityTranslucent(texture) && alpha >= 1.0F && !animatable.isInvisible();
         boolean hurt = chosen == RenderType.entityCutoutNoCull(texture);
         if ((plain || hurt) && GpuSkin.enabled() && FaunaShaders.available() && !FaunaShaders.packActive() && !(bufferSource instanceof OutlineBufferSource)) {
            String species = animatable.animSpeciesId();
            int found = SpeciesTextureArrays.layerFor(species, texture);
            if (found >= 0) {
               this.layer = found;
               this.vanillaChoice = plain ? FaunaRenderTypes.translucent(texture) : chosen;
               return plain ? FaunaRenderTypes.speciesTranslucent(species) : FaunaRenderTypes.speciesCutout(species);
            }
         }

         if (plain) {
            return FaunaRenderTypes.translucent(texture);
         }

         FaunaRenderTypes.noteChosen(chosen, hurt ? FaunaRenderTypes.Kind.HURT : FaunaRenderTypes.Kind.OTHER);
         return chosen;
      }
   }
}
