package net.magister.liberanimalium.spawn;

import org.jetbrains.annotations.Nullable;

public final class DespawnRule {
   private DespawnRule() {
   }

   public static boolean mayDespawn(DespawnRule.Facts facts) {
      return refusal(facts) == null;
   }

   public static int unwatched(int ticks, double nearestPlayer, SpawnSettings.Despawn despawn) {
      if (nearestPlayer <= despawn.safeRadius()) {
         return 0;
      } else {
         return ticks == Integer.MAX_VALUE ? ticks : ticks + 1;
      }
   }

   public static boolean timeUp(int ticks, double roll, SpawnSettings.Despawn despawn) {
      return ticks >= despawn.unwatchedTicks() && roll < despawn.chancePerTick();
   }

   @Nullable
   public static String refusal(DespawnRule.Facts facts) {
      if (facts.roosted()) {
         return "holds a roost";
      } else if (facts.tame()) {
         return "tame";
      } else if (facts.persistent()) {
         return "persistent";
      } else if (facts.young()) {
         return "young";
      } else if (facts.courted()) {
         return "a player has started taming it";
      } else if (facts.held()) {
         return "leashed";
      } else if (facts.paired()) {
         return "in a pair session";
      } else {
         return facts.lifeTicks() < 600L ? "younger than one spawn period" : null;
      }
   }

   public record Facts(boolean roosted, boolean tame, boolean persistent, boolean young, boolean courted, boolean held, boolean paired, long lifeTicks) {
   }
}
