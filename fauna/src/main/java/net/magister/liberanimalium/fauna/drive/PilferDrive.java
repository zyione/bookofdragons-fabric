package net.magister.liberanimalium.fauna.drive;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.jetbrains.annotations.Nullable;
import net.magister.liberanimalium.fauna.identity.Trait;
import net.magister.liberanimalium.fauna.know.Larder;
import net.magister.liberanimalium.fauna.know.Representation;
import net.minecraft.util.Mth;

public final class PilferDrive implements Drive {
   public static final String ID = "pilfer";

   @Override
   public String id() {
      return "pilfer";
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
      if (context.loot() != null) {
         return ForageDrive.keenness(context) * 0.7F;
      }

      Larder.Offer best = bestOffer(context, null);
      return best == null ? 0.0F : BidShape.felt(need(context, best), theta(context));
   }

   public static float need(DriveContext context, Larder.Offer offer) {
      float like = Mth.clamp(context.opinion(offer.holder()).like(), 0.0F, 1.0F);
      return BegDrive.worth(context, offer) * ForageDrive.keenness(context) * (1.0F - like);
   }

   public static float theta(DriveContext context) {
      float boldness = (context.trait(Trait.BRAVERY) + context.trait(Trait.AGGRESSION)) * 0.5F;
      return BidShape.threshold(boldness, 0.15F, 0.6F);
   }

   @Nullable
   public static Larder.Offer bestOffer(DriveContext context, @Nullable UUID incumbent) {
      List<Larder.Offer> candidates = new ArrayList<>();
      Larder.Offer current = null;

      for (Larder.Offer offer : context.offers()) {
         if (offer.edible() && heldByPlayer(context, offer.holder())) {
            candidates.add(offer);
            if (offer.holder().equals(incumbent)) {
               current = offer;
            }
         }
      }

      return Election.elect(current, candidates, offerx -> need(context, offerx), 2.0F);
   }

   private static boolean heldByPlayer(DriveContext context, UUID holder) {
      for (Representation rep : context.known()) {
         if (rep.id().equals(holder)) {
            return rep.isPlayer();
         }
      }

      return false;
   }
}
