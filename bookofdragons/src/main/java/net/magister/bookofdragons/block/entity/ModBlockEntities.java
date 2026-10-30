package net.magister.bookofdragons.block.entity;

import net.magister.bookofdragons.block.ModBlocks;
import net.magister.liberanimalium.platform.Services;
import net.magister.liberanimalium.platform.registry.RegEntry;
import net.magister.liberanimalium.platform.registry.Registrar;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.entity.BlockEntityType.Builder;

public class ModBlockEntities {
   public static final Registrar<BlockEntityType<?>> BLOCK_ENTITIES = Services.registries().create(Registries.BLOCK_ENTITY_TYPE, "bookofdragons");
   public static final RegEntry<BlockEntityType<RoostControllerBlockEntity>> ROOST_CONTROLLER_ENTITY = BLOCK_ENTITIES.register(
      "roost_controller_entity", () -> Builder.of(RoostControllerBlockEntity::new, new Block[]{(Block)ModBlocks.ROOST_CONTROLLER.get()}).build(null)
   );

   public static void register() {
   }
}
