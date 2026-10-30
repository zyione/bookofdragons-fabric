package net.magister.bookofdragons.item.custom;

import java.util.List;
import java.util.UUID;
import org.jetbrains.annotations.Nullable;
import net.magister.bookofdragons.advancement.DragonMilestones;
import net.magister.bookofdragons.datagen.data.DragonRegistryData;
import net.magister.bookofdragons.datagen.data.DragonRegistryEntry;
import net.magister.bookofdragons.entity.base.dragon.DragonBase;
import net.magister.bookofdragons.entity.data.DragonNames;
import net.magister.bookofdragons.network.Strip;
import net.magister.bookofdragons.sound.ModSounds;
import net.magister.liberanimalium.compat.StackData;
import net.magister.liberanimalium.platform.Services;
import net.magister.liberanimalium.platform.world.ChunkTickets;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.DyeableLeatherItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.item.Item.Properties;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;

public class DragonWhistleItem extends Item implements DyeableLeatherItem {
   private static final String TAG_BOUND_DRAGON_UUID = "BoundDragonUUID";
   private static final String TAG_BOUND_DRAGON_NAME = "BoundDragonName";
   private static final String TAG_DRAGON_POS_X = "DragonPosX";
   private static final String TAG_DRAGON_POS_Y = "DragonPosY";
   private static final String TAG_DRAGON_POS_Z = "DragonPosZ";
   private static final String TAG_DRAGON_DIMENSION = "DragonDimension";
   private static final int USE_DURATION = 20;
   private static final ChunkTickets CHUNK_TICKETS = Services.chunkTickets(new ResourceLocation("bookofdragons", "dragon_whistle"));

   public DragonWhistleItem(Properties pProperties) {
      super(pProperties);
   }

   public int getUseDuration(ItemStack stack) {
      return 20;
   }

   public UseAnim getUseAnimation(ItemStack stack) {
      return UseAnim.TOOT_HORN;
   }

   public InteractionResult interactLivingEntity(ItemStack stack, Player player, LivingEntity target, InteractionHand hand) {
      if (target instanceof DragonBase dragon) {
         if (player.level().isClientSide()) {
            return InteractionResult.SUCCESS;
         }

         if (dragon.isTame() && dragon.isOwnedBy(player)) {
            ItemStack heldStack = player.getItemInHand(hand);
            this.forgetForcedChunk(heldStack, player.level());
            UUID bound = this.getBoundDragonUUID(heldStack);
            if (bound != null && !bound.equals(dragon.getUUID())) {
               say(player, "already_bound", ChatFormatting.YELLOW);
               return InteractionResult.FAIL;
            }

            if (bound == null) {
               this.setBoundDragon(heldStack, dragon.getUUID(), DragonNames.of(dragon));
               DragonMilestones.reach(player, "bound", dragon);
            }

            Strip.say(player, Component.translatable("item.bookofdragons.dragon_whistle.bound", new Object[]{dragon.getName()}).withStyle(ChatFormatting.GREEN));
            return InteractionResult.SUCCESS;
         } else {
            say(player, "not_owner", ChatFormatting.RED);
            return InteractionResult.FAIL;
         }
      } else {
         return InteractionResult.PASS;
      }
   }

   public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
      ItemStack stack = player.getItemInHand(hand);
      if (level.isClientSide()) {
         if (!player.isShiftKeyDown() && this.hasBoundDragon(stack)) {
            player.startUsingItem(hand);
         }

         return InteractionResultHolder.success(stack);
      } else {
         ServerPlayer server = (ServerPlayer)player;
         this.forgetForcedChunk(stack, level);
         DragonRegistryData registry = DragonRegistryData.get(server.getServer());
         if (player.isShiftKeyDown()) {
            if (this.hasBoundDragon(stack)) {
               this.clearBoundDragon(stack);
               say(player, "unbound", ChatFormatting.GOLD);
               level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.EXPERIENCE_ORB_PICKUP, SoundSource.PLAYERS, 0.5F, 0.5F);
            } else {
               List<DragonRegistryData.Stored> kept = registry.storedOf(player.getUUID());
               if (kept.isEmpty()) {
                  say(player, "none_stored", ChatFormatting.GRAY);
               } else {
                  DragonRegistryData.Stored next = kept.get(0);
                  this.setBoundDragon(stack, next.dragon(), next.name());
                  Strip.say(player, Component.translatable("item.bookofdragons.dragon_whistle.bound", new Object[]{next.name()}).withStyle(ChatFormatting.GREEN));
               }
            }

            return InteractionResultHolder.success(stack);
         } else {
            UUID id = this.getBoundDragonUUID(stack);
            if (id == null) {
               say(player, "tooltip.unbound", ChatFormatting.GRAY);
               return InteractionResultHolder.fail(stack);
            } else {
               DragonBase live = WhistleStable.find(server.getServer(), id);
               DragonRegistryEntry entry = registry.getDragon(id);
               boolean mine = live != null
                  ? live.isTame() && live.isOwnedBy(player)
                  : (
                     registry.isStored(id)
                        ? registry.storedOf(player.getUUID()).stream().anyMatch(kept -> kept.dragon().equals(id))
                        : entry == null || player.getUUID().equals(entry.getOwnerUUID())
                  );
               if (!mine) {
                  this.clearBoundDragon(stack);
                  say(player, "not_yours", ChatFormatting.RED);
                  return InteractionResultHolder.fail(stack);
               } else if (!this.isValidSummonLocation(level, player)) {
                  say(player, "not_outside", ChatFormatting.RED);
                  return InteractionResultHolder.fail(stack);
               } else {
                  whistle(level, player);
                  WhistleStable.Call call = WhistleStable.call(server, (ServerLevel)level, id);
                  WhistleStable.tell(server, call, this.getBoundDragonName(stack));
                  player.getCooldowns().addCooldown(this, 100);
                  player.startUsingItem(hand);
                  return InteractionResultHolder.consume(stack);
               }
            }
         }
      }
   }

   public ItemStack finishUsingItem(ItemStack stack, Level level, LivingEntity entity) {
      return stack;
   }

   private static void whistle(Level level, Player player) {
      level.playSound(null, player.getX(), player.getY(), player.getZ(), (SoundEvent)ModSounds.DRAGON_WHISTLE.get(), SoundSource.PLAYERS, 1.0F, 1.0F);
   }

   private static void say(Player player, String key, ChatFormatting style) {
      Strip.say(player, Component.translatable("item.bookofdragons.dragon_whistle." + key).withStyle(style));
   }

   private boolean isValidSummonLocation(Level level, Player player) {
      if (!level.dimension().equals(Level.OVERWORLD)) {
         return false;
      }

      BlockPos playerPos = player.blockPosition();
      return level.canSeeSky(playerPos) || level.canSeeSky(playerPos.above());
   }

   private boolean hasBoundDragon(ItemStack stack) {
      CompoundTag tag = StackData.read(stack);
      return tag != null && tag.contains("BoundDragonUUID");
   }

   @Nullable
   private UUID getBoundDragonUUID(ItemStack stack) {
      CompoundTag tag = StackData.read(stack);
      if (tag != null && tag.contains("BoundDragonUUID")) {
         try {
            return UUID.fromString(tag.getString("BoundDragonUUID"));
         } catch (IllegalArgumentException e) {
            return null;
         }
      } else {
         return null;
      }
   }

   @Nullable
   private String getBoundDragonName(ItemStack stack) {
      CompoundTag tag = StackData.read(stack);
      return tag != null && tag.contains("BoundDragonName") ? tag.getString("BoundDragonName") : null;
   }

   private void setBoundDragon(ItemStack stack, UUID dragon, String name) {
      StackData.edit(stack, tag -> {
         tag.putString("BoundDragonUUID", dragon.toString());
         tag.putString("BoundDragonName", name);
      });
   }

   private void clearBoundDragon(ItemStack stack) {
      StackData.editIfPresent(stack, tag -> {
         tag.remove("BoundDragonUUID");
         tag.remove("BoundDragonName");
      });
   }

   private void forgetForcedChunk(ItemStack stack, Level level) {
      if (!level.isClientSide()) {
         CompoundTag tag = StackData.read(stack);
         if (tag != null && tag.contains("DragonDimension")) {
            BlockPos savedPos = new BlockPos(tag.getInt("DragonPosX"), tag.getInt("DragonPosY"), tag.getInt("DragonPosZ"));
            ResourceKey<Level> dimensionKey = ResourceKey.create(Registries.DIMENSION, new ResourceLocation(tag.getString("DragonDimension")));
            ServerLevel targetLevel = ((ServerLevel)level).getServer().getLevel(dimensionKey);
            if (targetLevel != null) {
               ChunkPos chunkPos = new ChunkPos(savedPos);
               CHUNK_TICKETS.force(targetLevel, savedPos, chunkPos.x, chunkPos.z, false, true);
            }

            StackData.edit(stack, kept -> {
               kept.remove("DragonPosX");
               kept.remove("DragonPosY");
               kept.remove("DragonPosZ");
               kept.remove("DragonDimension");
            });
         }
      }
   }

   public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltipComponents, TooltipFlag isAdvanced) {
      if (this.hasBoundDragon(stack)) {
         String dragonName = this.getBoundDragonName(stack);
         if (dragonName != null) {
            tooltipComponents.add(
               Component.translatable("item.bookofdragons.dragon_whistle.tooltip.bound", new Object[]{DragonNames.shown(dragonName)})
                  .withStyle(ChatFormatting.GREEN)
            );
         }

         tooltipComponents.add(
            Component.translatable("item.bookofdragons.dragon_whistle.tooltip.shift_unbind")
               .withStyle(new ChatFormatting[]{ChatFormatting.DARK_GRAY, ChatFormatting.ITALIC})
         );
      } else {
         tooltipComponents.add(Component.translatable("item.bookofdragons.dragon_whistle.tooltip.unbound").withStyle(ChatFormatting.GRAY));
      }

      super.appendHoverText(stack, level, tooltipComponents, isAdvanced);
   }
}
