package net.magister.liberanimalium.body.state;

import net.magister.liberanimalium.body.locomotion.FlightGait;
import net.magister.liberanimalium.body.locomotion.LocomotionOrchestrator;
import net.magister.liberanimalium.body.locomotion.transition.LandTransition;
import net.magister.liberanimalium.chassis.FaunaBase;
import net.magister.liberanimalium.chassis.component.DiveBreakComponent;
import net.minecraft.world.phys.Vec3;

public class FaunaStateEvaluator {
   public static void evaluate(FaunaBase body, FaunaStateContext context) {
      LocomotionOrchestrator orchestrator = body.getLocomotionOrchestrator();
      TransportMode transport = orchestrator != null ? orchestrator.activeMode().toTransportMode() : body.getTransportMode();
      context.setTransportMode(transport);
      if (transport == TransportMode.AIRBORNE && body.supportsFlying()) {
         evaluateFlightPhysics(body, context, orchestrator);
      }

      if (!body.level().isClientSide()) {
         if (body.supportsFlying() && body.steeredByRider()) {
            body.getEntityData().set(FaunaBase.DATA_SYNCED_PITCH, context.getCalculatedPitch());
         }

         body.setIsDiveBreaking(context.isDiveBreaking());
      } else if (body.isControlledByLocalInstance()) {
         context.syncWithServer();
      }
   }

   private static void evaluateFlightPhysics(FaunaBase body, FaunaStateContext context, LocomotionOrchestrator orchestrator) {
      LandTransition landing = orchestrator == null ? null : orchestrator.activeLanding();
      if (landing != null) {
         context.setCalculatedPitch(landing.attitude());
         context.setCachedGroundDistance(landing.groundDistance());
         context.setDiveBreaking(landing.diveBreaking());
      } else {
         Vec3 velocity = body.getDeltaMovement();
         double horizontalSpeed = Math.sqrt(velocity.horizontalDistanceSqr());
         Vec3 tellVelocity = FlightGait.followVelocity(context.getTellVelocity(), velocity);
         context.setTellVelocity(tellVelocity);
         if (tellVelocity.lengthSqr() > 1.0E-4) {
            context.setCalculatedPitch(FlightGait.pathPitch(tellVelocity.y, Math.sqrt(tellVelocity.horizontalDistanceSqr()), 0.15F));
         } else {
            context.setCalculatedPitch(0.0F);
         }

         boolean isDiving;
         if (body.isVehicle()) {
            isDiving = velocity.y < -0.1;
         } else {
            isDiving = velocity.y < -0.1 && horizontalSpeed > 0.05;
         }

         if (isDiving) {
            DiveBreakComponent diveBreak = body.componentRegistry.get(DiveBreakComponent.class);
            if (diveBreak != null) {
               float groundDist = diveBreak.calculateGroundDistance();
               context.setCachedGroundDistance(groundDist);
               boolean isBreaking = groundDist >= 0.0F && groundDist < DiveBreakComponent.tuning(body).range();
               context.setDiveBreaking(isBreaking);
            }
         } else {
            context.setDiveBreaking(false);
            context.setCachedGroundDistance(-1.0F);
         }
      }
   }
}
