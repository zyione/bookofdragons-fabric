package net.magister.liberanimalium.chassis.component;

import net.magister.liberanimalium.chassis.FaunaBase;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;

public interface IFaunaComponent {
   void onAttach(FaunaBase var1);

   default void serverTick() {
   }

   default void clientTick() {
   }

   default void save(CompoundTag tag) {
   }

   default void load(CompoundTag tag) {
   }

   default void onSyncedDataUpdated(EntityDataAccessor<?> key) {
   }
}
