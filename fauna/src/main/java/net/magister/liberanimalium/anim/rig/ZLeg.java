package net.magister.liberanimalium.anim.rig;

import org.joml.Quaternionf;
import org.joml.Vector3f;
import org.joml.Vector3fc;

public final class ZLeg {
   public static final float FOLD_MAX = (float)Math.toRadians(80.0);
   public static final float SIDE_KNOWN = (float)Math.asin(0.2F);
   private static final Quaternionf Q = new Quaternionf();
   private static final ZLeg.Circle CIRCLE = new ZLeg.Circle();

   private ZLeg() {
   }

   public static int side(Vector3fc hip, Vector3fc knee, Vector3fc ankle, Vector3fc foot, Vector3fc hinge, int remembered) {
      ZLeg.Circle c = CIRCLE.of(hip, knee, ankle, foot, hinge);
      if (c.a < 1.0E-9F) {
         return remembered;
      } else {
         float off = -c.straight;
         if (!(Math.abs(off) >= SIDE_KNOWN) && remembered != 0) {
            return remembered;
         } else {
            return off >= 0.0F ? 1 : -1;
         }
      }
   }

   public static Vector3f footAt(Vector3fc knee, Vector3fc ankle, Vector3fc foot, Vector3fc hinge, float phi, Vector3f dest) {
      Vector3f shank = dest.set(ankle).sub(knee);
      Q.rotationAxis(phi, hinge).transform(shank);
      return shank.add(knee).add(foot.x() - ankle.x(), foot.y() - ankle.y(), foot.z() - ankle.z());
   }

   public static float fold(Vector3fc hip, Vector3fc knee, Vector3fc ankle, Vector3fc foot, Vector3fc hinge, float distance, int side) {
      ZLeg.Circle c = CIRCLE.of(hip, knee, ankle, foot, hinge);
      if (c.a < 1.0E-9F) {
         return 0.0F;
      }

      int own = -c.straight >= 0.0F ? 1 : -1;
      if (side == 0) {
         side = own;
      }

      if (side == own && Math.abs(c.distance(0.0F) - distance) < 1.0E-6F) {
         return 0.0F;
      }

      float longest = (float)Math.sqrt(c.k + 2.0F * c.a);
      float d = Math.min(distance, longest);
      float cos = (d * d - c.k) / (2.0F * c.a);
      float turn = (float)Math.acos(Math.max(-1.0F, Math.min(1.0F, cos)));
      float phi = c.straight + side * turn;
      return side > 0 ? Math.min(FOLD_MAX, phi) : Math.max(-FOLD_MAX, phi);
   }

   public static float longest(Vector3fc hip, Vector3fc knee, Vector3fc ankle, Vector3fc foot, Vector3fc hinge) {
      ZLeg.Circle c = CIRCLE.of(hip, knee, ankle, foot, hinge);
      return Math.max(foot.distance(hip), (float)Math.sqrt(c.k + 2.0F * c.a));
   }

   private static final class Circle {
      float k;
      float a;
      float straight;

      ZLeg.Circle of(Vector3fc hip, Vector3fc knee, Vector3fc ankle, Vector3fc foot, Vector3fc hinge) {
         float sx = ankle.x() - knee.x();
         float sy = ankle.y() - knee.y();
         float sz = ankle.z() - knee.z();
         float along = sx * hinge.x() + sy * hinge.y() + sz * hinge.z();
         float px = sx - along * hinge.x();
         float py = sy - along * hinge.y();
         float pz = sz - along * hinge.z();
         float wx = knee.x() + along * hinge.x() + foot.x() - ankle.x() - hip.x();
         float wy = knee.y() + along * hinge.y() + foot.y() - ankle.y() - hip.y();
         float wz = knee.z() + along * hinge.z() + foot.z() - ankle.z() - hip.z();
         float cx = hinge.y() * pz - hinge.z() * py;
         float cy = hinge.z() * px - hinge.x() * pz;
         float cz = hinge.x() * py - hinge.y() * px;
         float ca = wx * px + wy * py + wz * pz;
         float sa = wx * cx + wy * cy + wz * cz;
         this.k = wx * wx + wy * wy + wz * wz + px * px + py * py + pz * pz;
         this.a = (float)Math.sqrt(ca * ca + sa * sa);
         this.straight = (float)Math.atan2(sa, ca);
         return this;
      }

      float distance(float phi) {
         return (float)Math.sqrt(Math.max(0.0F, this.k + 2.0F * this.a * (float)Math.cos(phi - this.straight)));
      }
   }
}
