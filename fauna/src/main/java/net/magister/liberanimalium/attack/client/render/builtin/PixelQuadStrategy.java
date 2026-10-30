package net.magister.liberanimalium.attack.client.render.builtin;

import com.mojang.blaze3d.vertex.VertexConsumer;
import net.magister.liberanimalium.attack.client.render.AttackRenderTypes;
import net.magister.liberanimalium.attack.client.render.BlendMode;
import net.magister.liberanimalium.attack.client.render.QuadSink;
import net.magister.liberanimalium.attack.client.render.RenderInput;
import net.magister.liberanimalium.attack.client.render.RenderStrategy;
import net.magister.liberanimalium.compat.Verts;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.joml.Vector3f;

public class PixelQuadStrategy implements RenderStrategy {
   private static final ResourceLocation TEXTURE = new ResourceLocation("liberanimalium", "textures/particle/white_pixel.png");
   private static final RenderType ADDITIVE = AttackRenderTypes.additiveEmissive(TEXTURE);
   private static final RenderType ALPHA = RenderType.entityTranslucentEmissive(TEXTURE);
   public static final PixelQuadStrategy INSTANCE = new PixelQuadStrategy();

   @Override
   public void emit(QuadSink sink, RenderInput in) {
      sink.poseStack().pushPose();
      sink.poseStack().mulPose(in.rotation());
      Matrix4f matrix = sink.poseStack().last().pose();
      Matrix3f normalMatrix = sink.poseStack().last().normal();
      VertexConsumer buffer = sink.getBuffer(in.blend() == BlendMode.ADDITIVE ? ADDITIVE : ALPHA);
      float h = in.size() / 2.0F;
      Vector3f[] v = new Vector3f[]{new Vector3f(-h, -h, 0.0F), new Vector3f(-h, h, 0.0F), new Vector3f(h, h, 0.0F), new Vector3f(h, -h, 0.0F)};
      float r = in.r();
      float g = in.g();
      float b = in.b();
      float a = in.alpha();
      int uLight = in.uLight();
      int vLight = in.vLight();
      vertex(buffer, matrix, normalMatrix, v[0], 0.0F, 1.0F, r, g, b, a, uLight, vLight);
      vertex(buffer, matrix, normalMatrix, v[1], 0.0F, 0.0F, r, g, b, a, uLight, vLight);
      vertex(buffer, matrix, normalMatrix, v[2], 1.0F, 0.0F, r, g, b, a, uLight, vLight);
      vertex(buffer, matrix, normalMatrix, v[3], 1.0F, 1.0F, r, g, b, a, uLight, vLight);
      sink.poseStack().popPose();
   }

   private static void vertex(
      VertexConsumer buffer, Matrix4f matrix, Matrix3f normalMatrix, Vector3f pos, float u, float v, float r, float g, float b, float a, int uLight, int vLight
   ) {
      Verts.entity(buffer, matrix, normalMatrix, pos.x(), pos.y(), pos.z(), r, g, b, a, u, v, 0, 10, uLight, vLight, 0.0F, 0.0F, 1.0F);
   }
}
