package net.magister.bookofdragons.event;

import net.magister.bookofdragons.compat.Trades;
import net.magister.bookofdragons.item.ModItems;
import net.magister.bookofdragons.item.custom.DragonRoostMapItem;
import net.magister.bookofdragons.villager.DragonRoostMapTrade;
import net.magister.bookofdragons.villager.ModVillagers;
import net.magister.liberanimalium.platform.event.CommonEvents;
import net.magister.liberanimalium.platform.world.GameRegistrar.TradeSink;
import net.minecraft.world.entity.npc.VillagerProfession;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ItemLike;

public class ModForgeEvents {
   public static void init() {
      CommonEvents.VILLAGER_TRADES.register(ModForgeEvents::addCustomTrades);
   }

   public static void addCustomTrades(TradeSink event) {
      VillagerProfession hooligan = (VillagerProfession)ModVillagers.HOOLIGAN.get();
      event.add(
         hooligan,
         1,
         (pTrader, pRandom) -> Trades.offer(new ItemStack(Items.IRON_INGOT, 1), new ItemStack((ItemLike)ModItems.BOOK_OF_DRAGONS.get(), 1), 1, 5, 0.02F)
      );
      event.add(hooligan, 1, (pTrader, pRandom) -> Trades.offer(new ItemStack(Items.EMERALD, 4), new ItemStack(Items.IRON_AXE, 1), 12, 5, 0.02F));
      Item caveMapItem = (Item)ModItems.CAVE_ROOST_MAP.get();
      if (caveMapItem instanceof DragonRoostMapItem dragonMap) {
         event.add(hooligan, 2, DragonRoostMapTrade.fromMapItem(new ItemStack(Items.EMERALD, 20), new ItemStack(Items.STONE, 16), dragonMap, 1, 10, 0.02F));
      }

      event.add(hooligan, 2, (pTrader, pRandom) -> Trades.offer(new ItemStack(Items.EMERALD, 8), new ItemStack(Items.MUTTON, 4), 24, 10, 0.02F));
      Item gladeMapItem = (Item)ModItems.GLADE_ROOST_MAP.get();
      if (gladeMapItem instanceof DragonRoostMapItem dragonMap) {
         event.add(hooligan, 3, DragonRoostMapTrade.fromMapItem(new ItemStack(Items.EMERALD, 20), new ItemStack(Items.IRON_INGOT, 8), dragonMap, 1, 20, 0.02F));
      }

      event.add(hooligan, 3, (pTrader, pRandom) -> Trades.offer(new ItemStack(Items.EMERALD, 6), new ItemStack(Items.CHICKEN, 4), 24, 20, 0.02F));
      Item wetlandMapItem = (Item)ModItems.WETLAND_ROOST_MAP.get();
      if (wetlandMapItem instanceof DragonRoostMapItem dragonMap) {
         event.add(hooligan, 4, DragonRoostMapTrade.fromMapItem(new ItemStack(Items.EMERALD, 20), new ItemStack(Items.ENDER_PEARL, 4), dragonMap, 1, 30, 0.02F));
      }

      event.add(hooligan, 4, (pTrader, pRandom) -> Trades.offer(new ItemStack(Items.EMERALD, 4), new ItemStack(Items.COD, 8), 24, 30, 0.02F));
      Item barrensMapItem = (Item)ModItems.BARRENS_ROOST_MAP.get();
      if (barrensMapItem instanceof DragonRoostMapItem dragonMap) {
         event.add(hooligan, 5, DragonRoostMapTrade.fromMapItem(new ItemStack(Items.EMERALD, 20), new ItemStack(Items.BLAZE_POWDER, 4), dragonMap, 1, 50, 0.02F));
      }

      Item cliffMapItem = (Item)ModItems.CLIFF_ROOST_MAP.get();
      if (cliffMapItem instanceof DragonRoostMapItem dragonMap) {
         event.add(hooligan, 5, DragonRoostMapTrade.fromMapItem(new ItemStack(Items.EMERALD, 20), new ItemStack(Items.DIAMOND, 4), dragonMap, 1, 50, 0.02F));
      }
   }
}
