package net.magister.bookofdragons.villager;

import net.magister.bookofdragons.compat.Trades;
import net.magister.bookofdragons.item.custom.DragonRoostMapItem;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.npc.VillagerTrades.ItemListing;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.trading.MerchantOffer;
import org.jetbrains.annotations.Nullable;

public class DragonRoostMapTrade implements ItemListing {
   private final ItemStack price1;
   private final ItemStack price2;
   private final int maxUses;
   private final int villagerXp;
   private final float priceMultiplier;
   private final Item outputItem;

   public DragonRoostMapTrade(ItemStack price1, ItemStack price2, int maxUses, int villagerXp, float priceMultiplier, Item outputItem) {
      this.price1 = price1;
      this.price2 = price2;
      this.maxUses = maxUses;
      this.villagerXp = villagerXp;
      this.priceMultiplier = priceMultiplier;
      this.outputItem = outputItem;
   }

   @Nullable
   public MerchantOffer getOffer(Entity trader, RandomSource random) {
      ItemStack outputStack = new ItemStack(this.outputItem);
      return this.price2.isEmpty()
         ? Trades.offer(this.price1, outputStack, this.maxUses, this.villagerXp, this.priceMultiplier)
         : Trades.offer(this.price1, this.price2, outputStack, this.maxUses, this.villagerXp, this.priceMultiplier);
   }

   public static DragonRoostMapTrade fromMapItem(
      ItemStack price1, ItemStack price2, DragonRoostMapItem mapItem, int maxUses, int villagerXp, float priceMultiplier
   ) {
      return new DragonRoostMapTrade(price1, price2, maxUses, villagerXp, priceMultiplier, mapItem);
   }
}
