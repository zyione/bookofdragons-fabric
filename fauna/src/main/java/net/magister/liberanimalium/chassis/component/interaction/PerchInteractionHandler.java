package net.magister.liberanimalium.chassis.component.interaction;

import net.magister.liberanimalium.chassis.FaunaBase;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;

public class PerchInteractionHandler implements IFaunaInteractionHandler {
   @Override
   public InteractionResult handle(FaunaBase body, Player player, InteractionHand hand) {
      if (hand == InteractionHand.MAIN_HAND && !player.isShiftKeyDown() && player.getItemInHand(hand).isEmpty() && body.isOwnedBy(player)) {
         if (!body.level().isClientSide()) {
            body.beckon(player);
         }

         return InteractionResult.SUCCESS;
      } else {
         return InteractionResult.PASS;
      }
   }
}
