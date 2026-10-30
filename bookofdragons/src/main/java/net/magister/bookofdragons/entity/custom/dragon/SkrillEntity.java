package net.magister.bookofdragons.entity.custom.dragon;

import net.magister.bookofdragons.entity.ability.LightningShieldAbility;
import net.magister.bookofdragons.entity.base.dragon.DragonBase;
import net.magister.bookofdragons.entity.data.DragonType;
import net.magister.bookofdragons.item.ModItems;
import net.magister.liberanimalium.body.stats.FaunaProfile;
import net.magister.liberanimalium.chassis.component.FaunaComponentRegistry;
import net.magister.liberanimalium.compat.SyncedDataSink;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier.Builder;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.Level;

public class SkrillEntity extends DragonBase {
   private static final EntityDataAccessor<Boolean> DATA_LIGHTNING_SHIELD_ACTIVE = SynchedEntityData.defineId(
      SkrillEntity.class, EntityDataSerializers.BOOLEAN
   );

   public SkrillEntity(EntityType<? extends TamableAnimal> pEntityType, Level pLevel) {
      super(pEntityType, pLevel);
   }

   @Override
   protected void defineData(SyncedDataSink data) {
      super.defineData(data);
      data.define(DATA_LIGHTNING_SHIELD_ACTIVE, false);
   }

   public static Builder setAttributes() {
      return DragonBase.createFaunaAttributes();
   }

   protected FaunaProfile buildProfile() {
      return dragonProfile().build();
   }

   protected void registerSpeciesComponents(FaunaComponentRegistry registry) {
      this.registerJawsAndBreath(registry);
      this.setSecondaryAbility(new LightningShieldAbility());
   }

   public boolean isLightningShieldActive() {
      return (Boolean)this.entityData.get(DATA_LIGHTNING_SHIELD_ACTIVE);
   }

   public void setLightningShieldActive(boolean active) {
      this.entityData.set(DATA_LIGHTNING_SHIELD_ACTIVE, active);
   }

   public boolean hurt(DamageSource source, float amount) {
      if (this.isLightningShieldActive() && this.getSecondaryAbility() instanceof LightningShieldAbility shield) {
         LivingEntity attacker = null;
         if (source.getEntity() instanceof LivingEntity living) {
            attacker = living;
         }

         amount = shield.onDamaged(this, source, attacker, amount);
      }

      return super.hurt(source, amount);
   }

   @Override
   public DragonType getDragonType() {
      return DragonType.SKRILL;
   }

   @Override
   public ItemStack getScaleItem() {
      return new ItemStack((ItemLike)ModItems.SKRILL_SCALES.get());
   }
}
