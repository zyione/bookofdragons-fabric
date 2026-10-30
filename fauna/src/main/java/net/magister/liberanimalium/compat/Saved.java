package net.magister.liberanimalium.compat;

import java.util.function.Function;
import java.util.function.Supplier;
import net.magister.liberanimalium.compat.v20.SavedImpl;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.storage.DimensionDataStorage;

public final class Saved {
   private Saved() {
   }

   public static <T extends SavedData> T getOrCreate(DimensionDataStorage storage, String name, Supplier<T> create, Function<CompoundTag, T> load) {
      return SavedImpl.getOrCreate(storage, name, create, load);
   }
}
