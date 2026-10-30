package net.magister.liberanimalium.fauna.drive;

public final class WanderDrive implements Drive {
   public static final String ID = "wander";
   public static final float FLOOR = 0.05F;

   @Override
   public String id() {
      return "wander";
   }

   @Override
   public float score(DriveContext context) {
      return 0.05F;
   }

   @Override
   public String store() {
      return null;
   }
}
