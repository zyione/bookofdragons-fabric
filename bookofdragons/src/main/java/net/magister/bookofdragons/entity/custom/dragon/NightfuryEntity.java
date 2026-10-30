package net.magister.bookofdragons.entity.custom.dragon;

import net.magister.bookofdragons.entity.ability.LightningStrikeAbility;
import net.magister.bookofdragons.entity.base.dragon.DragonBase;
import net.magister.bookofdragons.entity.data.DragonType;
import net.magister.bookofdragons.item.ModItems;
import net.magister.liberanimalium.body.stats.FaunaProfile;
import net.magister.liberanimalium.chassis.component.FaunaComponentRegistry;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier.Builder;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

public class NightfuryEntity extends DragonBase {
   private int diveStartSoundTicks = 0;
   private int continuousDiveTicks = 0;
   public static final int MIN_DIVE_TICKS_FOR_SOUND = 20;
   public static final int DIVE_START_DURATION_TICKS = 60;

   public int getDiveStartSoundTicks() {
      return this.diveStartSoundTicks;
   }

   public void setDiveStartSoundTicks(int ticks) {
      this.diveStartSoundTicks = ticks;
   }

   public int getContinuousDiveTicks() {
      return this.continuousDiveTicks;
   }

   public void setContinuousDiveTicks(int ticks) {
      this.continuousDiveTicks = ticks;
   }

   public NightfuryEntity(EntityType<? extends TamableAnimal> pEntityType, Level pLevel) {
      super(pEntityType, pLevel);
   }

   public static Builder setAttributes() {
      return DragonBase.createFaunaAttributes();
   }

   protected FaunaProfile buildProfile() {
      return dragonProfile().build();
   }

   protected void registerSpeciesComponents(FaunaComponentRegistry registry) {
      this.registerJawsAndBreath(registry);
      this.setSecondaryAbility(new LightningStrikeAbility());
   }

   public boolean isDiving() {
      if (!this.isFlying()) {
         return false;
      }

      Vec3 vel = this.getDeltaMovement();
      double total = vel.length();
      return total > 0.01 && vel.y / total < -0.94;
   }

   @Override
   public DragonType getDragonType() {
      return DragonType.NIGHTFURY;
   }

   @Override
   public ItemStack getScaleItem() {
      return new ItemStack((ItemLike)ModItems.NIGHTFURY_SCALES.get());
   }

   public boolean hurt(DamageSource pSource, float pAmount) {
      return pSource.is(DamageTypeTags.IS_LIGHTNING) ? false : super.hurt(pSource, pAmount);
   }
}
