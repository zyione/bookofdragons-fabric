package net.magister.liberanimalium.fauna.drive;

import java.util.ArrayList;
import java.util.List;
import org.jetbrains.annotations.Nullable;
import net.magister.liberanimalium.fauna.api.RelationType;
import net.magister.liberanimalium.fauna.identity.Trait;
import net.magister.liberanimalium.fauna.know.Representation;
import net.minecraft.util.Mth;

public final class RivalDrive implements Drive {
   public static final String ID = "rival";

   @Override
   public String id() {
      return "rival";
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
      return Presses.sum(context, RivalDrive::held);
   }

   @Override
   public boolean needsASubject() {
      return true;
   }

   public static float press(DriveContext context, Representation rep) {
      if (rep.relation().type() != RelationType.RIVAL) {
         return 0.0F;
      }

      float loudness = rep.alarm();
      if (loudness <= 0.0F) {
         return 0.0F;
      }

      if (context.settledWith(rep.id())) {
         return 0.0F;
      }

      float depth = Nearness.discount(
         context.estimate(rep).distanceTo(context.position()), SocialTuning.territoryRadius(context.territoryBase(), context.trait(Trait.DOMINANCE)), 0.25F
      );
      float patience = rep.unreachableAt(context.age(), 200) ? 0.25F : 1.0F;
      return Mth.clamp(loudness * patience * depth, 0.0F, 1.0F);
   }

   public static float held(DriveContext context, Representation rep) {
      float live = press(context, rep);
      if (context.disputeOpenWith(rep.id()) && !context.settledWith(rep.id())) {
         return rep.unreachableAt(context.age(), 200) ? live : Math.max(live, threshold(context));
      } else {
         return live;
      }
   }

   public static float threshold(DriveContext context) {
      return BidShape.threshold(context.trait(Trait.AGGRESSION), 0.2F, 0.7F);
   }

   public static float gradientOf(DriveContext context, Representation rep) {
      float radius = SocialTuning.territoryRadius(context.territoryBase(), context.trait(Trait.DOMINANCE));
      double distance = context.estimate(rep).distanceTo(context.position());
      return SocialTuning.gradient(distance, radius);
   }

   @Nullable
   public static Representation worstRival(DriveContext context) {
      return Presses.worst(context, RivalDrive::held);
   }

   public static List<Representation> rivals(DriveContext context) {
      List<Representation> out = new ArrayList<>();

      for (Representation rep : context.known()) {
         if (held(context, rep) > 0.0F) {
            out.add(rep);
         }
      }

      return out;
   }
}
