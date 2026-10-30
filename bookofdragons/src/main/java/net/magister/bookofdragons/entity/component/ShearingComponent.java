package net.magister.bookofdragons.entity.component;

import net.magister.bookofdragons.entity.base.dragon.DragonBase;
import net.magister.liberanimalium.chassis.FaunaBase;
import net.magister.liberanimalium.chassis.component.IFaunaComponent;
import net.magister.liberanimalium.compat.Entities;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

public class ShearingComponent implements IFaunaComponent {
   protected DragonBase dragon;

   public void onAttach(FaunaBase host) {
      this.dragon = (DragonBase)host;
   }

   public void serverTick() {
      if (this.getShearingCooldownTicks() > 0) {
         this.setShearingCooldownTicks(this.getShearingCooldownTicks() - 1);
      }
   }

   public void save(CompoundTag tag) {
      tag.putInt("ShearingCooldown", this.getShearingCooldownTicks());
   }

   public void load(CompoundTag tag) {
      if (tag.contains("ShearingCooldown")) {
         this.setShearingCooldownTicks(tag.getInt("ShearingCooldown"));
      }
   }

   public void shearForScales(Player player, InteractionHand hand) {
      ItemStack shears = player.getItemInHand(hand);
      ItemStack scalesItem = this.getScaleItem();
      if (!scalesItem.isEmpty()) {
         int scaleCount = 1 + this.dragon.getRandom().nextInt(2);
         ItemStack scaleDrop = scalesItem.copy();
         scaleDrop.setCount(scaleCount);
         if (this.dragon.level() instanceof ServerLevel serverLevel) {
            this.dragon.spawnAtLocation(scaleDrop);
            int durabilityDamage = 28 + this.dragon.getRandom().nextInt(9);
            Entities.damageHeld(shears, durabilityDamage, player, hand);
            this.dragon.level().playSound(null, this.dragon.blockPosition(), SoundEvents.SHEEP_SHEAR, this.dragon.getSoundSource(), 1.0F, 2.0F);
            serverLevel.sendParticles(ParticleTypes.CRIT, this.dragon.getX(), this.dragon.getY() + 1.0, this.dragon.getZ(), 5, 0.5, 0.5, 0.5, 0.1);
            this.dragon.addBond(player.getUUID(), 3 + this.dragon.getRandom().nextInt(5));
            this.dragon.level().broadcastEntityEvent(this.dragon, (byte)7);
            int cooldownTicks = 6000 + this.dragon.getRandom().nextInt(6001);
            this.setShearingCooldownTicks(cooldownTicks);
         }
      }
   }

   public ItemStack getScaleItem() {
      return this.dragon.getScaleItem();
   }

   public int getShearingCooldownTicks() {
      return (Integer)this.dragon.getEntityData().get(DragonBase.getShearingCooldownTicksData());
   }

   public void setShearingCooldownTicks(int ticks) {
      this.dragon.getEntityData().set(DragonBase.getShearingCooldownTicksData(), ticks);
   }
}
