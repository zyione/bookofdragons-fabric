package net.magister.liberanimalium.body.locomotion.transition;

import net.magister.liberanimalium.body.locomotion.LocomotionMode;
import net.magister.liberanimalium.chassis.FaunaBase;

public interface TransitionPrimitive {
   boolean precondition(FaunaBase var1);

   void begin(FaunaBase var1);

   boolean tick(FaunaBase var1);

   void interrupt(FaunaBase var1);

   String animSignal();

   LocomotionMode fallbackMode();

   default boolean ownsPhysics() {
      return false;
   }

   default boolean ownsAttitude() {
      return false;
   }
}
