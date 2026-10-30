package net.magister.bookofdragons.compat;

import net.magister.bookofdragons.compat.v20.TradesImpl;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.trading.MerchantOffer;

public final class Trades {
   private Trades() {
   }

   public static MerchantOffer offer(ItemStack price, ItemStack result, int maxUses, int xp, float priceMultiplier) {
      return TradesImpl.offer(price, result, maxUses, xp, priceMultiplier);
   }

   public static MerchantOffer offer(ItemStack price, ItemStack price2, ItemStack result, int maxUses, int xp, float priceMultiplier) {
      return TradesImpl.offer(price, price2, result, maxUses, xp, priceMultiplier);
   }
}
