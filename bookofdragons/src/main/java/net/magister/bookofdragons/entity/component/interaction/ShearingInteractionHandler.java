package net.magister.bookofdragons.entity.component.interaction;

import net.magister.bookofdragons.entity.component.ShearingComponent;
import net.magister.liberanimalium.chassis.FaunaBase;
import net.magister.liberanimalium.chassis.component.interaction.IFaunaInteractionHandler;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

public class ShearingInteractionHandler implements IFaunaInteractionHandler {
   public InteractionResult handle(FaunaBase dragon, Player player, InteractionHand hand) {
      ItemStack itemstack = player.getItemInHand(hand);
      boolean isOwner = dragon.getOwnerUUID() != null && player.getUUID().equals(dragon.getOwnerUUID());
      if (itemstack.is(Items.SHEARS) && dragon.isTame() && isOwner) {
         ShearingComponent shearingComponent = (ShearingComponent)dragon.componentRegistry.get(ShearingComponent.class);
         if (shearingComponent.getShearingCooldownTicks() > 0) {
            return InteractionResult.FAIL;
         }

         shearingComponent.shearForScales(player, hand);
         return InteractionResult.SUCCESS;
      } else {
         return InteractionResult.PASS;
      }
   }
}
