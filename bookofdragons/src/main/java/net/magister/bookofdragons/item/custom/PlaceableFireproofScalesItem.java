package net.magister.bookofdragons.item.custom;

import java.util.List;
import org.jetbrains.annotations.Nullable;
import net.magister.bookofdragons.entity.data.DragonType;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.Item.Properties;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;

public class PlaceableFireproofScalesItem extends BlockItem {
   private final DragonType dragonType;
   private final int color;

   public PlaceableFireproofScalesItem(Block block, Properties properties, DragonType dragonType, int color) {
      super(block, properties);
      this.dragonType = dragonType;
      this.color = color;
   }

   public DragonType getDragonType() {
      return this.dragonType;
   }

   public int getColor() {
      return this.color;
   }

   public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltipComponents, TooltipFlag isAdvanced) {
      super.appendHoverText(stack, level, tooltipComponents, isAdvanced);
      tooltipComponents.add(
         Component.translatable("item.bookofdragons.fireproof_scales.dragon_type", new Object[]{this.dragonType.getEntityType().getDescription()})
            .withStyle(ChatFormatting.GRAY)
      );
      tooltipComponents.add(Component.translatable("item.bookofdragons.fireproof_scales.description").withStyle(ChatFormatting.GOLD));
      tooltipComponents.add(Component.translatable("item.bookofdragons.fireproof_scales.crafting_hint").withStyle(ChatFormatting.BLUE));
      tooltipComponents.add(Component.translatable("item.bookofdragons.placeable_scales.placement_hint").withStyle(ChatFormatting.GREEN));
   }

   public boolean isFireResistant() {
      return true;
   }
}
