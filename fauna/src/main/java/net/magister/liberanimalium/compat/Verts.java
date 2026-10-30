package net.magister.liberanimalium.compat;

import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.PoseStack.Pose;
import net.magister.liberanimalium.compat.v20.VertsImpl;
import org.joml.Matrix3f;
import org.joml.Matrix4f;

public final class Verts {
   private Verts() {
   }

   public static void posColor(VertexConsumer c, Matrix4f m, float x, float y, float z, float r, float g, float b, float a) {
      VertsImpl.posColor(c, m, x, y, z, r, g, b, a);
   }

   public static void posTexColor(VertexConsumer c, Matrix4f m, float x, float y, float z, float u, float v, float r, float g, float b, float a) {
      VertsImpl.posTexColor(c, m, x, y, z, u, v, r, g, b, a);
   }

   public static void posColorNormal(VertexConsumer c, Pose p, float x, float y, float z, float r, float g, float b, float a, float nx, float ny, float nz) {
      VertsImpl.posColorNormal(c, p, x, y, z, r, g, b, a, nx, ny, nz);
   }

   public static void particle(VertexConsumer c, float x, float y, float z, float u, float v, float r, float g, float b, float a, int lightU, int lightV) {
      VertsImpl.particle(c, x, y, z, u, v, r, g, b, a, lightU, lightV);
   }

   public static void entity(
      VertexConsumer c,
      Matrix4f m,
      Matrix3f n,
      float x,
      float y,
      float z,
      float r,
      float g,
      float b,
      float a,
      float u,
      float v,
      int overlayU,
      int overlayV,
      int lightU,
      int lightV,
      float nx,
      float ny,
      float nz
   ) {
      VertsImpl.entity(c, m, n, x, y, z, r, g, b, a, u, v, overlayU, overlayV, lightU, lightV, nx, ny, nz);
   }

   public static void entity(
      VertexConsumer c,
      Matrix4f m,
      Matrix3f n,
      float x,
      float y,
      float z,
      float r,
      float g,
      float b,
      float a,
      float u,
      float v,
      int overlay,
      int light,
      float nx,
      float ny,
      float nz
   ) {
      VertsImpl.entity(c, m, n, x, y, z, r, g, b, a, u, v, overlay, light, nx, ny, nz);
   }

   public static void entity(
      VertexConsumer c, float x, float y, float z, float r, float g, float b, float a, float u, float v, int overlay, int light, float nx, float ny, float nz
   ) {
      VertsImpl.entity(c, x, y, z, r, g, b, a, u, v, overlay, light, nx, ny, nz);
   }
}
