package net.magister.liberanimalium.mixin;

import mod.azure.azurelib.animation.AzAnimator;
import mod.azure.azurelib.model.AzBone;
import mod.azure.azurelib.render.AzModelRenderer;
import mod.azure.azurelib.render.AzRendererPipelineContext;
import net.magister.liberanimalium.chassis.FaunaBase;
import net.magister.liberanimalium.util.PerfClock;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = AzModelRenderer.class, remap = false)
public class AzModelRendererPerfMixin {
   @Unique
   private static final PerfClock.Section liberanimalium$ANIMATE = PerfClock.client("azurelib.animate");
   @Unique
   private static final PerfClock.Section liberanimalium$CUBES = PerfClock.client("azurelib.cubes");
   @Unique
   private long liberanimalium$animateStart;
   @Unique
   private long liberanimalium$cubesStart;

   @Inject(method = "renderCubesOfBone", at = @At("HEAD"))
   private void liberanimalium$cubesHead(AzRendererPipelineContext<?, ?> context, AzBone bone, CallbackInfo ci) {
      this.liberanimalium$cubesStart = context.animatable() instanceof FaunaBase ? liberanimalium$CUBES.start() : -1L;
   }

   @Inject(method = "renderCubesOfBone", at = @At("RETURN"))
   private void liberanimalium$cubesReturn(AzRendererPipelineContext<?, ?> context, AzBone bone, CallbackInfo ci) {
      if (this.liberanimalium$cubesStart >= 0L) {
         liberanimalium$CUBES.end(this.liberanimalium$cubesStart);
      }
   }

   @Inject(method = "handleAnimation(Lmod/azure/azurelib/animation/AzAnimator;Ljava/lang/Object;F)V", at = @At("HEAD"))
   private void liberanimalium$animateHead(AzAnimator<?, ?> animator, Object animatable, float partialTick, CallbackInfo ci) {
      this.liberanimalium$animateStart = animatable instanceof FaunaBase ? liberanimalium$ANIMATE.start() : -1L;
   }

   @Inject(method = "handleAnimation(Lmod/azure/azurelib/animation/AzAnimator;Ljava/lang/Object;F)V", at = @At("RETURN"))
   private void liberanimalium$animateReturn(AzAnimator<?, ?> animator, Object animatable, float partialTick, CallbackInfo ci) {
      if (this.liberanimalium$animateStart >= 0L) {
         liberanimalium$ANIMATE.end(this.liberanimalium$animateStart);
      }
   }
}
