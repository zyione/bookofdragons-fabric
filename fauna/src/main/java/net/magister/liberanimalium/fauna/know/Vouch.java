package net.magister.liberanimalium.fauna.know;

import java.util.Collection;
import java.util.function.Function;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

public final class Vouch {
   private Vouch() {
   }

   public static float forPerson(Vec3 personAt, Collection<Representation> known, Function<Representation, Vec3> estimate, float sympathy) {
      float best = 0.0F;

      for (Representation rep : known) {
         if (rep.owned() && rep.sameSpecies() && !rep.isPlayer()) {
            double gap = estimate.apply(rep).distanceTo(personAt);
            if (!(gap > 6.0)) {
               float nearness = (float)(1.0 - gap / 6.0);
               best = Math.max(best, nearness);
            }
         }
      }

      return best <= 0.0F ? 0.0F : Mth.clamp(best * Mth.clamp(sympathy, 0.0F, 1.0F), 0.0F, 1.0F) * 0.9F;
   }

   public static float shade(float offering, float vouch) {
      return Math.max(Mth.clamp(offering, 0.0F, 1.0F), Mth.clamp(vouch, 0.0F, 1.0F));
   }
}
