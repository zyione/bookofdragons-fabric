package net.magister.liberanimalium.ride.client;

import net.magister.liberanimalium.anim.rig.Ease;
import net.magister.liberanimalium.body.physics.PhysicsSheet;
import net.magister.liberanimalium.chassis.FaunaBase;
import net.magister.liberanimalium.config.RideSettings;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaternionf;

public final class TurbulenceShake {
   private static final double FROM = 0.9;
   private static final double FULL = 1.85;
   private static final float DIVE_FROM = 15.0F;
   private static final float DIVE_FULL = 60.0F;
   private static final float LEVEL_SHARE = 0.45F;
   private static final float RISE = 0.25F;
   private static final float FALL = 0.5F;
   private static final float BUFFET_PITCH = 1.1F;
   private static final float BUFFET_YAW = 0.6F;
   private static final float BUFFET_ROLL = 1.4F;
   private static final float RATTLE_PITCH = 0.35F;
   private static final float RATTLE_YAW = 0.25F;
   private static final float RATTLE_ROLL = 0.4F;
   private static float strength;
   private static double clock;

   private TurbulenceShake() {
   }

   public static void update(FaunaBase body, float frameDelta) {
      float target = RideSettings.isTurbulenceEnabled() ? want(body) * (float)RideSettings.getTurbulenceStrength() : 0.0F;
      float tau = target > strength ? 0.25F : 0.5F;
      strength = strength + (target - strength) * Ease.lag(frameDelta, tau);
      if (strength < 0.001F && target == 0.0F) {
         strength = 0.0F;
      }

      clock = clock + frameDelta * (1.0 + strength * 0.5);
   }

   public static void reset() {
      strength = 0.0F;
   }

   static float want(FaunaBase body) {
      if (body.isFlying() && !body.isAquatic()) {
         Vec3 v = body.getDeltaMovement();
         double speed = v.length();
         double reference = Math.max(0.05, PhysicsSheet.of(body).flight().rollReferenceSpeed());
         float fast = smoothstep((speed / reference - 0.9) / 0.9500000000000001);
         if (fast <= 0.0F) {
            return 0.0F;
         }

         float diveAngle = speed < 1.0E-4 ? 0.0F : (float)Math.toDegrees(Math.asin(Mth.clamp(-v.y / speed, -1.0, 1.0)));
         float dive = Mth.clamp((diveAngle - 15.0F) / 45.0F, 0.0F, 1.0F);
         return fast * Mth.lerp(dive, 0.45F, 1.0F);
      } else {
         return 0.0F;
      }
   }

   private static float smoothstep(double t) {
      float x = (float)Mth.clamp(t, 0.0, 1.0);
      return x * x * (3.0F - 2.0F * x);
   }

   private static float buffet(double t, double a, double b) {
      return (float)(Math.sin(t * (float) (Math.PI * 2) * 2.3 + a) * 0.6 + Math.sin(t * (float) (Math.PI * 2) * 3.7 + b) * 0.4);
   }

   private static float rattle(double t, double a, double b) {
      return (float)(Math.sin(t * (float) (Math.PI * 2) * 11.0 + a) * 0.6 + Math.sin(t * (float) (Math.PI * 2) * 17.3 + b) * 0.4);
   }

   public static float pitch() {
      return strength == 0.0F ? 0.0F : strength * (1.1F * buffet(clock, 0.0, 1.3) + 0.35F * rattle(clock, 0.4, 2.1));
   }

   public static float yaw() {
      return strength == 0.0F ? 0.0F : strength * (0.6F * buffet(clock, 2.7, 0.6) + 0.25F * rattle(clock, 1.9, 0.2));
   }

   public static float roll() {
      return strength == 0.0F ? 0.0F : strength * (1.4F * buffet(clock, 4.1, 3.3) + 0.4F * rattle(clock, 5.0, 4.4));
   }

   public static Quaternionf onto(Quaternionf view) {
      float pitch = pitch();
      float yaw = yaw();
      float roll = roll();
      if (CameraShakeUtil.isShaking()) {
         pitch += CameraShakeUtil.getShakePitch();
         yaw += CameraShakeUtil.getShakeYaw();
      }

      return pitch == 0.0F && yaw == 0.0F && roll == 0.0F
         ? view
         : new Quaternionf(view).rotateY(-yaw * (float) (Math.PI / 180.0)).rotateX(pitch * (float) (Math.PI / 180.0)).rotateZ(roll * (float) (Math.PI / 180.0));
   }
}
