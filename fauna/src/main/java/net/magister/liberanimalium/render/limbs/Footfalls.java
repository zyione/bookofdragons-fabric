package net.magister.liberanimalium.render.limbs;

import net.magister.liberanimalium.anim.rig.Footing;
import net.magister.liberanimalium.chassis.FaunaBase;
import net.magister.liberanimalium.render.vfx.FootDust;
import net.minecraft.world.phys.Vec3;

public final class Footfalls {
   private Footfalls() {
   }

   static boolean down(boolean was, double share) {
      if (Double.isNaN(share)) {
         return was;
      } else if (share >= 0.5) {
         return true;
      } else {
         return share < 0.0 ? false : was;
      }
   }

   public static void tick(FaunaBase body, LimbRig rig) {
      int tick = body.tickCount;
      if (rig.footfallTick != tick) {
         rig.footfallTick = tick;
         Footing foot = body.footing();
         Vec3 was = foot.groundOld();
         double vx = foot.x() - was.x;
         double vz = foot.z() - was.z;

         for (Limb limb : rig.limbs) {
            LimbState st = limb.st;
            if (st.tick == tick && st.footTarget != null) {
               boolean down = down(st.down, st.plant.share);
               boolean landed = down && !st.down;
               st.down = down;
               if (landed) {
                  int stride = st.landedTick == Integer.MIN_VALUE ? 8 : tick - st.landedTick;
                  st.landedTick = tick;
                  double groundY = Double.isNaN(st.groundY) ? foot.y() : st.groundY;
                  Vec3 at = foot.toWorld(new Vec3(st.footTarget.x, groundY, st.footTarget.z));
                  FootDust.footfall(body, at.x, at.y, at.z, vx, vz, stride);
               }
            }
         }
      }
   }
}
