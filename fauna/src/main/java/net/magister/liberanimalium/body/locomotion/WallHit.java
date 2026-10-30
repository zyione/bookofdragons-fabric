package net.magister.liberanimalium.body.locomotion;

import net.magister.liberanimalium.chassis.FaunaBase;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

public final class WallHit {
   private WallHit() {
   }

   public static Vec3 impact(Vec3 before, Vec3 after) {
      return new Vec3(before.x - after.x, 0.0, before.z - after.z);
   }

   public static boolean isHit(Vec3 impact) {
      return impact.horizontalDistanceSqr() >= 0.02250000178813938;
   }

   public static float severity(Vec3 impact, double flyingSpeed) {
      return (float)(Math.sqrt(impact.horizontalDistanceSqr()) / Math.max(flyingSpeed, 1.0E-4));
   }

   public static WallHit.Choice choose(float strafe, float forward) {
      if (strafe > 0.5F) {
         return WallHit.Choice.LEFT;
      } else if (strafe < -0.5F) {
         return WallHit.Choice.RIGHT;
      } else {
         return forward < -0.5F ? WallHit.Choice.BACK : WallHit.Choice.NONE;
      }
   }

   public static float launchHeading(FaunaBase body, Vec3 impact, WallHit.Choice choice, float fallback) {
      Vec3 out = impact.scale(-1.0);
      float base = headingOf(out, fallback);

      return switch (choice) {
         case LEFT -> base + RideTuning.launchAngleDegrees(body);
         case RIGHT -> base - RideTuning.launchAngleDegrees(body);
         case BACK, NONE -> base;
      };
   }

   public static Vec3 launchVelocity(FaunaBase body, WallHit.Choice choice, float heading, double flyingSpeed) {
      return choice == WallHit.Choice.NONE ? Vec3.ZERO : Vec3.directionFromRotation(0.0F, heading).scale(RideTuning.launchThrottle(body) * flyingSpeed);
   }

   public static float launchThrottle(FaunaBase body, WallHit.Choice choice) {
      return choice == WallHit.Choice.NONE ? 0.0F : RideTuning.launchThrottle(body);
   }

   public static float headingOf(Vec3 direction, float fallback) {
      return direction.horizontalDistanceSqr() < 1.0E-12 ? fallback : (float)Math.toDegrees(Math.atan2(-direction.x, direction.z));
   }

   public static float turnToward(float current, float target, float rate) {
      float diff = Mth.wrapDegrees(target - current);
      return current + Mth.clamp(diff, -rate, rate);
   }

   public enum Choice {
      NONE,
      LEFT,
      RIGHT,
      BACK;
   }
}
