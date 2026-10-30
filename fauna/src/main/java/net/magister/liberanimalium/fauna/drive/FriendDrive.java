package net.magister.liberanimalium.fauna.drive;

import org.jetbrains.annotations.Nullable;
import net.magister.liberanimalium.fauna.identity.Trait;
import net.magister.liberanimalium.fauna.know.Representation;
import net.magister.liberanimalium.fauna.know.Wants;
import net.minecraft.util.Mth;

public final class FriendDrive implements Drive {
   public static final String ID = "friend";

   @Override
   public String id() {
      return "friend";
   }

   @Override
   public Wants.Want shows() {
      return Wants.Want.LONELY;
   }

   @Override
   public String store() {
      return "company";
   }

   @Override
   public float score(DriveContext context) {
      Representation friend = bestFriend(context);
      if (friend == null) {
         return 0.0F;
      }

      float like = context.opinion(friend.id()).like() * context.tie(friend);
      float apart = context.reservoir("company");
      float gap = BidShape.beyond(context.estimate(friend).distanceTo(context.position()), context.flockRadius(), context.sightRange());
      float theta = BidShape.threshold(context.trait(Trait.SYMPATHY), 0.25F, 0.75F);
      return Mth.clamp(like, 0.0F, 1.0F) * BidShape.felt(apart, theta) * BidShape.gate(gap, apart);
   }

   @Nullable
   public static Representation bestFriend(DriveContext context) {
      Representation best = null;
      float bestLike = 0.5F;

      for (Representation rep : context.known()) {
         float like = context.opinion(rep.id()).like();
         if (!(like <= bestLike) && !(context.tie(rep) <= 0.0F) && like > bestLike) {
            bestLike = like;
            best = rep;
         }
      }

      return best;
   }
}
