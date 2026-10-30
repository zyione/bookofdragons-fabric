package net.magister.liberanimalium.attack.impact.builtin;

import net.magister.liberanimalium.attack.data.ImpactConfig;
import net.magister.liberanimalium.attack.data.impact.ExplosionImpact;
import net.magister.liberanimalium.attack.entity.AttackProjectileEntity;
import net.magister.liberanimalium.attack.impact.ImpactEffect;
import net.magister.liberanimalium.config.WorldRules;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level.ExplosionInteraction;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

public class ExplosionEffect implements ImpactEffect {
   public static float computePower(float chargeScale, ExplosionImpact ex) {
      return Mth.lerp(chargeScale, ex.minPower(), ex.maxPower());
   }

   public static float computePower(float chargeScale, float speedRatio, ExplosionImpact ex) {
      return ex.power(chargeScale, speedRatio);
   }

   @Override
   public boolean apply(AttackProjectileEntity projectile, HitResult hit, ImpactConfig config) {
      ExplosionImpact ex = (ExplosionImpact)config;
      float power = computePower(projectile.getChargeScale(), projectile.speedRatio(), ex);
      if (power > 0.0F && !projectile.level().isClientSide()) {
         Entity shooter = projectile.getOwner();
         boolean breaks = ex.breaksBlocks() && WorldRules.mayBreakBlocks(projectile.level());
         boolean fire = ex.setsFire() && WorldRules.maySetFire(projectile.level());
         ExplosionInteraction interaction = breaks ? ExplosionInteraction.TNT : ExplosionInteraction.NONE;
         Vec3 pos = hit != null ? hit.getLocation() : projectile.position();
         projectile.level()
            .explode(
               projectile, projectile.damageSources().explosion(projectile, shooter), null, pos.x, pos.y, pos.z, power, fire, interaction
            );
      }

      projectile.discard();
      return true;
   }
}
