package net.magister.liberanimalium.anim.rig;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import net.minecraft.core.SectionPos;
import net.minecraft.core.BlockPos.MutableBlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.PowderSnowBlock;
import net.minecraft.world.level.block.ScaffoldingBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.shapes.BooleanOp;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

public final class BlockSampler {
   private static final int MAX_WINDOW_CELLS = 65536;
   private Level level;
   private CollisionContext context;
   private int x0;
   private int y0;
   private int z0;
   private int w;
   private int h;
   private int d;
   private byte[] read = new byte[0];
   private BlockState[] states = new BlockState[0];
   private VoxelShape[] shapes = new VoxelShape[0];
   private final MutableBlockPos pos = new MutableBlockPos();
   private BlockGetter chunk;
   private long chunkKey;
   private boolean sawDynamic;
   private boolean sawContextual;

   public BlockSampler begin(Entity entity, AABB window) {
      this.level = entity.level();
      this.context = CollisionContext.of(entity);
      this.chunk = null;
      this.w = this.h = this.d = 0;
      if (window != null) {
         int ax = Mth.floor(window.minX);
         int bx = Mth.floor(window.maxX);
         int ay = Mth.floor(window.minY);
         int by = Mth.floor(window.maxY);
         int az = Mth.floor(window.minZ);
         int bz = Mth.floor(window.maxZ);
         long cells = (long)(bx - ax + 1) * (by - ay + 1) * (bz - az + 1);
         if (cells > 0L && cells <= 65536L) {
            this.x0 = ax;
            this.y0 = ay;
            this.z0 = az;
            this.w = bx - ax + 1;
            this.h = by - ay + 1;
            this.d = bz - az + 1;
            int n = (int)cells;
            if (this.read.length < n) {
               this.read = new byte[n];
               this.states = new BlockState[n];
               this.shapes = new VoxelShape[n];
            } else {
               Arrays.fill(this.read, 0, n, (byte)0);
            }
         }
      }

      return this;
   }

   public List<AABB> gather(AABB region) {
      this.sawDynamic = false;
      this.sawContextual = false;
      List<AABB> boxes = new ArrayList<>();
      VoxelShape regionShape = null;
      int ax = Mth.floor(region.minX - 1.0E-7) - 1;
      int bx = Mth.floor(region.maxX + 1.0E-7) + 1;
      int ay = Mth.floor(region.minY - 1.0E-7) - 1;
      int by = Mth.floor(region.maxY + 1.0E-7) + 1;
      int az = Mth.floor(region.minZ - 1.0E-7) - 1;
      int bz = Mth.floor(region.maxZ + 1.0E-7) + 1;

      for (int z = az; z <= bz; z++) {
         for (int y = ay; y <= by; y++) {
            for (int x = ax; x <= bx; x++) {
               int type = (x != ax && x != bx ? 0 : 1) + (y != ay && y != by ? 0 : 1) + (z != az && z != bz ? 0 : 1);
               if (type != 3) {
                  int cell = this.cell(x, y, z);
                  BlockState state = this.state(cell, x, y, z);
                  if (state != null && (type != 1 || state.hasLargeCollisionShape()) && (type != 2 || state.is(Blocks.MOVING_PISTON))) {
                     if (state.getBlock().hasDynamicShape()) {
                        this.sawDynamic = true;
                     }

                     if (state.getBlock() instanceof ScaffoldingBlock || state.getBlock() instanceof PowderSnowBlock) {
                        this.sawContextual = true;
                     }

                     VoxelShape shape = this.shape(cell, x, y, z, state);
                     if (shape == Shapes.block()) {
                        if (region.intersects(x, y, z, x + 1.0, y + 1.0, z + 1.0)) {
                           boxes.add(new AABB(x, y, z, x + 1.0, y + 1.0, z + 1.0));
                        }
                     } else {
                        VoxelShape moved = shape.move(x, y, z);
                        if (!moved.isEmpty()) {
                           if (regionShape == null) {
                              regionShape = Shapes.create(region);
                           }

                           if (Shapes.joinIsNotEmpty(moved, regionShape, BooleanOp.AND)) {
                              boxes.addAll(moved.toAabbs());
                           }
                        }
                     }
                  }
               }
            }
         }
      }

      return boxes;
   }

   public boolean lastGatherVolatile() {
      return this.sawDynamic;
   }

   public boolean lastGatherContextual() {
      return this.sawContextual;
   }

   private int cell(int x, int y, int z) {
      int cx = x - this.x0;
      int cy = y - this.y0;
      int cz = z - this.z0;
      return this.w != 0 && cx >= 0 && cy >= 0 && cz >= 0 && cx < this.w && cy < this.h && cz < this.d ? (cz * this.h + cy) * this.w + cx : -1;
   }

   private BlockState state(int cell, int x, int y, int z) {
      if (cell >= 0 && this.read[cell] != 0) {
         return this.states[cell];
      }

      long key = ChunkPos.asLong(SectionPos.blockToSectionCoord(x), SectionPos.blockToSectionCoord(z));
      if (this.chunk == null || this.chunkKey != key) {
         this.chunk = this.level.getChunkForCollisions(SectionPos.blockToSectionCoord(x), SectionPos.blockToSectionCoord(z));
         this.chunkKey = key;
      }

      BlockState state = this.chunk == null ? null : this.chunk.getBlockState(this.pos.set(x, y, z));
      if (cell >= 0) {
         this.read[cell] = 1;
         this.states[cell] = state;
         this.shapes[cell] = null;
      }

      return state;
   }

   private VoxelShape shape(int cell, int x, int y, int z, BlockState state) {
      if (cell >= 0 && this.shapes[cell] != null) {
         return this.shapes[cell];
      }

      VoxelShape shape = state.getCollisionShape(this.level, this.pos.set(x, y, z), this.context);
      if (cell >= 0) {
         this.shapes[cell] = shape;
      }

      return shape;
   }
}
