package net.magister.liberanimalium.mixin;

import net.magister.liberanimalium.ride.render.RiderAnimator;
import net.magister.liberanimalium.ride.render.RiderPosedModel;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(PlayerModel.class)
public abstract class PlayerModelRiderPoseMixin implements RiderPosedModel {
   @Unique
   private boolean liberanimalium$riderPosed;
   @Shadow
   @Final
   private ModelPart cloak;

   @Override
   public boolean liberanimalium$riderPosed() {
      return this.liberanimalium$riderPosed;
   }

   @Inject(method = "setupAnim(Lnet/minecraft/world/entity/LivingEntity;FFFFF)V", at = @At("HEAD"))
   private void liberanimalium$startClean(
      LivingEntity entity, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch, CallbackInfo ci
   ) {
      PlayerModel<?> model = (PlayerModel<?>)(Object)this;
      if (this.liberanimalium$riderPosed) {
         model.head.resetPose();
         model.body.resetPose();
         model.rightArm.resetPose();
         model.leftArm.resetPose();
         model.rightLeg.resetPose();
         model.leftLeg.resetPose();
         this.liberanimalium$riderPosed = false;
      }

      if (RiderAnimator.wants(entity)) {
         model.crouching = false;
      }
   }

   @Inject(method = "setupAnim(Lnet/minecraft/world/entity/LivingEntity;FFFFF)V", at = @At("RETURN"))
   private void liberanimalium$pose(
      LivingEntity entity, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch, CallbackInfo ci
   ) {
      this.liberanimalium$riderPosed = RiderAnimator.apply((PlayerModel<?>)(Object)this, entity, netHeadYaw, headPitch);
      if (this.liberanimalium$riderPosed && entity.isCrouching()) {
         boolean bare = entity.getItemBySlot(EquipmentSlot.CHEST).isEmpty();
         this.cloak.z = bare ? 0.0F : -1.1F;
         this.cloak.y = bare ? 0.0F : -0.85F;
      }
   }
}
