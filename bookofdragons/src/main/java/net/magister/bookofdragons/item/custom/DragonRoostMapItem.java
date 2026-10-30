package net.magister.bookofdragons.item.custom;

import java.util.List;
import net.magister.bookofdragons.network.Strip;
import net.magister.liberanimalium.compat.StackData;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.TagKey;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.MapItem;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.Item.Properties;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.structure.Structure;
import org.jetbrains.annotations.Nullable;

public class DragonRoostMapItem extends Item {
   private final TagKey<Structure> targetStructureTag;
   private final String displayName;
   private final int mapColor;

   public DragonRoostMapItem(TagKey<Structure> targetStructureTag, String displayName, int mapColor, Properties properties) {
      super(properties);
      this.targetStructureTag = targetStructureTag;
      this.displayName = displayName;
      this.mapColor = mapColor;
   }

   public static ItemStack createFilledMapForStructure(
      Level level, BlockPos origin, TagKey<Structure> structureTag, Component name, int mapColor, int searchRadius
   ) {
      if (level instanceof ServerLevel serverLevel) {
         BlockPos structurePos = serverLevel.findNearestMapStructure(structureTag, origin, searchRadius, false);
         if (structurePos == null) {
            return ItemStack.EMPTY;
         }

         ItemStack mapStack = MapItem.create(serverLevel, structurePos.getX(), structurePos.getZ(), (byte)2, true, true);
         MapItem.renderBiomePreviewMap(serverLevel, mapStack);
         StackData.addRedX(mapStack, structurePos, "+");
         StackData.setName(mapStack, name.copy().withStyle(ChatFormatting.YELLOW));
         return mapStack;
      } else {
         return ItemStack.EMPTY;
      }
   }

   public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
      ItemStack itemStack = player.getItemInHand(hand);
      if (level.isClientSide) {
         player.playSound(SoundEvents.UI_CARTOGRAPHY_TABLE_TAKE_RESULT, 1.0F, 1.0F);
         return InteractionResultHolder.consume(itemStack);
      }

      if (!level.isClientSide && level instanceof ServerLevel serverLevel) {
         ItemStack mapStack = createFilledMapForStructure(serverLevel, player.blockPosition(), this.targetStructureTag, this.roostName(), this.mapColor, 200);
         if (!mapStack.isEmpty()) {
            if (!player.getAbilities().instabuild) {
               itemStack.shrink(1);
            }

            if (itemStack.isEmpty()) {
               return InteractionResultHolder.success(mapStack);
            }

            if (!player.getInventory().add(mapStack)) {
               player.drop(mapStack, false);
            }

            return InteractionResultHolder.consume(itemStack);
         } else {
            Strip.say(player, Component.translatable("item.bookofdragons.roost_map.fail").withStyle(ChatFormatting.RED));
            return InteractionResultHolder.fail(itemStack);
         }
      } else {
         return InteractionResultHolder.pass(itemStack);
      }
   }

   public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
      super.appendHoverText(stack, level, tooltip, flag);
      tooltip.add(Component.translatable("item.bookofdragons.roost_map.tooltip", new Object[]{this.roostName()}).withStyle(ChatFormatting.GRAY));
      tooltip.add(Component.translatable("item.bookofdragons.roost_map.instruction").withStyle(ChatFormatting.BLUE));
   }

   public Component roostName() {
      return Component.translatableWithFallback(this.getDescriptionId() + ".roost", this.displayName);
   }

   public String getDisplayName() {
      return this.displayName;
   }
}
