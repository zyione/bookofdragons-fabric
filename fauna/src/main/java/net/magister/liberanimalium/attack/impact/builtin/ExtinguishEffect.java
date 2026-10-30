package net.magister.liberanimalium.attack.impact.builtin;

import net.magister.liberanimalium.attack.data.ImpactConfig;
import net.magister.liberanimalium.attack.entity.AttackProjectileEntity;
import net.magister.liberanimalium.attack.impact.ImpactEffect;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.HitResult.Type;

public class ExtinguishEffect implements ImpactEffect {
   @Override
   public boolean apply(AttackProjectileEntity projectile, HitResult hit, ImpactConfig config) {
      boolean inWater = projectile.isInWater();
      if (!inWater && hit != null && hit.getType() == Type.BLOCK) {
         BlockHitResult blockHit = (BlockHitResult)hit;
         inWater = projectile.level().getFluidState(blockHit.getBlockPos()).is(FluidTags.WATER);
      }

      if (!inWater) {
         return false;
      }

      if (projectile.level() instanceof ServerLevel serverLevel) {
         Vec3 pos = hit != null ? hit.getLocation() : projectile.position();
         serverLevel.sendParticles(ParticleTypes.CAMPFIRE_COSY_SMOKE, pos.x, pos.y + 0.1, pos.z, 18, 0.3, 0.1, 0.3, 0.04);
         serverLevel.playSound(
            null, pos.x, pos.y, pos.z, SoundEvents.FIRE_EXTINGUISH, SoundSource.BLOCKS, 1.0F, 0.9F + serverLevel.random.nextFloat() * 0.2F
         );
      }

      projectile.discard();
      return true;
   }
}
