package net.magister.liberanimalium.fauna.drive;

public final class BuckDrive implements Drive {
   public static final String ID = "buck";

   @Override
   public String id() {
      return "buck";
   }

   @Override
   public boolean occupies(DriveContext context) {
      return true;
   }

   @Override
   public String store() {
      return null;
   }

   @Override
   public boolean needsASubject() {
      return true;
   }

   @Override
   public float score(DriveContext context) {
      return context.unwelcomeRider() == null ? 0.0F : BidShape.feltUrgent(1.0F, 0.25F);
   }
}
