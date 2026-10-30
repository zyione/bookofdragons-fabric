package net.magister.liberanimalium.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import net.magister.liberanimalium.ride.render.RiderPosedModel;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.layers.ElytraLayer;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ElytraLayer.class)
public abstract class ElytraLayerRiderMixin {
   @Unique
   private boolean liberanimalium$pushed;

   @Inject(
      method = "render(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;ILnet/minecraft/world/entity/LivingEntity;FFFFFF)V",
      at = @At("HEAD")
   )
   private void liberanimalium$onTheBack(
      PoseStack poseStack,
      MultiBufferSource buffers,
      int light,
      LivingEntity entity,
      float limbSwing,
      float limbSwingAmount,
      float partialTick,
      float ageInTicks,
      float netHeadYaw,
      float headPitch,
      CallbackInfo ci
   ) {
      EntityModel<? extends LivingEntity> model = ((ElytraLayer)(Object)this).getParentModel();
      this.liberanimalium$pushed = false;
      if (model instanceof RiderPosedModel posed && posed.liberanimalium$riderPosed() && model instanceof HumanoidModel<?> humanoid) {
         this.liberanimalium$pushed = true;
         poseStack.pushPose();
         humanoid.body.translateAndRotate(poseStack);
      }
   }

   @Inject(
      method = "render(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;ILnet/minecraft/world/entity/LivingEntity;FFFFFF)V",
      at = @At("RETURN")
   )
   private void liberanimalium$offTheBack(
      PoseStack poseStack,
      MultiBufferSource buffers,
      int light,
      LivingEntity entity,
      float limbSwing,
      float limbSwingAmount,
      float partialTick,
      float ageInTicks,
      float netHeadYaw,
      float headPitch,
      CallbackInfo ci
   ) {
      if (this.liberanimalium$pushed) {
         poseStack.popPose();
         this.liberanimalium$pushed = false;
      }
   }
}
