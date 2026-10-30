package net.magister.liberanimalium.ride.render;

import java.util.Locale;
import net.magister.liberanimalium.anim.molang.Namespace;

public final class RiderQueries {
   public static final String PREFIX = "rider.";
   public static final Namespace NS = new Namespace("rider.", RiderQueries.Var.values());

   private RiderQueries() {
   }

   public enum Var {
      LOAD_FORWARD,
      LOAD_RIGHT,
      LOAD_UP,
      ACCEL_FORWARD,
      ACCEL_RIGHT,
      ACCEL_UP,
      GRAVITY_FORWARD,
      GRAVITY_RIGHT,
      GRAVITY_UP,
      SPEED,
      TIME,
      MOUNTED,
      LOOK_YAW,
      LOOK_PITCH,
      BOB;

      public static final int COUNT = values().length;
      public final String key = this.name().toLowerCase(Locale.ROOT);

      public static RiderQueries.Var byKey(String key) {
         for (RiderQueries.Var v : values()) {
            if (v.key.equals(key)) {
               return v;
            }
         }

         return null;
      }
   }
}
