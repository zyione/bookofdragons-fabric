package net.magister.bookofdragons.compat.v20;

import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.pathfinder.BlockPathTypes;

public final class PathCostsImpl {
   private PathCostsImpl() {
   }

   public static void applyDragon(Mob mob) {
      mob.setPathfindingMalus(BlockPathTypes.WATER, -1.0F);
      mob.setPathfindingMalus(BlockPathTypes.WATER_BORDER, 8.0F);
      mob.setPathfindingMalus(BlockPathTypes.LAVA, 0.0F);
      mob.setPathfindingMalus(BlockPathTypes.DANGER_FIRE, 0.0F);
      mob.setPathfindingMalus(BlockPathTypes.DAMAGE_FIRE, 0.0F);
   }
}
