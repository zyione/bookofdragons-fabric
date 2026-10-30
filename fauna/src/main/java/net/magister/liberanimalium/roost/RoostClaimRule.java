package net.magister.liberanimalium.roost;

import org.jetbrains.annotations.Nullable;

public final class RoostClaimRule {
   private RoostClaimRule() {
   }

   @Nullable
   public static String refusal(RoostClaimRule.Facts f) {
      if (f.young()) {
         return "young";
      } else if (f.alreadyHolds()) {
         return "already holds it";
      } else if (f.freeSlots() <= 0) {
         return "full";
      } else if (f.conqueror()) {
         return null;
      } else if (f.tame() && !f.placedByPlayer()) {
         return "a tame creature claims only a roost a player built";
      } else {
         return !f.friendsWithAll() ? "held by a creature it is not friends with" : null;
      }
   }

   public record Facts(boolean young, boolean alreadyHolds, int freeSlots, boolean tame, boolean placedByPlayer, boolean friendsWithAll, boolean conqueror) {
   }
}
