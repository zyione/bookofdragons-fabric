package net.magister.liberanimalium.render;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;
import org.jetbrains.annotations.Nullable;
import mod.azure.azurelib.cache.object.GeoCube;
import mod.azure.azurelib.model.AzBakedModel;
import mod.azure.azurelib.model.AzBone;
import net.magister.liberanimalium.anim.rig.BoneMatrices;
import net.magister.liberanimalium.render.gpu.IrisQuad;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.joml.Vector3f;

public final class ModelMesh {
   public static final int OUT_PER_QUAD = 15;
   private static final Map<AzBakedModel, ModelMesh> MESHES = Collections.synchronizedMap(new WeakHashMap<>());
   private static final Map<AzBakedModel, Map<List<String>, ModelMesh>> WORN = Collections.synchronizedMap(new WeakHashMap<>());
   private final int quads;
   private final float[] pos;
   private final float[] nrm;
   private final float[] uv;
   private final int[] bone;
   private final byte[] flip;
   private final int[] boneFirstQuad;
   private final int[] boneQuadCount;
   private final float[] faceNormal;
   private final float[] tangent;
   private final float[] midTex;
   private static final Vector3f SCRATCH = new Vector3f();

   public static ModelMesh of(AzBakedModel model) {
      ModelMesh mesh = MESHES.get(model);
      if (mesh == null) {
         mesh = new ModelMesh(model);
         ModelMesh raced = MESHES.putIfAbsent(model, mesh);
         if (raced != null) {
            mesh = raced;
         }
      }

      return mesh;
   }

   public static ModelMesh worn(AzBakedModel worn, BoneMatrices wearer) {
      Map<List<String>, ModelMesh> byWearer = WORN.computeIfAbsent(worn, m -> Collections.synchronizedMap(new HashMap<>()));
      return byWearer.computeIfAbsent(wearer.names(), w -> new ModelMesh(worn, wearer));
   }

   public static void clear() {
      MESHES.clear();
      WORN.clear();
   }

   public static int cachedModels() {
      return MESHES.size();
   }

   private ModelMesh(AzBakedModel model) {
      this(model, null);
   }

   private ModelMesh(AzBakedModel model, @Nullable BoneMatrices wearer) {
      BoneMatrices layout = BoneMatrices.of(model);
      int[] row = new int[layout.size()];
      Map<String, Integer> wearerRows = new HashMap<>();
      if (wearer != null) {
         for (int j = 0; j < wearer.size(); j++) {
            wearerRows.put(wearer.bone(j).getName(), j);
         }
      }

      for (int i = 0; i < layout.size(); i++) {
         row[i] = wearer == null ? i : wearerRows.getOrDefault(layout.bone(i).getName(), -1);
      }

      int total = 0;

      for (int i = 0; i < layout.size(); i++) {
         if (row[i] >= 0) {
            for (GeoCube cube : layout.bone(i).getCubes()) {
               total += CubeMesh.of(cube).quads();
            }
         }
      }

      this.quads = total;
      this.pos = new float[total * 12];
      this.nrm = new float[total * 3];
      this.uv = new float[total * 8];
      this.bone = new int[total];
      this.flip = new byte[total];
      this.boneFirstQuad = new int[layout.size()];
      this.boneQuadCount = new int[layout.size()];
      int q = 0;

      for (int i = 0; i < layout.size(); i++) {
         AzBone b = layout.bone(i);
         this.boneFirstQuad[i] = q;
         if (row[i] < 0) {
            this.boneQuadCount[i] = 0;
         } else {
            for (GeoCube cube : b.getCubes()) {
               CubeMesh mesh = CubeMesh.of(cube);
               int n = mesh.quads();
               System.arraycopy(mesh.positions(), 0, this.pos, q * 12, n * 12);
               System.arraycopy(mesh.normals(), 0, this.nrm, q * 3, n * 3);
               System.arraycopy(mesh.uv(), 0, this.uv, q * 8, n * 8);
               byte flags = mesh.flipFlags();

               for (int k = 0; k < n; k++) {
                  this.bone[q + k] = row[i];
                  this.flip[q + k] = flags;
               }

               q += n;
            }

            this.boneQuadCount[i] = q - this.boneFirstQuad[i];
         }
      }

      this.faceNormal = new float[total * 3];
      this.tangent = new float[total * 4];
      this.midTex = new float[total * 2];

      for (int k = 0; k < total; k++) {
         IrisQuad.faceNormal(this.pos, k, this.faceNormal, k * 3);
         IrisQuad.tangent(this.pos, this.uv, k, this.faceNormal[k * 3], this.faceNormal[k * 3 + 1], this.faceNormal[k * 3 + 2], this.tangent, k * 4);
         IrisQuad.midTexCoord(this.uv, k, this.midTex, k * 2);
      }
   }

   public int firstQuadOf(int boneIndex) {
      return this.boneFirstQuad[boneIndex];
   }

   public int quadCountOf(int boneIndex) {
      return this.boneQuadCount[boneIndex];
   }

   public int transformBone(int boneIndex, Matrix4f pose, Matrix3f normal, float[] out) {
      int first = this.boneFirstQuad[boneIndex];
      int n = this.boneQuadCount[boneIndex];
      Vector3f v = SCRATCH;
      int k = 0;

      for (int o = 0; k < n; o += 15) {
         int q = first + k;
         v.set(this.nrm[q * 3], this.nrm[q * 3 + 1], this.nrm[q * 3 + 2]);
         normal.transform(v);
         float nx = v.x;
         float ny = v.y;
         float nz = v.z;
         int f = this.flip[q];
         if (nx < 0.0F && (f & 1) != 0) {
            nx = -nx;
         }

         if (ny < 0.0F && (f & 2) != 0) {
            ny = -ny;
         }

         if (nz < 0.0F && (f & 4) != 0) {
            nz = -nz;
         }

         out[o] = nx;
         out[o + 1] = ny;
         out[o + 2] = nz;

         for (int i = 0; i < 4; i++) {
            int p = (q * 4 + i) * 3;
            pose.transformPosition(this.pos[p], this.pos[p + 1], this.pos[p + 2], v);
            int d = o + 3 + i * 3;
            out[d] = v.x;
            out[d + 1] = v.y;
            out[d + 2] = v.z;
         }

         k++;
      }

      return n;
   }

   public int quads() {
      return this.quads;
   }

   public float[] positions() {
      return this.pos;
   }

   public float[] normals() {
      return this.nrm;
   }

   public float[] uv() {
      return this.uv;
   }

   public int[] bones() {
      return this.bone;
   }

   public byte[] flips() {
      return this.flip;
   }

   public float[] faceNormals() {
      return this.faceNormal;
   }

   public float[] tangents() {
      return this.tangent;
   }

   public float[] midTexCoords() {
      return this.midTex;
   }

   public void transform(Matrix4f[] palette, float[] out) {
      Vector3f v = new Vector3f();
      Matrix3f normal = new Matrix3f();
      int lastBone = -1;
      int q = 0;

      for (int o = 0; q < this.quads; o += 15) {
         int b = this.bone[q];
         Matrix4f pose = palette[b];
         if (b != lastBone) {
            pose.normal(normal);
            lastBone = b;
         }

         v.set(this.nrm[q * 3], this.nrm[q * 3 + 1], this.nrm[q * 3 + 2]);
         normal.transform(v);
         float nx = v.x;
         float ny = v.y;
         float nz = v.z;
         int f = this.flip[q];
         if (nx < 0.0F && (f & 1) != 0) {
            nx = -nx;
         }

         if (ny < 0.0F && (f & 2) != 0) {
            ny = -ny;
         }

         if (nz < 0.0F && (f & 4) != 0) {
            nz = -nz;
         }

         out[o] = nx;
         out[o + 1] = ny;
         out[o + 2] = nz;

         for (int i = 0; i < 4; i++) {
            int p = (q * 4 + i) * 3;
            pose.transformPosition(this.pos[p], this.pos[p + 1], this.pos[p + 2], v);
            int d = o + 3 + i * 3;
            out[d] = v.x;
            out[d + 1] = v.y;
            out[d + 2] = v.z;
         }

         q++;
      }
   }
}
