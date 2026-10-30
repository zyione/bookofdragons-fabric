package net.magister.liberanimalium.fauna.know;

import java.util.ArrayList;
import java.util.List;
import java.util.function.ToDoubleFunction;
import net.magister.liberanimalium.fauna.api.RelationType;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

public final class Company {
   private Company() {
   }

   public static float pull(float sympathy) {
      return sympathy + sympathy - 1.0F;
   }

   public static float score(Vec3 point, List<Company.Source> sources) {
      float total = 0.0F;

      for (Company.Source s : sources) {
         float fade = (float)Math.max(0.0, 1.0 - point.distanceTo(s.pos()) / s.falloff());
         total += s.weight() * fade;
      }

      return total;
   }

   public static List<Company.Source> sources(Iterable<Representation> reps, SocialBook book, long now, float companyPull, double reach) {
      return sources(reps, book, now, companyPull, reach, rep -> 1.0);
   }

   public static List<Company.Source> sources(
      Iterable<Representation> reps, SocialBook book, long now, float companyPull, double reach, ToDoubleFunction<Representation> tie
   ) {
      List<Company.Source> out = new ArrayList<>();
      float push = (float)reach;
      float aversion = push * 0.6666667F;

      for (Representation rep : reps) {
         float held = rep.find(now);
         if (!(held <= 0.0F)) {
            Vec3 pos = rep.lastKnownPos();
            float afraid = Math.max(rep.alarm(), Math.max(uncomfortable(rep), rep.crowding()));
            if (afraid > 0.0F) {
               out.add(new Company.Source(pos, -afraid * held * 0.6F, push));
            } else {
               float earned = book.of(rep.id()).like();
               if (earned > 0.0F) {
                  earned *= (float)tie.applyAsDouble(rep);
               }

               float disposition;
               if (companyPull > 0.0F) {
                  disposition = rep.sameSpecies() ? companyPull : 0.0F;
               } else {
                  disposition = companyPull;
               }

               float preference = Math.abs(earned) >= Math.abs(disposition) ? earned : disposition;
               preference = Mth.clamp(preference, -1.0F, 1.0F) * held;
               if (preference > 0.0F) {
                  out.add(new Company.Source(pos, preference * 0.3F, push));
               } else if (preference < 0.0F) {
                  out.add(new Company.Source(pos, preference * 0.3F, aversion));
               }
            }
         }
      }

      return out;
   }

   private static float uncomfortable(Representation rep) {
      return rep.relation().type() == RelationType.UNCOMFORTABLE ? rep.relation().intensity() : 0.0F;
   }

   public record Source(Vec3 pos, float weight, float falloff) {
   }
}
