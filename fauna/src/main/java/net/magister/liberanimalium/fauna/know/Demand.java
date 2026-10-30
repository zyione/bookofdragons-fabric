package net.magister.liberanimalium.fauna.know;

import net.magister.liberanimalium.fauna.api.Order;

public final class Demand {
   private Demand() {
   }

   public static float of(Order order, float worstAlarm, float ownerDanger, float bravery) {
      return switch (order) {
         case STAY -> 0.1F + 0.4F * worstAlarm;
         case FOLLOW -> 0.1F + 0.5F * ownerDanger * (1.0F - bravery);
         case FREE -> 0.0F;
      };
   }

   public static boolean obeys(float compliance, float demand) {
      return compliance >= demand;
   }
}
