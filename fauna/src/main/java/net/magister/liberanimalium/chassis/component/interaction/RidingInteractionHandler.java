package net.magister.liberanimalium.chassis.component.interaction;

import net.magister.liberanimalium.chassis.FaunaBase;
import net.magister.liberanimalium.chassis.component.CommandComponent;
import net.magister.liberanimalium.chassis.component.command.Modifier;
import net.magister.liberanimalium.chassis.component.command.Stance;
import net.magister.liberanimalium.progression.FaunaInteractionEvents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

public class RidingInteractionHandler implements IFaunaInteractionHandler {
   @Override
   public InteractionResult handle(FaunaBase body, Player player, InteractionHand hand) {
      ItemStack itemstack = player.getItemInHand(hand);
      if (itemstack.isEmpty() && !player.isShiftKeyDown() && !body.isVehicle()) {
         if (body.faunaMind() != null && body.faunaMind().knowledge().courtship().passedWithin(20)) {
            return InteractionResult.CONSUME;
         }

         if (body.canBeMountedBy(player)) {
            CommandComponent command = body.componentRegistry.get(CommandComponent.class);
            if (body.isTame() && command != null && command.getStance() != Stance.FOLLOW) {
               command.setCommandForced(Stance.FOLLOW, Modifier.TRAIL);
            }

            body.getNavigation().stop();
            player.setYRot(body.getYRot());
            player.setXRot(body.getXRot());
            boolean ridingSuccess = player.startRiding(body);
            if (ridingSuccess) {
               body.getNavigation().stop();
               FaunaInteractionEvents.post(FaunaInteractionEvents.Type.MOUNTED, player, body, null);
            }

            return ridingSuccess ? InteractionResult.SUCCESS : InteractionResult.FAIL;
         }
      }

      return InteractionResult.PASS;
   }
}
