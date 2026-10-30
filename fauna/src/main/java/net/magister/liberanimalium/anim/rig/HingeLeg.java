package net.magister.liberanimalium.anim.rig;

import org.joml.Quaternionf;
import org.joml.Quaternionfc;
import org.joml.Vector3f;
import org.joml.Vector3fc;

public final class HingeLeg {
   public static final float FOLD_MIN = (float)Math.toRadians(20.0);
   public static final float BEND_KNOWN_SIN = 0.2F;
   private static final Quaternionf Q = new Quaternionf();
   private static final Vector3f U = new Vector3f();
   private static final Vector3f V = new Vector3f();
   private static final Vector3f N = new Vector3f();

   private HingeLeg() {
   }

   public static double reach(double length, double drop) {
      double s = 0.985 * length;
      return Math.sqrt(Math.max(0.0, s * s - drop * drop));
   }

   public static boolean observe(Vector3fc hip, Vector3fc knee, Vector3fc foot, Quaternionfc parent, Vector3f hingeLocal, boolean known) {
      Vector3f u = U.set(knee).sub(hip);
      Vector3f v = V.set(foot).sub(knee);
      Vector3f n = u.cross(v, N);
      float lengths = u.length() * v.length();
      if (!(lengths < 1.0E-9F) && !(n.length() < 0.2F * lengths)) {
         n.normalize();
         Q.set(parent).conjugate().transform(n, hingeLocal);
         return true;
      } else {
         return known;
      }
   }

   public static float fold(Vector3fc hip, Vector3fc knee, Vector3fc foot, Vector3fc hinge, float l1, float l2, float distance) {
      float straight = 0.985F * (l1 + l2);
      float d = Math.max(Math.abs(l1 - l2) + 1.0E-4F, Math.min(distance, straight));
      float cos = (l1 * l1 + l2 * l2 - d * d) / (2.0F * l1 * l2);
      float wanted = (float)Math.acos(Math.max(-1.0F, Math.min(1.0F, cos)));
      wanted = Math.max(wanted, FOLD_MIN);
      Vector3f u = U.set(knee).sub(hip);
      Vector3f v = V.set(foot).sub(knee);
      float bend = (float)Math.atan2(u.cross(v, N).dot(hinge), u.dot(v));
      float now = (float) Math.PI - bend;
      return now - wanted;
   }

   public static Vector3f footAt(Vector3fc knee, Vector3fc foot, Vector3fc hinge, float phi, Vector3f dest) {
      Vector3f v = dest.set(foot).sub(knee);
      Q.rotationAxis(phi, hinge).transform(v);
      return v.add(knee);
   }
}
