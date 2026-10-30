package net.magister.bookofdragons.client.render.egg;

import com.mojang.blaze3d.vertex.PoseStack;
import mod.azure.azurelib.animation.AzAnimator;
import mod.azure.azurelib.render.entity.AzEntityRenderer;
import mod.azure.azurelib.render.entity.AzEntityRendererConfig;
import mod.azure.azurelib.render.lod.AzLodConfig;
import net.magister.bookofdragons.entity.custom.egg.DragonEggAnimator;
import net.magister.bookofdragons.entity.custom.egg.DragonEggEntity;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRendererProvider.Context;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;

public class DragonEggRendererAz extends AzEntityRenderer<DragonEggEntity> {
   public DragonEggRendererAz(Context context) {
      super(
         AzEntityRendererConfig.builder(DragonEggRendererAz::resolveModelLocation, DragonEggRendererAz::resolveTextureLocation)
            .setAnimatorProvider(() -> (AzAnimator)(new DragonEggAnimator()))
            .setShadowRadius(0.5F)
            .withLodConfig(AzLodConfig.DISABLED)
            .build(),
         context
      );
   }

   private static ResourceLocation resolveModelLocation(DragonEggEntity entity) {
      return entity.getDragonType().getEggModel();
   }

   private static ResourceLocation resolveTextureLocation(DragonEggEntity entity) {
      return entity.getDragonType().getEggTexture();
   }

   public void render(
      @NotNull DragonEggEntity entity,
      float entityYaw,
      float partialTick,
      @NotNull PoseStack poseStack,
      @NotNull MultiBufferSource bufferSource,
      int packedLight
   ) {
      float scale = entity.getDragonType().getRenderScale();
      poseStack.pushPose();
      poseStack.scale(scale, scale, scale);
      super.render(entity, entityYaw, partialTick, poseStack, bufferSource, packedLight);
      poseStack.popPose();
   }
}
