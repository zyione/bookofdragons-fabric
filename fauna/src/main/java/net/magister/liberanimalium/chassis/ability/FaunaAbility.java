package net.magister.liberanimalium.chassis.ability;

import net.magister.liberanimalium.chassis.FaunaBase;
import net.minecraft.world.entity.player.Player;
import org.jetbrains.annotations.Nullable;

public interface FaunaAbility {
   String getId();

   int getCooldownTicks();

   float getExhaustionCost();

   String getActivationAnimation();

   int getAnimationLengthTicks();

   default boolean bypassesAffectionRequirement() {
      return false;
   }

   boolean canActivate(FaunaBase var1);

   void onActivate(FaunaBase var1, @Nullable Player var2);

   void onTick(FaunaBase var1);

   void onDeactivate(FaunaBase var1);

   boolean isActive();

   int getRemainingCooldown();

   void setRemainingCooldown(int var1);

   default void tickCooldown() {
      if (this.getRemainingCooldown() > 0) {
         this.setRemainingCooldown(this.getRemainingCooldown() - 1);
      }
   }

   default boolean isAIEnabled() {
      return true;
   }

   boolean shouldAIActivate(FaunaBase var1, AbilityMoment var2);

   default boolean shouldAIDeactivate(FaunaBase body, AbilityMoment moment) {
      return false;
   }
}
