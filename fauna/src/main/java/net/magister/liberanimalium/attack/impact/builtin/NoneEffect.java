package net.magister.liberanimalium.attack.impact.builtin;

import net.magister.liberanimalium.attack.data.ImpactConfig;
import net.magister.liberanimalium.attack.entity.AttackProjectileEntity;
import net.magister.liberanimalium.attack.impact.ImpactEffect;
import net.minecraft.world.phys.HitResult;

public class NoneEffect implements ImpactEffect {
   @Override
   public boolean apply(AttackProjectileEntity projectile, HitResult hit, ImpactConfig config) {
      projectile.discard();
      return true;
   }
}
