package net.magister.liberanimalium.attack.light;

import it.unimi.dsi.fastutil.longs.LongArrayList;
import it.unimi.dsi.fastutil.longs.LongIterator;
import it.unimi.dsi.fastutil.longs.LongList;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import net.magister.liberanimalium.compat.Saved;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;

public final class LightScribeData extends SavedData {
   static final String NAME = "liberanimalium_lights";
   private static final String KEY = "lit";
   private final LongOpenHashSet lit = new LongOpenHashSet();

   public static LightScribeData of(ServerLevel level) {
      return Saved.getOrCreate(level.getDataStorage(), "liberanimalium_lights", LightScribeData::new, LightScribeData::load);
   }

   public static void absorb(ServerLevel level, String formerName) {
      LightScribeData former = Saved.getOrCreate(level.getDataStorage(), formerName, LightScribeData::new, LightScribeData::load);
      if (!former.lit.isEmpty()) {
         LightScribeData book = of(level);
         LongIterator var4 = former.lit.iterator();

         while (var4.hasNext()) {
            long pos = (Long)var4.next();
            book.add(pos);
         }

         former.lit.clear();
         former.setDirty();
      }
   }

   public static LightScribeData load(CompoundTag tag) {
      LightScribeData data = new LightScribeData();

      for (long pos : tag.getLongArray("lit")) {
         data.lit.add(pos);
      }

      return data;
   }

   public CompoundTag save(CompoundTag tag) {
      tag.putLongArray("lit", this.lit.toLongArray());
      return tag;
   }

   public void add(long pos) {
      if (this.lit.add(pos)) {
         this.setDirty();
      }
   }

   public void remove(long pos) {
      if (this.lit.remove(pos)) {
         this.setDirty();
      }
   }

   public boolean contains(long pos) {
      return this.lit.contains(pos);
   }

   public int size() {
      return this.lit.size();
   }

   public LongList inChunk(int chunkX, int chunkZ) {
      LongList out = new LongArrayList();
      LongIterator var4 = this.lit.iterator();

      while (var4.hasNext()) {
         long pos = (Long)var4.next();
         if (BlockPos.getX(pos) >> 4 == chunkX && BlockPos.getZ(pos) >> 4 == chunkZ) {
            out.add(pos);
         }
      }

      return out;
   }

   public LongList all() {
      return new LongArrayList(this.lit);
   }
}
