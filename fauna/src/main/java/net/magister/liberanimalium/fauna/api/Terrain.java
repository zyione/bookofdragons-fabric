package net.magister.liberanimalium.fauna.api;

import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.Vec3;

public interface Terrain {
   boolean solid(BlockPos var1);

   boolean fluid(BlockPos var1);

   int skyLight(BlockPos var1);

   boolean standable(BlockPos var1);

   default boolean chestTop(BlockPos pos) {
      return false;
   }

   float danger(Vec3 var1);
}
