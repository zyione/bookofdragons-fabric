package net.magister.liberanimalium.anim.rig;

import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

public final class Cascade {
   private Cascade() {
   }

   public static Vec3 spine(Vec3 accum, Vec3 omega, double strength, double decay, double maxBend, boolean leader) {
      double sign = leader ? 1.0 : -1.0;
      return clamp(
         accum.x * decay + omega.x * strength * sign,
         accum.y * decay + omega.y * strength * sign,
         accum.z * decay + omega.z * strength * sign,
         maxBend
      );
   }

   public static Vec3 limb(Vec3 accum, Vec3 omega, double strength, double decay, double maxBend, boolean left, boolean driven) {
      double y = accum.y * decay;
      if (driven) {
         double filtered = left ? Math.max(omega.y, 0.0) : Math.min(omega.y, 0.0);
         if (Math.abs(filtered) < 0.8) {
            filtered = 0.0;
         }

         y += filtered * strength;
      }

      return clamp(accum.x * decay, y, accum.z * decay, maxBend);
   }

   private static Vec3 clamp(double x, double y, double z, double maxBend) {
      return new Vec3(Mth.clamp(x, -maxBend, maxBend), Mth.clamp(y, -maxBend, maxBend), Mth.clamp(z, -maxBend, maxBend));
   }
}
