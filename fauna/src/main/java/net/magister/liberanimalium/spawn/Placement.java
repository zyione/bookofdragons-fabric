package net.magister.liberanimalium.spawn;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Random;
import org.jetbrains.annotations.Nullable;
import net.magister.liberanimalium.compat.Entities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.BlockPos.MutableBlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.SpawnPlacements;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap.Types;

public enum Placement {
   GROUND,
   AIR,
   CAVE,
   FLUID;

   @Nullable
   public static Placement parse(String name) {
      try {
         return valueOf(name.toUpperCase(Locale.ROOT));
      } catch (IllegalArgumentException e) {
         return null;
      }
   }

   @Nullable
   public BlockPos point(ServerLevel level, SpawnRule rule, BlockPos vanilla, int surface, Placement.Column column, RandomSource random) {
      EntityType<?> type = rule.type();

      return switch (this) {
         case GROUND -> {
            BlockPos top = groundTop(level, type, vanilla.getX(), vanilla.getZ());

            for (BlockPos near : ring(vanilla.getX(), vanilla.getZ())) {
               BlockPos at = groundTop(level, type, near.getX(), near.getZ());
               if (fits(level, type, at)) {
                  yield at;
               }
            }

            yield top;
         }
         case AIR -> {
            int ground = level.getHeight(Types.MOTION_BLOCKING_NO_LEAVES, vanilla.getX(), vanilla.getZ());
            int[] window = airWindow(rule.conditions(), ground, surface, level.getMaxBuildHeight() - 1 - Math.max(1, Mth.ceil(type.getHeight())));
            yield window[1] < window[0] ? null : new BlockPos(vanilla.getX(), Mth.randomBetweenInclusive(random, window[0], window[1]), vanilla.getZ());
         }
         case CAVE -> {
            List<BlockPos> floors = caveFloors(level, column, rule);
            if (floors.isEmpty()) {
               yield null;
            } else {
               Collections.shuffle(floors, new Random(random.nextLong()));

               for (BlockPos floor : floors.subList(0, Math.min(4, floors.size()))) {
                  for (BlockPos near : ring(floor.getX(), floor.getZ())) {
                     BlockPos at = new BlockPos(near.getX(), floor.getY(), near.getZ());
                     BlockPos below = at.below();
                     if (level.getBlockState(below).isFaceSturdy(level, below, Direction.UP) && fits(level, type, at)) {
                        yield at;
                     }
                  }
               }

               yield floors.get(0);
            }
         }
         case FLUID -> vanilla;
      };
   }

   private static BlockPos groundTop(ServerLevel level, EntityType<?> type, int x, int z) {
      return new BlockPos(x, level.getHeight(SpawnPlacements.getHeightmapType(type), x, z), z);
   }

   private static List<BlockPos> ring(int x, int z) {
      List<BlockPos> out = new ArrayList<>();

      for (int r = 0; r <= 2; r++) {
         for (int dx = -r; dx <= r; dx++) {
            for (int dz = -r; dz <= r; dz++) {
               if (Math.max(Math.abs(dx), Math.abs(dz)) == r) {
                  out.add(new BlockPos(x + dx, 0, z + dz));
               }
            }
         }
      }

      return out;
   }

   static boolean fits(ServerLevel level, EntityType<?> type, BlockPos pos) {
      return level.noCollision(type.getDimensions().makeBoundingBox(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5));
   }

   static int[] airWindow(List<SpawnCondition> conditions, int ground, int treetops, int ceiling) {
      double minAbove = 16.0;
      double maxAbove = 48.0;
      double minY = Double.NEGATIVE_INFINITY;
      double maxY = Double.POSITIVE_INFINITY;
      boolean aboveSet = false;

      for (SpawnCondition condition : conditions) {
         if (condition instanceof SpawnCondition.AboveGround above) {
            minAbove = aboveSet ? Math.max(minAbove, above.range().min()) : above.range().min();
            maxAbove = aboveSet ? Math.min(maxAbove, above.range().max()) : above.range().max();
            aboveSet = true;
         } else if (condition instanceof SpawnCondition.Y y) {
            minY = Math.max(minY, y.range().min());
            maxY = Math.min(maxY, y.range().max());
         }
      }

      double bottom = Math.max(Math.max(ground + minAbove, minY), treetops + 1);
      double top = Math.min(Math.min(ground + maxAbove, maxY), ceiling);
      return new int[]{(int)Math.ceil(bottom), (int)Math.floor(top)};
   }

   static List<BlockPos> caveFloors(ServerLevel level, Placement.Column column, SpawnRule rule) {
      int[] window = caveWindow(rule.conditions(), column.surface(level), level.getMinBuildHeight());
      int headroom = Math.max(1, Mth.ceil(rule.type().getHeight()));
      List<BlockPos> out = new ArrayList<>();

      for (int[] floor : column.floors(level)) {
         if (floor[0] >= window[0] && floor[0] <= window[1] && floor[1] >= headroom) {
            out.add(new BlockPos(column.x, floor[0], column.z));
         }
      }

      return out;
   }

   static int[] caveWindow(List<SpawnCondition> conditions, int surface, int minBuildHeight) {
      double minDepth = 12.0;
      double maxDepth = Double.POSITIVE_INFINITY;
      double minY = Double.NEGATIVE_INFINITY;
      double maxY = Double.POSITIVE_INFINITY;
      boolean depthSet = false;

      for (SpawnCondition condition : conditions) {
         if (condition instanceof SpawnCondition.BelowSurface below) {
            minDepth = depthSet ? Math.max(minDepth, below.range().min()) : below.range().min();
            maxDepth = Math.min(maxDepth, below.range().max());
            depthSet = true;
         } else if (condition instanceof SpawnCondition.Y y) {
            minY = Math.max(minY, y.range().min());
            maxY = Math.min(maxY, y.range().max());
         }
      }

      double top = Math.min(surface - minDepth, maxY);
      double bottom = Math.max(Math.max(surface - maxDepth, minY), minBuildHeight + 1);
      return new int[]{(int)Math.ceil(bottom), (int)Math.floor(Math.min(top, 2.147483647E9))};
   }

   @Nullable
   private static BlockPos caveFloor(ServerLevel level, BlockPos from) {
      for (int dy = 0; dy < 16; dy++) {
         BlockPos at = from.below(dy);
         if (at.getY() <= level.getMinBuildHeight()) {
            return null;
         }

         BlockPos below = at.below();
         if (level.getBlockState(at).isAir() && level.getBlockState(below).isFaceSturdy(level, below, Direction.UP)) {
            return at;
         }
      }

      return null;
   }

   @Nullable
   public BlockPos hop(ServerLevel level, EntityType<?> type, BlockPos hop) {
      return switch (this) {
         case GROUND -> new BlockPos(hop.getX(), level.getHeight(SpawnPlacements.getHeightmapType(type), hop.getX(), hop.getZ()), hop.getZ());
         case AIR, FLUID -> hop;
         case CAVE -> caveFloor(level, hop.above(2));
      };
   }

   @Nullable
   public String refusal(ServerLevel level, EntityType<?> type, BlockPos pos, RandomSource random) {
      if (!fits(level, type, pos)) {
         return "the body does not fit";
      }

      return switch (this) {
         case GROUND, CAVE -> !Entities.spawnPositionOk(type, level, pos)
            ? "no ground to stand on"
            : (
               !SpawnPlacements.checkSpawnRules(type, level, MobSpawnType.NATURAL, pos, random)
                  ? "its host's placement check refuses"
                  : (this == CAVE && level.canSeeSky(pos) ? "open to the sky" : null)
            );
         case AIR -> level.getFluidState(pos).isEmpty() ? null : "in a fluid";
         case FLUID -> Entities.spawnsInWater(level, pos, type) ? null : "not in water";
      };
   }

   public static final class Column {
      private final int x;
      private final int z;
      private int surface = Integer.MIN_VALUE;
      private List<int[]> floors;

      public Column(int x, int z) {
         this.x = x;
         this.z = z;
      }

      int surface(ServerLevel level) {
         if (this.surface == Integer.MIN_VALUE) {
            this.surface = level.getHeight(Types.MOTION_BLOCKING_NO_LEAVES, this.x, this.z);
         }

         return this.surface;
      }

      List<int[]> floors(ServerLevel level) {
         if (this.floors != null) {
            return this.floors;
         }

         this.floors = new ArrayList<>();
         MutableBlockPos at = new MutableBlockPos();
         int air = 0;

         for (int y = Math.min(this.surface(level), level.getMaxBuildHeight() - 1); y > level.getMinBuildHeight(); y--) {
            at.set(this.x, y, this.z);
            BlockState state = level.getBlockState(at);
            if (state.isAir()) {
               air++;
            } else {
               if (air > 0 && state.isFaceSturdy(level, at, Direction.UP)) {
                  this.floors.add(new int[]{y + 1, air});
               }

               air = 0;
            }
         }

         return this.floors;
      }
   }
}
