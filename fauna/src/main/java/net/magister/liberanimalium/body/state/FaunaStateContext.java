package net.magister.liberanimalium.body.state;

import net.magister.liberanimalium.body.anim.binding.AnimationBindingResolver;
import net.magister.liberanimalium.chassis.FaunaBase;
import net.magister.liberanimalium.chassis.FaunaDispatcher;
import net.magister.liberanimalium.network.FaunaNetwork;
import net.magister.liberanimalium.network.FlightTellsSyncMessage;
import net.magister.liberanimalium.util.Quantum;
import net.minecraft.world.phys.Vec3;

public class FaunaStateContext {
   private final FaunaBase body;
   private TransportMode transportMode = TransportMode.GROUNDED;
   private boolean isDiveBreaking = false;
   private float calculatedPitch = 0.0F;
   private float cachedGroundDistance = -1.0F;
   private Vec3 tellVelocity;
   private TransportMode lastSyncedTransport = TransportMode.GROUNDED;
   private float lastSyncedPitch = 0.0F;
   private boolean lastSyncedDiveBreak = false;
   private float lastSyncedMotionX;
   private float lastSyncedMotionY;
   private float lastSyncedMotionZ;
   private final AnimationBindingResolver bindingResolver = new AnimationBindingResolver();
   private boolean climbTold;

   public FaunaStateContext(FaunaBase body) {
      this.body = body;
   }

   public void update() {
      if (this.body.level().isClientSide() && this.body.isControlledByLocalInstance()) {
         FaunaStateEvaluator.evaluate(this.body, this);
         this.body.setLayerTransportModeOrdinal(this.transportMode.ordinal());
         this.driveAnimations(this.body.getDispatcher());
      } else if (this.body.level().isClientSide() || this.body.steeredByRider()) {
         this.setTransportMode(TransportMode.fromOrdinal(this.body.getLayerTransportModeOrdinal()));
         this.isDiveBreaking = this.body.isDiveBreaking();
         if (this.body.supportsFlying()) {
            boolean isAIOnServer = !this.body.level().isClientSide() && this.body.getControllingPassenger() == null;
            if (!isAIOnServer) {
               this.calculatedPitch = this.body.getSyncedPitch();
            }
         }

         if (this.body.level().isClientSide()) {
            this.driveAnimations(this.body.getDispatcher());
         }
      }
   }

   public TransportMode getTransportMode() {
      return this.transportMode;
   }

   public void setTransportMode(TransportMode transportMode) {
      this.transportMode = transportMode;
   }

   public boolean isDiveBreaking() {
      return this.isDiveBreaking;
   }

   public void setDiveBreaking(boolean diveBreaking) {
      this.isDiveBreaking = diveBreaking;
   }

   public float getCalculatedPitch() {
      return this.calculatedPitch;
   }

   public void setCalculatedPitch(float calculatedPitch) {
      this.calculatedPitch = calculatedPitch;
   }

   public Vec3 getTellVelocity() {
      return this.tellVelocity;
   }

   public void setTellVelocity(Vec3 tellVelocity) {
      this.tellVelocity = tellVelocity;
   }

   public float getCachedGroundDistance() {
      return this.cachedGroundDistance;
   }

   public boolean climbTold() {
      return this.climbTold;
   }

   public void tellClimb(boolean climbing) {
      this.climbTold = climbing;
   }

   public void setCachedGroundDistance(float distance) {
      this.cachedGroundDistance = distance;
   }

   public void syncWithServer() {
      if (this.body.level().isClientSide() && this.body.isControlledByLocalInstance()) {
         Vec3 v = this.body.getDeltaMovement();
         float q = 0.005F;
         float mx = Quantum.step((float)v.x, q);
         float my = Quantum.step((float)v.y, q);
         float mz = Quantum.step((float)v.z, q);
         boolean changed = this.transportMode != this.lastSyncedTransport
            || Math.abs(this.calculatedPitch - this.lastSyncedPitch) > 1.0F
            || this.isDiveBreaking != this.lastSyncedDiveBreak
            || mx != this.lastSyncedMotionX
            || my != this.lastSyncedMotionY
            || mz != this.lastSyncedMotionZ;
         if (changed) {
            FaunaNetwork.sendToServer(
               new FlightTellsSyncMessage(this.body.getId(), this.transportMode.ordinal(), this.calculatedPitch, this.isDiveBreaking, mx, my, mz)
            );
            this.lastSyncedTransport = this.transportMode;
            this.lastSyncedPitch = this.calculatedPitch;
            this.lastSyncedDiveBreak = this.isDiveBreaking;
            this.lastSyncedMotionX = mx;
            this.lastSyncedMotionY = my;
            this.lastSyncedMotionZ = mz;
         }
      }
   }

   public void driveAnimations(FaunaDispatcher dispatcher) {
      if (this.body.level().isClientSide() && dispatcher != null) {
         this.bindingResolver.resolve(this.body, this, dispatcher);
      }
   }

   public AnimationBindingResolver getBindingResolver() {
      return this.bindingResolver;
   }
}
