package net.magister.bookofdragons.entity.component.interaction;

import net.magister.bookofdragons.entity.component.InventoryComponent;
import net.magister.liberanimalium.body.stats.TamingStage;
import net.magister.liberanimalium.chassis.FaunaBase;
import net.magister.liberanimalium.chassis.component.interaction.IFaunaInteractionHandler;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

public class SaddleInteractionHandler implements IFaunaInteractionHandler {
   public InteractionResult handle(FaunaBase dragon, Player player, InteractionHand hand) {
      if (dragon.refusesTack()) {
         return InteractionResult.PASS;
      } else {
         InventoryComponent inventory = (InventoryComponent)dragon.componentRegistry.get(InventoryComponent.class);
         if (inventory == null) {
            return InteractionResult.PASS;
         } else {
            ItemStack itemstack = player.getItemInHand(hand);
            boolean isOwner = dragon.getOwnerUUID() != null && player.getUUID().equals(dragon.getOwnerUUID());
            if (itemstack.is(Items.SADDLE) && !inventory.isSaddled() && dragon.isTame() && isOwner) {
               inventory.getInventory().setItem(0, itemstack.split(1));
               inventory.updateEquipment();
               dragon.playSound(SoundEvents.HORSE_SADDLE, 1.0F, 1.0F);
               return InteractionResult.SUCCESS;
            } else if (itemstack.is(Items.CHEST)
               && !inventory.hasChest()
               && dragon.isTame()
               && isOwner
               && dragon.stageFor(player.getUUID()).atLeast(TamingStage.BONDED)) {
               inventory.getInventory().setItem(1, itemstack.split(1));
               inventory.updateEquipment();
               dragon.playSound(SoundEvents.DONKEY_CHEST, 1.0F, 1.0F);
               return InteractionResult.SUCCESS;
            } else {
               return InteractionResult.PASS;
            }
         }
      }
   }
}
