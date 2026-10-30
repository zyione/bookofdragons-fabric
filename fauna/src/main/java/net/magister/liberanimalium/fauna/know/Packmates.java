package net.magister.liberanimalium.fauna.know;

import java.util.UUID;
import java.util.function.Function;
import java.util.function.Predicate;
import org.jetbrains.annotations.Nullable;
import net.magister.liberanimalium.fauna.api.RelationType;
import net.minecraft.world.phys.Vec3;

public final class Packmates {
   private Packmates() {
   }

   @Nullable
   public static Vec3 centroid(Iterable<Representation> known, Function<Representation, Vec3> estimate, Predicate<UUID> exempt) {
      Vec3 sum = Vec3.ZERO;
      int count = 0;

      for (Representation rep : known) {
         if (rep.relation().type() == RelationType.PACK && !exempt.test(rep.id())) {
            sum = sum.add(estimate.apply(rep));
            count++;
         }
      }

      return count == 0 ? null : sum.scale(1.0 / count);
   }
}
