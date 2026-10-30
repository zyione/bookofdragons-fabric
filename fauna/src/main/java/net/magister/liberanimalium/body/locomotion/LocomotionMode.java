package net.magister.liberanimalium.body.locomotion;

import net.magister.liberanimalium.body.state.TransportMode;

public enum LocomotionMode {
   WALK,
   FLY,
   SWIM,
   DIVE,
   CLIMB;

   public TransportMode toTransportMode() {
      return switch (this) {
         case WALK -> TransportMode.GROUNDED;
         case FLY -> TransportMode.AIRBORNE;
         case SWIM, DIVE -> TransportMode.AQUATIC;
         case CLIMB -> TransportMode.CLIMBING;
      };
   }

   public static LocomotionMode fromTransportMode(TransportMode mode) {
      return switch (mode) {
         case GROUNDED -> WALK;
         case AIRBORNE -> FLY;
         case AQUATIC -> SWIM;
         case CLIMBING -> CLIMB;
      };
   }
}
