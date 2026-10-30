package net.magister.liberanimalium.compat;

import java.util.function.Consumer;
import org.jetbrains.annotations.Nullable;
import net.magister.liberanimalium.compat.v20.StackDataImpl;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

public final class StackData {
   private StackData() {
   }

   @Nullable
   public static CompoundTag read(ItemStack stack) {
      return StackDataImpl.read(stack);
   }

   public static boolean has(ItemStack stack) {
      return StackDataImpl.has(stack);
   }

   public static boolean has(ItemStack stack, String key) {
      return StackDataImpl.has(stack, key);
   }

   public static void edit(ItemStack stack, Consumer<CompoundTag> edit) {
      StackDataImpl.edit(stack, edit);
   }

   public static void editIfPresent(ItemStack stack, Consumer<CompoundTag> edit) {
      StackDataImpl.editIfPresent(stack, edit);
   }

   public static void write(ItemStack stack, CompoundTag data) {
      StackDataImpl.write(stack, data);
   }

   public static boolean hasName(ItemStack stack) {
      return StackDataImpl.hasName(stack);
   }

   public static void setName(ItemStack stack, Component name) {
      StackDataImpl.setName(stack, name);
   }

   public static int dyeTint(ItemStack stack, int defaultRgb) {
      return StackDataImpl.dyeTint(stack, defaultRgb);
   }

   public static void addRedX(ItemStack map, BlockPos target, String label) {
      StackDataImpl.addRedX(map, target, label);
   }
}
