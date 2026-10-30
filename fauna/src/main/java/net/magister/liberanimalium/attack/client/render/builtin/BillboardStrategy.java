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

public class BillboardStrategy implements RenderStrategy {
   private final ResourceLocation texture;
   private final boolean selfLit;

   public BillboardStrategy(ResourceLocation texture) {
      this(texture, true);
   }

   public BillboardStrategy(ResourceLocation texture, boolean selfLit) {
      this.texture = texture;
      this.selfLit = selfLit;
   }

   @Override
   public void emit(QuadSink sink, RenderInput in) {
      RenderType renderType = in.blend() == BlendMode.ADDITIVE
         ? AttackRenderTypes.additiveEmissive(this.texture)
         : (this.selfLit ? RenderType.entityTranslucentEmissive(this.texture) : RenderType.entityTranslucent(this.texture));
      sink.poseStack().pushPose();
      sink.poseStack().mulPose(in.rotation());
      Matrix4f matrix = sink.poseStack().last().pose();
      Matrix3f normalMatrix = sink.poseStack().last().normal();
      VertexConsumer buffer = sink.getBuffer(renderType);
      float h = in.size() / 2.0F;
      Vector3f[] v = new Vector3f[]{new Vector3f(-h, -h, 0.0F), new Vector3f(-h, h, 0.0F), new Vector3f(h, h, 0.0F), new Vector3f(h, -h, 0.0F)};
      float r = in.r();
      float g = in.g();
      float b = in.b();
      float a = in.alpha();
      int uLight = in.uLight();
      int vLight = in.vLight();
      Verts.entity(buffer, matrix, normalMatrix, v[0].x(), v[0].y(), v[0].z(), r, g, b, a, 0.0F, 1.0F, 0, 10, uLight, vLight, 0.0F, 0.0F, 1.0F);
      Verts.entity(buffer, matrix, normalMatrix, v[1].x(), v[1].y(), v[1].z(), r, g, b, a, 0.0F, 0.0F, 0, 10, uLight, vLight, 0.0F, 0.0F, 1.0F);
      Verts.entity(buffer, matrix, normalMatrix, v[2].x(), v[2].y(), v[2].z(), r, g, b, a, 1.0F, 0.0F, 0, 10, uLight, vLight, 0.0F, 0.0F, 1.0F);
      Verts.entity(buffer, matrix, normalMatrix, v[3].x(), v[3].y(), v[3].z(), r, g, b, a, 1.0F, 1.0F, 0, 10, uLight, vLight, 0.0F, 0.0F, 1.0F);
      sink.poseStack().popPose();
   }
}
