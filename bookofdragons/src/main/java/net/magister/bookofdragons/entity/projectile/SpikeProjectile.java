package net.magister.bookofdragons.entity.projectile;

import net.magister.bookofdragons.entity.ModEntities;
import net.magister.liberanimalium.platform.part.Parts;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.entity.projectile.AbstractArrow.Pickup;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

public class SpikeProjectile extends AbstractArrow {
   public SpikeProjectile(EntityType<? extends AbstractArrow> pEntityType, Level pLevel) {
      super(pEntityType, pLevel);
   }

   public SpikeProjectile(Level pLevel, LivingEntity pShooter) {
      super((EntityType)ModEntities.SPIKE_PROJECTILE.get(), pLevel);
      this.setPos(pShooter.getX(), pShooter.getEyeY() - 0.1F, pShooter.getZ());
      this.setOwner(pShooter);
      if (pShooter instanceof Player) {
         this.pickup = Pickup.ALLOWED;
      }
   }

   public void tick() {
      super.tick();
      if (this.level().isClientSide() && !this.inGround) {
         Vec3 v = this.getDeltaMovement();
         this.level()
            .addParticle(
               ParticleTypes.CRIT,
               this.getX() - v.x * 0.5,
               this.getY() - v.y * 0.5,
               this.getZ() - v.z * 0.5,
               -v.x * 0.15,
               -v.y * 0.15,
               -v.z * 0.15
            );
      }
   }

   protected ItemStack getPickupItem() {
      return ItemStack.EMPTY;
   }

   protected boolean canHitEntity(Entity target) {
      Entity owner = this.getOwner();
      return owner != null && bodyOf(target) == bodyOf(owner) ? false : super.canHitEntity(target);
   }

   private static Entity bodyOf(Entity entity) {
      Entity whole = Parts.isPart(entity) && Parts.parentOf(entity) != null ? Parts.parentOf(entity) : entity;
      return whole.getRootVehicle();
   }
}
