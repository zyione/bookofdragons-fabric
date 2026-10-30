package net.magister.bookofdragons.item.custom;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Predicate;
import org.jetbrains.annotations.Nullable;
import net.magister.bookofdragons.advancement.DragonMilestones;
import net.magister.bookofdragons.client.event.ClientForgeEvents;
import net.magister.bookofdragons.entity.base.dragon.DragonBase;
import net.magister.bookofdragons.network.Strip;
import net.magister.bookofdragons.sound.ModSounds;
import net.magister.liberanimalium.chassis.FaunaBase;
import net.magister.liberanimalium.chassis.component.CommandComponent;
import net.magister.liberanimalium.chassis.component.command.Modifier;
import net.magister.liberanimalium.chassis.component.command.Stance;
import net.magister.liberanimalium.compat.StackData;
import net.magister.liberanimalium.platform.client.ArmPoses;
import net.magister.liberanimalium.platform.client.HandPose;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.model.HumanoidModel.ArmPose;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.item.Item.Properties;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ClipContext.Block;
import net.minecraft.world.level.ClipContext.Fluid;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.HitResult.Type;

public class RattlestaffItem extends Item {
   private static final String TAG_BOUND_DRAGONS = "BoundDragons";
   private static final int HOLD_THRESHOLD_TICKS = 10;
   private static final double MAX_BLOCK_RANGE = 16.0;
   private static final double MAX_ENTITY_RANGE = 32.0;
   private static final double COMMAND_RANGE = 48.0;
   private static final double BIND_RANGE = 32.0;

   public RattlestaffItem(Properties pProperties) {
      super(pProperties);
   }

   public int getUseDuration(ItemStack stack) {
      return 72000;
   }

   public UseAnim getUseAnimation(ItemStack stack) {
      return UseAnim.BOW;
   }

   public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
      ItemStack itemStack = player.getItemInHand(hand);
      if (hand == InteractionHand.OFF_HAND) {
         return InteractionResultHolder.pass(itemStack);
      }

      if (level.isClientSide) {
         ClientForgeEvents.startRattlestaffAnimation(player.getUUID());
      }

      player.startUsingItem(hand);
      return InteractionResultHolder.consume(itemStack);
   }

   public void releaseUsing(ItemStack stack, Level level, LivingEntity entity, int timeLeft) {
      if (entity instanceof Player player) {
         if (!level.isClientSide) {
            int useDuration = this.getUseDuration(stack) - timeLeft;
            boolean isHoldCommand = useDuration >= 10;
            if (player.isShiftKeyDown()) {
               this.handleBindingRelease(player, stack, level, isHoldCommand);
            } else {
               List<DragonBase> dragonsToCommand = this.getDragonsToCommand(player, stack, level);
               if (dragonsToCommand.isEmpty()) {
                  List<UUID> boundDragons = this.getBoundDragons(stack);
                  if (boundDragons.isEmpty()) {
                     Strip.say(player, Component.translatable("item.bookofdragons.rattlestaff.no_dragons_near"));
                  } else {
                     Strip.say(player, Component.translatable("item.bookofdragons.rattlestaff.no_bound_dragons_near"));
                  }
               } else {
                  RattlestaffItem.TargetResult target = this.getTarget(player);
                  if (target.entity != null) {
                     this.executeEntityCommand(player, dragonsToCommand, target.entity, isHoldCommand, level);
                  } else if (target.blockPos != null) {
                     this.executeMoveTo(player, dragonsToCommand, target.blockPos, isHoldCommand, level);
                  } else {
                     this.executeDirectCommand(player, dragonsToCommand, isHoldCommand, level);
                  }

                  level.playSound(
                     null,
                     player.getX(),
                     player.getY(),
                     player.getZ(),
                     (SoundEvent)ModSounds.RATTLESTAFF_USE.get(),
                     SoundSource.PLAYERS,
                     0.5F,
                     isHoldCommand ? 0.7F : 1.0F
                  );
                  player.getCooldowns().addCooldown(this, 20);
               }
            }
         }
      }
   }

   public InteractionResult interactLivingEntity(ItemStack pStack, Player player, LivingEntity target, InteractionHand hand) {
      if (hand == InteractionHand.OFF_HAND) {
         return InteractionResult.PASS;
      }

      if (target instanceof DragonBase dragon) {
         if (player.isShiftKeyDown()) {
            if (!player.level().isClientSide) {
               ItemStack heldStack = player.getItemInHand(hand);
               this.handleDragonBinding(player, dragon, heldStack, player.level());
            }

            return InteractionResult.SUCCESS;
         } else {
            return InteractionResult.PASS;
         }
      } else {
         return InteractionResult.PASS;
      }
   }

   private RattlestaffItem.TargetResult getTarget(Player player) {
      Vec3 eyePos = player.getEyePosition();
      Vec3 lookVec = player.getViewVector(1.0F);
      BlockHitResult blockHit = player.level().clip(new ClipContext(eyePos, eyePos.add(lookVec.scale(32.0)), Block.OUTLINE, Fluid.NONE, player));
      double wall = blockHit.getType() == Type.BLOCK ? eyePos.distanceTo(blockHit.getLocation()) : 32.0;
      Optional<LivingEntity> entityHit = this.getTargetedEntity(player, wall);
      if (entityHit.isPresent()) {
         LivingEntity entity = entityHit.get();
         return entity instanceof DragonBase dragon && dragon.isTame() && dragon.isOwnedBy(player)
            ? new RattlestaffItem.TargetResult(null, null)
            : new RattlestaffItem.TargetResult(entity, null);
      } else if (blockHit.getType() == Type.BLOCK && wall <= 16.0) {
         BlockPos hitPos = blockHit.getBlockPos();
         return new RattlestaffItem.TargetResult(null, Vec3.atBottomCenterOf(hitPos.above()));
      } else {
         return new RattlestaffItem.TargetResult(null, null);
      }
   }

   private Optional<LivingEntity> getTargetedEntity(Player player, double range) {
      Vec3 eyePos = player.getEyePosition();
      Vec3 lookVec = player.getViewVector(1.0F);
      Vec3 endPos = eyePos.add(lookVec.scale(range));
      AABB searchBox = player.getBoundingBox().expandTowards(lookVec.scale(range)).inflate(1.0);
      Predicate<Entity> filter = e -> e instanceof LivingEntity && e != player && !e.isSpectator();
      LivingEntity closest = null;
      double closestDist = range;

      for (Entity entity : player.level().getEntities(player, searchBox, filter)) {
         AABB entityBox = entity.getBoundingBox().inflate(entity.getPickRadius());
         Optional<Vec3> intersection = entityBox.clip(eyePos, endPos);
         if (intersection.isPresent()) {
            double dist = eyePos.distanceTo(intersection.get());
            if (dist < closestDist) {
               closest = (LivingEntity)entity;
               closestDist = dist;
            }
         }
      }

      return Optional.ofNullable(closest);
   }

   private Optional<DragonBase> getTargetedDragonForBinding(Player player) {
      Optional<LivingEntity> entity = this.getTargetedEntity(player, 32.0);
      return entity.isPresent() && entity.get() instanceof DragonBase dragon ? Optional.of(dragon) : Optional.empty();
   }

   private void executeEntityCommand(Player player, List<DragonBase> dragons, LivingEntity targetEntity, boolean attack, Level level) {
      int count = 0;

      for (DragonBase dragon : dragons) {
         if (dragon != targetEntity) {
            dragon.urge(player, targetEntity, !attack);
            count++;
         }
      }

      if (level instanceof ServerLevel serverLevel) {
         Vec3 at = targetEntity instanceof FaunaBase animal
            ? animal.moodPoint()
            : new Vec3(targetEntity.getX(), targetEntity.getY() + targetEntity.getBbHeight(), targetEntity.getZ());
         if (attack) {
            serverLevel.sendParticles(ParticleTypes.ANGRY_VILLAGER, at.x, at.y, at.z, 5, 0.3, 0.3, 0.3, 0.02);
         } else {
            serverLevel.sendParticles(ParticleTypes.HEART, at.x, at.y, at.z, 3, 0.3, 0.3, 0.3, 0.02);
         }
      }

      String messageKey = attack ? "rattlestaff.attack" : "rattlestaff.stand_down";
      Strip.say(
         player,
         Component.translatable("item.bookofdragons." + messageKey, new Object[]{count, targetEntity.getName()})
            .withStyle(attack ? ChatFormatting.RED : ChatFormatting.GREEN)
      );
   }

   private void executeMoveTo(Player player, List<DragonBase> dragons, Vec3 targetPos, boolean sitAfterArrival, Level level) {
      Modifier modifier = sitAfterArrival ? Modifier.REST : Modifier.GUARD;
      double room = 1.0;

      for (DragonBase dragon : dragons) {
         room = Math.max(room, dragon.getBbWidth() + 0.5);
      }

      int obeyed = 0;

      for (int i = 0; i < dragons.size(); i++) {
         Vec3 post = spotAround(level, targetPos, i, room);
         if (order(dragons.get(i), Stance.STAY, modifier, post)) {
            obeyed++;
         }
      }

      if (level instanceof ServerLevel serverLevel) {
         serverLevel.sendParticles(ParticleTypes.END_ROD, targetPos.x, targetPos.y + 0.5, targetPos.z, 10, 0.3, 0.5, 0.3, 0.02);
      }

      String commandType = sitAfterArrival ? "rattlestaff.move_and_sit" : "rattlestaff.move_to";
      tell(
         player,
         Component.translatable("item.bookofdragons." + commandType, new Object[]{obeyed}).withStyle(sitAfterArrival ? ChatFormatting.GOLD : ChatFormatting.GREEN),
         dragons.size() - obeyed
      );
   }

   private void executeDirectCommand(Player player, List<DragonBase> dragons, boolean sitCommand, Level level) {
      Stance stance = sitCommand ? Stance.STAY : Stance.FOLLOW;
      int obeyed = 0;

      for (DragonBase dragon : dragons) {
         if (order(dragon, stance, stance.defaultModifier(), null)) {
            obeyed++;
         }
      }

      String messageKey = sitCommand ? "command.bookofdragons.sitting" : "command.bookofdragons.following";
      tell(
         player,
         Component.translatable("item.bookofdragons.rattlestaff.command_all", new Object[]{obeyed, Component.translatable(messageKey)})
            .withStyle(sitCommand ? ChatFormatting.GOLD : ChatFormatting.GREEN),
         dragons.size() - obeyed
      );
   }

   private static boolean order(DragonBase dragon, Stance stance, Modifier modifier, @Nullable Vec3 post) {
      CommandComponent cc = (CommandComponent)dragon.componentRegistry.get(CommandComponent.class);
      return cc != null && cc.orderFromStaff(stance, modifier, post, null);
   }

   private static void tell(Player player, MutableComponent answer, int refused) {
      if (refused > 0) {
         answer = answer.append(" ")
            .append(Component.translatable("item.bookofdragons.rattlestaff.refused", new Object[]{refused}).withStyle(ChatFormatting.RED));
      }

      Strip.say(player, answer);
   }

   private static Vec3 spotAround(Level level, Vec3 place, int i, double room) {
      if (i == 0) {
         return place;
      }

      double angle = i * 2.399963;
      double radius = room * Math.sqrt(i);
      BlockPos spot = BlockPos.containing(place.x + Math.cos(angle) * radius, place.y, place.z + Math.sin(angle) * radius);

      for (int dy : new int[]{0, 1, -1, 2, -2}) {
         BlockPos at = spot.above(dy);
         if (level.getBlockState(at).getCollisionShape(level, at).isEmpty() && !level.getBlockState(at.below()).getCollisionShape(level, at.below()).isEmpty()) {
            return Vec3.atBottomCenterOf(at);
         }
      }

      return place;
   }

   private void handleDragonBinding(Player player, DragonBase dragon, ItemStack stack, Level level) {
      if (dragon.isTame() && dragon.isOwnedBy(player)) {
         UUID dragonUUID = dragon.getUUID();
         List<UUID> boundDragons = this.getBoundDragons(stack);
         if (boundDragons.contains(dragonUUID)) {
            boundDragons.remove(dragonUUID);
            this.setBoundDragons(stack, boundDragons);
            Strip.say(player, Component.translatable("item.bookofdragons.rattlestaff.unbound", new Object[]{dragon.getName()}).withStyle(ChatFormatting.RED));
            level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.EXPERIENCE_ORB_PICKUP, SoundSource.PLAYERS, 0.5F, 0.5F);
         } else {
            boundDragons.add(dragonUUID);
            this.setBoundDragons(stack, boundDragons);
            DragonMilestones.reach(player, "bound", dragon);
            Strip.say(player, Component.translatable("item.bookofdragons.rattlestaff.bound", new Object[]{dragon.getName()}).withStyle(ChatFormatting.GREEN));
            level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.EXPERIENCE_ORB_PICKUP, SoundSource.PLAYERS, 0.5F, 1.2F);
         }
      } else {
         Strip.say(player, Component.translatable("item.bookofdragons.rattlestaff.not_owner"));
      }
   }

   private void handleBindingRelease(Player player, ItemStack stack, Level level, boolean wasHeld) {
      Optional<DragonBase> targetedDragon = this.getTargetedDragonForBinding(player);
      if (targetedDragon.isPresent()) {
         this.handleDragonBinding(player, targetedDragon.get(), stack, level);
      } else {
         List<UUID> boundDragons = this.getBoundDragons(stack);
         if (wasHeld) {
            if (boundDragons.isEmpty()) {
               Strip.say(player, Component.translatable("item.bookofdragons.rattlestaff.no_bindings").withStyle(ChatFormatting.GRAY));
            } else {
               int count = boundDragons.size();
               boundDragons.clear();
               this.setBoundDragons(stack, boundDragons);
               Strip.say(player, Component.translatable("item.bookofdragons.rattlestaff.all_unbound", new Object[]{count}).withStyle(ChatFormatting.RED));
               level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.EXPERIENCE_ORB_PICKUP, SoundSource.PLAYERS, 0.5F, 0.3F);
            }
         } else if (boundDragons.isEmpty()) {
            Strip.say(player, Component.translatable("item.bookofdragons.rattlestaff.no_bindings").withStyle(ChatFormatting.GRAY));
         } else {
            Strip.say(
               player, Component.translatable("item.bookofdragons.rattlestaff.bindings_count", new Object[]{boundDragons.size()}).withStyle(ChatFormatting.GOLD)
            );
         }
      }
   }

   private List<DragonBase> getDragonsToCommand(Player player, ItemStack stack, Level level) {
      List<UUID> boundDragons = this.getBoundDragons(stack);
      AABB area = new AABB(player.blockPosition()).inflate(48.0);
      List<DragonBase> nearbyDragons = level.getEntitiesOfClass(DragonBase.class, area, d -> d.isTame() && d.isOwnedBy(player));
      return boundDragons.isEmpty() ? nearbyDragons : nearbyDragons.stream().filter(d -> boundDragons.contains(d.getUUID())).toList();
   }

   private List<UUID> getBoundDragons(ItemStack stack) {
      List<UUID> result = new ArrayList<>();
      CompoundTag tag = StackData.read(stack);
      if (tag != null && tag.contains("BoundDragons", 9)) {
         ListTag list = tag.getList("BoundDragons", 8);

         for (int i = 0; i < list.size(); i++) {
            try {
               result.add(UUID.fromString(list.getString(i)));
            } catch (IllegalArgumentException var7) {
            }
         }
      }

      return result;
   }

   private void setBoundDragons(ItemStack stack, List<UUID> dragons) {
      StackData.edit(stack, tag -> {
         ListTag list = new ListTag();

         for (UUID uuid : dragons) {
            list.add(StringTag.valueOf(uuid.toString()));
         }

         tag.put("BoundDragons", list);
      });
   }

   public void appendHoverText(ItemStack pStack, @Nullable Level pLevel, List<Component> pTooltipComponents, TooltipFlag pIsAdvanced) {
      List<UUID> boundDragons = this.getBoundDragons(pStack);
      if (boundDragons.isEmpty()) {
         pTooltipComponents.add(Component.translatable("item.bookofdragons.rattlestaff.tooltip.all_dragons").withStyle(ChatFormatting.GRAY));
      } else {
         pTooltipComponents.add(
            Component.translatable("item.bookofdragons.rattlestaff.tooltip.bound_count", new Object[]{boundDragons.size()}).withStyle(ChatFormatting.GOLD)
         );
         if (!pIsAdvanced.isAdvanced() && !Screen.hasShiftDown()) {
            pTooltipComponents.add(
               Component.translatable("item.bookofdragons.rattlestaff.tooltip.hold_shift")
                  .withStyle(new ChatFormatting[]{ChatFormatting.DARK_GRAY, ChatFormatting.ITALIC})
            );
         } else if (pLevel != null) {
            for (UUID uuid : boundDragons) {
               Entity entity = null;
               if (pLevel.isClientSide) {
                  ClientLevel clientLevel = (ClientLevel)pLevel;

                  for (Entity e : clientLevel.entitiesForRendering()) {
                     if (e.getUUID().equals(uuid)) {
                        entity = e;
                        break;
                     }
                  }
               }

               if (entity != null) {
                  pTooltipComponents.add(Component.literal("  • ").append(entity.getName()).withStyle(ChatFormatting.YELLOW));
               } else {
                  String shortUUID = uuid.toString().substring(0, 8) + "...";
                  pTooltipComponents.add(
                     Component.literal("  • ")
                        .append(Component.translatable("item.bookofdragons.rattlestaff.tooltip.unloaded", new Object[]{shortUUID}))
                        .withStyle(ChatFormatting.DARK_GRAY)
                  );
               }
            }
         }
      }

      pTooltipComponents.add(
         Component.translatable("item.bookofdragons.rattlestaff.tooltip.usage_click")
            .withStyle(new ChatFormatting[]{ChatFormatting.DARK_GRAY, ChatFormatting.ITALIC})
      );
      pTooltipComponents.add(
         Component.translatable("item.bookofdragons.rattlestaff.tooltip.usage_hold")
            .withStyle(new ChatFormatting[]{ChatFormatting.DARK_GRAY, ChatFormatting.ITALIC})
      );
      pTooltipComponents.add(
         Component.translatable("item.bookofdragons.rattlestaff.tooltip.shift_bind")
            .withStyle(new ChatFormatting[]{ChatFormatting.DARK_GRAY, ChatFormatting.ITALIC})
      );
      super.appendHoverText(pStack, pLevel, pTooltipComponents, pIsAdvanced);
   }

   public static final class Hand implements HandPose {
      private static final ArmPose RATTLE_POSE = ArmPoses.custom("RATTLE_STAFF", false, (model, entity, arm) -> {
         float[] animData = ClientForgeEvents.getAnimationData(entity.getUUID());
         if (animData != null) {
            if (arm == HumanoidArm.RIGHT) {
               model.rightArm.xRot = animData[0];
               model.rightArm.yRot = animData[1];
               model.rightArm.zRot = animData[2];
            } else {
               model.leftArm.xRot = animData[0];
               model.leftArm.yRot = animData[1];
               model.leftArm.zRot = animData[2];
            }
         }
      });

      public ArmPose getArmPose(LivingEntity entityLiving, InteractionHand hand, ItemStack itemStack) {
         return ClientForgeEvents.isAnimationActive(entityLiving.getUUID()) ? RATTLE_POSE : ArmPose.EMPTY;
      }

      public boolean applyHandTransform(
         PoseStack poseStack, LocalPlayer player, HumanoidArm arm, ItemStack itemInHand, float partialTick, float equipProcess, float swingProcess
      ) {
         if (ClientForgeEvents.isAnimationActive(player.getUUID())) {
            int ticksRemaining = ClientForgeEvents.getAnimationTicks(player.getUUID());
            float animTime = 20 - ticksRemaining + partialTick;
            float shake = (float)Math.sin(animTime * 3.0F) * 3.0F;
            poseStack.mulPose(Axis.ZN.rotationDegrees(shake));
         }

         return false;
      }
   }

   private static class TargetResult {
      @Nullable
      final LivingEntity entity;
      @Nullable
      final Vec3 blockPos;

      TargetResult(@Nullable LivingEntity entity, @Nullable Vec3 blockPos) {
         this.entity = entity;
         this.blockPos = blockPos;
      }
   }
}
