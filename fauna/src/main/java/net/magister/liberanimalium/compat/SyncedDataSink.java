package net.magister.liberanimalium.compat;

import net.minecraft.network.syncher.EntityDataAccessor;

public interface SyncedDataSink {
   <T> void define(EntityDataAccessor<T> var1, T var2);
}
