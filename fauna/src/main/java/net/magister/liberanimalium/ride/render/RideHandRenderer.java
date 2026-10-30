package net.magister.liberanimalium.ride.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.magister.liberanimalium.anim.rig.DampedSpring;
import net.magister.liberanimalium.chassis.FaunaBase;
import net.magister.liberanimalium.interop.ForeignArms;
import net.magister.liberanimalium.platform.event.ClientEvents;
import net.magister.liberanimalium.platform.event.HandRender;
import net.magister.liberanimalium.ride.touch.TouchEase;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.phys.Vec3;

public final class RideHandRenderer {
   public static final ArmPose GRIP_POSE = new ArmPose(0.15F, 0.0F, 0.0F, 30.0F, -20.0F, 0.0F);
   private static final float SHOULDER_X = 0.504F;
   private static final float SHOULDER_Y = -0.819F;
   private static final float SHOULDER_Z = -0.663F;
   private static final DampedSpring GRIP = new DampedSpring();
   private static final DampedSpring SURGE = new DampedSpring();
   private static final DampedSpring SWAY = new DampedSpring();
   private static final DampedSpring HEAVE = new DampedSpring();
   private static final RiderClock CLOCK = new RiderClock();
   private static Vec3 lastVelocity = Vec3.ZERO;
   private static int lastTick = Integer.MIN_VALUE;
   private static double forward;
   private static double right;
   private static double up;

   private RideHandRenderer() {
   }

   public static void clientInit() {
      ClientEvents.RENDER_HAND.register(RideHandRenderer::onRenderHand);
   }

   public static boolean onRenderHand(HandRender event) {
      Minecraft mc = Minecraft.getInstance();
      AbstractClientPlayer player = mc.player;
      if (player == null) {
         return false;
      }

      float partial = event.getPartialTick();
      float grip = step(player, partial);
      boolean ours = grip > 0.0F && !player.isInvisible();
      if (ForeignArms.present()) {
         ours = ours && player.getMainHandItem().isEmpty() && player.getOffhandItem().isEmpty() && player.getAttackAnim(partial) <= 0.0F;
         if (ours) {
            ForeignArms.want();
         }
      }

      if (!ours) {
         return false;
      } else {
         boolean main = event.getHand() == InteractionHand.MAIN_HAND;
         if (!player.getItemInHand(event.getHand()).isEmpty()) {
            return false;
         } else if (main && player.getAttackAnim(partial) > 0.0F) {
            return false;
         } else if (mc.getEntityRenderDispatcher().getRenderer(player) instanceof PlayerRenderer renderer) {
            HumanoidArm arm = main ? player.getMainArm() : player.getMainArm().getOpposite();
            PoseStack pose = event.getPoseStack();
            pose.pushPose();
            boolean drawn = draw(renderer, player, arm, pose, event, grip);
            pose.popPose();
            return drawn;
         } else {
            return false;
         }
      }
   }

   private static float step(AbstractClientPlayer player, float partial) {
      Entity vehicle = player.getVehicle();
      boolean riding = vehicle instanceof FaunaBase;
      CLOCK.frame(player.tickCount + partial, System.nanoTime());
      double seconds = CLOCK.dtTicks() / 20.0;
      if (riding && vehicle.tickCount != lastTick) {
         Vec3 velocity = vehicle.getDeltaMovement();
         if (lastTick != Integer.MIN_VALUE) {
            Vec3 accel = velocity.subtract(lastVelocity);
            double yaw = Math.toRadians(player.getViewYRot(partial));
            forward = -accel.x * Math.sin(yaw) + accel.z * Math.cos(yaw);
            right = -accel.x * Math.cos(yaw) - accel.z * Math.sin(yaw);
            up = accel.y;
         }

         lastVelocity = velocity;
         lastTick = vehicle.tickCount;
      } else if (!riding) {
         lastTick = Integer.MIN_VALUE;
         up = 0.0;
         right = 0.0;
         forward = 0.0;
      }

      boolean touching = TouchEase.weight(player.getUUID()) > 0.0F;
      GRIP.step(riding && !touching ? 1.0 : 0.0, seconds, 2.0, 1.0, 0.0);
      SURGE.step(forward * 0.9, seconds, 2.2, 0.55, 0.12);
      SWAY.step(-right * 0.9, seconds, 2.2, 0.55, 0.12);
      HEAVE.step(-up * 0.9, seconds, 2.2, 0.55, 0.12);
      float weight = Mth.clamp((float)GRIP.value(), 0.0F, 1.0F);
      return weight < 0.01F ? 0.0F : weight;
   }

   private static boolean draw(PlayerRenderer renderer, AbstractClientPlayer player, HumanoidArm arm, PoseStack pose, HandRender event, float w) {
      float side = arm == HumanoidArm.RIGHT ? 1.0F : -1.0F;
      pose.translate((float)SWAY.value() * w, (float)HEAVE.value() * w - (1.0F - w) * 0.8F, (float)SURGE.value() * w);
      pose.translate(side * GRIP_POSE.x, GRIP_POSE.y, -GRIP_POSE.z);
      pose.translate(side * 0.504F, -0.819F, -0.663F);
      pose.mulPose(Axis.YP.rotationDegrees(side * GRIP_POSE.yaw));
      pose.mulPose(Axis.XP.rotationDegrees(GRIP_POSE.pitch));
      pose.mulPose(Axis.ZP.rotationDegrees(side * GRIP_POSE.roll));
      pose.translate(-side * 0.504F, 0.819F, 0.663F);
      pose.translate(side * 0.64F, -0.6F, -0.72F);
      pose.mulPose(Axis.YP.rotationDegrees(side * 45.0F));
      pose.translate(side * -1.0F, 3.6F, 3.5F);
      pose.mulPose(Axis.ZP.rotationDegrees(side * 120.0F));
      pose.mulPose(Axis.XP.rotationDegrees(200.0F));
      pose.mulPose(Axis.YP.rotationDegrees(side * -135.0F));
      pose.translate(side * 5.6F, 0.0F, 0.0F);
      if (arm == HumanoidArm.RIGHT) {
         renderer.renderRightHand(pose, event.getMultiBufferSource(), event.getPackedLight(), player);
      } else {
         renderer.renderLeftHand(pose, event.getMultiBufferSource(), event.getPackedLight(), player);
      }

      return true;
   }
}
