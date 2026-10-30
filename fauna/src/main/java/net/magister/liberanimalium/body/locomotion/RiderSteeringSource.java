package net.magister.liberanimalium.body.locomotion;

import net.magister.liberanimalium.chassis.FaunaBase;
import net.magister.liberanimalium.chassis.component.RiderInputComponent;
import net.minecraft.world.entity.LivingEntity;
import org.joml.Vector3f;

public class RiderSteeringSource {
   private final FaunaBase body;

   public RiderSteeringSource(FaunaBase body) {
      this.body = body;
   }

   public SteeringIntent groundIntent(LivingEntity rider) {
      return this.intent(rider);
   }

   public SteeringIntent flightIntent(LivingEntity rider) {
      return this.intent(rider);
   }

   public SteeringIntent swimIntent(LivingEntity rider) {
      return this.intent(rider);
   }

   public SteeringIntent climbIntent(LivingEntity rider) {
      RiderInputComponent input = this.body.componentRegistry.get(RiderInputComponent.class);
      ClimbEngine climb = this.body.componentRegistry.get(ClimbEngine.class);
      if (input != null && climb != null) {
         Vector3f asked = climb.askedDirection(rider);
         return new SteeringIntent(
            input.getForward(), input.getStrafe(), SurfaceFrame.yawOf(asked, rider.getYRot()), SurfaceFrame.pitchOf(asked), 0.0F, this.body.isSprinting()
         );
      } else {
         return SteeringIntent.none();
      }
   }

   private SteeringIntent intent(LivingEntity rider) {
      RiderInputComponent input = this.body.componentRegistry.get(RiderInputComponent.class);
      return input == null
         ? SteeringIntent.none()
         : new SteeringIntent(input.getForward(), input.getStrafe(), rider.getYRot(), rider.getXRot(), 0.0F, this.body.isSprinting());
   }
}
