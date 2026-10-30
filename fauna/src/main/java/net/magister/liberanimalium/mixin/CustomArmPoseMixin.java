package net.magister.liberanimalium.mixin;

import net.magister.liberanimalium.platform.fabric.FabricClientServices;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.HumanoidModel.ArmPose;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(HumanoidModel.class)
public abstract class CustomArmPoseMixin<T extends LivingEntity> {
    @Shadow public ArmPose leftArmPose;
    @Shadow public ArmPose rightArmPose;
    @Inject(method = "poseLeftArm", at = @At("HEAD"), cancellable = true)
    private void liberanimalium$left(T entity, CallbackInfo ci) {
        if (FabricClientServices.pose(leftArmPose, (HumanoidModel<?>)(Object)this, entity, HumanoidArm.LEFT)) ci.cancel();
    }
    @Inject(method = "poseRightArm", at = @At("HEAD"), cancellable = true)
    private void liberanimalium$right(T entity, CallbackInfo ci) {
        if (FabricClientServices.pose(rightArmPose, (HumanoidModel<?>)(Object)this, entity, HumanoidArm.RIGHT)) ci.cancel();
    }
}
