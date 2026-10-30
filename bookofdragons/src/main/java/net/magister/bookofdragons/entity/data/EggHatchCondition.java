package net.magister.bookofdragons.entity.data;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.material.Fluids;

public enum EggHatchCondition {
   LIGHT_LEVEL {
      @Override
      public boolean isMet(Level level, BlockPos pos, int parameter) {
         int lightLevel = level.getBrightness(LightLayer.BLOCK, pos);
         return lightLevel >= parameter;
      }

      @Override
      public Component getDescription(int parameter) {
         return Component.translatable("entity.bookofdragons.dragon_egg.condition.light_level", new Object[]{parameter});
      }
   },
   IN_WATER {
      @Override
      public boolean isMet(Level level, BlockPos pos, int parameter) {
         BlockState state = level.getBlockState(pos);
         return state.hasProperty(BlockStateProperties.WATERLOGGED) && state.getValue(BlockStateProperties.WATERLOGGED)
            ? true
            : level.getFluidState(pos).getType() == Fluids.WATER || state.getFluidState().getType() == Fluids.WATER;
      }

      @Override
      public Component getDescription(int parameter) {
         return Component.translatable("entity.bookofdragons.dragon_egg.condition.in_water");
      }
   },
   IN_LAVA {
      @Override
      public boolean isMet(Level level, BlockPos pos, int parameter) {
         BlockState state = level.getBlockState(pos);
         return level.getFluidState(pos).getType() == Fluids.LAVA || state.getFluidState().getType() == Fluids.LAVA;
      }

      @Override
      public Component getDescription(int parameter) {
         return Component.translatable("entity.bookofdragons.dragon_egg.condition.in_lava");
      }
   },
   ON_FIRE {
      @Override
      public boolean isMet(Level level, BlockPos pos, int parameter) {
         BlockState state = level.getBlockState(pos);
         return state.is(Blocks.FIRE) || state.is(Blocks.SOUL_FIRE);
      }

      @Override
      public Component getDescription(int parameter) {
         return Component.translatable("entity.bookofdragons.dragon_egg.condition.on_fire");
      }
   },
   DARKNESS {
      @Override
      public boolean isMet(Level level, BlockPos pos, int parameter) {
         int combinedLight = level.getMaxLocalRawBrightness(pos);
         return combinedLight <= parameter;
      }

      @Override
      public Component getDescription(int parameter) {
         return Component.translatable("entity.bookofdragons.dragon_egg.condition.darkness", new Object[]{parameter});
      }
   };

   public abstract boolean isMet(Level var1, BlockPos var2, int var3);

   public abstract Component getDescription(int var1);
}
