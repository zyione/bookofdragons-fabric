package net.magister.bookofdragons.item;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;
import net.magister.bookofdragons.block.ModBlocks;
import net.magister.bookofdragons.entity.ModEntities;
import net.magister.bookofdragons.entity.custom.toy.PlushEntity;
import net.magister.bookofdragons.entity.custom.toy.PlushKind;
import net.magister.bookofdragons.entity.data.DragonType;
import net.magister.bookofdragons.item.custom.BookOfDragonsItem;
import net.magister.bookofdragons.item.custom.DragonDebugTameStick;
import net.magister.bookofdragons.item.custom.DragonEggItem;
import net.magister.bookofdragons.item.custom.DragonRoostMapItem;
import net.magister.bookofdragons.item.custom.DragonWhistleItem;
import net.magister.bookofdragons.item.custom.FireproofScalemailItem;
import net.magister.bookofdragons.item.custom.PlaceableFireproofScalesItem;
import net.magister.bookofdragons.item.custom.PlushItem;
import net.magister.bookofdragons.item.custom.RattlestaffItem;
import net.magister.liberanimalium.compat.LegacyStacks;
import net.magister.liberanimalium.platform.Services;
import net.magister.liberanimalium.platform.registry.RegEntry;
import net.magister.liberanimalium.platform.registry.Registrar;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Item.Properties;
import net.minecraft.world.level.block.Block;

public class ModItems {
   public static final Registrar<Item> ITEMS = Services.registries().create(Registries.ITEM, "bookofdragons");
   public static final RegEntry<Item> RATTLE_STAFF = ITEMS.register("rattlestaff", () -> new RattlestaffItem(new Properties().stacksTo(1)));
   public static final RegEntry<Item> DRAGON_WHISTLE = ITEMS.register("dragon_whistle", () -> new DragonWhistleItem(new Properties().stacksTo(1)));
   public static final RegEntry<Item> BOOK_OF_DRAGONS = ITEMS.register("book_of_dragons", () -> new BookOfDragonsItem(new Properties().stacksTo(1).fireResistant()));
   public static final RegEntry<Item> DEBUG_TAME_STICK = ITEMS.register("debug_tame_stick", () -> new DragonDebugTameStick(new Properties().stacksTo(1)));
   public static final RegEntry<Item> SHEEP_PLUSH = plush("sheep_plush", () -> (EntityType<PlushEntity>)ModEntities.SHEEP_PLUSH.get(), PlushKind.SHEEP);
   public static final RegEntry<Item> FURY_PLUSH = plush("fury_plush", () -> (EntityType<PlushEntity>)ModEntities.FURY_PLUSH.get(), PlushKind.FURY);
   public static final RegEntry<Item> FISH_PLUSH = plush("fish_plush", () -> (EntityType<PlushEntity>)ModEntities.FISH_PLUSH.get(), PlushKind.FISH);
   public static final RegEntry<Item> STEAK_PLUSH = plush("steak_plush", () -> (EntityType<PlushEntity>)ModEntities.STEAK_PLUSH.get(), PlushKind.STEAK);
   public static final RegEntry<Item> BALL_PLUSH = plush("ball_plush", () -> (EntityType<PlushEntity>)ModEntities.BALL_PLUSH.get(), PlushKind.BALL);
   public static final RegEntry<Item> TERROR_PLUSH = plush("terror_plush", () -> (EntityType<PlushEntity>)ModEntities.TERROR_PLUSH.get(), PlushKind.TERROR);
   public static final RegEntry<Item> NIGHTMARE_PLUSH = plush(
      "nightmare_plush", () -> (EntityType<PlushEntity>)ModEntities.NIGHTMARE_PLUSH.get(), PlushKind.NIGHTMARE
   );
   public static final RegEntry<Item> CLIFF_ROOST_MAP = ITEMS.register(
      "cliff_roost_map",
      () -> new DragonRoostMapItem(
         TagKey.create(Registries.STRUCTURE, new ResourceLocation("bookofdragons", "roost_cliff")),
         "Cliff Roost",
         7372944,
         new Properties().stacksTo(1)
      )
   );
   public static final RegEntry<Item> STRIKE_ROOST_MAP = ITEMS.register(
      "strike_roost_map",
      () -> new DragonRoostMapItem(
         TagKey.create(Registries.STRUCTURE, new ResourceLocation("bookofdragons", "roost_strike")),
         "Strike Roost",
         4734347,
         new Properties().stacksTo(1)
      )
   );
   public static final RegEntry<Item> CAVE_ROOST_MAP = ITEMS.register(
      "cave_roost_map",
      () -> new DragonRoostMapItem(
         TagKey.create(Registries.STRUCTURE, new ResourceLocation("bookofdragons", "roost_cave")),
         "Cave Roost",
         9127187,
         new Properties().stacksTo(1)
      )
   );
   public static final RegEntry<Item> GLADE_ROOST_MAP = ITEMS.register(
      "glade_roost_map",
      () -> new DragonRoostMapItem(
         TagKey.create(Registries.STRUCTURE, new ResourceLocation("bookofdragons", "roost_glade")),
         "Glade Roost",
         2263842,
         new Properties().stacksTo(1)
      )
   );
   public static final RegEntry<Item> WETLAND_ROOST_MAP = ITEMS.register(
      "wetland_roost_map",
      () -> new DragonRoostMapItem(
         TagKey.create(Registries.STRUCTURE, new ResourceLocation("bookofdragons", "roost_wetland")),
         "Wetland Roost",
         3050327,
         new Properties().stacksTo(1)
      )
   );
   public static final RegEntry<Item> BARRENS_ROOST_MAP = ITEMS.register(
      "barrens_roost_map",
      () -> new DragonRoostMapItem(
         TagKey.create(Registries.STRUCTURE, new ResourceLocation("bookofdragons", "roost_barrens")),
         "Barrens Roost",
         16729344,
         new Properties().stacksTo(1)
      )
   );
   public static final RegEntry<Item> NIGHTFURY_SCALES = scales(
      "nightfury_scales", () -> (Block)ModBlocks.PLACEABLE_NIGHTFURY_SCALES.get(), DragonType.NIGHTFURY, 2894892
   );
   public static final RegEntry<Item> DEADLYNADDER_SCALES = scales(
      "deadlynadder_scales", () -> (Block)ModBlocks.PLACEABLE_NADDER_SCALES.get(), DragonType.DEADLY_NADDER, 16766720
   );
   public static final RegEntry<Item> MONSTROUSNIGHTMARE_SCALES = scales(
      "monstrousnightmare_scales", () -> (Block)ModBlocks.PLACEABLE_NIGHTMARE_SCALES.get(), DragonType.MONSTROUS_NIGHTMARE, 16729344
   );
   public static final RegEntry<Item> GRONCKLE_SCALES = scales(
      "gronckle_scales", () -> (Block)ModBlocks.PLACEABLE_GRONCKLE_SCALES.get(), DragonType.GRONCKLE, 9127187
   );
   public static final RegEntry<Item> ZIPPLEBACK_SCALES = scales(
      "hiddeouszippleback_scales", () -> (Block)ModBlocks.PLACEABLE_ZIPPLEBACK_SCALES.get(), DragonType.HIDEOUS_ZIPPLEBACK, 3329330
   );
   public static final RegEntry<Item> SKRILL_SCALES = scales("skrill_scales", () -> (Block)ModBlocks.PLACEABLE_SKRILL_SCALES.get(), DragonType.SKRILL, 9662683);
   public static final RegEntry<Item> WHISPERINGDEATH_SCALES = scales(
      "whisperingdeath_scales", () -> (Block)ModBlocks.PLACEABLE_WHISPERINGDEATH_SCALES.get(), DragonType.WHISPERING_DEATH, 9127187
   );
   public static final RegEntry<Item> TERRIBLE_TERROR_SCALES = scales(
      "terrible_terror_scales", () -> (Block)ModBlocks.PLACEABLE_TERRIBLETERROR_SCALES.get(), DragonType.TERRIBLE_TERROR, 3050327
   );
   public static final RegEntry<Item> SPEED_STINGER_SCALES = scales(
      "speed_stinger_scales", () -> (Block)ModBlocks.PLACEABLE_SPEEDSTINGER_SCALES.get(), DragonType.SPEED_STINGER, 16766720
   );
   public static final RegEntry<Item> FIREPROOF_SCALEMAIL = ITEMS.register(
      "fireproof_scalemail", () -> new FireproofScalemailItem(new Properties().stacksTo(16).fireResistant())
   );
   public static final RegEntry<Item> GRONCKLE_IRON = ITEMS.register("gronckle_iron", () -> new Item(new Properties().stacksTo(64)));
   public static final RegEntry<Item> NIGHTFURY_EGG = egg("nightfury_egg", () -> DragonType.NIGHTFURY);
   public static final RegEntry<Item> DEADLY_NADDER_EGG = egg("deadly_nadder_egg", () -> DragonType.DEADLY_NADDER);
   public static final RegEntry<Item> MONSTROUS_NIGHTMARE_EGG = egg("monstrous_nightmare_egg", () -> DragonType.MONSTROUS_NIGHTMARE);
   public static final RegEntry<Item> GRONCKLE_EGG = egg("gronckle_egg", () -> DragonType.GRONCKLE);
   public static final RegEntry<Item> HIDDEOUS_ZIPPLEBACK_EGG = egg("hiddeous_zippleback_egg", () -> DragonType.HIDEOUS_ZIPPLEBACK);
   public static final RegEntry<Item> SKRILL_EGG = egg("skrill_egg", () -> DragonType.SKRILL);
   public static final RegEntry<Item> WHISPERING_DEATH_EGG = egg("whispering_death_egg", () -> DragonType.WHISPERING_DEATH);
   public static final RegEntry<Item> TERRIBLE_TERROR_EGG = egg("terrible_terror_egg", () -> DragonType.TERRIBLE_TERROR);
   public static final RegEntry<Item> SPEED_STINGER_EGG = egg("speed_stinger_egg", () -> DragonType.SPEED_STINGER);
   public static final RegEntry<Item> NIGHTFURY_SPAWN_EGG = ITEMS.register(
      "nightfury_spawn_egg", () -> Services.items().spawnEgg(ModEntities.NIGHTFURY, 16777215, 16777215, new Properties())
   );
   public static final RegEntry<Item> DEADLYNADDER_SPAWN_EGG = ITEMS.register(
      "deadlynadder_spawn_egg", () -> Services.items().spawnEgg(ModEntities.DEADLYNADDER, 16777215, 16777215, new Properties())
   );
   public static final RegEntry<Item> MONSTROUSNIGHTMARE_SPAWN_EGG = ITEMS.register(
      "monstrousnightmare_spawn_egg", () -> Services.items().spawnEgg(ModEntities.MONSTROUSNIGHTMARE, 16777215, 16777215, new Properties())
   );
   public static final RegEntry<Item> GRONCKLE_SPAWN_EGG = ITEMS.register(
      "gronckle_spawn_egg", () -> Services.items().spawnEgg(ModEntities.GRONCKLE, 16777215, 16777215, new Properties())
   );
   public static final RegEntry<Item> HIDDEOUS_ZIPPLEBACK_SPAWN_EGG = ITEMS.register(
      "hiddeous_zippleback_spawn_egg", () -> Services.items().spawnEgg(ModEntities.HIDDEOUS_ZIPPLEBACK, 16777215, 16777215, new Properties())
   );
   public static final RegEntry<Item> SKRILL_SPAWN_EGG = ITEMS.register(
      "skrill_spawn_egg", () -> Services.items().spawnEgg(ModEntities.SKRILL, 16777215, 16777215, new Properties())
   );
   public static final RegEntry<Item> WHISPERING_DEATH_SPAWN_EGG = ITEMS.register(
      "whispering_death_spawn_egg", () -> Services.items().spawnEgg(ModEntities.WHISPERING_DEATH, 16777215, 16777215, new Properties())
   );
   public static final RegEntry<Item> TERRIBLE_TERROR_SPAWN_EGG = ITEMS.register(
      "terrible_terror_spawn_egg", () -> Services.items().spawnEgg(ModEntities.TERRIBLE_TERROR, 16777215, 16777215, new Properties())
   );
   public static final RegEntry<Item> SPEED_STINGER_SPAWN_EGG = ITEMS.register(
      "speed_stinger_spawn_egg", () -> Services.items().spawnEgg(ModEntities.SPEED_STINGER, 16777215, 16777215, new Properties())
   );
   public static final List<RegEntry<Item>> SPAWN_EGGS = List.of(
      NIGHTFURY_SPAWN_EGG,
      DEADLYNADDER_SPAWN_EGG,
      MONSTROUSNIGHTMARE_SPAWN_EGG,
      GRONCKLE_SPAWN_EGG,
      HIDDEOUS_ZIPPLEBACK_SPAWN_EGG,
      SKRILL_SPAWN_EGG,
      WHISPERING_DEATH_SPAWN_EGG,
      TERRIBLE_TERROR_SPAWN_EGG,
      SPEED_STINGER_SPAWN_EGG
   );
   private static final Map<DragonType, RegEntry<Item>> DRAGON_EGG_MAP = new EnumMap<>(DragonType.class);

   private static RegEntry<Item> plush(String id, Supplier<EntityType<PlushEntity>> toy, PlushKind kind) {
      return ITEMS.register(id, () -> new PlushItem(toy, kind, new Properties().stacksTo(16)));
   }

   public static RegEntry<Item> scales(String id, Supplier<Block> block, DragonType species, int colour) {
      return ITEMS.register(id, () -> new PlaceableFireproofScalesItem(block.get(), new Properties().stacksTo(64).fireResistant(), species, colour));
   }

   public static RegEntry<Item> egg(String id, Supplier<DragonType> type) {
      return ITEMS.register(id, () -> new DragonEggItem(type.get(), new Properties().stacksTo(16)));
   }

   private static void populateEggMap() {
      DRAGON_EGG_MAP.put(DragonType.NIGHTFURY, NIGHTFURY_EGG);
      DRAGON_EGG_MAP.put(DragonType.DEADLY_NADDER, DEADLY_NADDER_EGG);
      DRAGON_EGG_MAP.put(DragonType.MONSTROUS_NIGHTMARE, MONSTROUS_NIGHTMARE_EGG);
      DRAGON_EGG_MAP.put(DragonType.GRONCKLE, GRONCKLE_EGG);
      DRAGON_EGG_MAP.put(DragonType.HIDEOUS_ZIPPLEBACK, HIDDEOUS_ZIPPLEBACK_EGG);
      DRAGON_EGG_MAP.put(DragonType.SKRILL, SKRILL_EGG);
      DRAGON_EGG_MAP.put(DragonType.WHISPERING_DEATH, WHISPERING_DEATH_EGG);
      DRAGON_EGG_MAP.put(DragonType.TERRIBLE_TERROR, TERRIBLE_TERROR_EGG);
      DRAGON_EGG_MAP.put(DragonType.SPEED_STINGER, SPEED_STINGER_EGG);
   }

   public static void eggOf(DragonType type, RegEntry<Item> egg) {
      DRAGON_EGG_MAP.put(type, egg);
      LegacyStacks.keepEntityTag(egg.id());
   }

   public static Item getEggItemForType(DragonType type) {
      RegEntry<Item> itemSupplier = DRAGON_EGG_MAP.get(type);
      return itemSupplier != null ? (Item)itemSupplier.get() : (Item)NIGHTFURY_EGG.get();
   }

   public static void register() {
      populateEggMap();

      for (RegEntry<Item> egg : DRAGON_EGG_MAP.values()) {
         LegacyStacks.keepEntityTag(egg.id());
      }
   }
}
