package net.magister.liberanimalium.fauna.drive;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.jetbrains.annotations.Nullable;
import net.magister.liberanimalium.fauna.api.RelationType;
import net.magister.liberanimalium.fauna.identity.Trait;
import net.magister.liberanimalium.fauna.know.Courage;
import net.magister.liberanimalium.fauna.know.Herd;
import net.magister.liberanimalium.fauna.know.Representation;
import net.magister.liberanimalium.fauna.know.Wants;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

public final class ThreatDrive implements Drive {
   public static final String ID = "threat";

   @Override
   public String id() {
      return "threat";
   }

   @Override
   public Wants.Want shows() {
      return Wants.Want.THREATENED;
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
      return fear(context);
   }

   public static float fear(DriveContext context) {
      float store = threatStore(context);
      return store <= 0.0F ? 0.0F : BidShape.feltUrgent(store, theta(context));
   }

   @Override
   public float settles(DriveContext context) {
      float own = context.trait(Trait.BRAVERY);
      return Courage.calm(own >= 1.0F ? 0.0F : (context.courage() - own) / (1.0F - own));
   }

   public static float theta(DriveContext context) {
      float timidity = 1.0F - context.courage();
      return BidShape.threshold(timidity, 0.25F, SurvivalTuning.THREAT_THETA_BOLD);
   }

   public static float threatStore(DriveContext context) {
      float sum = 0.0F;

      for (Representation rep : context.known()) {
         sum += fright(rep);
      }

      float herd = context.contagion();
      if (herd >= 0.34F) {
         sum += herd;
      }

      return Mth.clamp(sum, 0.0F, 1.0F);
   }

   public static float fright(Representation rep) {
      return rep.relation().type() == RelationType.AFRAID ? rep.alarm() : 0.0F;
   }

   @Nullable
   public static UUID leadThreat(DriveContext context) {
      UUID lead = null;
      float most = 0.0F;

      for (Representation rep : context.known()) {
         float fright = fright(rep);
         if (fright > most && rep.id() != null) {
            most = fright;
            lead = rep.id();
         }
      }

      return lead;
   }

   public static List<Vec3> threatPoints(DriveContext context) {
      List<Vec3> out = new ArrayList<>();

      for (Representation rep : context.known()) {
         if (fright(rep) > 0.0F) {
            out.add(context.estimate(rep));
         }
      }

      if (context.contagion() >= 0.34F) {
         Vec3 run = Herd.fleeing(context.known(), context.age(), 30, 0.05);
         if (run != null) {
            out.add(context.position().subtract(run.normalize().scale(24.0)));
         }
      }

      return out;
   }
}
