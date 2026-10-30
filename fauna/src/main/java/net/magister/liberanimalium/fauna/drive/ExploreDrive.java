package net.magister.liberanimalium.fauna.drive;

import net.magister.liberanimalium.fauna.identity.Trait;
import net.magister.liberanimalium.fauna.know.Wants;

public final class ExploreDrive implements Drive {
   public static final String ID = "explore";

   @Override
   public String id() {
      return "explore";
   }

   @Override
   public Wants.Want shows() {
      return Wants.Want.RESTLESS;
   }

   @Override
   public float score(DriveContext context) {
      float freshness = context.freshGround();
      return freshness <= 0.0F ? 0.0F : 0.12F * freshness * context.trait(Trait.ENERGY);
   }
}
