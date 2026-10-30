package net.magister.bookofdragons.villager;

import net.magister.bookofdragons.compat.Trades;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.npc.VillagerTrades.ItemListing;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.trading.MerchantOffer;
import org.jetbrains.annotations.Nullable;

public class FixedTrade implements ItemListing {
   private final ItemStack price;
   private final ItemStack price2;
   private final ItemStack forSale;
   private final int maxTrades;
   private final int xp;
   private final float priceMult;

   public FixedTrade(ItemStack price, ItemStack price2, ItemStack forSale, int maxTrades, int xp, float priceMult) {
      this.price = price;
      this.price2 = price2;
      this.forSale = forSale;
      this.maxTrades = maxTrades;
      this.xp = xp;
      this.priceMult = priceMult;
   }

   public FixedTrade(ItemStack price, ItemStack forSale, int maxTrades, int xp, float priceMult) {
      this(price, ItemStack.EMPTY, forSale, maxTrades, xp, priceMult);
   }

   @Nullable
   public MerchantOffer getOffer(Entity trader, RandomSource random) {
      return Trades.offer(this.price, this.price2, this.forSale, this.maxTrades, this.xp, this.priceMult);
   }
}
