package net.magister.liberanimalium.render;

import java.util.IdentityHashMap;
import java.util.Map;
import mod.azure.azurelib.cache.object.GeoCube;
import mod.azure.azurelib.cache.object.GeoQuad;
import mod.azure.azurelib.cache.object.GeoVertex;
import mod.azure.azurelib.model.AzBone;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.joml.Vector3f;

public final class CubeMesh {
   public static final int OUT_PER_QUAD = 15;
   public static final int MAX_QUADS = 6;
   private final int quads;
   private final float[] pos;
   private final float[] nrm;
   private final float[] uv;
   private final boolean flipX;
   private final boolean flipY;
   private final boolean flipZ;
   private static final Map<GeoCube, CubeMesh> CACHE = new IdentityHashMap<>();
   private static final Vector3f SCRATCH = new Vector3f();

   private CubeMesh(int quads, float[] pos, float[] nrm, float[] uv, boolean flipX, boolean flipY, boolean flipZ) {
      this.quads = quads;
      this.pos = pos;
      this.nrm = nrm;
      this.uv = uv;
      this.flipX = flipX;
      this.flipY = flipY;
      this.flipZ = flipZ;
   }

   public static CubeMesh of(GeoCube cube) {
      CubeMesh mesh = CACHE.get(cube);
      if (mesh == null) {
         mesh = bake(cube);
         CACHE.put(cube, mesh);
      }

      return mesh;
   }

   Vector3f centre(Vector3f dest) {
      dest.zero();
      int vertices = this.quads * 4;
      if (vertices == 0) {
         return dest;
      }

      for (int i = 0; i < vertices; i++) {
         dest.add(this.pos[i * 3], this.pos[i * 3 + 1], this.pos[i * 3 + 2]);
      }

      return dest.div(vertices);
   }

   public void bounds(Vector3f min, Vector3f max) {
      int i = 0;

      for (int n = this.quads * 4; i < n; i++) {
         min.min(SCRATCH.set(this.pos[i * 3], this.pos[i * 3 + 1], this.pos[i * 3 + 2]));
         max.max(SCRATCH);
      }
   }

   public static void invalidate() {
      CACHE.clear();
   }

   public static int cached() {
      return CACHE.size();
   }

   static CubeMesh bake(GeoCube cube) {
      float px = (float)(cube.pivot().x() / 16.0);
      float py = (float)(cube.pivot().y() / 16.0);
      float pz = (float)(cube.pivot().z() / 16.0);
      float rx = (float)cube.rotation().x();
      float ry = (float)cube.rotation().y();
      float rz = (float)cube.rotation().z();
      Matrix4f local = new Matrix4f().translate(px, py, pz);
      Matrix3f rot = new Matrix3f();
      if (rz != 0.0F) {
         local.rotateZ(rz);
         rot.rotateZ(rz);
      }

      if (ry != 0.0F) {
         local.rotateY(ry);
         rot.rotateY(ry);
      }

      if (rx != 0.0F) {
         local.rotateX(rx);
         rot.rotateX(rx);
      }

      local.translate(-px, -py, -pz);
      int count = 0;

      for (GeoQuad quad : cube.quads()) {
         if (quad != null) {
            count++;
         }
      }

      float[] pos = new float[count * 12];
      float[] nrm = new float[count * 3];
      float[] uv = new float[count * 8];
      Vector3f v = new Vector3f();
      int q = 0;

      for (GeoQuad quad : cube.quads()) {
         if (quad != null) {
            v.set(quad.normal());
            rot.transform(v);
            nrm[q * 3] = v.x;
            nrm[q * 3 + 1] = v.y;
            nrm[q * 3 + 2] = v.z;
            GeoVertex[] vertices = quad.vertices();

            for (int i = 0; i < 4; i++) {
               Vector3f p = vertices[i].position();
               local.transformPosition(p.x(), p.y(), p.z(), v);
               int at = (q * 4 + i) * 3;
               pos[at] = v.x;
               pos[at + 1] = v.y;
               pos[at + 2] = v.z;
               int t = (q * 4 + i) * 2;
               uv[t] = vertices[i].texU();
               uv[t + 1] = vertices[i].texV();
            }

            q++;
         }
      }

      boolean flipX = cube.size().y() == 0.0 || cube.size().z() == 0.0;
      boolean flipY = cube.size().x() == 0.0 || cube.size().z() == 0.0;
      boolean flipZ = cube.size().x() == 0.0 || cube.size().y() == 0.0;
      return new CubeMesh(count, pos, nrm, uv, flipX, flipY, flipZ);
   }

   public int transform(Matrix4f pose, Matrix3f normal, float[] out) {
      Vector3f s = SCRATCH;
      int q = 0;

      for (int o = 0; q < this.quads; o += 15) {
         s.set(this.nrm[q * 3], this.nrm[q * 3 + 1], this.nrm[q * 3 + 2]);
         normal.transform(s);
         float nx = s.x;
         float ny = s.y;
         float nz = s.z;
         if (nx < 0.0F && this.flipX) {
            nx = -nx;
         }

         if (ny < 0.0F && this.flipY) {
            ny = -ny;
         }

         if (nz < 0.0F && this.flipZ) {
            nz = -nz;
         }

         out[o] = nx;
         out[o + 1] = ny;
         out[o + 2] = nz;

         for (int i = 0; i < 4; i++) {
            int p = (q * 4 + i) * 3;
            pose.transformPosition(this.pos[p], this.pos[p + 1], this.pos[p + 2], s);
            int d = o + 3 + i * 3;
            out[d] = s.x;
            out[d + 1] = s.y;
            out[d + 2] = s.z;
         }

         q++;
      }

      return this.quads;
   }

   public float[] uv() {
      return this.uv;
   }

   public float[] positions() {
      return this.pos;
   }

   public float[] normals() {
      return this.nrm;
   }

   public byte flipFlags() {
      return (byte)((this.flipX ? 1 : 0) | (this.flipY ? 2 : 0) | (this.flipZ ? 4 : 0));
   }

   public int quads() {
      return this.quads;
   }

   public static Vector3f farEnd(AzBone bone) {
      float x = 0.0F;
      float y = 0.0F;
      float z = 0.0F;
      float weight = 0.0F;
      Vector3f centre = new Vector3f();

      for (GeoCube cube : bone.getCubes()) {
         float volume = (float)Math.max(0.001, cube.size().x() * cube.size().y() * cube.size().z());
         of(cube).centre(centre);
         x += centre.x * volume;
         y += centre.y * volume;
         z += centre.z * volume;
         weight += volume;
      }

      if (weight <= 0.0F) {
         return null;
      }

      float px = bone.getPivotX() / 16.0F;
      float py = bone.getPivotY() / 16.0F;
      float pz = bone.getPivotZ() / 16.0F;
      Vector3f far = new Vector3f(2.0F * x / weight - px, 2.0F * y / weight - py, 2.0F * z / weight - pz);
      return far.distanceSquared(px, py, pz) < 1.0E-6F ? null : far;
   }
}
