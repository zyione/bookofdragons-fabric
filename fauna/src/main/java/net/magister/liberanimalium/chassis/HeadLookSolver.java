package net.magister.liberanimalium.chassis;

import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

public final class HeadLookSolver {
   public static final int YAW = 0;
   public static final int PITCH = 1;

   private HeadLookSolver() {
   }

   public static float[] solve(Vec3 eye, float bodyYawDeg, Vec3 target) {
      double dx = target.x - eye.x;
      double dy = target.y - eye.y;
      double dz = target.z - eye.z;
      double horizontalDist = Math.sqrt(dx * dx + dz * dz);
      float worldYaw = (float)(Mth.atan2(dz, dx) * (180.0 / Math.PI)) - 90.0F;
      float worldPitch = (float)(-(Mth.atan2(dy, horizontalDist) * (180.0 / Math.PI)));
      float relativeYaw = Mth.wrapDegrees(worldYaw - bodyYawDeg);
      return new float[]{relativeYaw, worldPitch};
   }
}
