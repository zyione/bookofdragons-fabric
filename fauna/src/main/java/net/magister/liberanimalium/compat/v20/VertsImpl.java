package net.magister.liberanimalium.compat.v20;

import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.PoseStack.Pose;
import org.joml.Matrix3f;
import org.joml.Matrix4f;

public final class VertsImpl {
   private VertsImpl() {
   }

   public static void posColor(VertexConsumer c, Matrix4f m, float x, float y, float z, float r, float g, float b, float a) {
      c.vertex(m, x, y, z).color(r, g, b, a).endVertex();
   }

   public static void posTexColor(VertexConsumer c, Matrix4f m, float x, float y, float z, float u, float v, float r, float g, float b, float a) {
      c.vertex(m, x, y, z).uv(u, v).color(r, g, b, a).endVertex();
   }

   public static void posColorNormal(VertexConsumer c, Pose p, float x, float y, float z, float r, float g, float b, float a, float nx, float ny, float nz) {
      c.vertex(p.pose(), x, y, z).color(r, g, b, a).normal(p.normal(), nx, ny, nz).endVertex();
   }

   public static void particle(VertexConsumer c, float x, float y, float z, float u, float v, float r, float g, float b, float a, int lightU, int lightV) {
      c.vertex(x, y, z).uv(u, v).color(r, g, b, a).uv2(lightU, lightV).endVertex();
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
      c.vertex(m, x, y, z).color(r, g, b, a).uv(u, v).overlayCoords(overlayU, overlayV).uv2(lightU, lightV).normal(n, nx, ny, nz).endVertex();
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
      c.vertex(m, x, y, z).color(r, g, b, a).uv(u, v).overlayCoords(overlay).uv2(light).normal(n, nx, ny, nz).endVertex();
   }

   public static void entity(
      VertexConsumer c, float x, float y, float z, float r, float g, float b, float a, float u, float v, int overlay, int light, float nx, float ny, float nz
   ) {
      c.vertex(x, y, z, r, g, b, a, u, v, overlay, light, nx, ny, nz);
   }
}
