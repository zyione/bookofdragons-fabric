package net.magister.liberanimalium.fauna.behavior;

import java.util.UUID;
import org.jetbrains.annotations.Nullable;

public final class AbilityElection {
   private AbilityElection() {
   }

   @Nullable
   public static UUID armedAgainst(@Nullable Fighting fight) {
      return fight != null && fight.armed() ? fight.engagedWith() : null;
   }

   public static boolean spent(@Nullable Fighting fight) {
      return fight != null && fight.engagement().breakingOff();
   }

   public static boolean fleeing(@Nullable Behavior running) {
      return running instanceof Flee;
   }

   public static int commitment(@Nullable Fighting fight, long now) {
      return fight == null ? 0 : fight.engagement().committedTicks(now);
   }
}
