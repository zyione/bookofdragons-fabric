package net.magister.liberanimalium.render;

public final class ChainForceSwitch {
   private static volatile ChainForce force = ChainForce.NONE;

   private ChainForceSwitch() {
   }

   public static ChainForce get() {
      return force;
   }

   public static void set(ChainForce value) {
      force = value == null ? ChainForce.NONE : value;
   }
}
