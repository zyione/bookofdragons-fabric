package net.magister.liberanimalium.fauna.drive;

import net.magister.liberanimalium.fauna.identity.Trait;
import net.magister.liberanimalium.fauna.know.Wants;
import net.minecraft.util.Mth;

public final class RestDrive implements Drive {
   public static final String ID = "rest";
   private static final RestDrive BID = new RestDrive();

   @Override
   public String id() {
      return "rest";
   }

   @Override
   public Wants.Want shows() {
      return Wants.Want.TIRED;
   }

   @Override
   public String store() {
      return "sleep";
   }

   @Override
   public boolean satiates() {
      return false;
   }

   @Override
   public float score(DriveContext context) {
      float debt = context.reservoir("sleep");
      if (debt <= 0.0F) {
         return 0.0F;
      }

      if (context.carrying()) {
         return 0.0F;
      }

      float languor = 1.0F - context.trait(Trait.ENERGY);
      float active = BidShape.threshold(languor, 0.9F, 1.0F);
      float inactive = BidShape.threshold(languor, 0.2F, 0.35F);
      float theta = Mth.lerp(context.sleepPhase(), active, inactive);
      if (context.asleep()) {
         theta *= 0.5F;
      }

      return BidShape.felt(debt, theta) * safety(context);
   }

   public static boolean overcome(DriveContext context) {
      return context.speciesWeight("rest") * BID.score(context) >= 0.85F;
   }

   private static float safety(DriveContext context) {
      return Mth.clamp(1.0F - ThreatDrive.threatStore(context), 0.0F, 1.0F);
   }
}
