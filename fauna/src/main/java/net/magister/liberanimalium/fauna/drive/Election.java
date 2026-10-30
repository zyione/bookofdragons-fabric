package net.magister.liberanimalium.fauna.drive;

import java.util.Collection;
import java.util.function.ToDoubleFunction;
import org.jetbrains.annotations.Nullable;
import net.magister.liberanimalium.fauna.know.Representation;

public final class Election {
   private Election() {
   }

   @Nullable
   public static Representation elect(DriveContext context, @Nullable Representation current, ToDoubleFunction<Representation> score) {
      return elect(current, context.known(), score, 2.0F);
   }

   @Nullable
   public static <T> T elect(@Nullable T current, Collection<T> candidates, ToDoubleFunction<T> score, float switchBias) {
      T best = null;
      double bestScore = 0.0;

      for (T candidate : candidates) {
         double value = score.applyAsDouble(candidate);
         if (!(value <= 0.0) && value > bestScore) {
            bestScore = value;
            best = candidate;
         }
      }

      if (best == null || current == null || best == current) {
         return best;
      }

      if (!candidates.contains(current)) {
         return best;
      }

      double incumbent = score.applyAsDouble(current);
      return incumbent > 0.0 && bestScore < incumbent * switchBias ? current : best;
   }
}
