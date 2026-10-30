package net.magister.liberanimalium.mixin;

import net.magister.liberanimalium.ride.render.RiderAnimator;
import net.minecraft.client.model.ElytraModel;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(ElytraModel.class)
public abstract class ElytraModelRiderMixin {
   @Redirect(
      method = "setupAnim(Lnet/minecraft/world/entity/LivingEntity;FFFFF)V",
      at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/LivingEntity;isCrouching()Z"),
      require = 0
   )
   private boolean liberanimalium$noCrouchFold(LivingEntity entity) {
      return RiderAnimator.wants(entity) ? false : entity.isCrouching();
   }
}
