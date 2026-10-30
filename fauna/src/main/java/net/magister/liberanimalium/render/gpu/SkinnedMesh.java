package net.magister.liberanimalium.render.gpu;

import com.mojang.blaze3d.vertex.VertexBuffer;
import com.mojang.blaze3d.vertex.VertexBuffer.Usage;
import com.mojang.blaze3d.vertex.VertexFormat.Mode;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;
import mod.azure.azurelib.model.AzBakedModel;
import net.magister.liberanimalium.anim.rig.BoneMatrices;
import net.magister.liberanimalium.compat.Meshes;
import net.magister.liberanimalium.render.ModelMesh;

public final class SkinnedMesh implements AutoCloseable {
   private static final Map<AzBakedModel, SkinnedMesh> MESHES = Collections.synchronizedMap(new WeakHashMap<>());
   private static final Map<String, SkinnedMesh.Canonical> CANONICAL = new HashMap<>();
   private static final Map<BoneMatrices, Boolean> FITS = Collections.synchronizedMap(new WeakHashMap<>());
   private final VertexBuffer buffer;
   private final int quads;
   private final int bones;
   private static final Map<AzBakedModel, Map<List<String>, SkinnedMesh>> WORN = Collections.synchronizedMap(new WeakHashMap<>());

   public static SkinnedMesh of(AzBakedModel model) {
      SkinnedMesh mesh = MESHES.get(model);
      if (mesh == null) {
         mesh = build(model);
         if (mesh == null) {
            return null;
         }

         SkinnedMesh raced = MESHES.putIfAbsent(model, mesh);
         if (raced != null) {
            mesh.close();
            mesh = raced;
         }
      }

      return mesh;
   }

   public static void clear() {
      synchronized (MESHES) {
         for (SkinnedMesh mesh : MESHES.values()) {
            mesh.close();
         }

         MESHES.clear();
         CANONICAL.clear();
         FITS.clear();
      }

      synchronized (WORN) {
         for (Map<List<String>, SkinnedMesh> byWearer : WORN.values()) {
            synchronized (byWearer) {
               for (SkinnedMesh mesh : byWearer.values()) {
                  mesh.close();
               }
            }
         }

         WORN.clear();
      }
   }

   public static int cachedModels() {
      return MESHES.size();
   }

   public static SkinnedMesh canonical(String species, SkinnedMesh mesh, BoneMatrices layout) {
      SkinnedMesh.Canonical c = CANONICAL.get(species);
      if (c == null) {
         c = new SkinnedMesh.Canonical(mesh, names(layout));
         CANONICAL.put(species, c);
         FITS.put(layout, Boolean.TRUE);
         return mesh;
      }

      if (mesh == c.mesh) {
         return mesh;
      }

      Boolean fits = FITS.get(layout);
      if (fits == null) {
         fits = c.names.equals(names(layout)) && mesh.quads() == c.mesh.quads() && mesh.bones() == c.mesh.bones();
         FITS.put(layout, fits);
      }

      return fits ? c.mesh : null;
   }

   private static List<String> names(BoneMatrices layout) {
      List<String> out = new ArrayList<>(layout.size());

      for (int i = 0; i < layout.size(); i++) {
         out.add(layout.bone(i).getName());
      }

      return out;
   }

   private SkinnedMesh(VertexBuffer buffer, int quads, int bones) {
      this.buffer = buffer;
      this.quads = quads;
      this.bones = bones;
   }

   public VertexBuffer buffer() {
      return this.buffer;
   }

   public int quads() {
      return this.quads;
   }

   public int bones() {
      return this.bones;
   }

   @Override
   public void close() {
      this.buffer.close();
   }

   public static SkinnedMesh worn(AzBakedModel worn, BoneMatrices wearer) {
      Map<List<String>, SkinnedMesh> byWearer = WORN.computeIfAbsent(worn, m -> Collections.synchronizedMap(new HashMap<>()));
      List<String> key = wearer.names();
      SkinnedMesh mesh = byWearer.get(key);
      if (mesh == null) {
         mesh = build(ModelMesh.worn(worn, wearer), wearer.size());
         if (mesh == null) {
            return null;
         }

         SkinnedMesh raced = byWearer.putIfAbsent(key, mesh);
         if (raced != null) {
            mesh.close();
            mesh = raced;
         }
      }

      return mesh;
   }

   private static SkinnedMesh build(AzBakedModel model) {
      return build(ModelMesh.of(model), BoneMatrices.of(model).size());
   }

   private static SkinnedMesh build(ModelMesh gathered, int bones) {
      int quads = gathered.quads();
      if (quads == 0) {
         return null;
      }

      Meshes.Vertices vertices = new Meshes.Vertices(SkinnedFormat.FORMAT, quads * 4);
      write(vertices, gathered);
      VertexBuffer buffer = new VertexBuffer(Usage.STATIC);
      buffer.bind();
      Meshes.upload(buffer, Mode.QUADS, vertices);
      VertexBuffer.unbind();
      return new SkinnedMesh(buffer, quads, bones);
   }

   static void write(Meshes.Vertices vertices, ModelMesh mesh) {
      float[] pos = mesh.positions();
      float[] nrm = mesh.normals();
      float[] uv = mesh.uv();
      int[] bone = mesh.bones();
      byte[] flip = mesh.flips();
      float[] face = mesh.faceNormals();
      float[] tan = mesh.tangents();
      float[] mid = mesh.midTexCoords();

      for (int q = 0; q < mesh.quads(); q++) {
         short boneIndex = (short)bone[q];
         short flags = (short)(flip[q] & 7);
         float nx = nrm[q * 3];
         float ny = nrm[q * 3 + 1];
         float nz = nrm[q * 3 + 2];

         for (int i = 0; i < 4; i++) {
            int p = (q * 4 + i) * 3;
            int t = (q * 4 + i) * 2;
            vertices.putFloat(0, pos[p]);
            vertices.putFloat(4, pos[p + 1]);
            vertices.putFloat(8, pos[p + 2]);
            vertices.nextElement();
            vertices.putFloat(0, uv[t]);
            vertices.putFloat(4, uv[t + 1]);
            vertices.nextElement();
            vertices.putFloat(0, nx);
            vertices.putFloat(4, ny);
            vertices.putFloat(8, nz);
            vertices.nextElement();
            vertices.putShort(0, boneIndex);
            vertices.putShort(2, flags);
            vertices.nextElement();
            vertices.putFloat(0, face[q * 3]);
            vertices.putFloat(4, face[q * 3 + 1]);
            vertices.putFloat(8, face[q * 3 + 2]);
            vertices.nextElement();
            vertices.putFloat(0, tan[q * 4]);
            vertices.putFloat(4, tan[q * 4 + 1]);
            vertices.putFloat(8, tan[q * 4 + 2]);
            vertices.putFloat(12, tan[q * 4 + 3]);
            vertices.nextElement();
            vertices.putFloat(0, mid[q * 2]);
            vertices.putFloat(4, mid[q * 2 + 1]);
            vertices.nextElement();
            vertices.nextVertex();
         }
      }
   }

   private record Canonical(SkinnedMesh mesh, List<String> names) {
   }
}
