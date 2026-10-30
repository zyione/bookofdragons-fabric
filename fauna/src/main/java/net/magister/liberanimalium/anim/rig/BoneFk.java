package net.magister.liberanimalium.anim.rig;

import java.util.ArrayList;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;
import mod.azure.azurelib.model.AzBakedModel;
import mod.azure.azurelib.model.AzBone;
import net.minecraft.util.Mth;
import org.joml.Math;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.joml.Matrix4fc;
import org.joml.Quaternionf;
import org.joml.Quaternionfc;
import org.joml.Vector3f;

public final class BoneFk {
   private static final Matrix4fc IDENTITY = new Matrix4f();
   private static final Map<AzBakedModel, Map<AzBone, List<AzBone>>> CHAINS = Collections.synchronizedMap(new WeakHashMap<>());
   private static final Matrix3f M3 = new Matrix3f();
   private static final Vector3f V_A = new Vector3f();
   private static final Vector3f V_B = new Vector3f();
   private static final Quaternionf Q_A = new Quaternionf();
   private static final Quaternionf Q_B = new Quaternionf();
   private static final Quaternionf Q_C = new Quaternionf();
   private static final Quaternionf Q_D = new Quaternionf();

   private BoneFk() {
   }

   public static Matrix4f localMatrix(AzBone b) {
      return localMatrix(b, b.getRotX(), b.getRotY(), b.getRotZ(), b.getPosX(), b.getPosY(), b.getPosZ());
   }

   public static Matrix4f localMatrix(AzBone b, float rotX, float rotY, float rotZ, float posX, float posY, float posZ) {
      return mulLocal(IDENTITY, b, rotX, rotY, rotZ, posX, posY, posZ, new Matrix4f());
   }

   public static Matrix4f mulLocal(Matrix4f accum, AzBone b) {
      return mulLocal(accum, b, b.getRotX(), b.getRotY(), b.getRotZ(), b.getPosX(), b.getPosY(), b.getPosZ(), accum);
   }

   public static Matrix4f mulLocal(Matrix4fc parent, AzBone b, Matrix4f dest) {
      return mulLocal(parent, b, b.getRotX(), b.getRotY(), b.getRotZ(), b.getPosX(), b.getPosY(), b.getPosZ(), dest);
   }

   public static Matrix4f mulLocal(Matrix4fc parent, AzBone b, float rotX, float rotY, float rotZ, float posX, float posY, float posZ, Matrix4f dest) {
      float sx;
      float cx;
      if (rotX == 0.0F) {
         sx = 0.0F;
         cx = 1.0F;
      } else {
         sx = Math.sin(rotX);
         cx = Math.cosFromSin(sx, rotX);
      }

      float sy;
      float cy;
      if (rotY == 0.0F) {
         sy = 0.0F;
         cy = 1.0F;
      } else {
         sy = Math.sin(rotY);
         cy = Math.cosFromSin(sy, rotY);
      }

      float sz;
      float cz;
      if (rotZ == 0.0F) {
         sz = 0.0F;
         cz = 1.0F;
      } else {
         sz = Math.sin(rotZ);
         cz = Math.cosFromSin(sz, rotZ);
      }

      float scx = b.getScaleX();
      float scy = b.getScaleY();
      float scz = b.getScaleZ();
      float l00 = cz * cy * scx;
      float l01 = sz * cy * scx;
      float l02 = -sy * scx;
      float l10 = (cz * sy * sx - sz * cx) * scy;
      float l11 = (sz * sy * sx + cz * cx) * scy;
      float l12 = cy * sx * scy;
      float l20 = (cz * sy * cx + sz * sx) * scz;
      float l21 = (sz * sy * cx - cz * sx) * scz;
      float l22 = cy * cx * scz;
      float px = b.getPivotX() / 16.0F;
      float py = b.getPivotY() / 16.0F;
      float pz = b.getPivotZ() / 16.0F;
      float tx = px - posX / 16.0F;
      float ty = py + posY / 16.0F;
      float tz = pz + posZ / 16.0F;
      float l30 = tx - (l00 * px + l10 * py + l20 * pz);
      float l31 = ty - (l01 * px + l11 * py + l21 * pz);
      float l32 = tz - (l02 * px + l12 * py + l22 * pz);
      float p00 = parent.m00();
      float p01 = parent.m01();
      float p02 = parent.m02();
      float p10 = parent.m10();
      float p11 = parent.m11();
      float p12 = parent.m12();
      float p20 = parent.m20();
      float p21 = parent.m21();
      float p22 = parent.m22();
      float p30 = parent.m30();
      float p31 = parent.m31();
      float p32 = parent.m32();
      return dest.set(
         p00 * l00 + p10 * l01 + p20 * l02,
         p01 * l00 + p11 * l01 + p21 * l02,
         p02 * l00 + p12 * l01 + p22 * l02,
         0.0F,
         p00 * l10 + p10 * l11 + p20 * l12,
         p01 * l10 + p11 * l11 + p21 * l12,
         p02 * l10 + p12 * l11 + p22 * l12,
         0.0F,
         p00 * l20 + p10 * l21 + p20 * l22,
         p01 * l20 + p11 * l21 + p21 * l22,
         p02 * l20 + p12 * l21 + p22 * l22,
         0.0F,
         p00 * l30 + p10 * l31 + p20 * l32 + p30,
         p01 * l30 + p11 * l31 + p21 * l32 + p31,
         p02 * l30 + p12 * l31 + p22 * l32 + p32,
         1.0F
      );
   }

   public static Vector3f offsetPoint(AzBone b) {
      return offsetPoint(b, new Vector3f());
   }

   public static Vector3f offsetPoint(AzBone b, Vector3f dest) {
      return dest.set((b.getPivotX() - b.getPosX()) / 16.0F, (b.getPivotY() + b.getPosY()) / 16.0F, (b.getPivotZ() + b.getPosZ()) / 16.0F);
   }

   public static List<AzBone> ancestorsTopDown(AzBone leaf, AzBakedModel model) {
      Map<AzBone, List<AzBone>> perModel = chainsFor(model);
      synchronized (perModel) {
         List<AzBone> cached = perModel.get(leaf);
         if (cached != null) {
            return cached;
         }
      }

      List<AzBone> up = new ArrayList<>();

      for (AzBone b = leaf; b != null; b = b.getParent()) {
         AzBone live = model.getBoneOrNull(b.getName());
         up.add(live != null ? live : b);
      }

      List<AzBone> down = new ArrayList<>(up.size());

      for (int i = up.size() - 1; i >= 0; i--) {
         down.add(up.get(i));
      }

      List<AzBone> immutable = List.copyOf(down);
      synchronized (perModel) {
         perModel.put(leaf, immutable);
         return immutable;
      }
   }

   private static Map<AzBone, List<AzBone>> chainsFor(AzBakedModel model) {
      Map<AzBone, List<AzBone>> perModel = CHAINS.get(model);
      if (perModel == null) {
         perModel = Collections.synchronizedMap(new IdentityHashMap<>());
         Map<AzBone, List<AzBone>> raced = CHAINS.putIfAbsent(model, perModel);
         if (raced != null) {
            perModel = raced;
         }
      }

      return perModel;
   }

   public static void clearChains() {
      CHAINS.clear();
   }

   public static int cachedModels() {
      return CHAINS.size();
   }

   public static void aimBone(AzBone bone, Matrix4f parentAccum, Vector3f joint, Vector3f childCurrent, Vector3f childTarget) {
      aimBone(bone, parentAccum, joint.x, joint.y, joint.z, childCurrent.x, childCurrent.y, childCurrent.z, childTarget.x, childTarget.y, childTarget.z);
   }

   public static void aimBone(AzBone bone, Matrix4fc parentAccum, float jx, float jy, float jz, float cx, float cy, float cz, float tx, float ty, float tz) {
      Vector3f dAnim = V_A.set(cx - jx, cy - jy, cz - jz);
      Vector3f dTarget = V_B.set(tx - jx, ty - jy, tz - jz);
      if (!(dAnim.lengthSquared() < 1.0E-9F) && !(dTarget.lengthSquared() < 1.0E-9F)) {
         dAnim.normalize();
         dTarget.normalize();
         Quaternionf dModel = Q_A.rotationTo(dAnim, dTarget);
         Quaternionf qParent = orthonormalRotation(parentAccum, Q_B);
         Quaternionf qLocalNew = Q_C.set(qParent).conjugate().mul(dModel).mul(qParent).mul(localQuat(bone, Q_D));
         writeEulerZYX(bone, qLocalNew);
      }
   }

   public static void lockWorldOrientation(AzBone bone, Matrix4fc parentAccumNew, Quaternionfc worldRot) {
      Quaternionf qParent = orthonormalRotation(parentAccumNew, Q_B);
      Quaternionf qLocalNew = qParent.conjugate().mul(worldRot);
      writeEulerZYX(bone, qLocalNew);
   }

   public static Quaternionf localQuat(AzBone bone) {
      return localQuat(bone, new Quaternionf());
   }

   public static Quaternionf localQuat(AzBone bone, Quaternionf dest) {
      return dest.rotationZYX(bone.getRotZ(), bone.getRotY(), bone.getRotX());
   }

   public static void writeEulerZYX(AzBone bone, Quaternionfc qLocal) {
      Matrix3f m = qLocal.get(M3);
      float r20 = m.m02;
      float r21 = m.m12;
      float r22 = m.m22;
      float r10 = m.m01;
      float r00 = m.m00;
      float r01 = m.m10;
      float r11 = m.m11;
      float beta = (float)java.lang.Math.asin(Mth.clamp(-r20, -1.0F, 1.0F));
      float alpha;
      float gamma;
      if (java.lang.Math.abs(java.lang.Math.cos(beta)) > 1.0E-4F) {
         alpha = (float)java.lang.Math.atan2(r21, r22);
         gamma = (float)java.lang.Math.atan2(r10, r00);
      } else {
         alpha = 0.0F;
         gamma = (float)java.lang.Math.atan2(-r01, r11);
      }

      bone.setRotX(alpha);
      bone.setRotY(beta);
      bone.setRotZ(gamma);
   }

   public static float[] eulerZYX(Quaternionf q) {
      Matrix3f m = q.get(new Matrix3f());
      float r20 = m.m02;
      float r21 = m.m12;
      float r22 = m.m22;
      float r10 = m.m01;
      float r00 = m.m00;
      float r01 = m.m10;
      float r11 = m.m11;
      float beta = (float)java.lang.Math.asin(Mth.clamp(-r20, -1.0F, 1.0F));
      float alpha;
      float gamma;
      if (java.lang.Math.abs(java.lang.Math.cos(beta)) > 1.0E-4F) {
         alpha = (float)java.lang.Math.atan2(r21, r22);
         gamma = (float)java.lang.Math.atan2(r10, r00);
      } else {
         alpha = 0.0F;
         gamma = (float)java.lang.Math.atan2(-r01, r11);
      }

      return new float[]{alpha, beta, gamma};
   }

   public static Quaternionf orthonormalRotation(Matrix4f m) {
      return orthonormalRotation(m, new Quaternionf());
   }

   public static Quaternionf orthonormalRotation(Matrix4fc m, Quaternionf dest) {
      Matrix3f r = m.get3x3(M3);
      float s = (float)java.lang.Math.sqrt(r.m00 * r.m00 + r.m01 * r.m01 + r.m02 * r.m02);
      if (s > 1.0E-6F) {
         r.scale(1.0F / s);
      }

      return r.getNormalizedRotation(dest);
   }
}
