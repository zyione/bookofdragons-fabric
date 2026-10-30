package net.magister.liberanimalium.mixin;

import net.magister.liberanimalium.body.grip.Grip;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LivingEntity.class)
public abstract class LivingEntityHeldMixin {
   @Inject(method = "isPushable", at = @At("RETURN"), cancellable = true)
   private void liberanimalium$heldIsNotPushed(CallbackInfoReturnable<Boolean> cir) {
      if (Grip.held((Entity)(Object)this)) {
         cir.setReturnValue(false);
      }
   }

   @Inject(method = "pushEntities", at = @At("HEAD"), cancellable = true)
   private void liberanimalium$heldPushesNothing(CallbackInfo ci) {
      if (Grip.held((Entity)(Object)this)) {
         ci.cancel();
      }
   }
}
