package net.magister.bookofdragons.item.custom;

import java.util.List;
import net.magister.bookofdragons.client.util.BookOfDragonsClientHelper;
import net.magister.bookofdragons.entity.base.dragon.DragonBase;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.Item.Properties;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

public class BookOfDragonsItem extends Item {
   public BookOfDragonsItem(Properties properties) {
      super(properties);
   }

   public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
      ItemStack itemStack = player.getItemInHand(hand);
      if (level.isClientSide()) {
         BookOfDragonsClientHelper.openBookScreen(null);
      }

      return InteractionResultHolder.success(itemStack);
   }

   public InteractionResult interactLivingEntity(ItemStack stack, Player player, LivingEntity entity, InteractionHand hand) {
      if (player.isShiftKeyDown() && entity instanceof DragonBase dragon && dragon.isTame() && dragon.getOwner() == player) {
         if (!entity.level().isClientSide() && player instanceof ServerPlayer serverPlayer) {
            dragon.openGui(serverPlayer);
         }

         return InteractionResult.SUCCESS;
      } else {
         return InteractionResult.PASS;
      }
   }

   public void appendHoverText(ItemStack pStack, @Nullable Level pLevel, List<Component> pTooltipComponents, TooltipFlag pIsAdvanced) {
      pTooltipComponents.add(Component.translatable("item.bookofdragons.book_of_dragons.tooltip"));
      pTooltipComponents.add(Component.translatable("item.bookofdragons.book_of_dragons.tooltip.shift_use"));
      super.appendHoverText(pStack, pLevel, pTooltipComponents, pIsAdvanced);
   }
}
