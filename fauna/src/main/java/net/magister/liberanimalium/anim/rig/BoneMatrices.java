package net.magister.liberanimalium.anim.rig;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;
import mod.azure.azurelib.model.AzBakedModel;
import mod.azure.azurelib.model.AzBone;
import net.minecraft.util.Mth;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.joml.Matrix4fc;
import org.joml.Quaternionf;

public final class BoneMatrices {
   private static final Map<AzBakedModel, BoneMatrices> LAYOUTS = Collections.synchronizedMap(new WeakHashMap<>());
   private final AzBone[] bones;
   private final int[] parent;
   private final Map<String, Integer> byName;
   private List<String> names;
   private static final Matrix4fc IDENTITY = new Matrix4f();
   private static final Quaternionf QUAT = new Quaternionf();

   public static BoneMatrices of(AzBakedModel model) {
      BoneMatrices layout = LAYOUTS.get(model);
      if (layout == null) {
         layout = new BoneMatrices(model);
         BoneMatrices raced = LAYOUTS.putIfAbsent(model, layout);
         if (raced != null) {
            layout = raced;
         }
      }

      return layout;
   }

   public static void clear() {
      LAYOUTS.clear();
   }

   public static int cachedModels() {
      return LAYOUTS.size();
   }

   private BoneMatrices(AzBakedModel model) {
      List<AzBone> order = new ArrayList<>();
      List<Integer> parents = new ArrayList<>();
      Map<AzBone, Integer> index = new IdentityHashMap<>();

      for (AzBone root : model.getTopLevelBones()) {
         walk(root, -1, order, parents, index);
      }

      this.bones = order.toArray(new AzBone[0]);
      this.parent = new int[parents.size()];

      for (int i = 0; i < this.parent.length; i++) {
         this.parent[i] = parents.get(i);
      }

      this.byName = new HashMap<>(this.bones.length * 2);

      for (int i = 0; i < this.bones.length; i++) {
         this.byName.put(this.bones[i].getName(), i);
      }
   }

   private static void walk(AzBone bone, int parentIndex, List<AzBone> order, List<Integer> parents, Map<AzBone, Integer> index) {
      int self = order.size();
      order.add(bone);
      parents.add(parentIndex);
      index.put(bone, self);

      for (AzBone child : bone.getChildBones()) {
         walk(child, self, order, parents, index);
      }
   }

   public int size() {
      return this.bones.length;
   }

   public AzBone bone(int i) {
      return this.bones[i];
   }

   public List<String> names() {
      if (this.names == null) {
         String[] out = new String[this.bones.length];

         for (int i = 0; i < this.bones.length; i++) {
            out[i] = this.bones[i].getName();
         }

         this.names = List.of(out);
      }

      return this.names;
   }

   public int parentOf(int i) {
      return this.parent[i];
   }

   public int indexOf(String boneName) {
      Integer i = this.byName.get(boneName);
      return i == null ? -1 : i;
   }

   public Matrix4f[] newPalette() {
      Matrix4f[] palette = new Matrix4f[this.bones.length];

      for (int i = 0; i < palette.length; i++) {
         palette[i] = new Matrix4f();
      }

      return palette;
   }

   public void compute(Matrix4f[] out) {
      for (int i = 0; i < this.bones.length; i++) {
         int p = this.parent[i];
         BoneFk.mulLocal((Matrix4fc)(p < 0 ? IDENTITY : out[p]), this.bones[i], out[i]);
      }
   }

   public Matrix3f[] newNormals() {
      Matrix3f[] normals = new Matrix3f[this.bones.length];

      for (int i = 0; i < normals.length; i++) {
         normals[i] = new Matrix3f();
      }

      return normals;
   }

   public void computeNormals(Matrix3f[] out) {
      for (int i = 0; i < this.bones.length; i++) {
         AzBone b = this.bones[i];
         int p = this.parent[i];
         Matrix3f n = out[i];
         if (p < 0) {
            n.identity();
         } else {
            n.set(out[p]);
         }

         float rotX = b.getRotX();
         float rotY = b.getRotY();
         float rotZ = b.getRotZ();
         if (rotZ != 0.0F) {
            n.rotate(QUAT.rotationXYZ(0.0F, 0.0F, rotZ));
         }

         if (rotY != 0.0F) {
            n.rotate(QUAT.rotationXYZ(0.0F, rotY, 0.0F));
         }

         if (rotX != 0.0F) {
            n.rotate(QUAT.rotationXYZ(rotX, 0.0F, 0.0F));
         }

         scaleNormal(n, b.getScaleX(), b.getScaleY(), b.getScaleZ());
      }
   }

   private static void scaleNormal(Matrix3f normal, float x, float y, float z) {
      if (x == y && y == z) {
         if (x > 0.0F) {
            return;
         }

         normal.scale(-1.0F);
      }

      float f = 1.0F / x;
      float f1 = 1.0F / y;
      float f2 = 1.0F / z;
      float f3 = Mth.fastInvCubeRoot(f * f1 * f2);
      normal.scale(f3 * f, f3 * f1, f3 * f2);
   }
}
