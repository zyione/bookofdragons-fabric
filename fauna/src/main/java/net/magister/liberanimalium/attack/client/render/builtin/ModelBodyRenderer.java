package net.magister.liberanimalium.attack.client.render.builtin;

import com.mojang.blaze3d.vertex.PoseStack;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import mod.azure.azurelib.render.AzLayerRenderer;
import mod.azure.azurelib.render.entity.AzEntityModelRenderer;
import mod.azure.azurelib.render.entity.AzEntityRenderer;
import mod.azure.azurelib.render.entity.AzEntityRendererConfig;
import mod.azure.azurelib.render.entity.AzEntityRendererPipeline;
import mod.azure.azurelib.render.lod.AzLodConfig;
import net.magister.liberanimalium.attack.client.render.AttackRenderTypes;
import net.magister.liberanimalium.attack.data.render.ModelRender;
import net.magister.liberanimalium.attack.entity.AttackProjectileEntity;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRendererProvider.Context;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import org.jetbrains.annotations.NotNull;
import org.joml.Quaternionf;

public final class ModelBodyRenderer extends AzEntityRenderer<AttackProjectileEntity> {
   private static final Map<ModelRender, ModelBodyRenderer> CACHE = new HashMap<>();
   private final ModelRender cfg;

   public static ModelBodyRenderer of(ModelRender cfg, Context context) {
      ModelBodyRenderer cached = CACHE.get(cfg);
      if (cached == null) {
         cached = new ModelBodyRenderer(cfg, context);
         CACHE.put(cfg, cached);
      }

      return cached;
   }

   private ModelBodyRenderer(ModelRender cfg, Context context) {
      super(configFor(cfg), context);
      this.cfg = cfg;
   }

   private static AzEntityRendererConfig<AttackProjectileEntity> configFor(ModelRender cfg) {
      return AzEntityRendererConfig.<AttackProjectileEntity>builder(entity -> cfg.geo(), entity -> frameTextureOf(cfg, entity))
         .setRenderType(entity -> renderTypeOf(cfg, frameTextureOf(cfg, entity)))
         .setModelRenderer((pipeline, layer) -> new ModelBodyRenderer.StaticModelRenderer((AzEntityRendererPipeline<AttackProjectileEntity>)pipeline, layer))
         .setShadowRadius(0.0F)
         .withLodConfig(AzLodConfig.DISABLED)
         .build();
   }

   private static ResourceLocation frameTextureOf(ModelRender cfg, AttackProjectileEntity entity) {
      return cfg.frameTexture(cfg.frameAt(entity.getFlightTime()));
   }

   private static RenderType renderTypeOf(ModelRender cfg, ResourceLocation texture) {
      return switch (cfg.blend()) {
         case "additive" -> AttackRenderTypes.additiveEmissive(texture);
         case "cutout", "glow" -> RenderType.entityCutoutNoCull(texture);
         default -> RenderType.entityTranslucentEmissive(texture);
      };
   }

   public void render(
      @NotNull AttackProjectileEntity entity,
      float entityYaw,
      float partialTick,
      @NotNull PoseStack poseStack,
      @NotNull MultiBufferSource bufferSource,
      int packedLight
   ) {
      poseStack.pushPose();
      List<Float> size = this.cfg.size();
      float min = size.size() >= 1 ? size.get(0) : 1.0F;
      float max = size.size() >= 2 ? size.get(1) : min;
      float scale = Mth.lerp(entity.getChargeScale(), min, max);
      poseStack.scale(scale, scale, scale);
      float yaw = Mth.lerp(partialTick, entity.yRotO, entity.getYRot());
      float pitch = Mth.lerp(partialTick, entity.xRotO, entity.getXRot());
      Quaternionf orientation = new Quaternionf()
         .rotationY(yaw * (float) (Math.PI / 180.0))
         .rotateX(-pitch * (float) (Math.PI / 180.0))
         .rotateY((float) Math.PI);
      if (this.cfg.rollSpeed() != 0.0F) {
         orientation.rotateZ((entity.tickCount + partialTick) * this.cfg.rollSpeed() * (float) (Math.PI / 180.0));
      }

      poseStack.mulPose(orientation);
      int light = "cutout".equals(this.cfg.blend()) ? packedLight : 15728880;
      super.render(entity, entityYaw, partialTick, poseStack, bufferSource, light);
      poseStack.popPose();
   }

   private static final class StaticModelRenderer extends AzEntityModelRenderer<AttackProjectileEntity> {
      StaticModelRenderer(AzEntityRendererPipeline<AttackProjectileEntity> pipeline, AzLayerRenderer<UUID, AttackProjectileEntity> layerRenderer) {
         super(pipeline, layerRenderer);
      }

      protected void applyRotations(
         AttackProjectileEntity animatable, PoseStack poseStack, float ageInTicks, float rotationYaw, float partialTick, float nativeScale
      ) {
      }
   }
}
