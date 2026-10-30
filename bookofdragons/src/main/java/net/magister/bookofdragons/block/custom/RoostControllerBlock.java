package net.magister.bookofdragons.block.custom;

import org.jetbrains.annotations.Nullable;
import net.magister.bookofdragons.block.entity.ModBlockEntities;
import net.magister.bookofdragons.block.entity.RoostControllerBlockEntity;
import net.magister.liberanimalium.roost.RoostBlock;
import net.magister.liberanimalium.roost.RoostBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.BlockBehaviour.Properties;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

public class RoostControllerBlock extends RoostBlock {
   private static final VoxelShape SHAPE = Block.box(0.0, 0.0, 0.0, 16.0, 8.0, 16.0);

   public RoostControllerBlock(Properties properties) {
      super(properties);
   }

   protected BlockEntityType<? extends RoostBlockEntity> roostType() {
      return (BlockEntityType<? extends RoostBlockEntity>)ModBlockEntities.ROOST_CONTROLLER_ENTITY.get();
   }

   public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext ctx) {
      return SHAPE;
   }

   @Nullable
   public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
      return new RoostControllerBlockEntity(pos, state);
   }
}
