package net.magister.liberanimalium.fauna.move;

import net.magister.liberanimalium.fauna.api.MoveMode;

public enum ModeTransition {
   TAKEOFF(MoveMode.AIR),
   LAND(MoveMode.GROUND),
   WATER_ENTER(MoveMode.WATER),
   WATER_EXIT(MoveMode.GROUND),
   SPLASH_DOWN(MoveMode.WATER),
   CLIMB_ON(MoveMode.CLIMB),
   CLIMB_OFF(MoveMode.GROUND);

   private final MoveMode destination;

   ModeTransition(MoveMode destination) {
      this.destination = destination;
   }

   public MoveMode destination() {
      return this.destination;
   }
}
