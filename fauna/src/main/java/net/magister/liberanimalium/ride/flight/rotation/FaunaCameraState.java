package net.magister.liberanimalium.ride.flight.rotation;

import org.joml.Quaternionf;
import org.joml.Vector3d;

public class FaunaCameraState {
   private static float currentBodyRoll = 0.0F;
   private static boolean hasStableAngles = false;
   private static float stableYaw = 0.0F;
   private static float stablePitch = 0.0F;
   private static float stableRoll = 0.0F;
   private static Quaternionf orientationQuaternion = null;
   private static boolean thirdPersonReverse = false;
   private static final FaunaCameraSmoother smoother = new FaunaCameraSmoother();

   public static void setCurrentRoll(float roll) {
      currentBodyRoll = roll;
   }

   public static float getCurrentRoll() {
      return hasStableAngles ? stableRoll : currentBodyRoll;
   }

   public static void setStableAngles(float yaw, float pitch, float roll) {
      hasStableAngles = true;
      stableYaw = yaw;
      stablePitch = pitch;
      stableRoll = roll;
      currentBodyRoll = roll;
   }

   public static boolean hasStableAngles() {
      return hasStableAngles;
   }

   public static float getStableYaw() {
      return stableYaw;
   }

   public static float getStablePitch() {
      return stablePitch;
   }

   public static float getStableRoll() {
      return stableRoll;
   }

   public static void setOrientationQuaternion(Quaternionf q) {
      if (q == null) {
         orientationQuaternion = null;
      } else {
         if (orientationQuaternion == null) {
            orientationQuaternion = new Quaternionf(q);
         } else {
            orientationQuaternion.set(q);
         }
      }
   }

   public static boolean hasOrientationQuaternion() {
      return orientationQuaternion != null;
   }

   public static Quaternionf getOrientationQuaternion() {
      return orientationQuaternion;
   }

   public static void setThirdPersonReverse(boolean reverse) {
      thirdPersonReverse = reverse;
   }

   public static boolean isThirdPersonReverse() {
      return thirdPersonReverse;
   }

   public static Quaternionf getOrientationConjugate() {
      if (orientationQuaternion == null) {
         return null;
      }

      Quaternionf conjugate = new Quaternionf(orientationQuaternion).conjugate();
      if (thirdPersonReverse) {
         conjugate.premul(new Quaternionf().rotateLocalY((float) Math.PI));
      }

      return conjugate;
   }

   public static void clearRoll() {
      currentBodyRoll = 0.0F;
      hasStableAngles = false;
      orientationQuaternion = null;
      thirdPersonReverse = false;
   }

   public static void updateSmoother(Quaternionf targetRot, Vector3d targetPos, float dt) {
      smoother.update(targetRot, targetPos, dt);
   }

   public static void resetSmoother(Quaternionf seedRot, Vector3d seedPos) {
      smoother.reset(seedRot, seedPos);
   }

   public static Quaternionf getSmoothedOrientation() {
      return smoother.getRotation();
   }

   public static Vector3d getSmoothedPosition() {
      return smoother.getPosition();
   }
}
