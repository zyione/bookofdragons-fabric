package net.magister.bookofdragons.compat;

import net.magister.bookofdragons.compat.v20.PathCostsImpl;
import net.minecraft.world.entity.Mob;

public final class PathCosts {
   private PathCosts() {
   }

   public static void applyDragon(Mob mob) {
      PathCostsImpl.applyDragon(mob);
   }
}
