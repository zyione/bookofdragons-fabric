package net.magister.liberanimalium.fauna.drive;

import java.util.ArrayList;
import java.util.List;
import org.jetbrains.annotations.Nullable;
import net.magister.liberanimalium.fauna.api.RelationType;
import net.magister.liberanimalium.fauna.know.Representation;
import net.minecraft.util.Mth;

public final class EnmityDrive implements Drive {
   public static final String ID = "enmity";

   @Override
   public String id() {
      return "enmity";
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
   public boolean satiates() {
      return false;
   }

   @Override
   public float score(DriveContext context) {
      Representation worst = worstEnemy(context, null);
      if (worst == null) {
         return 0.0F;
      }

      float menace = menace(context, worst);
      return menace <= 0.0F ? 0.0F : BidShape.felt(menace, theta(context));
   }

   public static float theta(DriveContext context) {
      return BidShape.threshold(context.courage(), 0.12F, 0.45F);
   }

   @Nullable
   public static Representation worstEnemy(DriveContext context, @Nullable Representation current) {
      return Election.elect(context, current, rep -> menace(context, rep));
   }

   @Override
   public boolean needsASubject() {
      return true;
   }

   public static float menace(DriveContext context, Representation rep) {
      return menace(context, rep, 1.0F);
   }

   public static float menace(DriveContext context, Representation rep, float shadeShare) {
      if (rep.relation().type() != RelationType.ATTACKS) {
         return 0.0F;
      }

      float intensity = rep.relation().intensity();
      if (intensity <= 0.0F) {
         return 0.0F;
      }

      float presence = Mth.clamp(rep.presence(), 0.0F, 1.0F);
      if (presence <= 0.0F) {
         return 0.0F;
      }

      float gate = rep.unreachableAt(context.age(), 200) ? 0.25F : 1.0F;
      double gap = context.estimate(rep).distanceTo(context.position());
      float nearness = Nearness.discount(gap, 48.0, 0.25F);
      float shade = 1.0F - Mth.clamp(rep.offering(), 0.0F, 1.0F) * Mth.clamp(shadeShare, 0.0F, 1.0F);
      return Mth.clamp(intensity * presence * gate * nearness * shade, 0.0F, 1.0F);
   }

   public static List<Representation> enemies(DriveContext context) {
      List<Representation> out = new ArrayList<>();

      for (Representation rep : context.known()) {
         if (menace(context, rep) > 0.0F) {
            out.add(rep);
         }
      }

      return out;
   }
}
