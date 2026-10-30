package net.magister.bookofdragons.worldgen;

import com.mojang.logging.LogUtils;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import org.jetbrains.annotations.Nullable;
import net.magister.bookofdragons.block.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureProcessor;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureProcessorType;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate.StructureBlockInfo;
import org.slf4j.Logger;

public class DragonSpawnProcessor extends StructureProcessor {
   private static final Logger LOGGER = LogUtils.getLogger();
   public static final Codec<DragonSpawnProcessor> CODEC = RecordCodecBuilder.create(
      instance -> instance.group(Codec.STRING.optionalFieldOf("archetype", "cliff").forGetter(p -> p.archetype)).apply(instance, DragonSpawnProcessor::new)
   );
   private final String archetype;

   public DragonSpawnProcessor(String archetype) {
      this.archetype = archetype;
   }

   public DragonSpawnProcessor() {
      this("cliff");
   }

   @Nullable
   public StructureBlockInfo processBlock(
      LevelReader level, BlockPos pos, BlockPos pivot, StructureBlockInfo ignored, StructureBlockInfo blockInfo, StructurePlaceSettings settings
   ) {
      BlockState state = blockInfo.state();
      if (state.is(Blocks.WHITE_WOOL)) {
         LOGGER.debug("=== ROOST CONTROLLER PLACEMENT === Archetype: {} at worldPos: {}", this.archetype, blockInfo.pos());
         BlockState controllerState = ((Block)ModBlocks.ROOST_CONTROLLER.get()).defaultBlockState();
         CompoundTag beTag = new CompoundTag();
         beTag.putString("Archetype", this.archetype);
         beTag.putString("id", "bookofdragons:roost_controller_entity");
         LOGGER.debug("Successfully returned RoostControllerBlock for placement at {}", blockInfo.pos());
         return new StructureBlockInfo(blockInfo.pos(), controllerState, beTag);
      } else {
         return blockInfo;
      }
   }

   protected StructureProcessorType<?> getType() {
      return (StructureProcessorType<?>)ModStructureProcessors.DRAGON_SPAWN.get();
   }
}
