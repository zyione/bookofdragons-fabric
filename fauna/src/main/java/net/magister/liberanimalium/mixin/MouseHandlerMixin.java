package net.magister.liberanimalium.mixin;

import net.magister.liberanimalium.body.locomotion.ClimbEngine;
import net.magister.liberanimalium.chassis.FaunaBase;
import net.magister.liberanimalium.chassis.component.FaunaFlightRotationComponent;
import net.minecraft.client.Minecraft;
import net.minecraft.client.MouseHandler;
import net.minecraft.util.Mth;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MouseHandler.class)
public class MouseHandlerMixin {
   @Shadow(aliases = "f_91503_")
   @Final
   private Minecraft minecraft;
   @Unique
   private double liberanimalium_lastEventTime = System.nanoTime() / 1.0E9;
   @Unique
   private boolean liberanimalium_capturing = false;
   @Unique
   private float liberanimalium_preXRot;
   @Unique
   private float liberanimalium_preYRot;
   @Unique
   private double liberanimalium_mouseDelta;
   @Unique
   private FaunaFlightRotationComponent liberanimalium_pendingRotComp;
   @Unique
   private ClimbEngine liberanimalium_pendingClimb;

   @Inject(method = "turnPlayer", at = @At("HEAD"))
   private void onTurnHead(CallbackInfo ci) {
      double currentTime = System.nanoTime() / 1.0E9;
      double mouseDelta = currentTime - this.liberanimalium_lastEventTime;
      this.liberanimalium_lastEventTime = currentTime;
      this.liberanimalium_capturing = false;
      this.liberanimalium_pendingRotComp = null;
      this.liberanimalium_pendingClimb = null;
      if (this.minecraft.player != null) {
         if (this.minecraft.mouseHandler.isMouseGrabbed() && this.minecraft.isWindowActive()) {
            if (this.minecraft.player.getVehicle() instanceof FaunaBase body) {
               FaunaFlightRotationComponent rotComp = body.componentRegistry.get(FaunaFlightRotationComponent.class);
               ClimbEngine climb = ClimbEngine.lookOf(body);
               if (climb != null || rotComp != null && rotComp.isRolling()) {
                  this.liberanimalium_preXRot = this.minecraft.player.getXRot();
                  this.liberanimalium_preYRot = this.minecraft.player.getYRot();
                  this.liberanimalium_capturing = true;
                  this.liberanimalium_pendingRotComp = rotComp;
                  this.liberanimalium_pendingClimb = climb;
                  if (climb != null) {
                     this.minecraft.player.setXRot(0.0F);
                  }

                  if (mouseDelta > 0.05 || mouseDelta <= 0.0) {
                     mouseDelta = 0.01;
                  }

                  this.liberanimalium_mouseDelta = mouseDelta;
               }
            }
         }
      }
   }

   @Inject(method = "turnPlayer", at = @At("RETURN"))
   private void onTurnReturn(CallbackInfo ci) {
      if (this.liberanimalium_capturing) {
         this.liberanimalium_capturing = false;
         FaunaFlightRotationComponent rotComp = this.liberanimalium_pendingRotComp;
         this.liberanimalium_pendingRotComp = null;
         ClimbEngine climb = this.liberanimalium_pendingClimb;
         this.liberanimalium_pendingClimb = null;
         if (this.minecraft.player != null && climb != null) {
            float pitchStep = this.minecraft.player.getXRot();
            float yawStep = Mth.wrapDegrees(this.minecraft.player.getYRot() - this.liberanimalium_preYRot);
            this.minecraft.player.setXRot(this.liberanimalium_preXRot);
            this.minecraft.player.setYRot(this.liberanimalium_preYRot);
            climb.turnLook(yawStep, pitchStep);
         } else if (this.minecraft.player != null && rotComp != null) {
            float pitchDelta = this.minecraft.player.getXRot() - this.liberanimalium_preXRot;
            float yawDelta = Mth.wrapDegrees(this.minecraft.player.getYRot() - this.liberanimalium_preYRot);
            this.minecraft.player.setXRot(this.liberanimalium_preXRot);
            this.minecraft.player.setYRot(this.liberanimalium_preYRot);
            rotComp.changeFlightLook(pitchDelta / 0.15, yawDelta / 0.15, 0.0, this.liberanimalium_mouseDelta);
         }
      }
   }
}
