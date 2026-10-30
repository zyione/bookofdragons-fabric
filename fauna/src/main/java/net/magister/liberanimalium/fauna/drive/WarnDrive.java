package net.magister.liberanimalium.fauna.drive;

import org.jetbrains.annotations.Nullable;
import net.magister.liberanimalium.fauna.know.Larder;
import net.magister.liberanimalium.fauna.know.Representation;
import net.magister.liberanimalium.fauna.know.Wants;
import net.minecraft.util.Mth;

public final class WarnDrive implements Drive {
   public static final String ID = "warn";

   @Override
   public String id() {
      return "warn";
   }

   @Override
   public Wants.Want shows() {
      return Wants.Want.THREATENED;
   }

   @Override
   public Representation warns(DriveContext context) {
      return subject(context);
   }

   @Override
   public boolean occupies(DriveContext context) {
      return true;
   }

   @Override
   public boolean needsASubject() {
      return true;
   }

   @Override
   public float score(DriveContext context) {
      Representation about = subject(context);
      if (about == null) {
         return 0.0F;
      }

      float lead = Math.min(Math.max(fearShadow(context), warShadow(context)), 0.2F);
      if (lead <= 0.0F) {
         return 0.0F;
      }

      double gap = context.estimate(about).distanceTo(context.position());
      float near = Nearness.discount(gap, context.sightRange(), 0.25F);
      return lead * near * (1.0F - offerRelief(context, about));
   }

   public static float offerRelief(DriveContext context, Representation about) {
      float craving = 0.0F;

      for (Larder.Offer offer : context.offers()) {
         if (offer.holder().equals(about.id())) {
            craving = Mth.clamp(offer.craving(), 0.0F, 1.0F);
            break;
         }
      }

      if (craving <= 0.0F) {
         return 0.0F;
      }

      float earned = Mth.clamp(context.opinion(about.id()).like() / 0.5F, 0.0F, 1.0F);
      return 0.75F * craving * earned;
   }

   public static float fearShadow(DriveContext context) {
      float store = ThreatDrive.threatStore(context);
      return store <= 0.0F ? 0.0F : BidShape.feltUrgent(store, 0.6F * ThreatDrive.theta(context));
   }

   public static float warShadow(DriveContext context) {
      Representation worst = EnmityDrive.worstEnemy(context, null);
      if (worst == null) {
         return 0.0F;
      }

      float menace = EnmityDrive.menace(context, worst, 0.5F);
      return menace <= 0.0F ? 0.0F : BidShape.felt(menace, 0.6F * EnmityDrive.theta(context));
   }

   @Nullable
   public static Representation subject(DriveContext context) {
      float fear = fearShadow(context);
      float war = warShadow(context);
      if (fear <= 0.0F && war <= 0.0F) {
         return null;
      } else {
         return war >= fear ? EnmityDrive.worstEnemy(context, null) : worstFright(context);
      }
   }

   @Nullable
   private static Representation worstFright(DriveContext context) {
      Representation best = null;
      float worst = 0.0F;

      for (Representation rep : context.known()) {
         float fright = ThreatDrive.fright(rep);
         if (fright > worst) {
            worst = fright;
            best = rep;
         }
      }

      return best;
   }

   public static float actBid(DriveContext context) {
      return Math.max(new ThreatDrive().score(context), new EnmityDrive().score(context));
   }

   public static String leadingSide(DriveContext context) {
      float fear = fearShadow(context);
      float war = warShadow(context);
      if (fear <= 0.0F && war <= 0.0F) {
         return "nothing";
      } else {
         return war >= fear ? "war" : "fear";
      }
   }
}
