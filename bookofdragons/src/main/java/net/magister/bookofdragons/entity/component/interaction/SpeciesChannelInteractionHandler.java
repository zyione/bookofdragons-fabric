package net.magister.bookofdragons.entity.component.interaction;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.magister.bookofdragons.entity.base.dragon.DragonBase;
import net.magister.liberanimalium.body.gameplay.SpeciesGameplayProfile.SpeciesChannel;
import net.magister.liberanimalium.body.stats.TamingStage;
import net.magister.liberanimalium.chassis.FaunaBase;
import net.magister.liberanimalium.chassis.component.BondingLedgerComponent;
import net.magister.liberanimalium.chassis.component.interaction.IFaunaInteractionHandler;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

public class SpeciesChannelInteractionHandler implements IFaunaInteractionHandler {
   private final Map<UUID, Long> lastUse = new HashMap<>();

   public InteractionResult handle(FaunaBase dragon, Player player, InteractionHand hand) {
      SpeciesChannel channel = dragon instanceof DragonBase d ? d.speciesChannel() : null;
      if (channel == null) {
         return InteractionResult.PASS;
      }

      ItemStack stack = player.getItemInHand(hand);
      if (stack.isEmpty() || !channel.match().test(dragon, player)) {
         return InteractionResult.PASS;
      }

      if (dragon.isTame() && dragon.stageFor(player.getUUID()).atLeast(TamingStage.TOLERANT)) {
         long now = dragon.level().getGameTime();
         Long last = this.lastUse.get(player.getUUID());
         if (last != null && now - last < channel.cooldown()) {
            return InteractionResult.CONSUME;
         }

         this.lastUse.put(player.getUUID(), now);
         award(dragon, player, channel, now);
         dragon.level().broadcastEntityEvent(dragon, (byte)7);
         return InteractionResult.SUCCESS;
      } else {
         return InteractionResult.PASS;
      }
   }

   private static void award(FaunaBase dragon, Player player, SpeciesChannel channel, long now) {
      BondingLedgerComponent ledger = (BondingLedgerComponent)dragon.componentRegistry.get(BondingLedgerComponent.class);
      if (ledger != null) {
         int repeats = repeatsFor(dragon, player);
         ledger.awardChannel(player.getUUID(), channel.points(), repeats);
      }
   }

   private static int repeatsFor(FaunaBase dragon, Player player) {
      if (dragon.faunaMind() == null) {
         return 0;
      }

      float familiarity = dragon.faunaMind().knowledge().social().of(player.getUUID()).familiarity();
      return Math.round(familiarity / 0.05F);
   }
}
