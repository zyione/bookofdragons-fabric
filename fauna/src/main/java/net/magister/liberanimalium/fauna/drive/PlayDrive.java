package net.magister.liberanimalium.fauna.drive;

import org.jetbrains.annotations.Nullable;
import net.magister.liberanimalium.fauna.api.Check;
import net.magister.liberanimalium.fauna.api.RelationType;
import net.magister.liberanimalium.fauna.identity.Trait;
import net.magister.liberanimalium.fauna.know.Courtship;
import net.magister.liberanimalium.fauna.know.Representation;
import net.magister.liberanimalium.fauna.know.Wants;
import net.minecraft.util.Mth;

public final class PlayDrive implements Drive {
   public static final String ID = "play";

   @Override
   public String id() {
      return "play";
   }

   @Override
   public Wants.Want shows() {
      return Wants.Want.PLAYFUL;
   }

   @Override
   public String store() {
      return "boredom";
   }

   @Override
   public boolean satiates() {
      return false;
   }

   @Override
   public boolean needsASubject() {
      return true;
   }

   @Override
   public float score(DriveContext context) {
      Representation mate = partner(context);
      if (mate == null) {
         return 0.0F;
      }

      float appetite = appetite(context);
      boolean asked = askedForGame(mate);
      if (mate.isPlayer() && context.playingWith(mate.id())) {
         float eager = Math.max(appetite, rough(context));
         float theta = BidShape.threshold(eager, 0.25F, 0.75F);
         float bid = BidShape.felt(eager, theta) * BidShape.gate(cue(context, mate), eager) * disposition(context, mate);
         return asked ? Math.max(bid, 0.35F) : bid;
      }

      if (asked) {
         return 0.35F * cue(context, mate);
      }

      float boredom = context.reservoir("boredom");
      return boredom <= 0.0F ? 0.0F : BidShape.felt(boredom, boredTheta(appetite)) * BidShape.gate(cue(context, mate), boredom) * disposition(context, mate);
   }

   private static float boredTheta(float appetite) {
      return BidShape.threshold(appetite, 0.3F, 1.5F);
   }

   private static boolean askedForGame(Representation rep) {
      Check asked = rep.asked();
      return rep.isPlayer() && asked != null && asked.engages() == Check.Engagement.TAG;
   }

   public static boolean playful(DriveContext context) {
      return context.reservoir("boredom") >= boredTheta(appetite(context));
   }

   public static float want(DriveContext context) {
      return BidShape.felt(context.reservoir("boredom"), boredTheta(appetite(context))) / 0.7F;
   }

   public static boolean playsRough(DriveContext context) {
      return rough(context) > appetite(context);
   }

   public static float appetite(DriveContext context) {
      float sympathy = context.trait(Trait.SYMPATHY);
      float briskness = context.trait(Trait.ENERGY);
      return Mth.clamp(sympathy * 0.65F + briskness * 0.35F, 0.0F, 1.0F);
   }

   public static float rough(DriveContext context) {
      return Mth.clamp((context.trait(Trait.DOMINANCE) + context.trait(Trait.AGGRESSION)) * 0.5F, 0.0F, 1.0F);
   }

   private static float cue(DriveContext context, Representation mate) {
      if (context.playingWith(mate.id())) {
         return 1.0F;
      }

      double gap = context.estimate(mate).distanceTo(context.position());
      return Nearness.discount(gap, 16.0, 0.25F);
   }

   static float disposition(DriveContext context, Representation mate) {
      if (context.playingWith(mate.id())) {
         return 1.0F;
      }

      float familiarity = !mate.answers() ? 1.0F : Mth.lerp(Mth.clamp(context.opinion(mate.id()).familiarity(), 0.0F, 1.0F), 0.25F, 1.0F);
      float freshness = Mth.lerp(1.0F - Mth.clamp(context.reservoir("energy"), 0.0F, 1.0F), 0.25F, 1.0F);
      return familiarity * freshness;
   }

   @Nullable
   public static Representation partner(DriveContext context) {
      Representation nearest = null;
      double nearestGap = Double.MAX_VALUE;
      Representation person = null;
      double personGap = Double.MAX_VALUE;
      Representation asked = null;
      double askedGap = Double.MAX_VALUE;
      Representation thing = null;
      double thingGap = Double.MAX_VALUE;

      for (Representation rep : context.known()) {
         if (context.playingWith(rep.id())) {
            return rep;
         }

         boolean plaything = !rep.answers();
         if ((!plaything || playsAt(rep))
            && (plaything || rep.isPlayer() || rep.playful())
            && (!rep.isPlayer() || rep.visible())
            && (plaything || playmate(context, rep))) {
            boolean wanted = askedForGame(rep);
            if (wanted || !context.playedWith(rep.id())) {
               double gap = context.estimate(rep).distanceTo(context.position());
               if (!(gap > 16.0)) {
                  if (plaything) {
                     if (gap < thingGap) {
                        thing = rep;
                        thingGap = gap;
                     }
                  } else if (wanted) {
                     if (gap < askedGap) {
                        asked = rep;
                        askedGap = gap;
                     }
                  } else if (rep.isPlayer()) {
                     if (gap < personGap) {
                        person = rep;
                        personGap = gap;
                     }
                  } else if (!(gap >= nearestGap)) {
                     nearest = rep;
                     nearestGap = gap;
                  }
               }
            }
         }
      }

      if (asked != null) {
         return asked;
      }

      if (aloof(context)) {
         if (thing != null) {
            return thing;
         } else {
            return person != null ? person : nearest;
         }
      } else if (nearest != null) {
         return nearest;
      } else {
         return person != null ? person : thing;
      }
   }

   static boolean aloof(DriveContext context) {
      return Courtship.comfort(context.trait(Trait.DOMINANCE), context.trait(Trait.NERVOUSNESS), context.trait(Trait.SYMPATHY)) >= 0.5F;
   }

   static boolean playsAt(Representation rep) {
      return rep.visible() && rep.relation().type() == RelationType.PLAYS_WITH;
   }

   public static boolean playmate(DriveContext context, Representation rep) {
      RelationType type = rep.relation().type();
      if (type == RelationType.PLAYS_WITH || type == RelationType.PACK) {
         return true;
      } else {
         return rep.sameSpecies() && type == RelationType.IGNORES ? true : context.opinion(rep.id()).like() > 0.5F;
      }
   }
}
