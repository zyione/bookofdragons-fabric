package net.magister.liberanimalium.fauna.drive;

import java.util.UUID;
import net.minecraft.util.Mth;

public final class PerchDrive implements Drive {
   public static final String ID = "perch";

   @Override
   public String id() {
      return "perch";
   }

   @Override
   public boolean needsASubject() {
      return true;
   }

   @Override
   public float score(DriveContext context) {
      UUID owner = context.perchInvitation();
      if (owner == null) {
         return 0.0F;
      }

      float fond = Mth.clamp(context.opinion(owner).like(), 0.0F, 1.0F);
      float bid = fond * context.complianceShare() * (1.0F - context.fightClaim()) * 0.7F;
      if (bid <= 0.0F) {
         return 0.0F;
      }

      if (context.perched()) {
         bid = Math.min(0.7F, bid + 0.1F);
      }

      return bid;
   }
}
