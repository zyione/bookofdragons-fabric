package net.magister.liberanimalium.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.PoseStack.Pose;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import mod.azure.azurelib.animation.AzAnimator;
import mod.azure.azurelib.cache.object.GeoCube;
import mod.azure.azurelib.model.AzBakedModel;
import mod.azure.azurelib.model.AzBone;
import mod.azure.azurelib.render.AzLayerRenderer;
import mod.azure.azurelib.render.AzRendererPipelineContext;
import mod.azure.azurelib.render.entity.AzEntityModelRenderer;
import mod.azure.azurelib.render.entity.AzEntityRendererPipeline;
import mod.azure.azurelib.render.layer.AzRenderLayer;
import net.magister.liberanimalium.anim.rig.BoneMatrices;
import net.magister.liberanimalium.chassis.FaunaBase;
import net.magister.liberanimalium.compat.AzColor;
import net.magister.liberanimalium.compat.Verts;
import net.magister.liberanimalium.render.gpu.GpuSkin;
import net.magister.liberanimalium.render.layer.BoneAudience;
import net.magister.liberanimalium.render.layer.LayerAudience;
import net.magister.liberanimalium.util.PerfClock;
import net.minecraft.client.renderer.OutlineBufferSource;
import org.joml.Matrix3f;
import org.joml.Matrix4f;

public class FaunaModelRenderer<T extends FaunaBase> extends AzEntityModelRenderer<T> {
   private static volatile FaunaModelRenderer.Path path = FaunaModelRenderer.Path.FAST;
   private static final PerfClock.Section BONES = PerfClock.client("azurelib.bones");
   private static final PerfClock.Section MODEL = PerfClock.client("azurelib.model");
   private static final PerfClock.Section LAYERS_FLAT = PerfClock.client("gpu.layers");
   private final LayerAudience audience = new LayerAudience();
   private final List<AzRenderLayer<UUID, T>> layers = new ArrayList<>();
   private boolean gpuActive;
   private boolean gpuDone;
   private boolean gpuAsked;
   private float[] out = new float[90];
   private int depth;

   public static FaunaModelRenderer.Path path() {
      return path;
   }

   public static void setPath(FaunaModelRenderer.Path value) {
      path = value == null ? FaunaModelRenderer.Path.FAST : value;
   }

   private Set<String> audienceFor(AzRendererPipelineContext<UUID, T> context) {
      this.layers.clear();
      this.layers.addAll(this.entityRendererPipeline.getRenderer().config().renderLayers());
      return this.audience.build(this.layers.size(), (index, into) -> {
         if (this.layers.get(index) instanceof BoneAudience declaring) {
            declaring.wantedBones(context, into);
            return true;
         } else {
            return false;
         }
      });
   }

   private void dispatchLayers(AzRendererPipelineContext<UUID, T> context, AzBone bone, boolean everyone) {
      if (everyone) {
         this.layerRenderer.applyRenderLayersForBone(context, bone);
      } else {
         String name = bone.getName();

         for (int n = 0; n < this.audience.activeCount(); n++) {
            if (this.audience.wants(n, name)) {
               this.layers.get(this.audience.activeLayer(n)).renderForBone(context, bone);
            }
         }
      }
   }

   public void render(AzRendererPipelineContext<UUID, T> context, boolean isReRender) {
      long start = MODEL.start();
      boolean wasActive = this.gpuActive;
      boolean wasDone = this.gpuDone;
      boolean wasAsked = this.gpuAsked;
      this.gpuActive = false;
      this.gpuDone = false;
      this.gpuAsked = false;

      try {
         super.render(context, isReRender);
      } finally {
         this.gpuActive = wasActive;
         this.gpuDone = wasDone;
         this.gpuAsked = wasAsked;
         MODEL.end(start);
      }
   }

   private GpuSkin.Decline wantsGpu(AzRendererPipelineContext<UUID, T> context, boolean isReRender) {
      if (isReRender) {
         return GpuSkin.Decline.RE_RENDER;
      } else {
         T body = (T)context.animatable();
         if (body == null) {
            return GpuSkin.Decline.NO_MODEL;
         } else if (context.getTextureOverride() != null) {
            return GpuSkin.Decline.TEXTURE_OVERRIDE;
         } else {
            return context.multiBufferSource() instanceof OutlineBufferSource
               ? GpuSkin.Decline.OUTLINE
               : GpuSkin.why(body.getId(), context.bakedModel(), context.renderType());
         }
      }
   }

   public void handleAnimation(AzAnimator<UUID, T> animator, T animatable, float partialTick) {
      AzBakedModel model = animator.context() == null ? null : animator.context().boneCache().getBakedModel();
      boolean evaluated = PoseInterpolator.evaluate(animatable, model, partialTick);
      if (evaluated) {
         super.handleAnimation(animator, animatable, partialTick);
         PoseInterpolator.captured(animatable, model, partialTick);
      }

      if (PoseInterpolator.path() == PoseInterpolator.Path.TICK && model != null) {
         FaunaProceduralAnimator.applyWorldAnchored(animatable, model, partialTick);
         PoseInterpolator.refreshMarks(animatable.getId(), model);
      }

      if (BodyWatch.watching(animatable.getId())) {
         BodyWatch.sample(animatable, model, partialTick, evaluated);
      }
   }

   public FaunaModelRenderer(AzEntityRendererPipeline<T> pipeline, AzLayerRenderer<UUID, T> layerRenderer) {
      super(pipeline, layerRenderer);
   }

   public void renderRecursively(AzRendererPipelineContext<UUID, T> context, AzBone bone, boolean isReRender) {
      if (this.depth == 0 && !this.gpuAsked) {
         this.gpuAsked = true;
         GpuSkin.Decline why = this.wantsGpu(context, isReRender);
         GpuSkin.note(why);
         this.gpuActive = why == GpuSkin.Decline.NONE;
      }

      if (this.gpuActive && this.depth == 0) {
         if (this.gpuDone) {
            return;
         }

         this.gpuDone = true;
         long start = BONES.start();

         boolean drew;
         try {
            drew = this.renderOnGpu(context);
         } finally {
            BONES.end(start);
         }

         if (drew) {
            return;
         }

         this.gpuActive = false;
      }

      long start = this.depth++ == 0 ? BONES.start() : 0L;

      try {
         super.renderRecursively(context, bone, isReRender);
      } finally {
         if (--this.depth == 0) {
            BONES.end(start);
         }
      }
   }

   private boolean renderOnGpu(AzRendererPipelineContext<UUID, T> context) {
      T body = (T)context.animatable();
      AzBakedModel model = context.bakedModel();
      BonePalettes.Frame frame = BonePalettes.frame(body.getId());
      if (frame == null) {
         return false;
      }

      PoseStack stack = context.poseStack();
      Matrix4f rootPose = new Matrix4f(stack.last().pose());
      Matrix3f rootNormal = new Matrix3f(stack.last().normal());
      BoneMatrices layout = frame.layout();
      boolean[] reachable = GpuSkin.reachableMask(layout);
      long layers = LAYERS_FLAT.start();

      try {
         Set<String> audience = this.audienceFor(context);

         for (int i = 0; i < layout.size(); i++) {
            if (reachable[i]) {
               AzBone at = layout.bone(i);
               if (audience == null || audience.contains(at.getName())) {
                  stack.pushPose();
                  stack.last().pose().set(rootPose).mul(frame.pose()[i]);
                  stack.last().normal().set(rootNormal).mul(frame.normals()[i]);
                  this.dispatchLayers(context, at, audience == null);
                  stack.popPose();
               }
            }
         }
      } finally {
         LAYERS_FLAT.end(layers);
      }

      int layer = context instanceof FaunaRendererPipeline.LayeredContext<?> layered ? layered.layer() : -1;
      return GpuSkin.draw(
         body.getId(),
         model,
         context.renderType(),
         stack,
         context.packedLight(),
         context.packedOverlay(),
         AzColor.r(context),
         AzColor.g(context),
         AzColor.b(context),
         AzColor.a(context),
         layer
      );
   }

   protected void renderCube(AzRendererPipelineContext<UUID, T> context, GeoCube cube) {
      if (path != FaunaModelRenderer.Path.STOCK && context.getTextureOverride() == null && context.quadFilter() == null) {
         CubeMesh mesh = CubeMesh.of(cube);
         Pose last = context.poseStack().last();
         int quads = mesh.transform(last.pose(), last.normal(), this.out);
         VertexConsumer buffer = context.vertexConsumer();
         float red = AzColor.r(context);
         float green = AzColor.g(context);
         float blue = AzColor.b(context);
         float alpha = AzColor.a(context);
         int overlay = context.packedOverlay();
         int light = context.packedLight();
         float[] uv = mesh.uv();
         int q = 0;

         for (int o = 0; q < quads; o += 15) {
            float nx = this.out[o];
            float ny = this.out[o + 1];
            float nz = this.out[o + 2];

            for (int i = 0; i < 4; i++) {
               int p = o + 3 + i * 3;
               int t = (q * 4 + i) * 2;
               Verts.entity(buffer, this.out[p], this.out[p + 1], this.out[p + 2], red, green, blue, alpha, uv[t], uv[t + 1], overlay, light, nx, ny, nz);
            }

            q++;
         }
      } else {
         super.renderCube(context, cube);
      }
   }

   public enum Path {
      FAST,
      STOCK;
   }
}
