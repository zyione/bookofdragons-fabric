package net.magister.liberanimalium.body.locomotion;

import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix3f;
import org.joml.Quaternionf;
import org.joml.Vector3f;

public final class SurfaceFrame {
   private static final float SAME = 1.0E-4F;

   private SurfaceFrame() {
   }

   public static Quaternionf turnToward(Quaternionf frame, Vec3 up, float maxDegrees) {
      Vector3f from = frame.transform(new Vector3f(0.0F, 1.0F, 0.0F));
      Vector3f to = new Vector3f((float)up.x, (float)up.y, (float)up.z).normalize();
      Vector3f axis = from.cross(to, new Vector3f());
      float sine = axis.length();
      float angle = (float)Math.atan2(sine, from.dot(to));
      if (angle < 1.0E-4F) {
         return new Quaternionf(frame);
      }

      if (sine < 1.0E-4F) {
         axis = frame.transform(new Vector3f(1.0F, 0.0F, 0.0F));
      }

      axis.normalize();
      float step = Math.min(angle, maxDegrees * (float) (Math.PI / 180.0));
      return new Quaternionf().rotationAxis(step, axis.x, axis.y, axis.z).mul(frame).normalize();
   }

   public static Quaternionf body(Quaternionf frame, float yaw) {
      return new Quaternionf(frame).rotateY((180.0F - yaw) * (float) (Math.PI / 180.0));
   }

   public static Quaternionf look(Quaternionf frame, float yaw, float pitch) {
      return body(frame, yaw).rotateX(-pitch * (float) (Math.PI / 180.0));
   }

   public static Vector3f forward(Quaternionf attitude) {
      return attitude.transform(new Vector3f(0.0F, 0.0F, -1.0F));
   }

   public static Vector3f up(Quaternionf attitude) {
      return attitude.transform(new Vector3f(0.0F, 1.0F, 0.0F));
   }

   public static float yawOf(Vector3f direction, float fallback) {
      return direction.x * direction.x + direction.z * direction.z < 1.0E-4F ? fallback : (float)Math.toDegrees(Math.atan2(-direction.x, direction.z));
   }

   public static float pitchOf(Vector3f direction) {
      return (float)Math.toDegrees(Math.asin(Mth.clamp(-direction.y, -1.0F, 1.0F)));
   }

   public static float localYaw(Quaternionf frame, Vec3 direction, float fallback) {
      return yawOf(local(frame, direction), fallback);
   }

   public static float localPitch(Quaternionf frame, Vec3 direction) {
      return pitchOf(local(frame, direction));
   }

   private static Vector3f local(Quaternionf frame, Vec3 direction) {
      return frame.transformInverse(new Vector3f((float)direction.x, (float)direction.y, (float)direction.z));
   }

   public static Quaternionf fromAxes(Vector3f up, Vector3f forward, Quaternionf fallback) {
      Vector3f y = new Vector3f(up).normalize();
      Vector3f f = new Vector3f(forward).sub(new Vector3f(y).mul(y.dot(forward)));
      if (f.lengthSquared() < 1.0E-4F) {
         return new Quaternionf(fallback);
      }

      f.normalize();
      Vector3f x = f.cross(y, new Vector3f());
      Matrix3f m = new Matrix3f(x.x, x.y, x.z, y.x, y.y, y.z, -f.x, -f.y, -f.z);
      return new Quaternionf().setFromNormalized(m);
   }

   public static double standOff(Vector3f up, double width, double height) {
      return (Math.abs(up.x) + Math.abs(up.z)) * width * 0.5 + Math.abs(up.y) * height * 0.5;
   }

   public static Quaternionf levelled(Quaternionf view, float share) {
      Vector3f nose = forward(view);
      Vector3f up = up(view);
      Vector3f want = new Vector3f(0.0F, 1.0F, 0.0F).sub(new Vector3f(nose).mul(nose.y));
      float square = want.length();
      if (square < 1.0E-4F) {
         return new Quaternionf(view);
      }

      want.div(square);
      float off = (float)Math.atan2(nose.dot(up.cross(want, new Vector3f())), up.dot(want));
      return new Quaternionf().rotationAxis(off * square * share, nose.x, nose.y, nose.z).mul(view).normalize();
   }

   public static float incline(Vector3f up) {
      return Mth.clamp((1.0F - up.y) * 0.5F, 0.0F, 1.0F);
   }

   public static float speedShare(float incline, float wall, float ceiling) {
      return incline <= 0.5F ? Mth.lerp(incline * 2.0F, 1.0F, wall) : Mth.lerp(incline * 2.0F - 1.0F, wall, ceiling);
   }
}
