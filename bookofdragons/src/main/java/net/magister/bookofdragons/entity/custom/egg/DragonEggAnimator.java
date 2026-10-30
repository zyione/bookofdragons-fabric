package net.magister.bookofdragons.entity.custom.egg;

import mod.azure.azurelib.animation.AzAnimatorConfig;
import mod.azure.azurelib.animation.controller.AzAnimationController;
import mod.azure.azurelib.animation.controller.AzAnimationControllerContainer;
import mod.azure.azurelib.animation.impl.AzEntityAnimator;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;

public class DragonEggAnimator extends AzEntityAnimator<DragonEggEntity> {
   public DragonEggAnimator() {
      super(AzAnimatorConfig.defaultConfig());
   }

   public void registerControllers(AzAnimationControllerContainer<DragonEggEntity> controllers) {
      controllers.add(AzAnimationController.builder(this, "egg_controller").build(), new AzAnimationController[0]);
   }

   @NotNull
   public ResourceLocation getAnimationLocation(DragonEggEntity egg) {
      return egg.getDragonType().getEggAnimations();
   }
}
