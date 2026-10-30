package net.magister.liberanimalium.chassis.component.melee;

import org.jetbrains.annotations.Nullable;
import net.magister.liberanimalium.chassis.component.ranged.IPrimaryAttackHandler;
import net.minecraft.world.entity.LivingEntity;

public interface IMeleeAttackHandler extends IPrimaryAttackHandler {
   double meleeReach();

   boolean attack(@Nullable LivingEntity var1);

   default boolean shove(LivingEntity victim) {
      return false;
   }

   @Nullable
   LivingEntity committedVictim();
}
