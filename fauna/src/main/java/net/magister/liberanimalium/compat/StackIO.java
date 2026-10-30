package net.magister.liberanimalium.compat;

import net.magister.liberanimalium.compat.v20.StackIOImpl;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;

public final class StackIO {
   private StackIO() {
   }

   public static CompoundTag save(Entity holder, ItemStack stack, CompoundTag into) {
      return StackIOImpl.save(holder, stack, into);
   }

   public static ItemStack load(Entity holder, CompoundTag saved) {
      return StackIOImpl.load(holder, saved);
   }
}
