package net.magister.bookofdragons.compat.v20;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.trading.MerchantOffer;

public final class TradesImpl {
   private TradesImpl() {
   }

   public static MerchantOffer offer(ItemStack price, ItemStack result, int maxUses, int xp, float priceMultiplier) {
      return new MerchantOffer(price, result, maxUses, xp, priceMultiplier);
   }

   public static MerchantOffer offer(ItemStack price, ItemStack price2, ItemStack result, int maxUses, int xp, float priceMultiplier) {
      return new MerchantOffer(price, price2, result, maxUses, xp, priceMultiplier);
   }
}
