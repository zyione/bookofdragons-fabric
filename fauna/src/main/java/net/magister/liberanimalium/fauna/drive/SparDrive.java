package net.magister.liberanimalium.fauna.drive;

import org.jetbrains.annotations.Nullable;
import net.magister.liberanimalium.fauna.api.Check;
import net.magister.liberanimalium.fauna.know.Representation;

public final class SparDrive implements Drive {
   public static final String ID = "spar";

   @Override
   public String id() {
      return "spar";
   }

   @Override
   public boolean needsASubject() {
      return true;
   }

   @Override
   public boolean occupies(DriveContext context) {
      return true;
   }

   @Override
   public float score(DriveContext context) {
      Representation mate = partner(context);
      if (mate == null) {
         return 0.0F;
      }

      if (context.sparringWith(mate.id())) {
         return 0.35F;
      }

      double gap = context.estimate(mate).distanceTo(context.position());
      return 0.35F * Nearness.discount(gap, 16.0, 0.25F);
   }

   @Nullable
   public static Representation partner(DriveContext context) {
      Representation best = null;
      double bestGap = Double.MAX_VALUE;

      for (Representation rep : context.known()) {
         if (context.sparringWith(rep.id())) {
            return rep;
         }

         Check asked = rep.asked();
         if (asked != null && asked.engages() == Check.Engagement.SPAR && rep.visible() && !context.sparredWith(rep.id())) {
            double gap = context.estimate(rep).distanceTo(context.position());
            if (!(gap > 16.0) && !(gap >= bestGap)) {
               best = rep;
               bestGap = gap;
            }
         }
      }

      return best;
   }
}
