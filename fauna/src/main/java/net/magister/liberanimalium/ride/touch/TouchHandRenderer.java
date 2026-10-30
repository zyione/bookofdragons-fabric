package net.magister.liberanimalium.ride.touch;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import java.util.Objects;
import net.magister.liberanimalium.body.touch.TouchState;
import net.magister.liberanimalium.compat.Skins;
import net.magister.liberanimalium.interop.ForeignArms;
import net.magister.liberanimalium.platform.event.ClientEvents;
import net.magister.liberanimalium.platform.event.CommonEvents;
import net.magister.liberanimalium.platform.event.HandRender;
import net.magister.liberanimalium.ride.render.ArmPose;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.LevelAccessor;

public final class TouchHandRenderer {
   public static final ArmPose POSE = new ArmPose(0.0F, 0.0F, 0.5F, 0.0F, -10.0F, -20.0F);
   private static final int STROKE_TICKS = 10;

   private TouchHandRenderer() {
   }

   public static void clientInit() {
      Objects.requireNonNull(TouchGestures.REACHING_POSE);
      ClientEvents.RENDER_HAND.register(TouchHandRenderer::onRenderHand);
      CommonEvents.LEVEL_UNLOAD.register(TouchHandRenderer::onLevelUnload);
   }

   public static boolean onRenderHand(HandRender event) {
      Minecraft mc = Minecraft.getInstance();
      AbstractClientPlayer player = mc.player;
      if (player == null) {
         return false;
      }

      float weight = TouchEase.step(player, event.getPartialTick());
      if (weight <= 0.0F) {
         return false;
      }

      TouchState.Gesture gesture = TouchEase.kind(player.getUUID());
      if (event.getHand() != InteractionHand.MAIN_HAND) {
         return false;
      }

      ItemStack held = player.getItemInHand(event.getHand());
      if (gesture == TouchState.Gesture.REACH && !held.isEmpty()) {
         return false;
      }

      if (gesture == TouchState.Gesture.BRUSH && held.isEmpty()) {
         return false;
      }

      boolean right = player.getMainArm() == HumanoidArm.RIGHT;
      HumanoidArm arm = right ? HumanoidArm.RIGHT : HumanoidArm.LEFT;
      PoseStack pose = event.getPoseStack();
      pose.pushPose();
      if (gesture == TouchState.Gesture.BRUSH) {
         brush(mc, player, held, arm, pose, event, weight);
      } else {
         reach(mc, player, arm, pose, event, weight);
      }

      pose.popPose();
      ForeignArms.want();
      return true;
   }

   private static void reach(Minecraft mc, AbstractClientPlayer player, HumanoidArm arm, PoseStack pose, HandRender event, float weight) {
      if (mc.getEntityRenderDispatcher().getRenderer(player) instanceof PlayerRenderer renderer) {
         float side = arm == HumanoidArm.RIGHT ? 1.0F : -1.0F;
         pose.translate(side * (0.64F + POSE.x * weight), -0.6F + POSE.y * weight, -0.72F - POSE.z * weight);
         pose.mulPose(Axis.YP.rotationDegrees(side * 45.0F));
         RenderSystem.setShaderTexture(0, Skins.texture(player));
         pose.translate(side * -1.0F, 3.6F, 3.5F);
         pose.mulPose(Axis.ZP.rotationDegrees(side * 120.0F));
         pose.mulPose(Axis.XP.rotationDegrees(200.0F));
         pose.mulPose(Axis.YP.rotationDegrees(side * -135.0F));
         pose.translate(side * 5.6F, 0.0F, 0.0F);
         pose.mulPose(Axis.YP.rotationDegrees(side * POSE.yaw * weight));
         pose.mulPose(Axis.XP.rotationDegrees(POSE.pitch * weight));
         pose.mulPose(Axis.ZP.rotationDegrees(side * POSE.roll * weight));
         if (arm == HumanoidArm.RIGHT) {
            renderer.renderRightHand(pose, event.getMultiBufferSource(), event.getPackedLight(), player);
         } else {
            renderer.renderLeftHand(pose, event.getMultiBufferSource(), event.getPackedLight(), player);
         }
      }
   }

   private static void brush(Minecraft mc, AbstractClientPlayer player, ItemStack held, HumanoidArm arm, PoseStack pose, HandRender event, float weight) {
      int side = arm == HumanoidArm.RIGHT ? 1 : -1;
      pose.translate(side * 0.56F, -0.52F, -0.72F);
      float f = (float)(9L - player.level().getGameTime() % 10L);
      float f1 = f - event.getPartialTick() + 1.0F;
      float f2 = 1.0F - f1 / 10.0F;
      float f7 = -15.0F + 75.0F * Mth.cos(f2 * 2.0F * (float) Math.PI);
      float w = weight;
      if (arm != HumanoidArm.RIGHT) {
         pose.translate(0.1 * w, 0.83 * w, 0.35 * w);
         pose.mulPose(Axis.XP.rotationDegrees(-80.0F * w));
         pose.mulPose(Axis.YP.rotationDegrees(-90.0F * w));
         pose.mulPose(Axis.XP.rotationDegrees(f7 * w));
         pose.translate(-0.3 * w, 0.22 * w, 0.35 * w);
      } else {
         pose.translate(-0.25 * w, 0.22 * w, 0.35 * w);
         pose.mulPose(Axis.XP.rotationDegrees(-80.0F * w));
         pose.mulPose(Axis.YP.rotationDegrees(90.0F * w));
         pose.mulPose(Axis.XP.rotationDegrees(f7 * w));
      }

      mc.getEntityRenderDispatcher()
         .getItemInHandRenderer()
         .renderItem(
            player,
            held,
            arm == HumanoidArm.RIGHT ? ItemDisplayContext.FIRST_PERSON_RIGHT_HAND : ItemDisplayContext.FIRST_PERSON_LEFT_HAND,
            arm != HumanoidArm.RIGHT,
            pose,
            event.getMultiBufferSource(),
            event.getPackedLight()
         );
   }

   public static void onLevelUnload(LevelAccessor level) {
      if (level.isClientSide()) {
         TouchGestures.clear();
      }
   }
}
