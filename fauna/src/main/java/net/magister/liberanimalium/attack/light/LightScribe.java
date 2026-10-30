package net.magister.liberanimalium.attack.light;

import com.mojang.logging.LogUtils;
import it.unimi.dsi.fastutil.longs.LongArrayList;
import it.unimi.dsi.fastutil.longs.LongList;
import it.unimi.dsi.fastutil.longs.LongListIterator;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.magister.liberanimalium.attack.data.LightConfig;
import net.minecraft.core.BlockPos;
import net.minecraft.core.BlockPos.MutableBlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LightBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.phys.Vec3;
import org.slf4j.Logger;

public final class LightScribe {
   private static final Logger LOGGER = LogUtils.getLogger();
   public static final int CAPACITY_PER_LEVEL = 256;
   private static final Map<ResourceKey<Level>, LightScribe> BY_LEVEL = new HashMap<>();
   private final LightLedger ledger = new LightLedger(256);
   private final LongList pending = new LongArrayList();

   private LightScribe() {
   }

   public static LightScribe of(ServerLevel level) {
      return BY_LEVEL.computeIfAbsent(level.dimension(), k -> new LightScribe());
   }

   public void illuminate(ServerLevel level, Vec3 at, int owner, LightConfig config, float intensity) {
      this.illuminate(level, at, owner, owner, config, intensity);
   }

   public void illuminate(ServerLevel level, Vec3 at, int owner, int source, LightConfig config, float intensity) {
      int lightLevel = config.levelAt(intensity);
      if (lightLevel > 0) {
         BlockPos pos = BlockPos.containing(at);
         if (level.isLoaded(pos)) {
            int expiry = (int)level.getGameTime() + config.ttlTicks();
            LightLedger.Verdict verdict = this.ledger.request(pos.asLong(), owner, source, lightLevel, expiry, config.maxPoints(), config.spacing());
            LightLedger.Entry entry = this.ledger.get(pos.asLong());
            if (entry != null) {
               if (verdict == LightLedger.Verdict.PLACED) {
                  BlockState there = level.getBlockState(pos);
                  if (!there.isAir()) {
                     this.ledger.remove(pos.asLong());
                     return;
                  }

                  entry.replaced = there;
                  LightScribeData.of(level).add(pos.asLong());
                  level.setBlock(pos, lit(lightLevel), 2);
               } else if (verdict == LightLedger.Verdict.REFRESHED) {
                  BlockState there = level.getBlockState(pos);
                  if (there.is(Blocks.LIGHT) && (Integer)there.getValue(LightBlock.LEVEL) < entry.level) {
                     level.setBlock(pos, lit(entry.level), 2);
                  }
               }
            }
         }
      }
   }

   public void release(ServerLevel level, int source) {
      for (LightLedger.Removed removed : this.ledger.drainSource(source)) {
         this.revert(level, removed.pos(), removed.entry());
      }
   }

   public void tick(ServerLevel level) {
      int now = (int)level.getGameTime();

      for (LightLedger.Removed removed : this.ledger.expire(now)) {
         this.revert(level, removed.pos(), removed.entry());
      }

      if (!this.pending.isEmpty()) {
         LongList keep = new LongArrayList();
         LongListIterator var8 = this.pending.iterator();

         while (var8.hasNext()) {
            long pos = (Long)var8.next();
            if (level.isLoaded(BlockPos.of(pos))) {
               this.revert(level, pos, null);
            } else {
               keep.add(pos);
            }
         }

         this.pending.clear();
         this.pending.addAll(keep);
      }
   }

   public void reclaim(ServerLevel level, ChunkPos chunk) {
      LightScribeData data = LightScribeData.of(level);
      if (data.size() != 0) {
         LongListIterator var4 = data.inChunk(chunk.x, chunk.z).iterator();

         while (var4.hasNext()) {
            long pos = (Long)var4.next();
            if (this.ledger.get(pos) == null && !this.pending.contains(pos)) {
               this.pending.add(pos);
            }
         }
      }
   }

   public void revertChunk(ServerLevel level, ChunkAccess chunk) {
      ChunkPos pos = chunk.getPos();
      LightScribeData data = LightScribeData.of(level);

      for (LightLedger.Removed removed : this.ledger.drainChunk(pos.x, pos.z)) {
         BlockPos at = BlockPos.of(removed.pos());
         if (chunk.getBlockState(at).is(Blocks.LIGHT)) {
            chunk.setBlockState(at, airOf(removed.entry()), false);
         }

         data.remove(removed.pos());
      }
   }

   public void revertAll(ServerLevel level) {
      int n = 0;

      for (LightLedger.Removed removed : this.ledger.drainAll()) {
         this.revert(level, removed.pos(), removed.entry());
         n++;
      }

      if (n > 0) {
         LOGGER.debug("[LightScribe] reverted {} light blocks in {}", n, level.dimension().location());
      }
   }

   public int size() {
      return this.ledger.size();
   }

   public List<LightScribe.Found> scan(ServerLevel level, BlockPos center, int radius) {
      List<LightScribe.Found> out = new ArrayList<>();
      LightScribeData data = LightScribeData.of(level);
      MutableBlockPos cursor = new MutableBlockPos();

      for (int x = -radius; x <= radius; x++) {
         for (int y = -radius; y <= radius; y++) {
            for (int z = -radius; z <= radius; z++) {
               cursor.set(center.getX() + x, center.getY() + y, center.getZ() + z);
               if (level.isLoaded(cursor)) {
                  BlockState state = level.getBlockState(cursor);
                  if (state.is(Blocks.LIGHT)) {
                     long key = cursor.asLong();
                     LightScribe.Provenance p = this.ledger.get(key) != null
                        ? LightScribe.Provenance.LEDGER
                        : (data.contains(key) ? LightScribe.Provenance.SAVED : LightScribe.Provenance.ORPHAN);
                     out.add(new LightScribe.Found(cursor.immutable(), (Integer)state.getValue(LightBlock.LEVEL), p));
                  }
               }
            }
         }
      }

      return out;
   }

   public int purge(ServerLevel level, BlockPos center, int radius) {
      LightScribeData data = LightScribeData.of(level);
      int n = 0;

      for (LightScribe.Found found : this.scan(level, center, radius)) {
         long key = found.pos().asLong();
         level.setBlock(found.pos(), Blocks.AIR.defaultBlockState(), 2);
         this.ledger.remove(key);
         data.remove(key);
         n++;
      }

      return n;
   }

   public static void forgetAll() {
      BY_LEVEL.clear();
   }

   public static void forget(ServerLevel level) {
      BY_LEVEL.remove(level.dimension());
   }

   private void revert(ServerLevel level, long key, LightLedger.Entry entry) {
      BlockPos at = BlockPos.of(key);
      if (level.isLoaded(at)) {
         if (level.getBlockState(at).is(Blocks.LIGHT)) {
            level.setBlock(at, airOf(entry), 2);
         }

         LightScribeData.of(level).remove(key);
      }
   }

   private static BlockState airOf(LightLedger.Entry entry) {
      return entry != null && entry.replaced instanceof BlockState state && state.isAir() ? state : Blocks.AIR.defaultBlockState();
   }

   private static BlockState lit(int lightLevel) {
      return (BlockState)Blocks.LIGHT.defaultBlockState().setValue(LightBlock.LEVEL, lightLevel);
   }

   public record Found(BlockPos pos, int level, LightScribe.Provenance provenance) {
   }

   public enum Provenance {
      LEDGER,
      SAVED,
      ORPHAN;
   }
}
