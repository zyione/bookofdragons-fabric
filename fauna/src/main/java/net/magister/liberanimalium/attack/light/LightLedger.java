package net.magister.liberanimalium.attack.light;

import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectIterator;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;

public final class LightLedger {
   private final Long2ObjectOpenHashMap<LightLedger.Entry> entries = new Long2ObjectOpenHashMap();
   private final int capacity;

   public LightLedger(int capacity) {
      this.capacity = capacity;
   }

   public LightLedger.Verdict request(long pos, int owner, int level, int expiryTick, int maxPerOwner, double spacing) {
      return this.request(pos, owner, owner, level, expiryTick, maxPerOwner, spacing);
   }

   public LightLedger.Verdict request(long pos, int owner, int source, int level, int expiryTick, int maxPerOwner, double spacing) {
      LightLedger.Entry existing = (LightLedger.Entry)this.entries.get(pos);
      if (existing != null) {
         existing.expiryTick = Math.max(existing.expiryTick, expiryTick);
         existing.level = Math.max(existing.level, level);
         existing.source = source;
         return LightLedger.Verdict.REFRESHED;
      }

      if (this.entries.size() >= this.capacity) {
         return LightLedger.Verdict.REJECTED;
      }

      if (this.countOwned(owner) >= maxPerOwner) {
         return LightLedger.Verdict.REJECTED;
      }

      if (spacing > 0.0 && this.ownerHasNear(owner, pos, spacing)) {
         return LightLedger.Verdict.REJECTED;
      }

      this.entries.put(pos, new LightLedger.Entry(owner, source, level, expiryTick));
      return LightLedger.Verdict.PLACED;
   }

   public List<LightLedger.Removed> drainSource(int source) {
      List<LightLedger.Removed> out = new ArrayList<>();
      ObjectIterator<it.unimi.dsi.fastutil.longs.Long2ObjectMap.Entry<LightLedger.Entry>> it = this.entries.long2ObjectEntrySet().fastIterator();

      while (it.hasNext()) {
         it.unimi.dsi.fastutil.longs.Long2ObjectMap.Entry<LightLedger.Entry> e = (it.unimi.dsi.fastutil.longs.Long2ObjectMap.Entry<LightLedger.Entry>)it.next();
         if (((LightLedger.Entry)e.getValue()).source == source) {
            out.add(new LightLedger.Removed(e.getLongKey(), (LightLedger.Entry)e.getValue()));
            it.remove();
         }
      }

      return out;
   }

   public void remove(long pos) {
      this.entries.remove(pos);
   }

   public LightLedger.Entry get(long pos) {
      return (LightLedger.Entry)this.entries.get(pos);
   }

   public List<LightLedger.Removed> expire(int nowTick) {
      List<LightLedger.Removed> out = new ArrayList<>();
      ObjectIterator<it.unimi.dsi.fastutil.longs.Long2ObjectMap.Entry<LightLedger.Entry>> it = this.entries.long2ObjectEntrySet().fastIterator();

      while (it.hasNext()) {
         it.unimi.dsi.fastutil.longs.Long2ObjectMap.Entry<LightLedger.Entry> e = (it.unimi.dsi.fastutil.longs.Long2ObjectMap.Entry<LightLedger.Entry>)it.next();
         if (((LightLedger.Entry)e.getValue()).expiryTick <= nowTick) {
            out.add(new LightLedger.Removed(e.getLongKey(), (LightLedger.Entry)e.getValue()));
            it.remove();
         }
      }

      return out;
   }

   public List<LightLedger.Removed> drainAll() {
      List<LightLedger.Removed> out = new ArrayList<>(this.entries.size());
      ObjectIterator var2 = this.entries.long2ObjectEntrySet().iterator();

      while (var2.hasNext()) {
         it.unimi.dsi.fastutil.longs.Long2ObjectMap.Entry<LightLedger.Entry> e = (it.unimi.dsi.fastutil.longs.Long2ObjectMap.Entry<LightLedger.Entry>)var2.next();
         out.add(new LightLedger.Removed(e.getLongKey(), (LightLedger.Entry)e.getValue()));
      }

      this.entries.clear();
      return out;
   }

   public List<LightLedger.Removed> drainChunk(int chunkX, int chunkZ) {
      List<LightLedger.Removed> out = new ArrayList<>();
      ObjectIterator<it.unimi.dsi.fastutil.longs.Long2ObjectMap.Entry<LightLedger.Entry>> it = this.entries.long2ObjectEntrySet().fastIterator();

      while (it.hasNext()) {
         it.unimi.dsi.fastutil.longs.Long2ObjectMap.Entry<LightLedger.Entry> e = (it.unimi.dsi.fastutil.longs.Long2ObjectMap.Entry<LightLedger.Entry>)it.next();
         long key = e.getLongKey();
         if (BlockPos.getX(key) >> 4 == chunkX && BlockPos.getZ(key) >> 4 == chunkZ) {
            out.add(new LightLedger.Removed(key, (LightLedger.Entry)e.getValue()));
            it.remove();
         }
      }

      return out;
   }

   public int size() {
      return this.entries.size();
   }

   public int countOwned(int owner) {
      int n = 0;
      ObjectIterator var3 = this.entries.values().iterator();

      while (var3.hasNext()) {
         LightLedger.Entry e = (LightLedger.Entry)var3.next();
         if (e.owner == owner) {
            n++;
         }
      }

      return n;
   }

   private boolean ownerHasNear(int owner, long pos, double spacing) {
      int x = BlockPos.getX(pos);
      int y = BlockPos.getY(pos);
      int z = BlockPos.getZ(pos);
      double r2 = spacing * spacing;
      ObjectIterator var11 = this.entries.long2ObjectEntrySet().iterator();

      while (var11.hasNext()) {
         it.unimi.dsi.fastutil.longs.Long2ObjectMap.Entry<LightLedger.Entry> e = (it.unimi.dsi.fastutil.longs.Long2ObjectMap.Entry<LightLedger.Entry>)var11.next();
         if (((LightLedger.Entry)e.getValue()).owner == owner) {
            long k = e.getLongKey();
            double dx = BlockPos.getX(k) - x;
            double dy = BlockPos.getY(k) - y;
            double dz = BlockPos.getZ(k) - z;
            if (dx * dx + dy * dy + dz * dz < r2) {
               return true;
            }
         }
      }

      return false;
   }

   public static final class Entry {
      public final int owner;
      public int source;
      public int level;
      public int expiryTick;
      public Object replaced;

      Entry(int owner, int source, int level, int expiryTick) {
         this.owner = owner;
         this.source = source;
         this.level = level;
         this.expiryTick = expiryTick;
      }
   }

   public record Removed(long pos, LightLedger.Entry entry) {
   }

   public enum Verdict {
      PLACED,
      REFRESHED,
      REJECTED;
   }
}
