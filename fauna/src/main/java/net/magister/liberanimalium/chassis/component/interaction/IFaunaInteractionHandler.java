package net.magister.liberanimalium.chassis.component.interaction;

import net.magister.liberanimalium.chassis.FaunaBase;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;

public interface IFaunaInteractionHandler {
   InteractionResult handle(FaunaBase var1, Player var2, InteractionHand var3);
}
