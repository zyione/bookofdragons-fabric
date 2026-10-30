package net.magister.liberanimalium.ride.render;

import java.util.Locale;

public enum RiderBone {
   ROOT,
   TORSO,
   HEAD,
   RIGHT_ARM,
   LEFT_ARM,
   RIGHT_LEG,
   LEFT_LEG;

   public static final int COUNT = values().length;
   public final String key = this.name().toLowerCase(Locale.ROOT);

   public RiderBone mirror() {
      return switch (this) {
         case RIGHT_ARM -> LEFT_ARM;
         case LEFT_ARM -> RIGHT_ARM;
         case RIGHT_LEG -> LEFT_LEG;
         case LEFT_LEG -> RIGHT_LEG;
         default -> this;
      };
   }

   public static RiderBone byKey(String key) {
      for (RiderBone b : values()) {
         if (b.key.equals(key)) {
            return b;
         }
      }

      return null;
   }

   public enum Dof {
      RX,
      RY,
      RZ,
      TX,
      TY,
      TZ;

      public static final int COUNT = values().length;
      public final String key = this.name().toLowerCase(Locale.ROOT);

      public boolean rotation() {
         return this.ordinal() < 3;
      }

      public boolean mirrorNegates() {
         return this == RY || this == RZ || this == TX;
      }

      public static RiderBone.Dof byKey(String key) {
         for (RiderBone.Dof d : values()) {
            if (d.key.equals(key)) {
               return d;
            }
         }

         return null;
      }
   }
}
