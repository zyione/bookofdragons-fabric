package net.magister.liberanimalium.chassis;

import java.util.Random;
import java.util.function.Predicate;
import mod.azure.azurelib.animation.controller.keyframe.AzBoneAnimation;
import mod.azure.azurelib.animation.dispatch.command.AzCommand;
import mod.azure.azurelib.animation.impl.AzEntityAnimator;
import mod.azure.azurelib.animation.primitive.AzBakedAnimation;
import net.magister.liberanimalium.fauna.api.ExpressionKey;
import net.magister.liberanimalium.fauna.api.PoseFlag;

public class FaunaDispatcher {
   private final FaunaBase body;
   private int blinkTimer = 100 + new Random().nextInt(301);

   public FaunaDispatcher(FaunaBase body) {
      this.body = body;
   }

   public void cancelSlot(String controllerName) {
      AzCommand.controllerBuilder().cancel(controllerName).build().sendForEntity(this.body);
   }

   public boolean hasAnimation(String fullClipName) {
      AzEntityAnimator<FaunaBase> animator = (AzEntityAnimator<FaunaBase>)this.body.getAnimatorOrNull();
      return animator == null ? false : animator.getAnimation(this.body, fullClipName) != null;
   }

   public boolean clipKeys(String fullClipName, Predicate<String> bone) {
      AzEntityAnimator<FaunaBase> animator = (AzEntityAnimator<FaunaBase>)this.body.getAnimatorOrNull();
      if (animator == null) {
         return false;
      }

      AzBakedAnimation clip = animator.getAnimation(this.body, fullClipName);
      if (clip == null) {
         return false;
      }

      for (AzBoneAnimation channel : clip.boneAnimations()) {
         if (bone.test(channel.boneName())) {
            return true;
         }
      }

      return false;
   }

   public boolean ready() {
      return this.body.getAnimatorOrNull() == null ? false : ((AzEntityAnimator)this.body.getAnimatorOrNull()).context().animatable() != null;
   }

   public void tickBlink() {
      if (!this.body.level().isClientSide()) {
         if (this.blinkTimer > 0) {
            this.blinkTimer--;
         } else {
            this.blinkTimer = 100 + this.body.getRandom().nextInt(301);
            if (!this.body.hasFaunaPose(PoseFlag.SLEEPING) && !this.body.getSyncedIsIncapacitated()) {
               this.body.express(ExpressionKey.body(ExpressionKey.Body.BLINK));
            }
         }
      }
   }
}
