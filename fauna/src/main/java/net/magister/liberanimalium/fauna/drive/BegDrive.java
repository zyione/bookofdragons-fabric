package net.magister.liberanimalium.fauna.drive;

import java.util.List;
import java.util.UUID;
import org.jetbrains.annotations.Nullable;
import net.magister.liberanimalium.fauna.identity.Trait;
import net.magister.liberanimalium.fauna.know.Larder;
import net.magister.liberanimalium.fauna.know.Representation;
import net.magister.liberanimalium.fauna.know.Wants;
import net.minecraft.util.Mth;

public final class BegDrive implements Drive {
   public static final String ID = "beg";

   @Override
   public String id() {
      return "beg";
   }

   @Override
   public Wants.Want shows() {
      return Wants.Want.HUNGRY;
   }

   @Override
   public boolean needsASubject() {
      return true;
   }

   @Override
   public float score(DriveContext context) {
      Larder.Offer best = bestOffer(context, null);
      return best == null ? 0.0F : worth(context, best) * keenness(context, best) * band(context, best.holder());
   }

   public static float keenness(DriveContext context, Larder.Offer offer) {
      return offer.edible() ? ForageDrive.keenness(context) : Mth.lerp(Mth.clamp(context.trait(Trait.SYMPATHY), 0.0F, 1.0F), 0.35F, 1.0F);
   }

   public static float band(DriveContext context, UUID holder) {
      float like = Mth.clamp(context.opinion(holder).like() / 0.5F, 0.0F, 1.0F);
      return Mth.lerp(like, 0.15F, 0.35F);
   }

   @Nullable
   public static Larder.Offer bestOffer(DriveContext context, @Nullable UUID incumbent) {
      List<Larder.Offer> candidates = context.offers();
      Larder.Offer current = null;
      if (incumbent != null) {
         for (Larder.Offer offer : candidates) {
            if (offer.holder().equals(incumbent)) {
               current = offer;
               break;
            }
         }
      }

      return Election.elect(current, candidates, offerx -> worth(context, offerx), 2.0F);
   }

   public static float worth(DriveContext context, Larder.Offer offer) {
      Representation holder = null;

      for (Representation rep : context.known()) {
         if (rep.id().equals(offer.holder())) {
            holder = rep;
            break;
         }
      }

      if (holder == null) {
         return 0.0F;
      }

      double gap = context.estimate(holder).distanceTo(context.position());
      float near = Nearness.discount(gap, context.sightRange(), 0.25F);
      return Mth.clamp(offer.craving(), 0.0F, 1.0F) * (1.0F - Mth.clamp(holder.alarm(), 0.0F, 1.0F)) * near;
   }
}
