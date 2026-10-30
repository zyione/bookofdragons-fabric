package net.magister.liberanimalium.mixin;

import net.magister.liberanimalium.compat.ClientFrame;
import net.magister.liberanimalium.ride.touch.TouchEase;
import net.magister.liberanimalium.ride.touch.TouchGestures;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.HumanoidModel.ArmPose;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import net.minecraft.world.entity.HumanoidArm;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(PlayerRenderer.class)
public abstract class PlayerArmPoseMixin {
   @Inject(method = "setModelProperties", at = @At("TAIL"))
   private void liberanimalium$touchGesture(AbstractClientPlayer player, CallbackInfo ci) {
      if (!(TouchEase.step(player, ClientFrame.partialTick()) <= 0.0F)) {
         ArmPose pose = TouchGestures.REACHING_POSE;
         PlayerModel<AbstractClientPlayer> model = (PlayerModel<AbstractClientPlayer>)((PlayerRenderer)(Object)this).getModel();
         if (player.getMainArm() == HumanoidArm.RIGHT) {
            model.rightArmPose = pose;
         } else {
            model.leftArmPose = pose;
         }
      }
   }
}
