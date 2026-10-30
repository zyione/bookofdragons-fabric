package net.magister.liberanimalium.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import net.magister.liberanimalium.body.grip.Grip;
import net.magister.liberanimalium.body.grip.GripGeometry;
import net.magister.liberanimalium.chassis.FaunaBase;
import net.magister.liberanimalium.chassis.component.GripComponent;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import org.joml.Quaternionf;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.At.Shift;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(EntityRenderDispatcher.class)
public abstract class HeldRenderMixin {
   @Unique
   private static final ThreadLocal<Quaternionf> liberanimalium$TURN = new ThreadLocal<>();

   @Inject(method = "render", at = @At("HEAD"), cancellable = true)
   private <E extends Entity> void liberanimalium$drawHeldInTheGrip(
      E entity,
      double x,
      double y,
      double z,
      float rotationYaw,
      float partialTicks,
      PoseStack poseStack,
      MultiBufferSource buffer,
      int packedLight,
      CallbackInfo ci
   ) {
      if (liberanimalium$TURN.get() == null) {
         FaunaBase holder = Grip.holderOf(entity);
         GripComponent grip = holder == null ? null : holder.grip();
         GripGeometry.Placement at = grip == null ? null : grip.drawn(partialTicks);
         if (at != null) {
            double camX = Mth.lerp(partialTicks, entity.xOld, entity.getX()) - x;
            double camY = Mth.lerp(partialTicks, entity.yOld, entity.getY()) - y;
            double camZ = Mth.lerp(partialTicks, entity.zOld, entity.getZ()) - z;
            float drawnYaw = entity instanceof LivingEntity living ? Mth.rotLerp(partialTicks, living.yBodyRotO, living.yBodyRot) : rotationYaw;
            liberanimalium$TURN.set(at.around(drawnYaw));

            try {
               ((EntityRenderDispatcher)(Object)this)
                  .render(
                     entity,
                     at.feet().x - camX,
                     at.feet().y - camY,
                     at.feet().z - camZ,
                     rotationYaw,
                     partialTicks,
                     poseStack,
                     buffer,
                     packedLight
                  );
            } finally {
               liberanimalium$TURN.remove();
            }

            ci.cancel();
         }
      }
   }

   @Inject(
      method = "render",
      at = @At(
         value = "INVOKE",
         target = "Lnet/minecraft/client/renderer/entity/EntityRenderer;render(Lnet/minecraft/world/entity/Entity;FFLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;I)V"
      )
   )
   private <E extends Entity> void liberanimalium$turnHeld(
      E entity,
      double x,
      double y,
      double z,
      float rotationYaw,
      float partialTicks,
      PoseStack poseStack,
      MultiBufferSource buffer,
      int packedLight,
      CallbackInfo ci
   ) {
      Quaternionf turn = liberanimalium$TURN.get();
      if (turn != null) {
         poseStack.pushPose();
         poseStack.mulPose(turn);
      }
   }

   @Inject(
      method = "render",
      at = @At(
         value = "INVOKE",
         target = "Lnet/minecraft/client/renderer/entity/EntityRenderer;render(Lnet/minecraft/world/entity/Entity;FFLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;I)V",
         shift = Shift.AFTER
      )
   )
   private <E extends Entity> void liberanimalium$unturnHeld(
      E entity,
      double x,
      double y,
      double z,
      float rotationYaw,
      float partialTicks,
      PoseStack poseStack,
      MultiBufferSource buffer,
      int packedLight,
      CallbackInfo ci
   ) {
      if (liberanimalium$TURN.get() != null) {
         poseStack.popPose();
      }
   }
}
