package net.magister.liberanimalium.chassis.component.interaction;

import java.util.UUID;
import net.magister.liberanimalium.chassis.FaunaBase;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;

public class LootInteractionHandler implements IFaunaInteractionHandler {
   @Override
   public InteractionResult handle(FaunaBase body, Player player, InteractionHand hand) {
      UUID robbed = body.loot();
      if (robbed != null && robbed.equals(player.getUUID())) {
         if (!body.level().isClientSide()) {
            body.dropLoot();
         }

         return InteractionResult.SUCCESS;
      } else {
         return InteractionResult.PASS;
      }
   }
}
