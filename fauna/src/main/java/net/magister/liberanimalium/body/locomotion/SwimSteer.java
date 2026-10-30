package net.magister.liberanimalium.body.locomotion;

import net.minecraft.world.phys.Vec3;

public final class SwimSteer {
   private SwimSteer() {
   }

   public static Vec3 servo(Vec3 v, Vec3 target, Vec3 nose, double axial, double cross) {
      Vec3 error = target.subtract(v);
      Vec3 along = nose.scale(error.dot(nose));
      Vec3 across = error.subtract(along);
      return v.add(along.scale(axial)).add(across.scale(cross));
   }

   public static float righting(float angle, float rate, float seconds) {
      float step = (float)(rate * Math.sin(Math.toRadians(angle)) * seconds);
      return Math.abs(step) > Math.abs(angle) ? angle : step;
   }
}
