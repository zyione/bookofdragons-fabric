package net.magister.liberanimalium.body.locomotion;

import net.magister.liberanimalium.chassis.FaunaBase;
import net.magister.liberanimalium.chassis.component.IFaunaComponent;

public interface ModeEngine extends IFaunaComponent {
   LocomotionMode mode();

   void tick(SteeringIntent var1);

   @Override
   default void onAttach(FaunaBase host) {
   }

   @Override
   default void serverTick() {
   }
}
