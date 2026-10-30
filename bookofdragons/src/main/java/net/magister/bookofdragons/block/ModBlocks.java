package net.magister.bookofdragons.block;

import java.util.function.Supplier;
import net.magister.bookofdragons.block.custom.PlaceableDragonScaleBlock;
import net.magister.bookofdragons.block.custom.RoostControllerBlock;
import net.magister.bookofdragons.block.custom.VikingAnvilBlock;
import net.magister.bookofdragons.item.ModItems;
import net.magister.liberanimalium.platform.Services;
import net.magister.liberanimalium.platform.registry.RegEntry;
import net.magister.liberanimalium.platform.registry.Registrar;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour.Properties;

public class ModBlocks {
   public static final Registrar<Block> BLOCKS = Services.registries().create(Registries.BLOCK, "bookofdragons");
   public static final RegEntry<Block> VIKING_ANVIL = registerBlockWithItem(
      "viking_anvil", () -> new VikingAnvilBlock(Properties.of().strength(5.0F, 1200.0F).requiresCorrectToolForDrops().sound(SoundType.ANVIL).noOcclusion())
   );
   public static final RegEntry<Block> PLACEABLE_NIGHTFURY_SCALES = BLOCKS.register(
      "placeable_nightfury_scales",
      () -> new PlaceableDragonScaleBlock(Properties.of().instabreak().sound(SoundType.WOOL).noCollission().noOcclusion(), ModItems.NIGHTFURY_SCALES)
   );
   public static final RegEntry<Block> PLACEABLE_NADDER_SCALES = BLOCKS.register(
      "placeable_nadder_scales",
      () -> new PlaceableDragonScaleBlock(Properties.of().instabreak().sound(SoundType.WOOL).noCollission().noOcclusion(), ModItems.DEADLYNADDER_SCALES)
   );
   public static final RegEntry<Block> PLACEABLE_NIGHTMARE_SCALES = BLOCKS.register(
      "placeable_nightmare_scales",
      () -> new PlaceableDragonScaleBlock(
         Properties.of().instabreak().sound(SoundType.WOOL).noCollission().noOcclusion(), ModItems.MONSTROUSNIGHTMARE_SCALES
      )
   );
   public static final RegEntry<Block> PLACEABLE_GRONCKLE_SCALES = BLOCKS.register(
      "placeable_gronckle_scales",
      () -> new PlaceableDragonScaleBlock(Properties.of().instabreak().sound(SoundType.WOOL).noCollission().noOcclusion(), ModItems.GRONCKLE_SCALES)
   );
   public static final RegEntry<Block> PLACEABLE_ZIPPLEBACK_SCALES = BLOCKS.register(
      "placeable_zippleback_scales",
      () -> new PlaceableDragonScaleBlock(Properties.of().instabreak().sound(SoundType.WOOL).noCollission().noOcclusion(), ModItems.ZIPPLEBACK_SCALES)
   );
   public static final RegEntry<Block> PLACEABLE_SKRILL_SCALES = BLOCKS.register(
      "placeable_skrill_scales",
      () -> new PlaceableDragonScaleBlock(Properties.of().instabreak().sound(SoundType.WOOL).noCollission().noOcclusion(), ModItems.SKRILL_SCALES)
   );
   public static final RegEntry<Block> PLACEABLE_WHISPERINGDEATH_SCALES = BLOCKS.register(
      "placeable_whisperingdeath_scales",
      () -> new PlaceableDragonScaleBlock(Properties.of().instabreak().sound(SoundType.WOOL).noCollission().noOcclusion(), ModItems.WHISPERINGDEATH_SCALES)
   );
   public static final RegEntry<Block> PLACEABLE_TERRIBLETERROR_SCALES = BLOCKS.register(
      "placeable_terribleterror_scales",
      () -> new PlaceableDragonScaleBlock(Properties.of().instabreak().sound(SoundType.WOOL).noCollission().noOcclusion(), ModItems.TERRIBLE_TERROR_SCALES)
   );
   public static final RegEntry<Block> PLACEABLE_SPEEDSTINGER_SCALES = BLOCKS.register(
      "placeable_speedstinger_scales",
      () -> new PlaceableDragonScaleBlock(Properties.of().instabreak().sound(SoundType.WOOL).noCollission().noOcclusion(), ModItems.SPEED_STINGER_SCALES)
   );
   public static final RegEntry<Block> ROOST_CONTROLLER = registerBlockWithItem(
      "roost_controller", () -> new RoostControllerBlock(Properties.of().strength(1.5F).sound(SoundType.GRASS).noOcclusion())
   );

   private static <T extends Block> RegEntry<T> registerBlockWithItem(String name, Supplier<T> block) {
      RegEntry<T> toReturn = BLOCKS.register(name, block);
      ModItems.ITEMS.register(name, () -> new BlockItem((Block)toReturn.get(), new net.minecraft.world.item.Item.Properties()));
      return toReturn;
   }

   public static void register() {
   }
}
