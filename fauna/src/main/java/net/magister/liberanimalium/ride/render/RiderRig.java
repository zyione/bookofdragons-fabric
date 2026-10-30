package net.magister.liberanimalium.ride.render;

import net.minecraft.util.Mth;
import org.joml.Quaternionf;
import org.joml.Vector3f;

public final class RiderRig {
   public static final int PART_BODY = 0;
   public static final int PART_HEAD = 1;
   public static final int PART_RIGHT_ARM = 2;
   public static final int PART_LEFT_ARM = 3;
   public static final int PART_RIGHT_LEG = 4;
   public static final int PART_LEFT_LEG = 5;
   public static final int PARTS = 6;
   public static final int X = 0;
   public static final int Y = 1;
   public static final int Z = 2;
   public static final int XROT = 3;
   public static final int YROT = 4;
   public static final int ZROT = 5;
   private final Quaternionf root = new Quaternionf();
   private final Quaternionf torso = new Quaternionf();
   private final Quaternionf upper = new Quaternionf();
   private final Quaternionf upperStatic = new Quaternionf();
   private final Quaternionf motion = new Quaternionf();
   private final Quaternionf head = new Quaternionf();
   private final Quaternionf part = new Quaternionf();
   private final Vector3f v = new Vector3f();
   private final Vector3f euler = new Vector3f();
   private final float[][] posed = new float[6][6];

   public static int partOf(RiderBone bone) {
      return bone.ordinal() - 1;
   }

   public void solve(float[][] vanilla, float[][] delta, float[][] pose, float headFollow, float weight, float[][] out) {
      float[] dRoot = delta[RiderBone.ROOT.ordinal()];
      float[] dTorso = delta[RiderBone.TORSO.ordinal()];
      this.root.rotationZYX(dRoot[2], dRoot[1], dRoot[0]);
      this.torso.rotationZYX(dTorso[2], dTorso[1], dTorso[0]);
      this.root.mul(this.torso, this.upper);
      float[] sRoot = pose[RiderBone.ROOT.ordinal()];
      float[] sTorso = pose[RiderBone.TORSO.ordinal()];
      this.upperStatic.rotationZYX(sRoot[2], sRoot[1], sRoot[0]);
      this.motion.rotationZYX(sTorso[2], sTorso[1], sTorso[0]);
      this.upperStatic.mul(this.motion).invert();
      this.upper.mul(this.upperStatic, this.motion);
      this.head.identity().slerp(this.motion, Mth.clamp(headFollow, 0.0F, 1.0F));
      this.leg(vanilla, delta, RiderBone.RIGHT_LEG, dRoot);
      this.leg(vanilla, delta, RiderBone.LEFT_LEG, dRoot);
      this.upperPart(vanilla[0], null, dRoot, dTorso, this.upper, this.posed[0]);
      this.upperPart(vanilla[1], delta[RiderBone.HEAD.ordinal()], dRoot, dTorso, this.head, this.posed[1]);
      this.upperPart(vanilla[2], delta[RiderBone.RIGHT_ARM.ordinal()], dRoot, dTorso, this.upper, this.posed[2]);
      this.upperPart(vanilla[3], delta[RiderBone.LEFT_ARM.ordinal()], dRoot, dTorso, this.upper, this.posed[3]);
      float w = Mth.clamp(weight, 0.0F, 1.0F);

      for (int p = 0; p < 6; p++) {
         for (int i = 0; i < 6; i++) {
            float from = vanilla[p][i];
            float diff = this.posed[p][i] - from;
            if (i >= 3) {
               diff = wrapRadians(diff);
            }

            out[p][i] = from + w * diff;
         }
      }
   }

   private void leg(float[][] vanilla, float[][] delta, RiderBone bone, float[] dRoot) {
      float[] van = vanilla[partOf(bone)];
      float[] d = delta[bone.ordinal()];
      float[] o = this.posed[partOf(bone)];
      this.v.set(van[0] + d[3], van[1] + d[4] - 12.0F, van[2] + d[5]);
      this.root.transform(this.v);
      o[0] = this.v.x + dRoot[3];
      o[1] = this.v.y + 12.0F + dRoot[4];
      o[2] = this.v.z + dRoot[5];
      this.part.rotationZYX(van[5] + d[2], van[4] + d[1], van[3] + d[0]);
      this.root.mul(this.part, this.part);
      this.writeEuler(this.part, o);
   }

   private void upperPart(float[] van, float[] d, float[] dRoot, float[] dTorso, Quaternionf turn, float[] o) {
      float tx = d == null ? 0.0F : d[3];
      float ty = d == null ? 0.0F : d[4];
      float tz = d == null ? 0.0F : d[5];
      this.v.set(van[0] + tx, van[1] + ty - 12.0F, van[2] + tz);
      this.torso.transform(this.v);
      this.v.add(dTorso[3], dTorso[4], dTorso[5]);
      this.root.transform(this.v);
      o[0] = this.v.x + dRoot[3];
      o[1] = this.v.y + 12.0F + dRoot[4];
      o[2] = this.v.z + dRoot[5];
      float rx = d == null ? 0.0F : d[0];
      float ry = d == null ? 0.0F : d[1];
      float rz = d == null ? 0.0F : d[2];
      this.part.rotationZYX(van[5] + rz, van[4] + ry, van[3] + rx);
      turn.mul(this.part, this.part);
      this.writeEuler(this.part, o);
   }

   static void toModelPartAngles(Quaternionf q, Vector3f out) {
      float x = q.x;
      float y = q.y;
      float z = q.z;
      float w = q.w;
      float r00 = 1.0F - 2.0F * (y * y + z * z);
      float r10 = 2.0F * (x * y + w * z);
      float r20 = 2.0F * (x * z - w * y);
      float r21 = 2.0F * (y * z + w * x);
      float r22 = 1.0F - 2.0F * (x * x + y * y);
      if (Math.abs(r20) < 0.99999F) {
         out.x = (float)Math.atan2(r21, r22);
         out.y = (float)Math.asin(-r20);
         out.z = (float)Math.atan2(r10, r00);
      } else {
         float r11 = 1.0F - 2.0F * (x * x + z * z);
         float r12 = 2.0F * (y * z - w * x);
         out.x = (float)Math.atan2(-r12, r11);
         out.y = r20 < 0.0F ? (float) (Math.PI / 2) : (float) (-Math.PI / 2);
         out.z = 0.0F;
      }
   }

   private void writeEuler(Quaternionf q, float[] o) {
      toModelPartAngles(q, this.euler);
      o[3] = this.euler.x;
      o[4] = this.euler.y;
      o[5] = this.euler.z;
   }

   static float wrapRadians(float a) {
      float twoPi = (float) (Math.PI * 2);
      a %= twoPi;
      if (a > Math.PI) {
         a -= twoPi;
      }

      if (a <= -Math.PI) {
         a += twoPi;
      }

      return a;
   }
}
