package net.magister.liberanimalium.compat.v20;

import java.util.function.Consumer;
import org.jetbrains.annotations.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.saveddata.maps.MapItemSavedData;
import net.minecraft.world.level.saveddata.maps.MapDecoration.Type;

public final class StackDataImpl {
   private StackDataImpl() {
   }

   @Nullable
   public static CompoundTag read(ItemStack stack) {
      return stack.getTag();
   }

   public static boolean has(ItemStack stack) {
      return stack.hasTag();
   }

   public static boolean has(ItemStack stack, String key) {
      return stack.hasTag() && stack.getTag().contains(key);
   }

   public static void edit(ItemStack stack, Consumer<CompoundTag> edit) {
      edit.accept(stack.getOrCreateTag());
   }

   public static void editIfPresent(ItemStack stack, Consumer<CompoundTag> edit) {
      CompoundTag tag = stack.getTag();
      if (tag != null) {
         edit.accept(tag);
      }
   }

   public static void write(ItemStack stack, CompoundTag data) {
      stack.setTag(data);
   }

   public static boolean hasName(ItemStack stack) {
      return stack.hasCustomHoverName();
   }

   public static void setName(ItemStack stack, Component name) {
      stack.setHoverName(name);
   }

   public static int dyeTint(ItemStack stack, int defaultRgb) {
      CompoundTag display = stack.getTagElement("display");
      return display != null && display.contains("color", 99) ? display.getInt("color") : defaultRgb;
   }

   public static void addRedX(ItemStack map, BlockPos target, String label) {
      MapItemSavedData.addTargetDecoration(map, target, label, Type.RED_X);
   }
}
