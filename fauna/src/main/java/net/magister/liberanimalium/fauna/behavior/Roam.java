package net.magister.liberanimalium.fauna.behavior;

import java.util.function.Predicate;
import java.util.function.ToDoubleFunction;
import org.jetbrains.annotations.Nullable;
import net.magister.liberanimalium.fauna.api.AnimalHandle;
import net.magister.liberanimalium.fauna.core.Seeds;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.Vec3;

public final class Roam {
   private Roam() {
   }

   @Nullable
   public static BlockPos pick(AnimalHandle handle, long now, int minRange, int maxRange) {
      return pick(handle, now, minRange, maxRange, null, null);
   }

   @Nullable
   public static BlockPos pick(AnimalHandle handle, long now, int minRange, int maxRange, @Nullable Vec3 biasDirection, @Nullable Predicate<BlockPos> filter) {
      return pick(handle, now, minRange, maxRange, biasDirection, filter, null);
   }

   @Nullable
   public static BlockPos pick(
      AnimalHandle handle,
      long now,
      int minRange,
      int maxRange,
      @Nullable Vec3 biasDirection,
      @Nullable Predicate<BlockPos> filter,
      @Nullable ToDoubleFunction<Vec3> regard
   ) {
      int span = Math.max(1, maxRange - minRange);
      BlockPos origin = BlockPos.containing(handle.bodyPosition());
      BlockPos best = null;
      double bestRegard = Double.NEGATIVE_INFINITY;
      double baseAngle = 0.0;
      boolean hasBias = biasDirection != null && biasDirection.horizontalDistanceSqr() > 1.0E-4;
      if (hasBias) {
         baseAngle = Math.atan2(biasDirection.x, biasDirection.z);
      }

      for (int attempt = 0; attempt < 10; attempt++) {
         long attemptSalt = attempt * 31L;
         int range = minRange + Math.floorMod(Seeds.mix(handle.faunaId(), now + attemptSalt + 1L), span);
         double angle;
         if (hasBias) {
            double delta = (Math.floorMod(Seeds.mix(handle.faunaId(), now + attemptSalt + 2L), 150) - 75) * (float) (Math.PI / 180.0);
            angle = baseAngle + delta;
         } else {
            angle = Math.floorMod(Seeds.mix(handle.faunaId(), now + attemptSalt + 2L), 360) * (float) (Math.PI / 180.0);
         }

         BlockPos approx = origin.offset((int)Math.round(Math.sin(angle) * range), 0, (int)Math.round(Math.cos(angle) * range));
         BlockPos candidate = handle.findStandableNear(approx);
         if (candidate != null && (filter == null || filter.test(candidate))) {
            if (regard == null) {
               return candidate;
            }

            double score = regard.applyAsDouble(Vec3.atBottomCenterOf(candidate));
            if (score > bestRegard) {
               bestRegard = score;
               best = candidate;
            }
         }
      }

      return best;
   }

   @Nullable
   public static BlockPos pickAloft(
      AnimalHandle handle,
      long now,
      int minRange,
      int maxRange,
      @Nullable Vec3 biasDirection,
      @Nullable Predicate<BlockPos> filter,
      @Nullable ToDoubleFunction<Vec3> regard,
      int floor,
      int ceiling
   ) {
      BlockPos ground = pick(handle, now, minRange, maxRange, biasDirection, filter, regard);
      if (ground == null) {
         return null;
      }

      int span = Math.max(1, ceiling - floor + 1);
      int lift = floor + Math.floorMod(Seeds.mix(handle.faunaId(), now + 1L + 2L), span);
      BlockPos aloft = ground.above(lift);
      return handle.isBlockedAt(Vec3.atCenterOf(aloft)) ? null : aloft;
   }
}
