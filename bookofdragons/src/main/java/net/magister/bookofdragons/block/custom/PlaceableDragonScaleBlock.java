package net.magister.bookofdragons.block.custom;

import java.util.function.Supplier;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Direction.Plane;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.BlockBehaviour.Properties;
import net.minecraft.world.level.block.state.StateDefinition.Builder;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

public class PlaceableDragonScaleBlock extends Block {
   public static final IntegerProperty AMOUNT = IntegerProperty.create("amount", 1, 4);
   public static final DirectionProperty FACING = BlockStateProperties.HORIZONTAL_FACING;
   private static final VoxelShape SHAPE = Block.box(0.0, 0.0, 0.0, 16.0, 1.0, 16.0);
   private final Supplier<Item> scaleItem;

   public PlaceableDragonScaleBlock(Properties properties, Supplier<Item> scaleItem) {
      super(properties);
      this.scaleItem = scaleItem;
      this.registerDefaultState((BlockState)((BlockState)((BlockState)this.stateDefinition.any()).setValue(AMOUNT, 1)).setValue(FACING, Direction.NORTH));
   }

   protected void createBlockStateDefinition(Builder<Block, BlockState> builder) {
      builder.add(new Property[]{AMOUNT, FACING});
   }

   public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
      return SHAPE;
   }

   public BlockState getStateForPlacement(BlockPlaceContext context) {
      BlockState existingState = context.getLevel().getBlockState(context.getClickedPos());
      if (existingState.is(this)) {
         int currentAmount = (Integer)existingState.getValue(AMOUNT);
         if (currentAmount < 4) {
            return (BlockState)existingState.setValue(AMOUNT, currentAmount + 1);
         }
      }

      Direction randomFacing = Plane.HORIZONTAL.getRandomDirection(context.getLevel().getRandom());
      return (BlockState)((BlockState)this.defaultBlockState().setValue(AMOUNT, 1)).setValue(FACING, randomFacing);
   }

   public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
      ItemStack heldItem = player.getItemInHand(hand);
      if (heldItem.is(this.scaleItem.get())) {
         int currentAmount = (Integer)state.getValue(AMOUNT);
         if (currentAmount < 4) {
            if (!level.isClientSide()) {
               level.setBlock(pos, (BlockState)state.setValue(AMOUNT, currentAmount + 1), 3);
               if (!player.isCreative()) {
                  heldItem.shrink(1);
               }
            }

            return InteractionResult.sidedSuccess(level.isClientSide());
         }
      }

      return InteractionResult.PASS;
   }

   public boolean canBeReplaced(BlockState state, BlockPlaceContext context) {
      if (context.getItemInHand().is(this.scaleItem.get())) {
         int currentAmount = (Integer)state.getValue(AMOUNT);
         return currentAmount < 4;
      } else {
         return false;
      }
   }

   public boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
      BlockPos below = pos.below();
      BlockState belowState = level.getBlockState(below);
      return belowState.isFaceSturdy(level, below, Direction.UP);
   }

   public BlockState updateShape(BlockState state, Direction direction, BlockState neighborState, LevelAccessor level, BlockPos currentPos, BlockPos neighborPos) {
      return !state.canSurvive(level, currentPos) ? Blocks.AIR.defaultBlockState() : super.updateShape(state, direction, neighborState, level, currentPos, neighborPos);
   }
}
