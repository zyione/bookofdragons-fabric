package net.magister.bookofdragons.block.roost;

import com.mojang.logging.LogUtils;
import java.util.ArrayDeque;
import java.util.HashSet;
import java.util.Queue;
import java.util.Set;
import net.magister.liberanimalium.roost.RoostBlockEntity;
import net.magister.liberanimalium.roost.RoostModule;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import org.slf4j.Logger;

public class StructureFootprintModule implements RoostModule {
   private static final Logger LOGGER = LogUtils.getLogger();
   private static final int RADIUS = 32;
   private boolean scanned;

   public String id() {
      return "footprint";
   }

   public void onHeartbeat(RoostBlockEntity roost, ServerLevel level) {
      if (!this.scanned && !roost.placedByPlayer()) {
         this.scanned = true;
         if (!roost.hasFootprint()) {
            BlockPos origin = roost.getBlockPos();
            BlockPos min = origin;
            BlockPos max = origin;
            Set<BlockPos> visited = new HashSet<>();
            Queue<BlockPos> queue = new ArrayDeque<>();
            queue.add(origin);

            while (!queue.isEmpty()) {
               BlockPos current = queue.poll();
               if (visited.add(current) && current.distManhattan(origin) <= 32) {
                  BlockState state = level.getBlockState(current);
                  if (!state.isAir() && !isNatural(state)) {
                     min = new BlockPos(
                        Math.min(min.getX(), current.getX()),
                        Math.min(min.getY(), current.getY()),
                        Math.min(min.getZ(), current.getZ())
                     );
                     max = new BlockPos(
                        Math.max(max.getX(), current.getX()),
                        Math.max(max.getY(), current.getY()),
                        Math.max(max.getZ(), current.getZ())
                     );

                     for (Direction dir : Direction.values()) {
                        queue.add(current.relative(dir));
                     }
                  }
               }
            }

            roost.setFootprint(min, max);
            LOGGER.debug("Roost at {} scanned: footprint {} to {}", new Object[]{origin, min, max});
         }
      }
   }

   private static boolean isNatural(BlockState state) {
      return state.is(BlockTags.DIRT)
         || state.is(BlockTags.BASE_STONE_OVERWORLD)
         || state.is(Blocks.GRASS_BLOCK)
         || state.is(Blocks.SAND)
         || state.is(Blocks.GRAVEL)
         || state.is(BlockTags.LEAVES)
         || state.is(BlockTags.LOGS)
         || state.is(Blocks.WATER)
         || state.is(Blocks.LAVA);
   }

   public void save(CompoundTag tag) {
      tag.putBoolean("Scanned", this.scanned);
   }

   public void load(CompoundTag tag) {
      this.scanned = tag.getBoolean("Scanned");
   }
}
