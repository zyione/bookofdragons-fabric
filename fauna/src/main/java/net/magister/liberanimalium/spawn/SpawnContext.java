package net.magister.liberanimalium.spawn;

import org.jetbrains.annotations.Nullable;
import net.magister.liberanimalium.fauna.api.SpeciesProfile;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.levelgen.Heightmap.Types;
import net.minecraft.world.phys.Vec3;

public record SpawnContext(
   ServerLevel level, BlockPos pos, SpawnRule rule, @Nullable SpeciesProfile species, Census census, @Nullable ServerPlayer nearest, RandomSource random
) {
   public double nearestDistance() {
      return this.nearest == null ? Double.POSITIVE_INFINITY : this.nearest.position().distanceTo(Vec3.atBottomCenterOf(this.pos));
   }

   public int surface() {
      return this.level.getHeight(Types.MOTION_BLOCKING_NO_LEAVES, this.pos.getX(), this.pos.getZ());
   }
}
