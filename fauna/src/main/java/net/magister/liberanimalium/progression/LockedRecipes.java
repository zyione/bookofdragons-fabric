package net.magister.liberanimalium.progression;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Predicate;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

public final class LockedRecipes {
   private static final List<Predicate<ResourceLocation>> LOCKS = new CopyOnWriteArrayList<>();

   private LockedRecipes() {
   }

   public static void lock(Predicate<ResourceLocation> which) {
      LOCKS.add(which);
   }

   public static boolean locked(ResourceLocation recipe) {
      for (Predicate<ResourceLocation> lock : LOCKS) {
         if (lock.test(recipe)) {
            return true;
         }
      }

      return false;
   }

   public static boolean mayCraft(ServerPlayer player, ResourceLocation recipe) {
      return !locked(recipe) || player.getRecipeBook().contains(recipe);
   }
}
