package net.magister.liberanimalium.compat.v20;

import java.util.function.Function;
import java.util.function.Supplier;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.storage.DimensionDataStorage;

public final class SavedImpl {
   private SavedImpl() {
   }

   public static <T extends SavedData> T getOrCreate(DimensionDataStorage storage, String name, Supplier<T> create, Function<CompoundTag, T> load) {
      return (T)storage.computeIfAbsent(load, create, name);
   }
}
