package net.magister.bookofdragons.entity.custom.dragon;

import net.magister.bookofdragons.entity.ability.GasIgniteAbility;
import net.magister.bookofdragons.entity.base.dragon.DragonBase;
import net.magister.bookofdragons.entity.data.DragonType;
import net.magister.bookofdragons.item.ModItems;
import net.magister.liberanimalium.body.stats.FaunaProfile;
import net.magister.liberanimalium.chassis.component.FaunaComponentRegistry;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier.Builder;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.Level;

public class HiddeousZipplebackEntity extends DragonBase {
   public HiddeousZipplebackEntity(EntityType<? extends TamableAnimal> pEntityType, Level pLevel) {
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
      this.setSecondaryAbility(new GasIgniteAbility());
   }

   @Override
   public DragonType getDragonType() {
      return DragonType.HIDEOUS_ZIPPLEBACK;
   }

   @Override
   public ItemStack getScaleItem() {
      return new ItemStack((ItemLike)ModItems.ZIPPLEBACK_SCALES.get());
   }
}
