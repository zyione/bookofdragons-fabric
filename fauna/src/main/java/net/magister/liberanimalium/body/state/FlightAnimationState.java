package net.magister.liberanimalium.body.state;

import net.magister.liberanimalium.chassis.FaunaBase;

public record FlightAnimationState(boolean isUpsideDown, boolean isDiving, boolean antiRollEnabled, boolean neckPhysicsEnabled) {
   public static FlightAnimationState compute(FaunaBase body) {
      FaunaStateContext context = body.getStateContext();
      boolean isUpsideDown = FlightTells.upsideDown(body);
      boolean isDiving = context.getCalculatedPitch() >= 70.0F;
      return new FlightAnimationState(isUpsideDown, isDiving, !isUpsideDown && !isDiving, !isUpsideDown);
   }
}
