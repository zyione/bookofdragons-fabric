package net.magister.liberanimalium.mixin;

import com.mojang.math.Axis;
import net.magister.liberanimalium.ride.flight.rotation.FaunaCameraState;
import net.magister.liberanimalium.platform.client.CameraRoll;
import net.magister.liberanimalium.platform.event.ClientEvents;
import net.magister.liberanimalium.platform.event.FieldOfView;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import org.joml.Quaternionf;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(GameRenderer.class)
public class GameRendererMixin {
   @Redirect(method = "renderLevel", at = @At(value = "INVOKE", target = "Lcom/mojang/math/Axis;rotationDegrees(F)Lorg/joml/Quaternionf;", ordinal = 2))
   private Quaternionf liberanimalium$cameraRollRedirect(Axis axis, float angle) {
      Quaternionf viewQuat = FaunaCameraState.getOrientationConjugate();
      return viewQuat != null ? viewQuat : Axis.ZP.rotationDegrees(((CameraRoll)Minecraft.getInstance().gameRenderer.getMainCamera()).liberanimalium$roll()).mul(axis.rotationDegrees(angle));
   }

   @Redirect(method = "renderLevel", at = @At(value = "INVOKE", target = "Lcom/mojang/math/Axis;rotationDegrees(F)Lorg/joml/Quaternionf;", ordinal = 3))
   private Quaternionf liberanimalium$cameraPitchRedirect(Axis axis, float angle) {
      return FaunaCameraState.hasOrientationQuaternion() ? new Quaternionf() : axis.rotationDegrees(angle);
   }

   @Inject(method = "getFov", at = @At("RETURN"), cancellable = true)
   private void liberanimalium$fov(Camera camera, float partialTick, boolean changing, CallbackInfoReturnable<Double> cir) {
      FieldOfView event = new FieldOfView(camera, partialTick, cir.getReturnValue());
      ClientEvents.FOV.allEach(l -> l.accept(event));
      cir.setReturnValue(event.getFOV());
   }
}
