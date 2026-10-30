package net.magister.liberanimalium.body.locomotion;

import net.magister.liberanimalium.body.physics.PhysicsSheet;
import net.magister.liberanimalium.chassis.FaunaBase;

public final class RideTuning {
   public static final float HOVER_STRAFE_SHARE = 0.9F;
   public static final float STRAFE_RAMP_TICKS = 10.0F;
   public static final float SINK_PITCH_DEG = 30.0F;
   public static final float HIT_LINE = 0.15F;
   public static final float SEVERITY_MAX = 2.5F;

   private RideTuning() {
   }

   public static float pushTurnRate(FaunaBase body) {
      return 180.0F / pushOffTicks(body);
   }

   public static int ejectGraceTicks(FaunaBase body) {
      return pushOffTicks(body);
   }

   public static float launchThrottle(FaunaBase body) {
      return PhysicsSheet.of(body).ride().launchThrottle();
   }

   public static float launchAngleDegrees(FaunaBase body) {
      return PhysicsSheet.of(body).ride().launchAngle();
   }

   public static int pushOffTicks(FaunaBase body) {
      return PhysicsSheet.of(body).ride().pushOffTicks();
   }

   public static int doubleTapTicks(FaunaBase body) {
      return PhysicsSheet.of(body).ride().doubleTapTicks();
   }
}
