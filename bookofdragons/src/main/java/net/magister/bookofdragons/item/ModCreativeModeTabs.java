package net.magister.bookofdragons.item;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Supplier;
import net.magister.bookofdragons.block.ModBlocks;
import net.magister.liberanimalium.platform.Services;
import net.magister.liberanimalium.platform.registry.RegEntry;
import net.magister.liberanimalium.platform.registry.Registrar;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ItemLike;

public class ModCreativeModeTabs {
   public static final Registrar<CreativeModeTab> CREATIVE_MODE_TABS = Services.registries().create(Registries.CREATIVE_MODE_TAB, "bookofdragons");
   public static final List<Supplier<? extends ItemLike>> EXTRA = new CopyOnWriteArrayList<>();
   public static final RegEntry<CreativeModeTab> BOOK_OF_DRAGONS_TAB = CREATIVE_MODE_TABS.register(
      "bookofdragons_tab",
      () -> net.fabricmc.fabric.api.itemgroup.v1.FabricItemGroup.builder()
         .icon(() -> new ItemStack((ItemLike)ModItems.RATTLE_STAFF.get()))
         .title(Component.translatable("creativetab.bookofdragons_tab"))
         .displayItems((pParameters, pOutput) -> {
            pOutput.accept((ItemLike)ModItems.RATTLE_STAFF.get());
            pOutput.accept((ItemLike)ModItems.DRAGON_WHISTLE.get());
            pOutput.accept((ItemLike)ModItems.BOOK_OF_DRAGONS.get());
            pOutput.accept((ItemLike)ModBlocks.VIKING_ANVIL.get());
            pOutput.accept((ItemLike)ModBlocks.ROOST_CONTROLLER.get());
            if (pParameters.hasPermissions()) {
               pOutput.accept((ItemLike)ModItems.DEBUG_TAME_STICK.get());
            }

            pOutput.accept((ItemLike)ModItems.SHEEP_PLUSH.get());
            pOutput.accept((ItemLike)ModItems.FURY_PLUSH.get());
            pOutput.accept((ItemLike)ModItems.FISH_PLUSH.get());
            pOutput.accept((ItemLike)ModItems.STEAK_PLUSH.get());
            pOutput.accept((ItemLike)ModItems.BALL_PLUSH.get());
            pOutput.accept((ItemLike)ModItems.TERROR_PLUSH.get());
            pOutput.accept((ItemLike)ModItems.NIGHTMARE_PLUSH.get());
            pOutput.accept((ItemLike)ModItems.CLIFF_ROOST_MAP.get());
            pOutput.accept((ItemLike)ModItems.CAVE_ROOST_MAP.get());
            pOutput.accept((ItemLike)ModItems.GLADE_ROOST_MAP.get());
            pOutput.accept((ItemLike)ModItems.WETLAND_ROOST_MAP.get());
            pOutput.accept((ItemLike)ModItems.BARRENS_ROOST_MAP.get());
            pOutput.accept((ItemLike)ModItems.NIGHTFURY_SCALES.get());
            pOutput.accept((ItemLike)ModItems.DEADLYNADDER_SCALES.get());
            pOutput.accept((ItemLike)ModItems.MONSTROUSNIGHTMARE_SCALES.get());
            pOutput.accept((ItemLike)ModItems.GRONCKLE_SCALES.get());
            pOutput.accept((ItemLike)ModItems.ZIPPLEBACK_SCALES.get());
            pOutput.accept((ItemLike)ModItems.SKRILL_SCALES.get());
            pOutput.accept((ItemLike)ModItems.WHISPERINGDEATH_SCALES.get());
            pOutput.accept((ItemLike)ModItems.TERRIBLE_TERROR_SCALES.get());
            pOutput.accept((ItemLike)ModItems.SPEED_STINGER_SCALES.get());
            pOutput.accept((ItemLike)ModItems.FIREPROOF_SCALEMAIL.get());
            pOutput.accept((ItemLike)ModItems.GRONCKLE_IRON.get());

            for (RegEntry<Item> egg : ModItems.SPAWN_EGGS) {
               pOutput.accept((ItemLike)egg.get());
            }

            for (Supplier<? extends ItemLike> item : EXTRA) {
               pOutput.accept(item.get());
            }
         })
         .build()
   );

   public static void register() {
   }
}
