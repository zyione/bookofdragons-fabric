package net.magister.liberanimalium.ride.flight.rotation;

import net.magister.liberanimalium.config.RideSettings;
import net.minecraft.util.Mth;
import org.joml.Quaternionf;
import org.joml.Vector3d;
import org.joml.Vector3f;

public final class FaunaCameraSmoother {
   private static final float MAX_DT = 0.033333335F;
   private static final float LAG_REST = 0.999999F;
   private final Quaternionf springRot = new Quaternionf();
   private final Vector3f angularVelocity = new Vector3f();
   private final Vector3d springPos = new Vector3d();
   private final Vector3d velocityPos = new Vector3d();
   private final Quaternionf currentRot = new Quaternionf();
   private final Vector3d currentPos = new Vector3d();
   private boolean initialized = false;
   private final Quaternionf lag = new Quaternionf();

   public void update(Quaternionf targetRot, Vector3d targetPos, float dt) {
      this.update(
         targetRot,
         targetPos,
         dt,
         (float)RideSettings.getCinematicRotStiffness(),
         (float)RideSettings.getCinematicRotDamping(),
         (float)RideSettings.getCinematicPosStiffness(),
         (float)RideSettings.getCinematicPosDamping(),
         (float)RideSettings.getCinematicRotLooseness(),
         (float)RideSettings.getCinematicPosLoosenessLateral(),
         (float)RideSettings.getCinematicPosLoosenessForward()
      );
   }

   void update(
      Quaternionf targetRot,
      Vector3d targetPos,
      float dt,
      float rotStiffness,
      float rotDamping,
      float posStiffness,
      float posDamping,
      float rotLooseness,
      float posLoosenessLateral,
      float posLoosenessForward
   ) {
      dt = Mth.clamp(dt, 0.0F, 0.033333335F);
      if (!this.initialized) {
         this.springRot.set(targetRot).normalize();
         this.currentRot.set(targetRot).normalize();
         this.angularVelocity.zero();
         this.lag.identity();
         if (targetPos != null) {
            this.springPos.set(targetPos);
            this.currentPos.set(targetPos);
            this.velocityPos.zero();
         }

         this.initialized = true;
      } else {
         Quaternionf delta = new Quaternionf(targetRot).mul(new Quaternionf(this.springRot).conjugate());
         if (delta.dot(this.lag) < 0.0F) {
            delta.set(-delta.x(), -delta.y(), -delta.z(), -delta.w());
         }

         delta.normalize();
         if (delta.w() < -0.999999F) {
            delta.set(-delta.x(), -delta.y(), -delta.z(), -delta.w());
         }

         this.lag.set(delta);
         float sinHalf = (float)Math.sqrt(Math.max(0.0F, 1.0F - delta.w() * delta.w()));
         float lagAngle = 0.0F;
         Vector3f lagAxis = null;
         if (sinHalf < 1.0E-4F) {
            this.angularVelocity.mul(Math.max(0.0F, 1.0F - rotDamping * dt));
         } else {
            lagAngle = 2.0F * (float)Math.acos(Mth.clamp(delta.w(), -1.0F, 1.0F));
            lagAxis = new Vector3f(delta.x(), delta.y(), delta.z()).div(sinHalf);
            Vector3f torque = new Vector3f(lagAxis).mul(rotStiffness * lagAngle).sub(new Vector3f(this.angularVelocity).mul(rotDamping));
            this.angularVelocity.add(new Vector3f(torque).mul(dt));
         }

         float omega = this.angularVelocity.length();
         if (omega > 1.0E-4F) {
            Vector3f axis = new Vector3f(this.angularVelocity).div(omega);
            float halfAngle = omega * dt * 0.5F;
            float s = (float)Math.sin(halfAngle);
            float c = (float)Math.cos(halfAngle);
            Quaternionf step = new Quaternionf(axis.x() * s, axis.y() * s, axis.z() * s, c);
            step.mul(this.springRot, this.springRot);
         }

         this.springRot.normalize();
         if (targetPos != null) {
            Vector3d posError = new Vector3d(targetPos).sub(this.springPos);
            Vector3d posForce = new Vector3d(posError).mul(posStiffness).sub(new Vector3d(this.velocityPos).mul(posDamping));
            this.velocityPos.add(new Vector3d(posForce).mul(dt));
            this.springPos.add(new Vector3d(this.velocityPos).mul(dt));
         }

         if (lagAxis == null) {
            this.currentRot.set(targetRot);
         } else {
            this.currentRot.rotationAxis(lagAngle * (1.0F - rotLooseness), lagAxis.x(), lagAxis.y(), lagAxis.z()).mul(this.springRot);
         }

         this.currentRot.normalize();
         if (targetPos != null) {
            Vector3d totalStretch = new Vector3d(this.springPos).sub(targetPos);
            Vector3f forwardF = new Vector3f(0.0F, 0.0F, 1.0F).rotate(targetRot).normalize();
            Vector3d forwardVec = new Vector3d(forwardF.x(), forwardF.y(), forwardF.z());
            double forwardStretchMag = totalStretch.dot(forwardVec);
            Vector3d forwardStretch = new Vector3d(forwardVec).mul(forwardStretchMag);
            Vector3d lateralStretch = new Vector3d(totalStretch).sub(forwardStretch);
            forwardStretch.mul(posLoosenessForward);
            lateralStretch.mul(posLoosenessLateral);
            this.currentPos.set(targetPos).add(forwardStretch).add(lateralStretch);
         }
      }
   }

   public void reset(Quaternionf seedRot, Vector3d seedPos) {
      this.lag.identity();
      if (seedRot != null && seedPos != null) {
         this.springRot.set(seedRot).normalize();
         this.currentRot.set(seedRot).normalize();
         this.angularVelocity.zero();
         this.springPos.set(seedPos);
         this.currentPos.set(seedPos);
         this.velocityPos.zero();
         this.initialized = true;
      } else {
         this.initialized = false;
      }
   }

   public Quaternionf getRotation() {
      return this.initialized ? this.currentRot : null;
   }

   public Vector3d getPosition() {
      return this.initialized ? this.currentPos : null;
   }
}
