package net.magister.bookofdragons.item.custom;

import java.util.UUID;
import net.magister.bookofdragons.entity.base.dragon.DragonBase;
import net.magister.bookofdragons.entity.component.InventoryComponent;
import net.magister.liberanimalium.chassis.component.CommandComponent;
import net.magister.liberanimalium.chassis.component.command.Modifier;
import net.magister.liberanimalium.chassis.component.command.Stance;
import net.magister.liberanimalium.fauna.core.FaunaMind;
import net.magister.liberanimalium.fauna.know.Courtship;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.Item.Properties;

public class DragonDebugTameStick extends Item {
   private static final int PERMISSION_LEVEL = 2;

   public DragonDebugTameStick(Properties properties) {
      super(properties);
   }

   public InteractionResult interactLivingEntity(ItemStack stack, Player player, LivingEntity target, InteractionHand hand) {
      if (!player.hasPermissions(2)) {
         return InteractionResult.PASS;
      } else if (player.level().isClientSide()) {
         return InteractionResult.SUCCESS;
      } else if (!(target instanceof DragonBase dragon)) {
         return InteractionResult.PASS;
      } else {
         if (dragon.isTame()) {
            String owner = dragon.getOwner() != null ? dragon.getOwner().getName().getString() : "owner offline";
            player.displayClientMessage(Component.literal("§6[DEBUG] Already tamed by: §f" + owner), true);
            return InteractionResult.SUCCESS;
         }

         UUID id = player.getUUID();
         FaunaMind mind = dragon.faunaMind();
         if (mind != null) {
            mind.knowledge().social().stage(id, 1.0F);
         }

         boolean throughTheLadder = mind != null && mind.forceStage(id, Courtship.length(dragon.rideable()));
         if (!throughTheLadder) {
            dragon.adopt(id);
         }

         dragon.setBond(id, 1000);
         InventoryComponent inventory = (InventoryComponent)dragon.componentRegistry.get(InventoryComponent.class);
         if (inventory != null && dragon.getInventory().getItem(0).isEmpty()) {
            dragon.getInventory().setItem(0, new ItemStack(Items.SADDLE));
            inventory.updateEquipment();
         }

         ((CommandComponent)dragon.componentRegistry.get(CommandComponent.class)).setCommandForced(Stance.STAY, Modifier.REST);
         player.displayClientMessage(
            Component.literal(
               "§a[DEBUG] Tamed + saddled + bonded, STAY: §f" + dragon.getName().getString() + (throughTheLadder ? "" : " §7(direct: it had not seen you yet)")
            ),
            true
         );
         dragon.level().broadcastEntityEvent(dragon, (byte)7);
         return InteractionResult.SUCCESS;
      }
   }
}
