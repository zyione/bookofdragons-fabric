package net.magister.liberanimalium.platform.client;

import com.mojang.blaze3d.vertex.PoseStack;
import org.jetbrains.annotations.Nullable;
import net.minecraft.client.model.HumanoidModel.ArmPose;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;

public interface HandPose {
   @Nullable
   default ArmPose getArmPose(LivingEntity entityLiving, InteractionHand hand, ItemStack itemStack) {
      return null;
   }

   default boolean applyHandTransform(
      PoseStack poseStack, LocalPlayer player, HumanoidArm arm, ItemStack itemInHand, float partialTick, float equipProcess, float swingProcess
   ) {
      return false;
   }
}
