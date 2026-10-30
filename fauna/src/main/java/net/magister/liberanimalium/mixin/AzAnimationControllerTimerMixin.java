package net.magister.liberanimalium.mixin;

import mod.azure.azurelib.animation.controller.AzAnimationController;
import mod.azure.azurelib.animation.controller.AzAnimationControllerTimer;
import mod.azure.azurelib.animation.controller.state.machine.AzAnimationControllerStateMachine;
import mod.azure.azurelib.animation.controller.state.machine.AzAnimationControllerStateMachine.Context;
import mod.azure.azurelib.animation.play_behavior.AzPlayBehaviors;
import mod.azure.azurelib.animation.primitive.AzQueuedAnimation;
import mod.azure.azurelib.animation.property.AzAnimationProperties;
import net.magister.liberanimalium.chassis.FaunaBase;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = AzAnimationControllerTimer.class, remap = false)
public class AzAnimationControllerTimerMixin {
   @Shadow
   private double adjustedTick;
   @Shadow
   @Final
   private AzAnimationController<?> animationController;
   @Unique
   private double liberanimalium$lastAnimTime;

   @Unique
   private boolean liberanimalium$ours() {
      return ((Context)(Object)this.animationController.stateMachine().getContext()).animationContext().animatable() instanceof FaunaBase;
   }

   @Inject(method = "update()V", at = @At("HEAD"), cancellable = true)
   private void liberanimalium$integrateSpeed(CallbackInfo ci) {
      if (this.liberanimalium$ours()) {
         AzAnimationProperties props = this.animationController.animationProperties();
         double animTime = ((Context)(Object)this.animationController.stateMachine().getContext()).animationContext().timer().getAnimTime();
         double freezeTick = this.animationController.effectiveFreezeTickOffset();
         if (freezeTick > 0.0 && this.adjustedTick >= freezeTick) {
            this.adjustedTick = freezeTick;
            this.liberanimalium$lastAnimTime = animTime;
            ci.cancel();
         } else {
            double delta = animTime - this.liberanimalium$lastAnimTime;
            if (delta < 0.0) {
               delta = 0.0;
            }

            double speed = props.animationSpeed();
            AzAnimationControllerStateMachine<?> machine = this.animationController.stateMachine();
            if (machine.isTransitioning()) {
               speed = Math.abs(speed);
            }

            this.adjustedTick += speed * delta;
            if (this.adjustedTick < 0.0 && !machine.isTransitioning()) {
               AzQueuedAnimation current = this.animationController.currentAnimation();
               double length = current == null ? 0.0 : current.animation().length();
               if (current != null && length > 0.0 && current.playBehavior() == AzPlayBehaviors.LOOP) {
                  this.adjustedTick = this.adjustedTick % length + length;
               } else {
                  this.adjustedTick = 0.0;
               }
            }

            this.liberanimalium$lastAnimTime = animTime;
            ci.cancel();
         }
      }
   }

   @Inject(method = "reset()V", at = @At("TAIL"))
   private void liberanimalium$seedAfterReset(CallbackInfo ci) {
      if (this.liberanimalium$ours()) {
         AzAnimationProperties props = this.animationController.animationProperties();
         double animTime = ((Context)(Object)this.animationController.stateMachine().getContext()).animationContext().timer().getAnimTime();
         this.adjustedTick = props.animationSpeed() * props.startTickOffset();
         this.liberanimalium$lastAnimTime = animTime;
      }
   }
}
