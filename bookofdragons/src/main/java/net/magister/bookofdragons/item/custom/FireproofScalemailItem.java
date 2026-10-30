package net.magister.bookofdragons.item.custom;

import java.util.List;
import org.jetbrains.annotations.Nullable;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.Item.Properties;
import net.minecraft.world.level.Level;

public class FireproofScalemailItem extends Item {
   public FireproofScalemailItem(Properties properties) {
      super(properties);
   }

   public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltipComponents, TooltipFlag isAdvanced) {
      super.appendHoverText(stack, level, tooltipComponents, isAdvanced);
      tooltipComponents.add(Component.translatable("item.bookofdragons.fireproof_scalemail.description").withStyle(ChatFormatting.GOLD));
      tooltipComponents.add(Component.translatable("item.bookofdragons.fireproof_scalemail.properties").withStyle(ChatFormatting.GRAY));
      tooltipComponents.add(Component.translatable("item.bookofdragons.fireproof_scalemail.crafting_hint").withStyle(ChatFormatting.BLUE));
   }

   public boolean isFireResistant() {
      return true;
   }
}
