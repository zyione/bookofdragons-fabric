package net.magister.bookofdragons.item.custom;

import java.util.List;
import java.util.function.Supplier;
import org.jetbrains.annotations.Nullable;
import net.magister.bookofdragons.entity.custom.toy.PlushEntity;
import net.magister.bookofdragons.entity.custom.toy.PlushKind;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.BlockSource;
import net.minecraft.core.Direction;
import net.minecraft.core.dispenser.DefaultDispenseItemBehavior;
import net.minecraft.core.dispenser.DispenseItemBehavior;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.Item.Properties;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.DispenserBlock;

public class PlushItem extends Item {
   private final Supplier<EntityType<PlushEntity>> toy;
   private final PlushKind kind;
   public static final DispenseItemBehavior DISPENSE = new DefaultDispenseItemBehavior() {
      protected ItemStack execute(BlockSource source, ItemStack stack) {
         ServerLevel level = source.getLevel();
         BlockPos from = source.getPos();
         Direction facing = (Direction)source.getBlockState().getValue(DispenserBlock.FACING);
         BlockPos at = from.relative(facing);
         if (stack.getItem() instanceof PlushItem item && level.getBlockState(at).getCollisionShape(level, at).isEmpty()) {
            PlushEntity plush = (PlushEntity)item.toy.get().create(level);
            if (plush == null) {
               return super.execute(source, stack);
            }

            plush.place(at.getX() + 0.5, at.getY(), at.getZ() + 0.5, facing.toYRot());
            plush.color(PlushEntity.colorOf(stack));
            level.addFreshEntity(plush);
            level.playSound(null, at, SoundEvents.WOOL_PLACE, SoundSource.NEUTRAL, 0.8F, 1.0F);
            stack.shrink(1);
            return stack;
         } else {
            return super.execute(source, stack);
         }
      }
   };

   public PlushItem(Supplier<EntityType<PlushEntity>> toy, PlushKind kind, Properties properties) {
      super(properties);
      this.toy = toy;
      this.kind = kind;
   }

   public Component getName(ItemStack stack) {
      return super.getName(stack).copy().withStyle(this.kind.dragon() ? ChatFormatting.GOLD : ChatFormatting.LIGHT_PURPLE);
   }

   public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
      super.appendHoverText(stack, level, tooltip, flag);
      tooltip.add(this.description());
   }

   private Component description() {
      return Component.translatable("item.bookofdragons." + this.kind.id() + ".desc").withStyle(new ChatFormatting[]{ChatFormatting.GRAY, ChatFormatting.ITALIC});
   }

   public InteractionResult useOn(UseOnContext context) {
      Level level = context.getLevel();
      if (level.isClientSide()) {
         return InteractionResult.SUCCESS;
      }

      PlushEntity plush = (PlushEntity)this.toy.get().create(level);
      if (plush == null) {
         return InteractionResult.FAIL;
      }

      BlockPos at = context.getClickedPos().relative(context.getClickedFace());
      Player player = context.getPlayer();
      plush.place(at.getX() + 0.5, at.getY(), at.getZ() + 0.5, player == null ? 0.0F : player.getYRot() + 180.0F);
      plush.color(PlushEntity.colorOf(context.getItemInHand()));
      level.addFreshEntity(plush);
      level.playSound(null, at, SoundEvents.WOOL_PLACE, SoundSource.NEUTRAL, 0.8F, 1.0F);
      if (player == null || !player.isCreative()) {
         context.getItemInHand().shrink(1);
      }

      return InteractionResult.CONSUME;
   }
}
