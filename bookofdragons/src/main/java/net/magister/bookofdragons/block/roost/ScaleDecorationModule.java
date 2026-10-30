package net.magister.bookofdragons.block.roost;

import net.magister.bookofdragons.block.ModBlocks;
import net.magister.bookofdragons.block.custom.PlaceableDragonScaleBlock;
import net.magister.bookofdragons.entity.base.dragon.DragonBase;
import net.magister.bookofdragons.entity.data.DragonType;
import net.magister.liberanimalium.chassis.FaunaBase;
import net.magister.liberanimalium.roost.RoostBlockEntity;
import net.magister.liberanimalium.roost.RoostModule;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Direction.Plane;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

public class ScaleDecorationModule implements RoostModule {
   public String id() {
      return "scales";
   }

   public void onClaim(RoostBlockEntity roost, ServerLevel level, FaunaBase claimant) {
      if (claimant instanceof DragonBase dragon) {
         Block scales = scalesOf(dragon.getDragonType());
         if (scales != null) {
            BlockPos origin = roost.getBlockPos();
            int clusters = 10 + level.random.nextInt(11);

            for (int i = 0; i < clusters; i++) {
               BlockPos target = origin.offset(level.random.nextInt(31) - 15, 0, level.random.nextInt(31) - 15);
               BlockPos floor = floorNear(level, target, origin.getY());
               if (floor != null) {
                  BlockState state = (BlockState)((BlockState)scales.defaultBlockState().setValue(PlaceableDragonScaleBlock.AMOUNT, 1 + level.random.nextInt(4)))
                     .setValue(PlaceableDragonScaleBlock.FACING, Plane.HORIZONTAL.getRandomDirection(level.random));
                  level.setBlock(floor, state, 3);
               }
            }
         }
      }
   }

   private static BlockPos floorNear(ServerLevel level, BlockPos column, int roostY) {
      for (int dy = 6; dy >= -6; dy--) {
         BlockPos at = column.atY(roostY + dy);
         BlockPos below = at.below();
         if (level.getBlockState(at).isAir() && level.getBlockState(below).isFaceSturdy(level, below, Direction.UP)) {
            return at;
         }
      }

      return null;
   }

   private static Block scalesOf(DragonType type) {
      return switch (type) {
         case NIGHTFURY -> (Block)ModBlocks.PLACEABLE_NIGHTFURY_SCALES.get();
         case DEADLY_NADDER -> (Block)ModBlocks.PLACEABLE_NADDER_SCALES.get();
         case MONSTROUS_NIGHTMARE -> (Block)ModBlocks.PLACEABLE_NIGHTMARE_SCALES.get();
         case GRONCKLE -> (Block)ModBlocks.PLACEABLE_GRONCKLE_SCALES.get();
         case HIDEOUS_ZIPPLEBACK -> (Block)ModBlocks.PLACEABLE_ZIPPLEBACK_SCALES.get();
         case SKRILL -> (Block)ModBlocks.PLACEABLE_SKRILL_SCALES.get();
         case WHISPERING_DEATH -> (Block)ModBlocks.PLACEABLE_WHISPERINGDEATH_SCALES.get();
         case TERRIBLE_TERROR -> (Block)ModBlocks.PLACEABLE_TERRIBLETERROR_SCALES.get();
         case SPEED_STINGER -> (Block)ModBlocks.PLACEABLE_SPEEDSTINGER_SCALES.get();
         default -> null;
      };
   }
}
