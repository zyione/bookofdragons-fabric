package net.magister.liberanimalium.fauna.drive;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.jetbrains.annotations.Nullable;
import net.magister.liberanimalium.fauna.identity.Trait;
import net.magister.liberanimalium.fauna.know.Chests;
import net.magister.liberanimalium.fauna.know.Representation;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;

public final class LoafDrive implements Drive {
   public static final String ID = "loaf";

   @Override
   public String id() {
      return "loaf";
   }

   @Override
   public boolean needsASubject() {
      return true;
   }

   @Override
   public float score(DriveContext context) {
      Chests.Chest best = bestChest(context, null);
      return best == null ? 0.0F : BidShape.feltUnder(need(context, best), theta(context), 0.15F) * Arousal.appetite(context);
   }

   public static float need(DriveContext context, Chests.Chest chest) {
      float fond = Mth.clamp(context.opinion(chest.opener()).like(), 0.0F, 1.0F);
      return fond <= 0.0F ? 0.0F : chest.belief(context.age()) * fond * attendance(context, chest.opener());
   }

   public static float attendance(DriveContext context, UUID opener) {
      for (Representation rep : context.known()) {
         if (rep.id().equals(opener)) {
            return Mth.clamp(rep.find(context.age()), 0.0F, 1.0F);
         }
      }

      return 0.0F;
   }

   public static float theta(DriveContext context) {
      return BidShape.threshold(context.trait(Trait.SYMPATHY), 0.25F, 0.75F);
   }

   @Nullable
   public static Chests.Chest bestChest(DriveContext context, @Nullable BlockPos incumbent) {
      List<Chests.Chest> candidates = new ArrayList<>();
      Chests.Chest current = null;

      for (Chests.Chest chest : context.chests()) {
         candidates.add(chest);
         if (chest.at().equals(incumbent)) {
            current = chest;
         }
      }

      return Election.elect(current, candidates, chestx -> need(context, chestx), 2.0F);
   }
}
