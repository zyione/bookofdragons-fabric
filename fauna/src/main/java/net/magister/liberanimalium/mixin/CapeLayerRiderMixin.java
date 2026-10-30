package net.magister.liberanimalium.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import net.magister.liberanimalium.ride.render.RiderPosedModel;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.layers.CapeLayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(CapeLayer.class)
public abstract class CapeLayerRiderMixin {
   @Unique
   private boolean liberanimalium$pushed;

   @Inject(
      method = "render(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;ILnet/minecraft/client/player/AbstractClientPlayer;FFFFFF)V",
      at = @At("HEAD")
   )
   private void liberanimalium$onTheBack(
      PoseStack poseStack,
      MultiBufferSource buffers,
      int light,
      AbstractClientPlayer player,
      float limbSwing,
      float limbSwingAmount,
      float partialTick,
      float ageInTicks,
      float netHeadYaw,
      float headPitch,
      CallbackInfo ci
   ) {
      PlayerModel<AbstractClientPlayer> model = (PlayerModel<AbstractClientPlayer>)((CapeLayer)(Object)this).getParentModel();
      this.liberanimalium$pushed = model instanceof RiderPosedModel posed && posed.liberanimalium$riderPosed();
      if (this.liberanimalium$pushed) {
         poseStack.pushPose();
         model.body.translateAndRotate(poseStack);
      }
   }

   @Inject(
      method = "render(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;ILnet/minecraft/client/player/AbstractClientPlayer;FFFFFF)V",
      at = @At("RETURN")
   )
   private void liberanimalium$offTheBack(
      PoseStack poseStack,
      MultiBufferSource buffers,
      int light,
      AbstractClientPlayer player,
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

   @Redirect(
      method = "render(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;ILnet/minecraft/client/player/AbstractClientPlayer;FFFFFF)V",
      at = @At(value = "INVOKE", target = "Lnet/minecraft/client/player/AbstractClientPlayer;isCrouching()Z"),
      require = 0
   )
   private boolean liberanimalium$noCrouchUnderAPose(AbstractClientPlayer player) {
      PlayerModel<AbstractClientPlayer> model = (PlayerModel<AbstractClientPlayer>)((CapeLayer)(Object)this).getParentModel();
      return model instanceof RiderPosedModel posed && posed.liberanimalium$riderPosed() ? false : player.isCrouching();
   }
}
