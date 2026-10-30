package net.magister.liberanimalium.body.grip;

import net.magister.liberanimalium.body.physics.PhysicsSheet;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaternionf;
import org.joml.Quaternionfc;
import org.joml.Vector3f;
import org.joml.Vector3fc;

public final class GripGeometry {
   private GripGeometry() {
   }

   public static Quaternionf attitude(float yawDeg, float xRotDeg, float rollDeg, Quaternionf dest) {
      return dest.identity()
         .rotateY(-(yawDeg - 180.0F) * (float) (Math.PI / 180.0))
         .rotateX(-xRotDeg * (float) (Math.PI / 180.0))
         .rotateZ(rollDeg * (float) (Math.PI / 180.0));
   }

   public static Quaternionf yawOnly(float yawDeg, Quaternionf dest) {
      return dest.identity().rotateY(-(yawDeg - 180.0F) * (float) (Math.PI / 180.0));
   }

   public static double back(Quaternionfc attitude, double speed, PhysicsSheet.Grip physics, double back) {
      Vector3f nose = attitude.transform(new Vector3f(0.0F, 0.0F, -1.0F));
      double rise = Math.asin(Mth.clamp(nose.y, -1.0F, 1.0F));
      double trail = Math.toRadians(physics.trail()) * Mth.clamp(speed, 0.0, 1.0);
      double most = Math.toRadians(rise >= 0.0 ? physics.hang() : physics.sweep());
      return Math.min(Math.min(Math.abs(rise) + trail, Math.max(most, trail)), Math.toRadians(back));
   }

   public static Quaternionf sweptBack(Quaternionfc attitude, double back, Quaternionf dest) {
      Vector3f right = attitude.transform(new Vector3f(1.0F, 0.0F, 0.0F));
      return dest.set(new Quaternionf().rotationAxis((float)(-back), right).mul(attitude));
   }

   public static double backOf(Vec3 direction, Vector3fc down, Vector3fc right) {
      return -turnOf(direction, down, right);
   }

   public static double turnOf(Vec3 direction, Vector3fc from, Vector3fc axis) {
      Vec3 r = new Vec3(axis.x(), axis.y(), axis.z());
      Vec3 d = new Vec3(from.x(), from.y(), from.z());
      Vec3 dp = d.subtract(r.scale(d.dot(r)));
      Vec3 vp = direction.subtract(r.scale(direction.dot(r)));
      return !(dp.lengthSqr() < 1.0E-12) && !(vp.lengthSqr() < 1.0E-12) ? Math.atan2(r.dot(dp.cross(vp)), dp.dot(vp)) : Double.NaN;
   }

   public static Vec3 worldDirection(Quaternionfc attitude, Vec3 body) {
      Vector3f v = attitude.transform(new Vector3f((float)body.x, (float)body.y, (float)(-body.z)));
      return new Vec3(v.x, v.y, v.z);
   }

   public static Vec3 worldPoint(Vec3 position, double halfHeight, Quaternionfc attitude, Vec3 body) {
      Vector3f v = new Vector3f((float)body.x, (float)(body.y - halfHeight), (float)(-body.z));
      attitude.transform(v);
      return new Vec3(position.x + v.x, position.y + halfHeight + v.y, position.z + v.z);
   }

   public static Vec3 bodyPoint(Vec3 position, double halfHeight, Quaternionfc attitude, Vec3 world) {
      Vector3f v = new Vector3f(
         (float)(world.x - position.x), (float)(world.y - position.y - halfHeight), (float)(world.z - position.z)
      );
      new Quaternionf(attitude).conjugate().transform(v);
      return new Vec3(v.x, v.y + halfHeight, -v.z);
   }

   public static Vec3 toBody(Quaternionfc attitude, Vec3 world) {
      Vector3f v = new Vector3f((float)world.x, (float)world.y, (float)world.z);
      new Quaternionf(attitude).conjugate().transform(v);
      return new Vec3(v.x, v.y, -v.z);
   }

   public static GripGeometry.Placement hang(Vec3 pivot, Quaternionfc rotation, double height, double bandMid) {
      Vector3f toFeet = rotation.transform(new Vector3f(0.0F, (float)(-bandMid * height), 0.0F));
      Vector3f middle = rotation.transform(new Vector3f(0.0F, (float)((0.5 - bandMid) * height), 0.0F));
      Vec3 centre = pivot.add(middle.x, middle.y, middle.z);
      return new GripGeometry.Placement(
         pivot, new Quaternionf(rotation), pivot.add(toFeet.x, toFeet.y, toFeet.z), centre, centre.subtract(0.0, height * 0.5, 0.0)
      );
   }

   public record Box(Vec3 centre, Quaternionf rotation, double halfWidth, double height) {
      public Vector3f local(Vec3 world) {
         Vector3f v = new Vector3f(
            (float)(world.x - this.centre.x), (float)(world.y - this.centre.y), (float)(world.z - this.centre.z)
         );
         return new Quaternionf(this.rotation).conjugate().transform(v);
      }

      public Vec3 world(Vector3f local) {
         Vector3f v = this.rotation.transform(new Vector3f(local));
         return this.centre.add(v.x, v.y, v.z);
      }

      public double depth(Vec3 world) {
         Vector3f l = this.local(world);
         double hh = this.height * 0.5;
         double qx = Math.abs(l.x) - this.halfWidth;
         double qy = Math.abs(l.y) - hh;
         double qz = Math.abs(l.z) - this.halfWidth;
         double outside = Math.sqrt(Math.pow(Math.max(qx, 0.0), 2.0) + Math.pow(Math.max(qy, 0.0), 2.0) + Math.pow(Math.max(qz, 0.0), 2.0));
         return outside + Math.min(Math.max(qx, Math.max(qy, qz)), 0.0);
      }

      public boolean contains(Vec3 world, double margin) {
         Vector3f l = this.local(world);
         return Math.abs(l.x) <= this.halfWidth + margin && Math.abs(l.z) <= this.halfWidth + margin && Math.abs(l.y) <= this.height * 0.5 + margin;
      }

      public void grip(float side, double forward, double bandMid, double bite, Vector3f point, Vector3f inward) {
         float hw = (float)this.halfWidth;
         float hh = (float)(this.height * 0.5);
         float z = Mth.clamp((float)(-forward), -hw, hw);
         Vector3f local;
         Vector3f normal;
         if (side != 0.0F) {
            float s = Math.signum(side);
            local = new Vector3f(s * (hw - (float)bite), (float)(bandMid * this.height) - hh, z);
            normal = new Vector3f(-s, 0.0F, 0.0F);
         } else {
            local = new Vector3f(0.0F, hh - (float)bite, z);
            normal = new Vector3f(0.0F, -1.0F, 0.0F);
         }

         Vec3 world = this.world(local);
         point.set((float)world.x, (float)world.y, (float)world.z);
         this.rotation.transform(inward.set(normal));
      }
   }

   public record Placement(Vec3 clutch, Quaternionf rotation, Vec3 feet, Vec3 centre, Vec3 boxOrigin) {
      public Quaternionf around(float drawnYaw) {
         return new Quaternionf(this.rotation).mul(GripGeometry.yawOnly(drawnYaw, new Quaternionf()).conjugate());
      }
   }
}
