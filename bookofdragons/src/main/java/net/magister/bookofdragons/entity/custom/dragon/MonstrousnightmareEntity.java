package net.magister.bookofdragons.entity.custom.dragon;

import net.magister.bookofdragons.entity.ability.SelfImmolationAbility;
import net.magister.bookofdragons.entity.base.dragon.DragonBase;
import net.magister.bookofdragons.entity.data.DragonType;
import net.magister.bookofdragons.item.ModItems;
import net.magister.liberanimalium.body.locomotion.LocomotionMode;
import net.magister.liberanimalium.body.stats.FaunaProfile;
import net.magister.liberanimalium.chassis.component.FaunaComponentRegistry;
import net.magister.liberanimalium.compat.SyncedDataSink;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier.Builder;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.Level;

public class MonstrousnightmareEntity extends DragonBase {
   private static final EntityDataAccessor<Boolean> DATA_IS_SELF_IMMOLATING = SynchedEntityData.defineId(
      MonstrousnightmareEntity.class, EntityDataSerializers.BOOLEAN
   );

   @Override
   protected void defineData(SyncedDataSink data) {
      super.defineData(data);
      data.define(DATA_IS_SELF_IMMOLATING, false);
   }

   public MonstrousnightmareEntity(EntityType<? extends TamableAnimal> pEntityType, Level pLevel) {
      super(pEntityType, pLevel);
   }

   public static Builder setAttributes() {
      return DragonBase.createFaunaAttributes();
   }

   protected FaunaProfile buildProfile() {
      return dragonProfile().locomotion(LocomotionMode.SWIM, 1.0F).locomotion(LocomotionMode.DIVE, 1.0F).locomotion(LocomotionMode.CLIMB, 1.0F).build();
   }

   protected void registerSpeciesComponents(FaunaComponentRegistry registry) {
      this.registerJawsAndBreath(registry);
      this.setSecondaryAbility(new SelfImmolationAbility());
   }

   public boolean isSelfImmolating() {
      return (Boolean)this.entityData.get(DATA_IS_SELF_IMMOLATING);
   }

   public void setSelfImmolating(boolean isImmolating) {
      this.entityData.set(DATA_IS_SELF_IMMOLATING, isImmolating);
   }

   public boolean abilityLoopActive() {
      return this.isSelfImmolating();
   }

   @Override
   public DragonType getDragonType() {
      return DragonType.MONSTROUS_NIGHTMARE;
   }

   @Override
   public ItemStack getScaleItem() {
      return new ItemStack((ItemLike)ModItems.MONSTROUSNIGHTMARE_SCALES.get());
   }
}
