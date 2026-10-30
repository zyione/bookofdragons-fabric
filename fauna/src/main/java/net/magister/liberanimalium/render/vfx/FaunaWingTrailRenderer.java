package net.magister.liberanimalium.render.vfx;

import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.PoseStack.Pose;
import net.magister.liberanimalium.compat.Verts;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix3f;
import org.joml.Vector3f;

public class FaunaWingTrailRenderer {
   public static void render(FaunaWingTrail trail, MultiBufferSource vertexConsumerProvider, Pose matrices) {
      int count = trail.getPointCount();
      if (count >= 2) {
         FaunaWingTrailTexture.getInstance().tick();
         ResourceLocation dynamicLoc = FaunaWingTrailTexture.getInstance().getResourceLocation();
         VertexConsumer lineVertexConsumer = vertexConsumerProvider.getBuffer(RenderType.beaconBeam(dynamicLoc, true));
         int light = 15728640;
         Vec3 pos = Minecraft.getInstance().gameRenderer.getMainCamera().getPosition();
         Matrix3f matrix = matrices.normal();
         int head = trail.getHeadIndex();
         int capacity = trail.getCapacity();
         float totalLength = count - 1;
         Vector3f[] rightVectors = new Vector3f[count];
         Vector3f[] points = new Vector3f[count];

         for (int i = 0; i < count; i++) {
            int index = (head + i) % capacity;
            int offset = index * 4;
            points[i] = new Vector3f(
               (float)(trail.buffer[offset] - pos.x), (float)(trail.buffer[offset + 1] - pos.y), (float)(trail.buffer[offset + 2] - pos.z)
            );
         }

         for (int i = 0; i < count; i++) {
            Vector3f p = points[i];
            Vector3f dir = new Vector3f();
            if (i > 0 && i < count - 1) {
               dir.add(new Vector3f(points[i]).sub(points[i - 1]).normalize());
               dir.add(new Vector3f(points[i + 1]).sub(points[i]).normalize());
               dir.normalize();
            } else if (i == 0) {
               dir.set(new Vector3f(points[1]).sub(points[0]).normalize());
            } else {
               dir.set(new Vector3f(points[i]).sub(points[i - 1]).normalize());
            }

            if (dir.lengthSquared() < 1.0E-5F) {
               rightVectors[i] = new Vector3f(1.0F, 0.0F, 0.0F);
            } else {
               Vector3f camToP = new Vector3f(p).normalize();
               rightVectors[i] = new Vector3f(dir).cross(camToP).normalize().mul(0.15F);
            }
         }

         for (int i = 0; i < count - 1; i++) {
            int index1 = (head + i) % capacity;
            int index2 = (head + i + 1) % capacity;
            int offset1 = index1 * 4;
            int offset2 = index2 * 4;
            float factor1 = (float)i / count;
            float factor2 = (float)(i + 1) / count;
            int a1 = (int)(factor1 * trail.buffer[offset1 + 3] * 255.0F);
            int a2 = (int)(factor2 * trail.buffer[offset2 + 3] * 255.0F);
            float u1 = i / totalLength;
            float u2 = (i + 1) / totalLength;
            Vector3f p1 = points[i];
            Vector3f p2 = points[i + 1];
            Vector3f right1 = rightVectors[i];
            Vector3f right2 = rightVectors[i + 1];
            Vector3f c1 = new Vector3f(p1).add(right1);
            Vector3f c2 = new Vector3f(p1).sub(right1);
            Vector3f c3 = new Vector3f(p2).sub(right2);
            Vector3f c4 = new Vector3f(p2).add(right2);
            matrix.transform(c1);
            matrix.transform(c2);
            matrix.transform(c3);
            matrix.transform(c4);
            vertexPos(lineVertexConsumer, c1, u1, 0.0F, a1, light);
            vertexPos(lineVertexConsumer, c2, u1, 1.0F, a1, light);
            vertexPos(lineVertexConsumer, c3, u2, 1.0F, a2, light);
            vertexPos(lineVertexConsumer, c4, u2, 0.0F, a2, light);
            vertexPos(lineVertexConsumer, c4, u2, 0.0F, a2, light);
            vertexPos(lineVertexConsumer, c3, u2, 1.0F, a2, light);
            vertexPos(lineVertexConsumer, c2, u1, 1.0F, a1, light);
            vertexPos(lineVertexConsumer, c1, u1, 0.0F, a1, light);
         }
      }
   }

   private static void vertexPos(VertexConsumer lineVertexConsumer, Vector3f p, float u, float v, int a, int light) {
      Verts.entity(lineVertexConsumer, p.x, p.y, p.z, 1.0F, 1.0F, 1.0F, a / 255.0F, u, v, OverlayTexture.NO_OVERLAY, light, 1.0F, 0.0F, 0.0F);
   }
}
