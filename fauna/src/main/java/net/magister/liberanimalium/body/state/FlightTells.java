package net.magister.liberanimalium.body.state;

import net.magister.liberanimalium.chassis.FaunaBase;
import net.magister.liberanimalium.chassis.component.FaunaFlightRotationComponent;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

public final class FlightTells {
   public static final double HOVER_SPEED = 0.15F;
   static final float CLIMB_PITCH_ENTER = -30.0F;
   static final float CLIMB_PITCH_EXIT = -20.0F;
   static final double CLIMB_RISE_ENTER = 0.08;
   static final double CLIMB_RISE_EXIT = 0.04;
   static final double RIDDEN_CLIMB_ENTER = 0.5;
   static final double RIDDEN_CLIMB_EXIT = 0.4;
   public static final float UPSIDE_DOWN_DEGREES = 140.0F;

   private FlightTells() {
   }

   public static boolean hovering(FaunaBase body) {
      if (!body.supportsFlying()) {
         return true;
      }

      Vec3 velocity = RiderMotion.of(body);
      return velocity.horizontalDistanceSqr() < 0.02250000178813938 && Math.abs(velocity.y) < 0.05;
   }

   public static boolean climbing(FaunaBase body) {
      FaunaStateContext context = body.getStateContext();
      boolean was = context != null && context.climbTold();
      boolean now = climbing(body, was);
      if (context != null) {
         context.tellClimb(now);
      }

      return now;
   }

   static boolean climbing(FaunaBase body, boolean was) {
      Vec3 velocity = RiderMotion.of(body);
      double totalSpeed = velocity.length();
      double normalizedY = totalSpeed > 0.01 ? velocity.y / totalSpeed : 0.0;
      boolean isControlledByPlayer = body.isVehicle() && body.getControllingPassenger() instanceof Player;
      boolean isClimbing = isControlledByPlayer
         ? normalizedY > (was ? 0.4 : 0.5)
         : body.getXRot() < (was ? -20.0F : -30.0F) && normalizedY > (was ? 0.0 : 0.05);
      return isClimbing
         && (
            body.isAccelerating()
               || body.isBoosting()
               || !isControlledByPlayer && velocity.y > (was ? 0.04 : 0.08)
               || body.onGround() && velocity.y > 0.01
         );
   }

   public static float rollDegrees(FaunaBase body) {
      if (body.isVehicle() && body.getControllingPassenger() instanceof Player) {
         FaunaFlightRotationComponent rotComp = body.componentRegistry.get(FaunaFlightRotationComponent.class);
         return rotComp == null ? 0.0F : Mth.wrapDegrees(rotComp.getRoll());
      } else {
         return 0.0F;
      }
   }

   public static boolean upsideDown(FaunaBase body) {
      return Math.abs(rollDegrees(body)) > 140.0F;
   }
}
