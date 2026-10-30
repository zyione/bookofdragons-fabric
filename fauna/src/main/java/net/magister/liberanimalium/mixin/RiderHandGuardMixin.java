package net.magister.liberanimalium.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import net.magister.liberanimalium.ride.render.RiderAnimator;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(PlayerRenderer.class)
public abstract class RiderHandGuardMixin {
   @Inject(method = "renderHand", at = @At("HEAD"))
   private void liberanimalium$handBegin(
      PoseStack poseStack, MultiBufferSource buffer, int light, AbstractClientPlayer player, ModelPart arm, ModelPart armwear, CallbackInfo ci
   ) {
      RiderAnimator.beginHandPass();
   }

   @Inject(method = "renderHand", at = @At("RETURN"))
   private void liberanimalium$handEnd(
      PoseStack poseStack, MultiBufferSource buffer, int light, AbstractClientPlayer player, ModelPart arm, ModelPart armwear, CallbackInfo ci
   ) {
      RiderAnimator.endHandPass();
   }
}
