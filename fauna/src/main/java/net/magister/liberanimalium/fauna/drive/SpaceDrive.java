package net.magister.liberanimalium.fauna.drive;

import org.jetbrains.annotations.Nullable;
import net.magister.liberanimalium.fauna.api.RelationType;
import net.magister.liberanimalium.fauna.identity.Trait;
import net.magister.liberanimalium.fauna.know.Bend;
import net.magister.liberanimalium.fauna.know.Representation;

public final class SpaceDrive implements Drive {
   public static final String ID = "space";

   @Override
   public String id() {
      return "space";
   }

   @Override
   public Representation warns(DriveContext context) {
      return worstCrowder(context);
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
   public float score(DriveContext context) {
      float store = pressure(context);
      return store <= 0.0F ? 0.0F : BidShape.felt(store, threshold(context));
   }

   public static float pressure(DriveContext context) {
      return Presses.sum(context, SpaceDrive::held);
   }

   public static float held(DriveContext context, Representation rep) {
      return !crowdable(context, rep) ? 0.0F : rep.crowding();
   }

   public static boolean crowdable(DriveContext context, Representation rep) {
      RelationType type = rep.relation().type();
      if (Bend.isNegative(type)) {
         return false;
      } else if (type == RelationType.PACK || type == RelationType.PLAYS_WITH) {
         return false;
      } else if (type == RelationType.EATS) {
         return false;
      } else {
         return rep.afloat() ? false : context.opinion(rep.id()).like() <= 0.5F;
      }
   }

   public static float threshold(DriveContext context) {
      return SocialTuning.spaceThreshold(context.trait(Trait.AGGRESSION), context.trait(Trait.SYMPATHY));
   }

   @Nullable
   public static Representation worstCrowder(DriveContext context) {
      return Presses.worst(context, SpaceDrive::held);
   }
}
