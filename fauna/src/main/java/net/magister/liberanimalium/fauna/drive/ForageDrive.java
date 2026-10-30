package net.magister.liberanimalium.fauna.drive;

import java.util.List;
import java.util.UUID;
import org.jetbrains.annotations.Nullable;
import net.magister.liberanimalium.fauna.identity.Trait;
import net.magister.liberanimalium.fauna.know.Larder;
import net.magister.liberanimalium.fauna.know.Wants;
import net.minecraft.util.Mth;

public final class ForageDrive implements Drive {
   public static final String ID = "forage";

   @Override
   public String id() {
      return "forage";
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
      Larder.Morsel best = bestMorsel(context, null);
      return best == null ? 0.0F : worth(context, best) * 0.7F;
   }

   @Nullable
   public static Larder.Morsel bestMorsel(DriveContext context, @Nullable UUID incumbent) {
      List<Larder.Morsel> candidates = context.morsels();
      Larder.Morsel current = null;
      if (incumbent != null) {
         for (Larder.Morsel morsel : candidates) {
            if (morsel.id().equals(incumbent)) {
               current = morsel;
               break;
            }
         }
      }

      return Election.elect(current, candidates, morselx -> worth(context, morselx), 2.0F);
   }

   public static float worth(DriveContext context, Larder.Morsel morsel) {
      double gap = morsel.pileAt().distanceTo(context.position());
      float near = Nearness.discount(gap, context.sightRange(), 0.25F);
      float base = Mth.clamp(morsel.craving(), 0.0F, 1.0F) * (1.0F - Mth.clamp(morsel.loom(), 0.0F, 1.0F)) * near * Arousal.appetite(context);
      return morsel.edible() ? base * keenness(context) : base;
   }

   public static float keenness(DriveContext context) {
      float theta = BidShape.threshold(context.trait(Trait.AGGRESSION), 0.3F, 0.7F);
      float felt = BidShape.felt(context.reservoir("hunger"), theta);
      return Mth.lerp(Mth.clamp(felt, 0.0F, 1.0F), 0.35F, 1.0F);
   }
}
