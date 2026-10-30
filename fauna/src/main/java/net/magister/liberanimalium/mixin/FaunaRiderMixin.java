package net.magister.liberanimalium.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.PoseStack.Pose;
import com.mojang.math.Axis;
import net.magister.liberanimalium.chassis.FaunaBase;
import net.magister.liberanimalium.compat.Verts;
import net.magister.liberanimalium.render.RiderMatrixDataflow;
import net.magister.liberanimalium.ride.render.RiderAnimator;
import net.magister.liberanimalium.ride.render.RiderPoseProfile;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(PlayerRenderer.class)
public abstract class FaunaRiderMixin {
   @Unique
   private static final boolean liberanimalium$VISUAL_DEBUG = false;

   @Inject(
      method = "render(Lnet/minecraft/client/player/AbstractClientPlayer;FFLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;I)V",
      at = @At("HEAD"),
      cancellable = true
   )
   private void liberanimalium$transformRiderOnMount(
      AbstractClientPlayer player, float entityYaw, float partialTick, PoseStack poseStack, MultiBufferSource buffer, int packedLight, CallbackInfo ci
   ) {
      if (player.getVehicle() instanceof FaunaBase body) {
         long var26 = RiderMatrixDataflow.getLastRenderTime(body.getId());
         if (System.currentTimeMillis() - var26 > 200L) {
            liberanimalium$logDebug("[RiderMixin] BODY CULLED (age " + (System.currentTimeMillis() - var26) + "ms) - Cancelling rider render");
            ci.cancel();
         } else {
            Matrix4f seatFromBody = RiderMatrixDataflow.get(body.getId());
            if (seatFromBody == null) {
               liberanimalium$logDebug("[RiderMixin] NO MATRIX for body " + body.getId());
            } else {
               long lastUpdate = RiderMatrixDataflow.getTimestamp(body.getId());
               long age = System.currentTimeMillis() - lastUpdate;
               if (age > 200L) {
                  liberanimalium$logDebug("[RiderMixin] STALE MATRIX (age " + age + "ms) for body " + body.getId() + " - Falling back to vanilla");
               } else {
                  RiderPoseProfile.Seat seat = RiderAnimator.seat(body);
                  float seatOffsetX = seat.x();
                  float seatOffsetY = seat.y();
                  float seatOffsetZ = seat.z();
                  Vec3 renderOffset = ((PlayerRenderer)(Object)this).getRenderOffset(player, partialTick);
                  float toBodyX = (float)(
                     Mth.lerp(partialTick, body.xOld, body.getX())
                        - Mth.lerp(partialTick, player.xOld, player.getX())
                        - renderOffset.x
                  );
                  float toBodyY = (float)(
                     Mth.lerp(partialTick, body.yOld, body.getY())
                        - Mth.lerp(partialTick, player.yOld, player.getY())
                        - renderOffset.y
                  );
                  float toBodyZ = (float)(
                     Mth.lerp(partialTick, body.zOld, body.getZ())
                        - Mth.lerp(partialTick, player.zOld, player.getZ())
                        - renderOffset.z
                  );
                  Matrix4f playerMatrix = new Matrix4f(poseStack.last().pose()).translate(toBodyX, toBodyY, toBodyZ).mul(seatFromBody);
                  playerMatrix.normalize3x3();
                  if (Float.isFinite(playerMatrix.m00()) && Float.isFinite(playerMatrix.m11()) && Float.isFinite(playerMatrix.m22())) {
                     poseStack.last().pose().set(playerMatrix);
                     poseStack.translate(seatOffsetX, seatOffsetY, seatOffsetZ);
                     float bodyYaw = RiderAnimator.vanillaBodyYaw(body, player, partialTick);
                     poseStack.mulPose(Axis.YP.rotationDegrees(bodyYaw - 180.0F));
                     liberanimalium$logDebug(
                        String.format("[RiderMixin] Attached | Offset=(%.2f, %.2f, %.2f) | BodyYaw=%.1f", seatOffsetX, seatOffsetY, seatOffsetZ, bodyYaw)
                     );
                  } else {
                     liberanimalium$logDebug("[RiderMixin] INVALID MATRIX (NaN/Inf) for body " + body.getId() + " - Falling back to vanilla");
                  }
               }
            }
         }
      }
   }

   @Unique
   private static void liberanimalium$logDebug(String message) {
   }

   @Unique
   private static void liberanimalium$renderDebugMarkers(PoseStack poseStack, MultiBufferSource buffer, float pivotOffset) {
      VertexConsumer lineConsumer = buffer.getBuffer(RenderType.debugLineStrip(2.0));
      float size = 0.1F;
      poseStack.pushPose();
      poseStack.translate(0.0F, -pivotOffset, 0.0F);
      liberanimalium$drawDebugCube(poseStack, lineConsumer, size, 1.0F, 0.0F, 0.0F);
      poseStack.popPose();
      liberanimalium$drawDebugCube(poseStack, lineConsumer, size, 1.0F, 1.0F, 0.0F);
   }

   @Unique
   private static void liberanimalium$drawDebugCube(PoseStack poseStack, VertexConsumer consumer, float size, float r, float g, float b) {
      Pose pose = poseStack.last();
      float h = size / 2.0F;
      liberanimalium$line(pose, consumer, -h, -h, -h, h, -h, -h, r, g, b);
      liberanimalium$line(pose, consumer, h, -h, -h, h, -h, h, r, g, b);
      liberanimalium$line(pose, consumer, h, -h, h, -h, -h, h, r, g, b);
      liberanimalium$line(pose, consumer, -h, -h, h, -h, -h, -h, r, g, b);
      liberanimalium$line(pose, consumer, -h, h, -h, h, h, -h, r, g, b);
      liberanimalium$line(pose, consumer, h, h, -h, h, h, h, r, g, b);
      liberanimalium$line(pose, consumer, h, h, h, -h, h, h, r, g, b);
      liberanimalium$line(pose, consumer, -h, h, h, -h, h, -h, r, g, b);
      liberanimalium$line(pose, consumer, -h, -h, -h, -h, h, -h, r, g, b);
      liberanimalium$line(pose, consumer, h, -h, -h, h, h, -h, r, g, b);
      liberanimalium$line(pose, consumer, h, -h, h, h, h, h, r, g, b);
      liberanimalium$line(pose, consumer, -h, -h, h, -h, h, h, r, g, b);
   }

   @Unique
   private static void liberanimalium$line(
      Pose pose, VertexConsumer consumer, float x1, float y1, float z1, float x2, float y2, float z2, float r, float g, float b
   ) {
      Verts.posColorNormal(consumer, pose, x1, y1, z1, r, g, b, 1.0F, 0.0F, 1.0F, 0.0F);
      Verts.posColorNormal(consumer, pose, x2, y2, z2, r, g, b, 1.0F, 0.0F, 1.0F, 0.0F);
   }
}
