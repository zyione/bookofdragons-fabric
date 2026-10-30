package net.magister.liberanimalium.anim.rig;

import java.util.Locale;
import net.magister.liberanimalium.anim.molang.Namespace;

public final class GaitQueries {
   public static final String PREFIX = "gait.";
   public static final Namespace NS = new Namespace("gait.", GaitQueries.Var.values());

   private GaitQueries() {
   }

   public enum Var {
      FRONT_SWAY,
      REAR_SWAY,
      FRONT_LIFT,
      REAR_LIFT,
      FRONT_STRIDE,
      REAR_STRIDE,
      SWAY,
      LIFT,
      STRIDE,
      SPEED,
      FORWARD_SPEED,
      LATERAL_SPEED,
      YAW_RATE,
      ACCEL_FORWARD,
      ACCEL_RIGHT,
      SUPPORT_RIGHT,
      SUPPORT_FORWARD;

      public static final int COUNT = values().length;
      public final String key = this.name().toLowerCase(Locale.ROOT);

      public static GaitQueries.Var byKey(String key) {
         for (GaitQueries.Var v : values()) {
            if (v.key.equals(key)) {
               return v;
            }
         }

         return null;
      }
   }
}
