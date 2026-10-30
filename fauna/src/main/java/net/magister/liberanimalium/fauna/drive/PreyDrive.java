package net.magister.liberanimalium.fauna.drive;

import org.jetbrains.annotations.Nullable;
import net.magister.liberanimalium.fauna.api.Check;
import net.magister.liberanimalium.fauna.api.RelationType;
import net.magister.liberanimalium.fauna.identity.Trait;
import net.magister.liberanimalium.fauna.know.Representation;
import net.magister.liberanimalium.fauna.know.Wants;
import net.minecraft.util.Mth;

public final class PreyDrive implements Drive {
   public static final String ID = "prey";

   @Override
   public String id() {
      return "prey";
   }

   @Override
   public Wants.Want shows() {
      return Wants.Want.HUNGRY;
   }

   @Override
   public boolean occupies(DriveContext context) {
      return true;
   }

   @Override
   public String store() {
      return "hunger";
   }

   @Override
   public float score(DriveContext context) {
      float need = Math.max(context.reservoir("hunger"), bestProvided(context)) * Arousal.appetite(context);
      if (need <= 0.0F) {
         return 0.0F;
      }

      float theta = BidShape.threshold(context.trait(Trait.AGGRESSION), 0.3F, 0.7F);
      Representation best = bestPrey(context, null);
      float cue = best == null ? 0.0F : attract(context, best);
      float bid = BidShape.felt(need, theta) * BidShape.gate(cue, need);
      return best != null && askedToHuntTogether(context) ? Math.max(bid, 0.35F * need) : bid;
   }

   private static boolean askedToHuntTogether(DriveContext context) {
      for (Representation rep : context.known()) {
         Check asked = rep.asked();
         if (asked != null && rep.visible() && asked.engages() == Check.Engagement.HUNT_TOGETHER && context.estimate(rep).distanceTo(context.position()) <= 16.0) {
            return true;
         }
      }

      return false;
   }

   public static float bestProvided(DriveContext context) {
      float best = 0.0F;

      for (Representation rep : context.known()) {
         if (rep.provided() > best) {
            best = rep.provided();
         }
      }

      return best;
   }

   @Nullable
   public static Representation bestPrey(DriveContext context, @Nullable Representation current) {
      return Election.elect(context, current, rep -> attract(context, rep));
   }

   public static float attract(DriveContext context, Representation rep) {
      if (rep.relation().type() != RelationType.EATS) {
         return 0.0F;
      }

      float intensity = rep.relation().intensity();
      if (intensity <= 0.0F) {
         return 0.0F;
      }

      float find = Mth.clamp(rep.presence(), 0.0F, 1.0F);
      if (find <= 0.0F) {
         return 0.0F;
      }

      double distance = !rep.tethered() && rep.leashHolder() == null ? context.estimate(rep).distanceTo(context.position()) : 0.0;
      float gate = rep.unreachableAt(context.age(), 200) ? 0.25F : 1.0F;
      float clear = 1.0F - Mth.clamp(rep.loom(), 0.0F, 1.0F);
      return (float)Mth.clamp(intensity * find * gate * clear / falloff(distance), 0.0, 1.0);
   }

   public static double falloff(double distance) {
      return 0.5 * Math.pow(Math.max(0.0, distance), 1.5) + 0.5;
   }
}
