package net.magister.liberanimalium.fauna.know;

import org.jetbrains.annotations.Nullable;
import net.magister.liberanimalium.fauna.api.RelationType;
import net.minecraft.world.phys.Vec3;

public final class Herd {
   private Herd() {
   }

   @Nullable
   public static Vec3 heading(Iterable<Representation> known, long now, double walking) {
      Vec3 sum = Vec3.ZERO;
      float weight = 0.0F;

      for (Representation rep : known) {
         if (rep.relation().type() == RelationType.PACK) {
            float find = rep.find(now);
            if (!(find <= 0.0F)) {
               Vec3 v = rep.lastVelocity();
               sum = sum.add(v.x * find, 0.0, v.z * find);
               weight += find;
            }
         }
      }

      if (weight <= 0.0F) {
         return null;
      }

      Vec3 mean = sum.scale(1.0 / weight);
      return mean.length() < walking ? null : mean;
   }

   @Nullable
   public static Vec3 fleeing(Iterable<Representation> known, long now, int window, double walking) {
      Vec3 sum = Vec3.ZERO;
      int count = 0;

      for (Representation rep : known) {
         if (rep.relation().type() == RelationType.PACK && rep.startledWithin(now, window)) {
            Vec3 v = rep.lastVelocity();
            sum = sum.add(v.x, 0.0, v.z);
            count++;
         }
      }

      if (count == 0) {
         return null;
      }

      Vec3 mean = sum.scale(1.0 / count);
      return mean.length() < walking ? null : mean;
   }

   public static float movingShare(Iterable<Representation> known, long now, double walking) {
      float moving = 0.0F;
      float weight = 0.0F;

      for (Representation rep : known) {
         if (rep.relation().type() == RelationType.PACK) {
            float find = rep.find(now);
            if (!(find <= 0.0F)) {
               weight += find;
               Vec3 v = rep.lastVelocity();
               if (Math.hypot(v.x, v.z) >= walking) {
                  moving += find;
               }
            }
         }
      }

      return weight <= 0.0F ? 0.0F : moving / weight;
   }
}
