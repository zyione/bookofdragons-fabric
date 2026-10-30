package net.magister.bookofdragons.item.custom;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import net.magister.bookofdragons.entity.custom.egg.DragonEggEntity;
import net.magister.bookofdragons.entity.data.DragonType;
import net.magister.bookofdragons.item.ModItems;
import net.magister.liberanimalium.compat.StackData;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.Item.Properties;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

public class DragonEggItem extends Item {
   private final DragonType dragonType;

   public DragonEggItem(DragonType dragonType, Properties properties) {
      super(properties);
      this.dragonType = dragonType;
   }

   public InteractionResult useOn(UseOnContext context) {
      Level level = context.getLevel();
      if (level.isClientSide()) {
         return InteractionResult.SUCCESS;
      }

      BlockPos clickedPos = context.getClickedPos();
      Direction face = context.getClickedFace();
      BlockPos spawnPos = clickedPos.relative(face);
      Vec3 position = new Vec3(spawnPos.getX() + 0.5, spawnPos.getY(), spawnPos.getZ() + 0.5);
      DragonEggEntity eggEntity = new DragonEggEntity(level, position, this.dragonType);
      ItemStack stack = context.getItemInHand();
      if (StackData.has(stack)) {
         CompoundTag itemTag = StackData.read(stack);
         if (itemTag != null && itemTag.contains("EntityTag")) {
            eggEntity.loadFromItemStack(stack);
         }
      }

      level.addFreshEntity(eggEntity);
      Player player = context.getPlayer();
      if (player != null && !player.isCreative()) {
         stack.shrink(1);
      }

      return InteractionResult.CONSUME;
   }

   public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
      super.appendHoverText(stack, level, tooltip, flag);
      tooltip.add(
         Component.translatable("item.bookofdragons.dragon_egg.type", new Object[]{this.dragonType.getEntityType().getDescription()}).withStyle(ChatFormatting.GOLD)
      );
      CompoundTag entityTag = this.getEntityTag(stack);
      if (entityTag != null && !entityTag.isEmpty()) {
         this.addDetailedTooltips(tooltip, entityTag);
      } else {
         tooltip.add(Component.translatable("item.bookofdragons.dragon_egg.fresh").withStyle(new ChatFormatting[]{ChatFormatting.GRAY, ChatFormatting.ITALIC}));
      }
   }

   private void addDetailedTooltips(List<Component> tooltip, CompoundTag entityTag) {
      if (entityTag.contains("CurrentHatchTime") && entityTag.contains("TotalHatchTime")) {
         int currentTime = entityTag.getInt("CurrentHatchTime");
         int totalTime = entityTag.getInt("TotalHatchTime");
         float progress = (1.0F - (float)currentTime / totalTime) * 100.0F;
         ChatFormatting color = progress < 25.0F
            ? ChatFormatting.RED
            : (progress < 50.0F ? ChatFormatting.YELLOW : (progress < 75.0F ? ChatFormatting.GREEN : ChatFormatting.AQUA));
         tooltip.add(Component.translatable("item.bookofdragons.dragon_egg.progress", new Object[]{String.format("%.1f", progress)}).withStyle(color));
      }

      if (entityTag.getBoolean("IsFromTamedParents")) {
         tooltip.add(Component.literal(""));
         tooltip.add(Component.translatable("item.bookofdragons.dragon_egg.tamed_lineage").withStyle(ChatFormatting.LIGHT_PURPLE));
      }

      if (!entityTag.isEmpty()) {
         tooltip.add(Component.literal(""));
         tooltip.add(
            Component.translatable("item.bookofdragons.dragon_egg.has_data").withStyle(new ChatFormatting[]{ChatFormatting.DARK_GRAY, ChatFormatting.ITALIC})
         );
      }
   }

   @Nullable
   private CompoundTag getEntityTag(ItemStack stack) {
      CompoundTag tag = StackData.read(stack);
      return tag != null && tag.contains("EntityTag") ? tag.getCompound("EntityTag") : null;
   }

   public static ItemStack createFreshEgg(DragonType type) {
      return new ItemStack(ModItems.getEggItemForType(type));
   }

   public static ItemStack createEggWithData(DragonType type, boolean isFromTamedParents, Optional<UUID> ownerUUID, int totalHatchTime, int currentHatchTime) {
      ItemStack stack = new ItemStack(ModItems.getEggItemForType(type));
      CompoundTag entityTag = new CompoundTag();
      entityTag.putString("DragonType", type.getSerializedName());
      entityTag.putBoolean("IsFromTamedParents", isFromTamedParents);
      ownerUUID.ifPresent(uuid -> entityTag.putUUID("OwnerUUID", uuid));
      entityTag.putInt("TotalHatchTime", totalHatchTime);
      entityTag.putInt("CurrentHatchTime", currentHatchTime);
      CompoundTag itemTag = new CompoundTag();
      itemTag.put("EntityTag", entityTag);
      StackData.write(stack, itemTag);
      return stack;
   }

   public DragonType getDragonType() {
      return this.dragonType;
   }
}
