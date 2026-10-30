package net.magister.liberanimalium.compat.v20;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;

public final class StackIOImpl {
   private StackIOImpl() {
   }

   public static CompoundTag save(Entity holder, ItemStack stack, CompoundTag into) {
      return stack.save(into);
   }

   public static ItemStack load(Entity holder, CompoundTag saved) {
      return ItemStack.of(saved);
   }
}
