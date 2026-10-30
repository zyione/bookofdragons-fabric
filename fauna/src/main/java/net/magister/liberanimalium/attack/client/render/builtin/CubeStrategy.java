package net.magister.liberanimalium.attack.client.render.builtin;

import com.mojang.blaze3d.vertex.VertexConsumer;
import net.magister.liberanimalium.attack.client.render.QuadSink;
import net.magister.liberanimalium.attack.client.render.RenderInput;
import net.magister.liberanimalium.attack.client.render.RenderStrategy;
import net.magister.liberanimalium.compat.Verts;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.joml.Vector3f;

public class CubeStrategy implements RenderStrategy {
   private static final ResourceLocation TEXTURE = new ResourceLocation("liberanimalium", "textures/particle/white_pixel.png");
   private static final RenderType RENDER_TYPE = RenderType.entityTranslucentEmissive(TEXTURE);
   public static final CubeStrategy INSTANCE = new CubeStrategy();

   @Override
   public void emit(QuadSink sink, RenderInput in) {
      sink.poseStack().pushPose();
      sink.poseStack().mulPose(in.rotation());
      Matrix4f matrix = sink.poseStack().last().pose();
      Matrix3f normalMatrix = sink.poseStack().last().normal();
      VertexConsumer buffer = sink.getBuffer(RENDER_TYPE);
      float h = in.size() / 2.0F;
      float hZ = h * in.elongation();
      Vector3f[] v = new Vector3f[]{
         new Vector3f(-h, -h, -hZ),
         new Vector3f(h, -h, -hZ),
         new Vector3f(h, h, -hZ),
         new Vector3f(-h, h, -hZ),
         new Vector3f(-h, -h, hZ),
         new Vector3f(h, -h, hZ),
         new Vector3f(h, h, hZ),
         new Vector3f(-h, h, hZ)
      };
      renderFace(buffer, matrix, normalMatrix, v[0], v[1], v[2], v[3], in, 1.0F);
      renderFace(buffer, matrix, normalMatrix, v[5], v[4], v[7], v[6], in, 0.9F);
      renderFace(buffer, matrix, normalMatrix, v[4], v[0], v[3], v[7], in, 0.8F);
      renderFace(buffer, matrix, normalMatrix, v[1], v[5], v[6], v[2], in, 0.7F);
      renderFace(buffer, matrix, normalMatrix, v[3], v[2], v[6], v[7], in, 0.6F);
      renderFace(buffer, matrix, normalMatrix, v[1], v[0], v[4], v[5], in, 0.5F);
      sink.poseStack().popPose();
   }

   private static void renderFace(
      VertexConsumer buffer, Matrix4f matrix, Matrix3f normalMatrix, Vector3f p1, Vector3f p2, Vector3f p3, Vector3f p4, RenderInput in, float shade
   ) {
      float r = in.r() * shade;
      float g = in.g() * shade;
      float b = in.b() * shade;
      float a = in.alpha();
      int uLight = in.uLight();
      int vLight = in.vLight();
      vertex(buffer, matrix, normalMatrix, p4, 0.0F, 1.0F, r, g, b, a, uLight, vLight);
      vertex(buffer, matrix, normalMatrix, p3, 0.0F, 0.0F, r, g, b, a, uLight, vLight);
      vertex(buffer, matrix, normalMatrix, p2, 1.0F, 0.0F, r, g, b, a, uLight, vLight);
      vertex(buffer, matrix, normalMatrix, p1, 1.0F, 1.0F, r, g, b, a, uLight, vLight);
   }

   private static void vertex(
      VertexConsumer buffer, Matrix4f matrix, Matrix3f normalMatrix, Vector3f pos, float u, float v, float r, float g, float b, float a, int uLight, int vLight
   ) {
      Verts.entity(buffer, matrix, normalMatrix, pos.x(), pos.y(), pos.z(), r, g, b, a, u, v, 0, 10, uLight, vLight, 0.0F, 1.0F, 0.0F);
   }

   public static float[] rgbToHsv(float r, float g, float b) {
      float max = Math.max(r, Math.max(g, b));
      float min = Math.min(r, Math.min(g, b));
      float v = max;
      float d = max - min;
      float s = max == 0.0F ? 0.0F : d / max;
      float h;
      if (max == min) {
         h = 0.0F;
      } else {
         if (max == r) {
            h = (g - b) / d + (g < b ? 6 : 0);
         } else if (max == g) {
            h = (b - r) / d + 2.0F;
         } else {
            h = (r - g) / d + 4.0F;
         }

         h /= 6.0F;
      }

      return new float[]{h, s, v};
   }

   public static float[] hsvToRgb(float h, float s, float v) {
      float r = 0.0F;
      float g = 0.0F;
      float b = 0.0F;
      int i = (int)(h * 6.0F);
      float f = h * 6.0F - i;
      float p = v * (1.0F - s);
      float q = v * (1.0F - f * s);
      float t = v * (1.0F - (1.0F - f) * s);
      switch (i % 6) {
         case 0:
            r = v;
            g = t;
            b = p;
            break;
         case 1:
            r = q;
            g = v;
            b = p;
            break;
         case 2:
            r = p;
            g = v;
            b = t;
            break;
         case 3:
            r = p;
            g = q;
            b = v;
            break;
         case 4:
            r = t;
            g = p;
            b = v;
            break;
         case 5:
            r = v;
            g = p;
            b = q;
      }

      return new float[]{r, g, b};
   }

   public static float lerpHue(float h1, float h2, float t) {
      float d = h2 - h1;
      if (d > 0.5F) {
         h1++;
      } else if (d < -0.5F) {
         h2++;
      }

      float h = Mth.lerp(t, h1, h2);
      if (h >= 1.0F) {
         h--;
      }

      if (h < 0.0F) {
         h++;
      }

      return h;
   }
}
