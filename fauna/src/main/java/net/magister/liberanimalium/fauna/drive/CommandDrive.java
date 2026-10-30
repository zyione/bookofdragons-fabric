package net.magister.liberanimalium.fauna.drive;

import net.magister.liberanimalium.fauna.api.Order;

public final class CommandDrive implements Drive {
   public static final String ID = "command";

   @Override
   public String id() {
      return "command";
   }

   @Override
   public boolean needsASubject() {
      return true;
   }

   @Override
   public boolean occupies(DriveContext context) {
      return context.order() == Order.FOLLOW;
   }

   @Override
   public float score(DriveContext context) {
      return context.order() == Order.FREE ? 0.0F : context.complianceShare() * (1.0F - context.fightClaim()) * 0.7F;
   }
}
