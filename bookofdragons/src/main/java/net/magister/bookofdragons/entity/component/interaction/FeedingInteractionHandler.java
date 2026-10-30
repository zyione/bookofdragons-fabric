package net.magister.bookofdragons.entity.component.interaction;

import net.magister.bookofdragons.entity.base.dragon.DragonBase;
import net.magister.bookofdragons.sound.RefusalTelegraph;
import net.magister.liberanimalium.chassis.FaunaBase;
import net.magister.liberanimalium.chassis.component.interaction.IFaunaInteractionHandler;
import net.magister.liberanimalium.fauna.api.Delivery;
import net.magister.liberanimalium.fauna.api.Kindness;
import net.magister.liberanimalium.fauna.core.FaunaMind;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

public class FeedingInteractionHandler implements IFaunaInteractionHandler {
   public InteractionResult handle(FaunaBase dragon, Player player, InteractionHand hand) {
      ItemStack itemstack = player.getItemInHand(hand);
      if (dragon instanceof DragonBase picky && picky.isDislikedFood(itemstack)) {
         picky.refuse(player, itemstack);
         return InteractionResult.sidedSuccess(dragon.level().isClientSide());
      } else {
         if (!dragon.isFood(itemstack)) {
            return InteractionResult.PASS;
         }

         if ((!dragon.isTame() || dragon.isEstranged()) && !beggingAt(dragon, player)) {
            RefusalTelegraph.emit(dragon);
            return InteractionResult.FAIL;
         }

         dragon.awardGift(player, itemstack, Delivery.HAND);
         float craving = dragon.cravingFor(itemstack);
         float relish = dragon.relishFor(itemstack);
         dragon.getNeedsSystem().eat(itemstack);
         FaunaMind mind = dragon.faunaMind();
         if (mind != null && !dragon.level().isClientSide()) {
            mind.perception()
               .deliver(dragon, new Kindness(player.getUUID(), dragon.kindOfAttacker(player), player.position(), mind.age(), Delivery.HAND, craving, relish));
         }

         if (dragon.getGrowthStage() < 2) {
            int growthBonus = dragon instanceof DragonBase d && d.isFavoriteFood(itemstack) ? 2400 : 1200;
            dragon.setGrowthProgress(dragon.getGrowthProgress() + growthBonus);
         }

         if (!player.getAbilities().instabuild) {
            itemstack.shrink(1);
         }

         dragon.level().broadcastEntityEvent(dragon, (byte)7);
         return InteractionResult.SUCCESS;
      }
   }

   private static boolean beggingAt(FaunaBase dragon, Player player) {
      FaunaMind mind = dragon.faunaMind();
      return mind != null && mind.beggingAt(player.getUUID());
   }
}
