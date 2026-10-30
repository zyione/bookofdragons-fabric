package net.magister.liberanimalium.roost;

import org.jetbrains.annotations.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.BlockBehaviour.Properties;
import net.minecraft.world.phys.BlockHitResult;

public abstract class RoostBlock extends BaseEntityBlock {
   protected RoostBlock(Properties properties) {
      super(properties);
   }

   protected abstract BlockEntityType<? extends RoostBlockEntity> roostType();

   protected int maxBlocks() {
      return 9;
   }

   public void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean movedByPiston) {
      super.onPlace(state, level, pos, oldState, movedByPiston);
      if (!oldState.is(this) && level instanceof ServerLevel server) {
         RoostNetwork.placed(server, pos, this, this.maxBlocks());
      }
   }

   public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
      if (!state.is(newState.getBlock()) && level instanceof ServerLevel server) {
         RoostNetwork.removing(server, pos);
      }

      super.onRemove(state, level, pos, newState, movedByPiston);
   }

   public RenderShape getRenderShape(BlockState state) {
      return RenderShape.MODEL;
   }

   @Nullable
   public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
      return !level.isClientSide() && type == this.roostType()
         ? (tickLevel, pos, tickState, entity) -> RoostBlockEntity.serverTick(tickLevel, pos, tickState, (RoostBlockEntity)entity)
         : null;
   }

   public void setPlacedBy(Level level, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack stack) {
      super.setPlacedBy(level, pos, state, placer, stack);
      if (!level.isClientSide() && placer instanceof Player player && level.getBlockEntity(pos) instanceof RoostBlockEntity roost) {
         roost.setPlacedBy(player.getUUID());
      }
   }

   protected InteractionResult useRoost(Level level, BlockPos pos, Player player, InteractionHand hand, ItemStack stack) {
      return level.getBlockEntity(pos) instanceof RoostBlockEntity roost ? roost.use(player, hand, stack) : InteractionResult.PASS;
   }

   public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
      return this.useRoost(level, pos, player, hand, player.getItemInHand(hand));
   }
}
