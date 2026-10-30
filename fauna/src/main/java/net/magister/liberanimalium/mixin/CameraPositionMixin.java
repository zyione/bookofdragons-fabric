package net.magister.liberanimalium.mixin;

import net.magister.liberanimalium.body.anim.extraction.CameraBoneExtractor;
import net.magister.liberanimalium.body.locomotion.ClimbEngine;
import net.magister.liberanimalium.body.locomotion.SurfaceFrame;
import net.magister.liberanimalium.body.physics.PhysicsSheet;
import net.magister.liberanimalium.chassis.FaunaBase;
import net.magister.liberanimalium.chassis.component.FaunaFlightRotationComponent;
import net.magister.liberanimalium.config.RideSettings;
import net.magister.liberanimalium.render.AttitudePass;
import net.magister.liberanimalium.ride.client.CameraLeanData;
import net.magister.liberanimalium.ride.client.TurbulenceShake;
import net.magister.liberanimalium.ride.flight.rotation.FaunaCameraState;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix3f;
import org.joml.Quaternionf;
import org.joml.Vector3d;
import org.joml.Vector3f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Camera.class)
public abstract class CameraPositionMixin {
   @Shadow
   private Vector3f up;
   @Shadow
   private Vector3f forwards;
   @Shadow
   private Vector3f left;
   @Shadow
   private Quaternionf rotation;

   @Shadow
   public abstract void setPosition(double var1, double var3, double var5);

   @Shadow
   public abstract void setPosition(Vec3 var1);

   @Shadow
   public abstract double getMaxZoom(double var1);

   @Inject(method = "rotation", at = @At("HEAD"), cancellable = true)
   private void liberanimalium$overrideCameraRotation(CallbackInfoReturnable<Quaternionf> cir) {
      Quaternionf orient = FaunaCameraState.getOrientationQuaternion();
      if (orient != null) {
         Quaternionf cameraRot = new Quaternionf(orient);
         if (!FaunaCameraState.isThirdPersonReverse()) {
            cameraRot.rotateY((float) Math.PI);
         }

         cir.setReturnValue(cameraRot);
      }
   }

   @Inject(method = "setup", at = @At("HEAD"))
   private void preSetupSyncRoll(BlockGetter level, Entity entity, boolean detached, boolean thirdPersonReverse, float partialTick, CallbackInfo ci) {
      FaunaCameraState.setThirdPersonReverse(thirdPersonReverse);
      if (entity != null && entity.getVehicle() instanceof FaunaBase body) {
         FaunaFlightRotationComponent rotComp = body.componentRegistry.get(FaunaFlightRotationComponent.class);
         ClimbEngine climb = ClimbEngine.lookOf(body);
         if (climb != null) {
            Quaternionf look = climb.look(partialTick);
            if (detached && RideSettings.isCameraLooseFollowEnabled()) {
               Quaternionf smoothed = FaunaCameraState.getSmoothedOrientation();
               if (smoothed != null) {
                  look = smoothed;
               }
            }

            look = TurbulenceShake.onto(look);
            float[] pyr = FaunaFlightRotationComponent.decomposeSwingTwist(new Matrix3f().set(look));
            FaunaCameraState.setStableAngles(Float.isNaN(pyr[1]) ? entity.getYRot() : pyr[1], pyr[0], Float.isNaN(pyr[2]) ? 0.0F : pyr[2]);
            FaunaCameraState.setOrientationQuaternion(look);
         } else if (rotComp != null && rotComp.isRolling()) {
            float[] stable = rotComp.getStableCameraAngles();
            FaunaCameraState.setStableAngles(stable[0], stable[1], stable[2]);
            Quaternionf orient;
            if (detached && RideSettings.isCameraLooseFollowEnabled()) {
               Quaternionf smoothed = FaunaCameraState.getSmoothedOrientation();
               orient = smoothed != null ? smoothed : rotComp.getRenderOrientation(partialTick);
            } else {
               orient = rotComp.getRenderOrientation(partialTick);
            }

            orient = TurbulenceShake.onto(orient);
            FaunaCameraState.setOrientationQuaternion(orient);
         } else {
            float rollDegrees = rotComp != null ? Mth.wrapDegrees(rotComp.getRoll(partialTick)) : 0.0F;
            FaunaCameraState.setCurrentRoll(-rollDegrees);
            FaunaCameraState.setOrientationQuaternion(null);
         }
      } else {
         FaunaCameraState.clearRoll();
      }
   }

   @Inject(method = "setup", at = @At("TAIL"))
   private void postSetupSaddlePosition(BlockGetter level, Entity entity, boolean detached, boolean thirdPersonReverse, float partialTick, CallbackInfo ci) {
      Quaternionf orient = FaunaCameraState.getOrientationQuaternion();
      if (orient != null) {
         Quaternionf cameraRot = new Quaternionf(orient);
         if (!FaunaCameraState.isThirdPersonReverse()) {
            cameraRot.rotateY((float) Math.PI);
         }

         this.rotation.set(cameraRot);
         float fDir = thirdPersonReverse ? 1.0F : -1.0F;
         float lDir = thirdPersonReverse ? 1.0F : -1.0F;
         this.forwards.set(0.0F, 0.0F, fDir).rotate(orient);
         this.up.set(0.0F, 1.0F, 0.0F).rotate(orient);
         this.left.set(lDir, 0.0F, 0.0F).rotate(orient);
      }

      if (entity != null && entity.getVehicle() instanceof FaunaBase body) {
         Vec3 saddleOffset = CameraBoneExtractor.get(body.getId());
         if (saddleOffset != null && Math.abs(saddleOffset.y) < 20.0 && Math.abs(saddleOffset.x) < 20.0 && Math.abs(saddleOffset.z) < 20.0
            )
          {
            double interpX = Mth.lerp(partialTick, body.xo, body.getX());
            double interpY = Mth.lerp(partialTick, body.yo, body.getY());
            double interpZ = Mth.lerp(partialTick, body.zo, body.getZ());
            if (detached && RideSettings.isCameraLooseFollowEnabled()) {
               Vector3d smoothedPos = FaunaCameraState.getSmoothedPosition();
               if (smoothedPos != null) {
                  interpX = smoothedPos.x();
                  interpY = smoothedPos.y();
                  interpZ = smoothedPos.z();
               }
            }

            Vector3f upVec = this.up;
            float eyeHeight = entity.getEyeHeight();
            double lift = AttitudePass.shiftAt(body, partialTick);
            ClimbEngine climbing = body.isClimbing() ? body.componentRegistry.get(ClimbEngine.class) : null;
            Vector3f liftUp = climbing == null ? new Vector3f(0.0F, 1.0F, 0.0F) : SurfaceFrame.up(climbing.attitude(partialTick));
            Vec3 pivot = new Vec3(
               interpX + saddleOffset.x + liftUp.x() * lift + upVec.x() * eyeHeight,
               interpY + saddleOffset.y + liftUp.y() * lift + upVec.y() * eyeHeight,
               interpZ + saddleOffset.z + liftUp.z() * lift + upVec.z() * eyeHeight
            );
            double leanX = CameraLeanData.getLeanX();
            double leanY = CameraLeanData.getLeanY();
            double leanZ = CameraLeanData.getLeanZ();
            if (Math.abs(leanX) > 0.001 || Math.abs(leanY) > 0.001 || Math.abs(leanZ) > 0.001) {
               Vector3f leftVec = this.left;
               Vector3f forwardVec = this.forwards;
               pivot = pivot.add(
                  forwardVec.x() * leanZ + upVec.x() * leanY + leftVec.x() * leanX,
                  forwardVec.y() * leanZ + upVec.y() * leanY + leftVec.y() * leanX,
                  forwardVec.z() * leanZ + upVec.z() * leanY + leftVec.z() * leanX
               );
            }

            this.setPosition(pivot);
            if (detached) {
               double targetDist = RideSettings.getThirdPersonDistance();
               if (PhysicsSheet.loaded()) {
                  targetDist *= PhysicsSheet.of(body).weight(body.getWeightClass()).cameraDistance();
               }

               double actualDist = this.getMaxZoom(targetDist);
               Vec3 thirdPersonPos = pivot.add(this.forwards.x() * -actualDist, this.forwards.y() * -actualDist, this.forwards.z() * -actualDist);
               this.setPosition(thirdPersonPos);
            }
         }
      }

      this.liberanimalium$rideLean();
   }

   @Unique
   private void liberanimalium$rideLean() {
      Player player = Minecraft.getInstance().player;
      if (player != null && player.getVehicle() instanceof FaunaBase) {
         double leanX = CameraLeanData.getLeanX();
         double leanY = CameraLeanData.getLeanY();
         double leanZ = CameraLeanData.getLeanZ();
         if (Math.abs(leanX) > 0.001 || Math.abs(leanY) > 0.001 || Math.abs(leanZ) > 0.001) {
            double dx = this.forwards.x() * leanZ + this.up.x() * leanY + this.left.x() * leanX;
            double dy = this.forwards.y() * leanZ + this.up.y() * leanY + this.left.y() * leanX;
            double dz = this.forwards.z() * leanZ + this.up.z() * leanY + this.left.z() * leanX;
            Vec3 at = ((Camera)(Object)this).getPosition();
            this.setPosition(new Vec3(at.x + dx, at.y + dy, at.z + dz));
         }
      }
   }
}
