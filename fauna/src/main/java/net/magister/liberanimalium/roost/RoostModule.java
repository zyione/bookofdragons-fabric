package net.magister.liberanimalium.roost;

import java.util.UUID;
import net.magister.liberanimalium.chassis.FaunaBase;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

public interface RoostModule {
   String id();

   default void save(CompoundTag tag) {
   }

   default void load(CompoundTag tag) {
   }

   default boolean accepts(RoostBlockEntity roost, FaunaBase claimant) {
      return true;
   }

   default boolean describes(String word) {
      return false;
   }

   default int slots(RoostBlockEntity roost, int slots) {
      return slots;
   }

   default void onClaim(RoostBlockEntity roost, ServerLevel level, FaunaBase claimant) {
   }

   default void onRelease(RoostBlockEntity roost, ServerLevel level, UUID occupant) {
   }

   default void onForfeit(RoostBlockEntity roost, ServerLevel level, UUID loser, FaunaBase winner) {
   }

   default void onBlockAdded(RoostBlockEntity roost, ServerLevel level, BlockPos pos) {
   }

   default void onBlockRemoved(RoostBlockEntity roost, ServerLevel level, BlockPos pos) {
   }

   default void onHeartbeat(RoostBlockEntity roost, ServerLevel level) {
   }

   default InteractionResult use(RoostBlockEntity roost, Player player, InteractionHand hand, ItemStack stack) {
      return InteractionResult.PASS;
   }
}
