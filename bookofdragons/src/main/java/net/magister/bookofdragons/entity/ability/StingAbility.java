package net.magister.bookofdragons.entity.ability;

import net.magister.liberanimalium.chassis.FaunaBase;
import net.magister.liberanimalium.chassis.ability.AbilityMoment;
import net.magister.liberanimalium.chassis.ability.AbstractFaunaAbility;
import net.magister.liberanimalium.chassis.component.melee.CombatReach;
import net.magister.liberanimalium.chassis.component.melee.IMeleeAttackHandler;
import net.magister.liberanimalium.chassis.component.melee.OmniMeleeAttackHandler;
import net.magister.liberanimalium.chassis.component.melee.data.MeleeAttackDefinition;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

public class StingAbility extends AbstractFaunaAbility {
   public static final ResourceLocation STING = new ResourceLocation("bookofdragons", "speed_stinger_sting");
   private static final int STING_WINDUP = 20;
   private static final int FALLBACK_COOLDOWN = 200;
   @Nullable
   private LivingEntity electedAt;
   @Nullable
   private FaunaBase body;

   public String getId() {
      return "sting";
   }

   public int getCooldownTicks() {
      MeleeAttackDefinition sting = definition(this.body);
      return sting == null ? 200 : sting.cooldownTicks();
   }

   public float getExhaustionCost() {
      return 1.0F;
   }

   public String getActivationAnimation() {
      return "";
   }

   public int getAnimationLengthTicks() {
      MeleeAttackDefinition sting = definition(this.body);
      return sting == null ? 0 : sting.windupTicks() + sting.activeTicks() + sting.recoveryTicks();
   }

   public boolean canActivate(FaunaBase dragon) {
      OmniMeleeAttackHandler jaws = jaws(dragon);
      return super.canActivate(dragon) && jaws != null && !jaws.isAttacking();
   }

   public void onActivate(FaunaBase dragon, @Nullable Player activator) {
      this.body = dragon;
      OmniMeleeAttackHandler jaws = jaws(dragon);
      LivingEntity at = activator == null ? this.electedAt : null;
      this.electedAt = null;
      if (jaws != null && jaws.attackWith(STING, at)) {
         super.onActivate(dragon, activator);
         this.active = true;
      }
   }

   public void onTick(FaunaBase dragon) {
      OmniMeleeAttackHandler jaws = jaws(dragon);
      if (this.active && (jaws == null || !jaws.isAttacking())) {
         this.onDeactivate(dragon);
      }
   }

   public boolean shouldAIActivate(FaunaBase dragon, AbilityMoment moment) {
      LivingEntity target = moment.target();
      if (target == null || !target.isAlive() || moment.spent() || moment.fleeing()) {
         return false;
      }

      if (!moment.committedFor(20)) {
         return false;
      }

      MeleeAttackDefinition sting = definition(dragon);
      if (sting == null) {
         return false;
      }

      Vec3 origin = CombatReach.biteOrigin(dragon, target.position());
      double reach = sting.reach() * 0.8;
      if (origin.distanceToSqr(CombatReach.closestPoint(target, origin)) > reach * reach) {
         return false;
      }

      this.electedAt = target;
      return true;
   }

   @Nullable
   private static OmniMeleeAttackHandler jaws(@Nullable FaunaBase dragon) {
      if (dragon == null) {
         return null;
      } else {
         return dragon.componentRegistry.get(IMeleeAttackHandler.class) instanceof OmniMeleeAttackHandler handler ? handler : null;
      }
   }

   @Nullable
   private static MeleeAttackDefinition definition(@Nullable FaunaBase dragon) {
      OmniMeleeAttackHandler jaws = jaws(dragon);
      return jaws == null ? null : jaws.definitionOf(STING);
   }
}
