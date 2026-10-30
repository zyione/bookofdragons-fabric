package net.magister.liberanimalium.render.vfx;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;

public enum WingWashGround {
   GRASS(0.67F, 0.75F, 0.75F, 0, 0.2F, 0.18F, 0.06F, 0.08F),
   SOIL(0.67F, 0.75F, 0.0F, -1, 0.0F, 0.0F, 0.0F, 0.0F),
   SAND(1.0F, 0.8F, 1.33F, 6, 0.26F, 0.2F, 0.04F, 0.07F),
   STONE(0.42F, 0.5F, 0.58F, 12, 0.22F, 0.18F, 0.05F, 0.08F),
   LEAVES(0.0F, 0.0F, 0.92F, 18, 0.18F, 0.14F, 0.04F, 0.06F),
   SNOW(1.0F, 0.85F, 1.0F, 24, 0.2F, 0.18F, 0.06F, 0.1F),
   PINE(0.0F, 0.0F, 0.92F, 30, 0.18F, 0.14F, 0.04F, 0.06F),
   DUST(1.0F, 0.75F, 0.0F, -1, 0.0F, 0.0F, 0.0F, 0.0F);

   public static final int LAST_SPRITE = 35;
   public final float dustShare;
   public final float dustAlpha;
   public final float debrisShare;
   public final int debrisSprites;
   public final float outMin;
   public final float outSpan;
   public final float upMin;
   public final float upSpan;
   private static final String[] CONIFERS = new String[]{"spruce", "pine", "fir", "redwood", "larch", "cedar", "cypress", "hemlock"};

   WingWashGround(float dustShare, float dustAlpha, float debrisShare, int debrisSprites, float outMin, float outSpan, float upMin, float upSpan) {
      this.dustShare = dustShare;
      this.dustAlpha = dustAlpha;
      this.debrisShare = debrisShare;
      this.debrisSprites = debrisSprites;
      this.outMin = outMin;
      this.outSpan = outSpan;
      this.upMin = upMin;
      this.upSpan = upSpan;
   }

   public static WingWashGround of(BlockGetter level, BlockPos pos) {
      BlockState state = level.getBlockState(pos);
      if (!state.is(BlockTags.SNOW)
         && !level.getBlockState(pos.above()).is(BlockTags.SNOW)
         && (!state.hasProperty(BlockStateProperties.SNOWY) || !(Boolean)state.getValue(BlockStateProperties.SNOWY))) {
         if (state.is(BlockTags.LEAVES)) {
            return conifer(state) ? PINE : LEAVES;
         } else if (state.is(BlockTags.SAND)) {
            return SAND;
         } else if (state.is(Blocks.GRASS_BLOCK)) {
            return GRASS;
         } else if (state.is(BlockTags.DIRT)) {
            return SOIL;
         } else {
            return state.is(BlockTags.MINEABLE_WITH_PICKAXE) ? STONE : DUST;
         }
      } else {
         return SNOW;
      }
   }

   static boolean conifer(BlockState state) {
      return conifer(BuiltInRegistries.BLOCK.getKey(state.getBlock()).getPath());
   }

   static boolean conifer(String path) {
      for (String word : CONIFERS) {
         if (path.contains(word)) {
            return true;
         }
      }

      return false;
   }
}
