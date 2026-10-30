package net.magister.liberanimalium.platform.client;

import net.magister.liberanimalium.platform.Services;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.HumanoidModel.ArmPose;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.LivingEntity;

public final class ArmPoses {
   private ArmPoses() {
   }

   public static ArmPose custom(String name, boolean twoHanded, ArmPoses.Transform transform) {
      return Services.client().armPose(name, twoHanded, transform);
   }

   @FunctionalInterface
   public interface Transform {
      void apply(HumanoidModel<?> var1, LivingEntity var2, HumanoidArm var3);
   }
}
