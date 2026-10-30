package net.magister.liberanimalium.util;

import net.minecraft.nbt.CompoundTag;

public final class SaveKeys {
   private SaveKeys() {
   }

   public static String of(CompoundTag tag, String now, String before) {
      return !tag.contains(now) && tag.contains(before) ? before : now;
   }
}
