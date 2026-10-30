package net.magister.liberanimalium.fauna.core;

import net.minecraft.nbt.CompoundTag;

final class MindClock {
   private static final long NEVER = Long.MIN_VALUE;
   private long lastTicked = Long.MIN_VALUE;

   long step(long now) {
      long elapsed = this.lastTicked == Long.MIN_VALUE ? 1L : now - this.lastTicked;
      this.lastTicked = now;
      return Math.max(1L, elapsed);
   }

   void save(CompoundTag tag, long age) {
      tag.putLong("Age", age);
      if (this.lastTicked != Long.MIN_VALUE) {
         tag.putLong("LastTicked", this.lastTicked);
      }
   }

   long load(CompoundTag tag) {
      this.lastTicked = tag.contains("LastTicked") ? tag.getLong("LastTicked") : Long.MIN_VALUE;
      return tag.getLong("Age");
   }
}
