package net.magister.liberanimalium.render.waves;

import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap.Entry;
import it.unimi.dsi.fastutil.objects.ObjectIterator;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Random;
import net.magister.liberanimalium.anim.rig.BlockChanges;
import net.magister.liberanimalium.render.vfx.WaterSpray;
import net.magister.liberanimalium.render.waves.simulation.WaveSimulationChunk;
import net.magister.liberanimalium.util.PerfClock;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.BlockPos.MutableBlockPos;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.Mth;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.phys.Vec3;

public class FaunaWaveManager {
   private static final FaunaWaveManager INSTANCE = new FaunaWaveManager();
   private final Long2ObjectMap<WaveSimulationChunk> activeChunks = new Long2ObjectOpenHashMap();
   private final List<WaveSimulationChunk> renderList = new ArrayList<>();
   private final MutableBlockPos probe = new MutableBlockPos();
   private static final PerfClock.Section CLOCK = PerfClock.client("waves.step");
   private static final float CREST = 0.6F;
   private static final float CREST_CHANCE = 0.04F;
   private static final int CREST_BUDGET = 16;
   private static final int CREST_RANGE_CHUNKS = 3;
   private static final float RIDGE = 0.9F;
   private static final double RIDGE_STEP = 0.25;
   private static final float BREAKS_FROM = 0.6F;
   private static final float BREAK_WHOLE = 0.35F;
   private final Random random = new Random();

   private FaunaWaveManager() {
   }

   public static FaunaWaveManager getInstance() {
      return INSTANCE;
   }

   public static long key(int chunkX, int y, int chunkZ) {
      return BlockPos.asLong(chunkX, y, chunkZ);
   }

   public static long key(WaveSimulationChunk chunk) {
      return key(chunk.x, chunk.y, chunk.z);
   }

   public boolean hasChunk(long key) {
      return this.activeChunks.containsKey(key);
   }

   public void tick() {
      Minecraft mc = Minecraft.getInstance();
      if (!mc.isPaused() && !this.activeChunks.isEmpty()) {
         long clock = CLOCK.start();

         try {
            this.step(mc.level);
         } finally {
            CLOCK.end(clock);
         }
      }
   }

   private void step(ClientLevel level) {
      List<WaveSimulationChunk> chunksToCreate = new ArrayList<>();
      int spray = 16;
      Iterator<Entry<WaveSimulationChunk>> it = this.activeChunks.long2ObjectEntrySet().iterator();

      while (it.hasNext()) {
         Entry<WaveSimulationChunk> entry = it.next();
         WaveSimulationChunk chunk = (WaveSimulationChunk)entry.getValue();
         if (!chunk.isActive) {
            it.remove();
         } else {
            if (!chunk.isDormant) {
               if (level != null && waterChanged(chunk)) {
                  this.readWater(chunk, level);
               }

               if (!chunk.isDormant) {
                  this.syncBoundaries(chunk);
               }
            }

            chunk.tick();
            if (!chunk.isDormant && spray > 0) {
               spray = this.sprayCrests(chunk, spray);
            }

            if (!chunk.isDormant) {
               this.collectNeighboringChunks(chunk, chunksToCreate);
            }
         }
      }

      for (WaveSimulationChunk newChunk : chunksToCreate) {
         long chunkKey = key(newChunk);
         if (!this.activeChunks.containsKey(chunkKey)) {
            if (level != null) {
               this.readWater(newChunk, level);
            }

            this.activeChunks.put(chunkKey, newChunk);
         }
      }

      ObjectIterator var10 = this.activeChunks.values().iterator();

      while (var10.hasNext()) {
         WaveSimulationChunk chunk = (WaveSimulationChunk)var10.next();
         if (!chunk.isDormant) {
            this.syncBoundaries(chunk);
         }
      }
   }

   private static boolean waterChanged(WaveSimulationChunk chunk) {
      int x0 = chunk.x << 4;
      int z0 = chunk.z << 4;
      return BlockChanges.changedSince(chunk.maskMark, x0 - 1, chunk.y, z0 - 1, x0 + 16, chunk.y + 1, z0 + 16);
   }

   private void readWater(WaveSimulationChunk chunk, ClientLevel level) {
      int side = 18;
      boolean[] blocks = new boolean[side * side];
      int x0 = (chunk.x << 4) - 1;
      int z0 = (chunk.z << 4) - 1;

      for (int bz = 0; bz < side; bz++) {
         for (int bx = 0; bx < side; bx++) {
            this.probe.set(x0 + bx, chunk.y, z0 + bz);
            FluidState here = level.getFluidState(this.probe);
            boolean open = here.is(FluidTags.WATER) && here.isSource();
            if (open) {
               this.probe.setY(chunk.y + 1);
               open = !level.getFluidState(this.probe).is(FluidTags.WATER);
            }

            blocks[bz * side + bx] = open;
         }
      }

      chunk.maskMark = BlockChanges.mark();
      chunk.setWater(blocks);
   }

   private int sprayCrests(WaveSimulationChunk chunk, int budget) {
      Minecraft mc = Minecraft.getInstance();
      ClientLevel level = mc.level;
      if (level == null) {
         return budget;
      }

      Vec3 camera = mc.gameRenderer.getMainCamera().getPosition();
      if (Math.abs(chunk.x - (Mth.floor(camera.x) >> 4)) <= 3 && Math.abs(chunk.z - (Mth.floor(camera.z) >> 4)) <= 3) {
         int size = 64;

         for (int j = 1; j <= size && budget > 0; j += 2) {
            for (int i = 1; i <= size && budget > 0; i += 2) {
               float h = chunk.getSmoothedHeight(i, j);
               if (!(h < 0.6F)) {
                  float rise = chunk.getRise(i, j);
                  if (!(rise <= 0.0F)) {
                     float chance = 0.04F * Math.min(1.0F, 0.25F + (h - 0.6F) / 0.6F);
                     if (!(level.random.nextFloat() >= chance)) {
                        double x = chunk.x * 16 + (i - 0.5) / 4.0;
                        double z = chunk.z * 16 + (j - 0.5) / 4.0;
                        WaterSpray.crest(level, x, chunk.y + 0.89088887F, z, rise);
                        budget--;
                     }
                  }
               }
            }
         }

         return budget;
      } else {
         return budget;
      }
   }

   private void collectNeighboringChunks(WaveSimulationChunk chunk, List<WaveSimulationChunk> chunksToCreate) {
      int size = 64;
      float threshold = 0.04F;
      if (this.checkBoundaryEnergy(chunk, 1, size, threshold, false, 1)) {
         this.wakeOrQueueNeighbor(chunk.x, chunk.z - 1, chunk.y, chunksToCreate);
      }

      if (this.checkBoundaryEnergy(chunk, 1, size, threshold, false, size)) {
         this.wakeOrQueueNeighbor(chunk.x, chunk.z + 1, chunk.y, chunksToCreate);
      }

      if (this.checkBoundaryEnergy(chunk, 1, size, threshold, true, 1)) {
         this.wakeOrQueueNeighbor(chunk.x - 1, chunk.z, chunk.y, chunksToCreate);
      }

      if (this.checkBoundaryEnergy(chunk, 1, size, threshold, true, size)) {
         this.wakeOrQueueNeighbor(chunk.x + 1, chunk.z, chunk.y, chunksToCreate);
      }
   }

   private boolean checkBoundaryEnergy(WaveSimulationChunk chunk, int start, int end, float threshold, boolean isVerticalCol, int fixedCoord) {
      if (isVerticalCol) {
         for (int z = start; z <= end; z++) {
            if (Math.abs(chunk.getHeight(fixedCoord, z)) > threshold) {
               return true;
            }
         }
      } else {
         for (int x = start; x <= end; x++) {
            if (Math.abs(chunk.getHeight(x, fixedCoord)) > threshold) {
               return true;
            }
         }
      }

      return false;
   }

   private void wakeOrQueueNeighbor(int chunkX, int chunkZ, int chunkY, List<WaveSimulationChunk> chunksToCreate) {
      WaveSimulationChunk existing = (WaveSimulationChunk)this.activeChunks.get(key(chunkX, chunkY, chunkZ));
      if (existing != null) {
         if (existing.isDormant) {
            existing.wakeUp();
         }
      } else {
         WaveSimulationChunk newChunk = new WaveSimulationChunk(chunkX, chunkZ, chunkY);
         chunksToCreate.add(newChunk);
      }
   }

   private WaveSimulationChunk neighbour(WaveSimulationChunk chunk, int dx, int dz) {
      WaveSimulationChunk other = (WaveSimulationChunk)this.activeChunks.get(key(chunk.x + dx, chunk.y, chunk.z + dz));
      return other != null && !other.dry ? other : null;
   }

   private void syncBoundaries(WaveSimulationChunk chunk) {
      int size = 64;
      WaveSimulationChunk north = this.neighbour(chunk, 0, -1);
      if (north != null) {
         for (int x = 1; x <= size; x++) {
            chunk.setBoundary(x, 0, north.getHeight(x, size));
         }
      }

      WaveSimulationChunk south = this.neighbour(chunk, 0, 1);
      if (south != null) {
         for (int x = 1; x <= size; x++) {
            chunk.setBoundary(x, size + 1, south.getHeight(x, 1));
         }
      }

      WaveSimulationChunk west = this.neighbour(chunk, -1, 0);
      if (west != null) {
         for (int z = 1; z <= size; z++) {
            chunk.setBoundary(0, z, west.getHeight(size, z));
         }
      }

      WaveSimulationChunk east = this.neighbour(chunk, 1, 0);
      if (east != null) {
         for (int z = 1; z <= size; z++) {
            chunk.setBoundary(size + 1, z, east.getHeight(1, z));
         }
      }

      WaveSimulationChunk nw = this.neighbour(chunk, -1, -1);
      if (nw != null) {
         chunk.setBoundary(0, 0, nw.getHeight(size, size));
      }

      WaveSimulationChunk ne = this.neighbour(chunk, 1, -1);
      if (ne != null) {
         chunk.setBoundary(size + 1, 0, ne.getHeight(1, size));
      }

      WaveSimulationChunk sw = this.neighbour(chunk, -1, 1);
      if (sw != null) {
         chunk.setBoundary(0, size + 1, sw.getHeight(size, 1));
      }

      WaveSimulationChunk se = this.neighbour(chunk, 1, 1);
      if (se != null) {
         chunk.setBoundary(size + 1, size + 1, se.getHeight(1, 1));
      }
   }

   private WaveSimulationChunk getOrCreateChunk(int chunkX, int chunkZ, int chunkY) {
      long chunkKey = key(chunkX, chunkY, chunkZ);
      WaveSimulationChunk chunk = (WaveSimulationChunk)this.activeChunks.get(chunkKey);
      if (chunk == null) {
         chunk = new WaveSimulationChunk(chunkX, chunkZ, chunkY);
         ClientLevel level = Minecraft.getInstance().level;
         if (level != null) {
            this.readWater(chunk, level);
         }

         this.activeChunks.put(chunkKey, chunk);
      }

      return chunk;
   }

   public void addWakeSlice(Vec3 from, Vec3 to, float widthBlocks, float depth, double speed) {
      double dist = from.distanceTo(to);
      if (!(dist > 32.0) && !(dist < 0.01)) {
         Vec3 dir = to.subtract(from).normalize();
         Vec3 perp = new Vec3(-dir.z, 0.0, dir.x);
         int steps = Math.max(1, (int)Math.ceil(dist * 4.0));
         Vec3 stepVec = to.subtract(from).scale(1.0 / steps);
         float halfWidth = widthBlocks * 0.5F;
         float speedMul = Math.min(2.0F, (float)speed * 0.8F);
         float troughDepth = depth * speedMul;
         float bermHeight = depth * speedMul * 0.9F;

         for (int s = 0; s <= steps; s++) {
            Vec3 center = from.add(stepVec.scale(s));
            this.addWaveForce(center.x, center.y, center.z, -troughDepth);
            if (halfWidth > 0.3F) {
               for (int side = -1; side <= 1; side += 2) {
                  float thrown = bermHeight * (0.5F + this.random.nextFloat());
                  double out = side * (halfWidth + (this.random.nextFloat() - 0.5F) * 0.2F);
                  this.addWaveForce(center.x + perp.x * out, center.y, center.z + perp.z * out, thrown);
                  out += side * 0.25;
                  this.addWaveForce(center.x + perp.x * out, center.y, center.z + perp.z * out, thrown * 0.6F);
               }
            }
         }
      }
   }

   public void addRadialForce(double x, double y, double z, float radiusBlocks, float force) {
      if (Math.abs(force) < 0.6F) {
         this.stampRadial(x, y, z, radiusBlocks, force);
      } else {
         this.stampRadial(x, y, z, radiusBlocks, force * 0.35F);
         int lumps = Mth.clamp(Math.round(radiusBlocks * 6.0F), 5, 14);

         for (int k = 0; k < lumps; k++) {
            double angle = this.random.nextDouble() * Math.PI * 2.0;
            double out = Math.sqrt(this.random.nextDouble()) * radiusBlocks * 1.1;
            float size = Math.max(0.3F, radiusBlocks * (0.2F + this.random.nextFloat() * 0.25F));
            float thrown = force * (0.3F + this.random.nextFloat() * 0.5F) * (this.random.nextFloat() < 0.25F ? -1.0F : 1.0F);
            this.stampRadial(x + Math.cos(angle) * out, y, z + Math.sin(angle) * out, size, thrown);
         }
      }
   }

   private void stampRadial(double x, double y, double z, float radiusBlocks, float force) {
      int blockX = (int)Math.floor(x);
      int blockY = (int)Math.floor(y);
      int blockZ = (int)Math.floor(z);
      int chunkX = blockX >> 4;
      int chunkZ = blockZ >> 4;
      WaveSimulationChunk chunk = this.getOrCreateChunk(chunkX, chunkZ, blockY);
      float pixelRadius = radiusBlocks * 4.0F;
      chunk.addRadialImpulse(local(x, chunkX), local(z, chunkZ), pixelRadius, force);
   }

   public void addWaveForce(double x, double y, double z, float force) {
      int blockX = (int)Math.floor(x);
      int blockY = (int)Math.floor(y);
      int blockZ = (int)Math.floor(z);
      int chunkX = blockX >> 4;
      int chunkZ = blockZ >> 4;
      WaveSimulationChunk chunk = this.getOrCreateChunk(chunkX, chunkZ, blockY);
      chunk.addImpulse(local(x, chunkX), local(z, chunkZ), force);
   }

   static int local(double coordinate, int chunk) {
      return (int)((coordinate - chunk * 16) * 4.0) + 1;
   }

   public List<WaveSimulationChunk> getVisibleChunks(Camera camera) {
      this.renderList.clear();
      BlockPos camPos = camera.getEntity().blockPosition();
      int camChunkX = camPos.getX() >> 4;
      int camChunkZ = camPos.getZ() >> 4;
      ObjectIterator var5 = this.activeChunks.values().iterator();

      while (var5.hasNext()) {
         WaveSimulationChunk chunk = (WaveSimulationChunk)var5.next();
         if (Math.abs(chunk.x - camChunkX) <= 6 && Math.abs(chunk.z - camChunkZ) <= 6) {
            this.renderList.add(chunk);
         }
      }

      return this.renderList;
   }

   public void reset() {
      this.activeChunks.clear();
   }
}
