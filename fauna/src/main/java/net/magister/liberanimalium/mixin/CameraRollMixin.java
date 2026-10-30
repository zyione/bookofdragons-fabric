package net.magister.liberanimalium.mixin;

import java.util.function.Consumer;
import net.magister.liberanimalium.platform.event.CameraAngles;
import net.magister.liberanimalium.platform.event.ClientEvents;
import net.magister.liberanimalium.platform.client.CameraRoll;
import net.minecraft.client.Camera;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.BlockGetter;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Camera.class)
public class CameraRollMixin implements CameraRoll {
   @Unique
   private float liberanimalium$roll;
   @Unique
   private float liberanimalium$partialTick;
   @Unique
   private boolean liberanimalium$settingUp;

   @Override
   public float liberanimalium$roll() {
      return this.liberanimalium$roll;
   }

   @Inject(method = "setup", at = @At("HEAD"))
   private void liberanimalium$beginSetup(BlockGetter level, Entity entity, boolean detached, boolean thirdPersonReverse, float partialTick, CallbackInfo ci) {
      this.liberanimalium$partialTick = partialTick;
      this.liberanimalium$settingUp = true;
   }

   @Inject(method = "setup", at = @At("TAIL"))
   private void liberanimalium$endSetup(BlockGetter level, Entity entity, boolean detached, boolean thirdPersonReverse, float partialTick, CallbackInfo ci) {
      this.liberanimalium$settingUp = false;
   }

   @Redirect(method = "setup", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/Camera;setRotation(FF)V", ordinal = 0))
   private void liberanimalium$setAngles(Camera camera, float yaw, float pitch) {
      CameraAngles angles = new CameraAngles(camera, this.liberanimalium$partialTick, yaw, pitch, 0.0F);

      for (Consumer<CameraAngles> listener : ClientEvents.CAMERA_ANGLES.all()) {
         listener.accept(angles);
      }

      this.liberanimalium$roll = angles.getRoll();
      camera.setRotation(angles.getYaw(), angles.getPitch());
   }

   @Redirect(method = "setup", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/Camera;setRotation(FF)V", ordinal = 1))
   private void liberanimalium$reverseAngles(Camera camera, float yaw, float pitch) {
      this.liberanimalium$roll = -this.liberanimalium$roll;
      camera.setRotation(yaw, pitch);
   }

   @Redirect(method = "setup", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/Camera;setRotation(FF)V", ordinal = 2))
   private void liberanimalium$bedAngles(Camera camera, float yaw, float pitch) {
      this.liberanimalium$roll = 0.0F;
      camera.setRotation(yaw, pitch);
   }

   @ModifyArg(method = "setRotation", at = @At(value = "INVOKE", target = "Lorg/joml/Quaternionf;rotationYXZ(FFF)Lorg/joml/Quaternionf;"), index = 2)
   private float liberanimalium$rollTheRotation(float roll) {
      return this.liberanimalium$settingUp ? this.liberanimalium$roll * (float) (Math.PI / 180.0) : roll;
   }
}
