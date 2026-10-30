package net.magister.liberanimalium.chassis.ability;

import net.magister.liberanimalium.body.data.CrippledFeature;
import net.magister.liberanimalium.chassis.FaunaBase;
import net.minecraft.world.entity.player.Player;
import org.jetbrains.annotations.Nullable;

public abstract class AbstractFaunaAbility implements FaunaAbility {
   protected boolean active = false;
   protected int remainingCooldown = 0;

   @Override
   public boolean isActive() {
      return this.active;
   }

   @Override
   public int getRemainingCooldown() {
      return this.remainingCooldown;
   }

   @Override
   public void setRemainingCooldown(int ticks) {
      this.remainingCooldown = Math.max(0, ticks);
   }

   @Override
   public boolean canActivate(FaunaBase body) {
      if (body.getSyncedIsFeatureCrippled(CrippledFeature.SECONDARY_ABILITY)) {
         return false;
      } else if (!this.worksWet() && body.isInWaterRainOrBubble()) {
         return false;
      } else if (this.getRemainingCooldown() > 0) {
         return false;
      } else {
         return this.isActive() ? false : body.getNeedsSystem() == null || body.getNeedsSystem().canUseRangedAttack();
      }
   }

   protected boolean worksWet() {
      return true;
   }

   @Override
   public void onActivate(FaunaBase body, @Nullable Player activator) {
      if (body.getNeedsSystem() != null) {
         body.getNeedsSystem().addExhaustion(this.getExhaustionCost());
      }
   }

   @Override
   public void onTick(FaunaBase body) {
   }

   @Override
   public void onDeactivate(FaunaBase body) {
      this.active = false;
      this.setRemainingCooldown(this.getCooldownTicks());
   }

   @Override
   public int getAnimationLengthTicks() {
      return 0;
   }

   @Override
   public String getActivationAnimation() {
      return "";
   }
}
