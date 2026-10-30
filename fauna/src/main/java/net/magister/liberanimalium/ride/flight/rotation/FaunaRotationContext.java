package net.magister.liberanimalium.ride.flight.rotation;

import net.magister.liberanimalium.chassis.FaunaBase;

public class FaunaRotationContext {
   private final FaunaRotationInstant currentRotation;
   private FaunaRotationInstant rotationDelta;
   private final double renderDelta;
   private final FaunaBase body;

   public FaunaRotationContext(FaunaRotationInstant currentRotation, FaunaRotationInstant rotationDelta, double renderDelta, FaunaBase body) {
      this.currentRotation = currentRotation;
      this.rotationDelta = rotationDelta;
      this.renderDelta = renderDelta;
      this.body = body;
   }

   public FaunaRotationInstant getCurrentRotation() {
      return this.currentRotation;
   }

   public FaunaRotationInstant getRotationDelta() {
      return this.rotationDelta;
   }

   public double getRenderDelta() {
      return this.renderDelta;
   }

   public FaunaBase getBody() {
      return this.body;
   }

   public FaunaRotationContext useModifier(FaunaRotationContext.RotationModifier mod) {
      this.rotationDelta = mod.apply(this.rotationDelta, this);
      return this;
   }

   @FunctionalInterface
   public interface RotationModifier {
      FaunaRotationInstant apply(FaunaRotationInstant var1, FaunaRotationContext var2);
   }
}
