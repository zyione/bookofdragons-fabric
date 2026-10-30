package net.magister.liberanimalium.body.locomotion.transition;

import net.magister.liberanimalium.body.locomotion.LocomotionMode;
import net.magister.liberanimalium.chassis.FaunaBase;

public class WaterExitTransition implements TransitionPrimitive {
   @Override
   public boolean precondition(FaunaBase body) {
      return body.profile().hasMode(LocomotionMode.WALK);
   }

   @Override
   public void begin(FaunaBase body) {
   }

   @Override
   public boolean tick(FaunaBase body) {
      return true;
   }

   @Override
   public void interrupt(FaunaBase body) {
   }

   @Override
   public String animSignal() {
      return "WATER_EXIT";
   }

   @Override
   public LocomotionMode fallbackMode() {
      return LocomotionMode.SWIM;
   }
}
