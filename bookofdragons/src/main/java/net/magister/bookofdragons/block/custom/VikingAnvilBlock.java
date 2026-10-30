package net.magister.bookofdragons.block.custom;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.BlockBehaviour.Properties;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

public class VikingAnvilBlock extends Block {
   private static final VoxelShape SHAPE = Shapes.or(
      Block.box(0.0, 0.0, 0.0, 16.0, 4.0, 16.0),
      new VoxelShape[]{Block.box(2.0, 4.0, 2.0, 14.0, 12.0, 14.0), Block.box(0.0, 12.0, 0.0, 16.0, 16.0, 16.0)}
   );

   public VikingAnvilBlock(Properties properties) {
      super(properties);
   }

   public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
      return SHAPE;
   }
}
