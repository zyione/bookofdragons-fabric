package net.magister.liberanimalium.fauna.know;

import net.minecraft.nbt.CompoundTag;

public interface Faculty {
   String id();

   default void integrate(FacultyContext ctx) {
   }

   default void settle(FacultyContext ctx) {
   }

   default void save(CompoundTag tag) {
   }

   default void load(CompoundTag tag) {
   }
}
