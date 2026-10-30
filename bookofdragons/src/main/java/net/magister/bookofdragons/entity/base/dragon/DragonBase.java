package net.magister.bookofdragons.entity.base.dragon;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Supplier;
import net.magister.bookofdragons.advancement.DragonMilestones;
import net.magister.bookofdragons.compat.PathCosts;
import net.magister.bookofdragons.discovery.DiscoveryManager;
import net.magister.bookofdragons.effect.Paralysis;
import net.magister.bookofdragons.entity.component.FeedingComponent;
import net.magister.bookofdragons.entity.component.InventoryComponent;
import net.magister.bookofdragons.entity.component.LineageComponent;
import net.magister.bookofdragons.entity.component.RegistryComponent;
import net.magister.bookofdragons.entity.component.ShearingComponent;
import net.magister.bookofdragons.entity.component.TamingComponent;
import net.magister.bookofdragons.entity.component.interaction.FeedingInteractionHandler;
import net.magister.bookofdragons.entity.component.interaction.SaddleInteractionHandler;
import net.magister.bookofdragons.entity.component.interaction.ShearingInteractionHandler;
import net.magister.bookofdragons.entity.component.interaction.SpeciesChannelInteractionHandler;
import net.magister.bookofdragons.entity.component.interaction.TamingInteractionHandler;
import net.magister.bookofdragons.entity.data.DragonType;
import net.magister.bookofdragons.entity.data.PartWounds;
import net.magister.bookofdragons.item.ModItems;
import net.magister.bookofdragons.item.custom.RattlestaffItem;
import net.magister.bookofdragons.network.ControlNetwork;
import net.magister.bookofdragons.network.Strip;
import net.magister.bookofdragons.network.message.BookStripMessage;
import net.magister.bookofdragons.sound.RefusalTelegraph;
import net.magister.liberanimalium.body.data.FaunaWeightClass;
import net.magister.liberanimalium.body.gameplay.SpeciesGameplayProfile;
import net.magister.liberanimalium.body.gameplay.SpeciesGameplayRegistry;
import net.magister.liberanimalium.body.gameplay.SpeciesGameplayProfile.SpeciesChannel;
import net.magister.liberanimalium.body.locomotion.LocomotionMode;
import net.magister.liberanimalium.body.part.FaunaPartEntity;
import net.magister.liberanimalium.body.stats.BondLevel;
import net.magister.liberanimalium.body.stats.FaunaProfile;
import net.magister.liberanimalium.body.stats.FaunaProfile.Builder;
import net.magister.liberanimalium.chassis.FaunaBase;
import net.magister.liberanimalium.chassis.component.FaunaComponentRegistry;
import net.magister.liberanimalium.chassis.component.RollComponent;
import net.magister.liberanimalium.chassis.component.interaction.IFaunaInteractionHandler;
import net.magister.liberanimalium.chassis.component.melee.OmniMeleeAttackHandler;
import net.magister.liberanimalium.chassis.component.ranged.OmniAttackHandler;
import net.magister.liberanimalium.compat.SyncedDataSink;
import net.magister.liberanimalium.fauna.api.Check;
import net.magister.liberanimalium.fauna.api.Delivery;
import net.magister.liberanimalium.fauna.api.MoveIntent;
import net.magister.liberanimalium.fauna.nature.Nature;
import net.magister.liberanimalium.platform.registry.Regs;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.TagKey;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.Entity.RemovalReason;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

public abstract class DragonBase extends FaunaBase {
   public static final int STAGE_TINY_TOOTH = 0;
   public static final int STAGE_SHORT_WING = 1;
   public static final int STAGE_BROAD_WING = 2;
   public static final int STAGE_TITAN_WING = 3;
   public static final float DRAGON_SWIM = 0.6F;
   private static final EntityDataAccessor<Boolean> DATA_AWAITING_TAMING_RITUAL = SynchedEntityData.defineId(DragonBase.class, EntityDataSerializers.BOOLEAN);
   private static final EntityDataAccessor<Integer> DATA_TAMING_RITUAL_TIMER = SynchedEntityData.defineId(DragonBase.class, EntityDataSerializers.INT);
   private static final EntityDataAccessor<Boolean> DATA_TAMING_RITUAL_COMPLETED = SynchedEntityData.defineId(
      DragonBase.class, EntityDataSerializers.BOOLEAN
   );
   private static final EntityDataAccessor<Boolean> DATA_IS_FROM_TAMED_PARENTS = SynchedEntityData.defineId(DragonBase.class, EntityDataSerializers.BOOLEAN);
   public static final EntityDataAccessor<Optional<UUID>> DATA_TAMED_LINEAGE_OWNER = SynchedEntityData.defineId(
      DragonBase.class, EntityDataSerializers.OPTIONAL_UUID
   );
   private static final EntityDataAccessor<Integer> DATA_SHEARING_COOLDOWN_TICKS = SynchedEntityData.defineId(
      DragonBase.class, EntityDataSerializers.INT
   );
   public static final TagKey<Item> STAPLE = TagKey.create(Registries.ITEM, ResourceLocation.tryParse("bookofdragons:dragon_food"));
   private TamingInteractionHandler touchHandler;
   public static final List<Supplier<IFaunaInteractionHandler>> FIRST_HANDLERS = new CopyOnWriteArrayList<>();
   private static final int GROUND_REFUSAL_TICKS = 20;
   private static final double GROUND_REFUSAL_REACH = 3.0;
   private final Set<UUID> refusedOnTheGround = new HashSet<>();
   private boolean wasFlying;
   private boolean wasDodging;
   private static final EntityDataAccessor<Boolean> DATA_WOUND_GROUNDED = SynchedEntityData.defineId(DragonBase.class, EntityDataSerializers.BOOLEAN);
   private float woundDamage;
   private int woundGroundedTicks;
   private static final EntityDataAccessor<Boolean> DATA_HAS_CHEST = SynchedEntityData.defineId(DragonBase.class, EntityDataSerializers.BOOLEAN);
   private static final EntityDataAccessor<Boolean> DATA_IS_SADDLED = SynchedEntityData.defineId(DragonBase.class, EntityDataSerializers.BOOLEAN);

   protected void sayToCourter(Player who, String kind, @Nullable Check check, Component line) {
      if (who instanceof ServerPlayer player) {
         String id = check == null ? "" : check.id();
         AskReading.say(this, player, kind, id.substring(id.indexOf(58) + 1));
      }
   }

   public int awardGift(LivingEntity giver, ItemStack stack, Delivery delivery) {
      if (giver instanceof ServerPlayer player && !stack.isEmpty()) {
         boolean gift = delivery == Delivery.GIFT;
         DiscoveryManager.learn(player, this.getDragonType(), (gift ? "gift:" : "food:") + BuiltInRegistries.ITEM.getKey(stack.getItem()));
      }

      return super.awardGift(giver, stack, delivery);
   }

   public DragonBase(EntityType<? extends TamableAnimal> entityType, Level level) {
      super(entityType, level);
      PathCosts.applyDragon(this);
   }

   protected static Builder dragonProfile() {
      return FaunaProfile.builder().canFly(true).canBeRidden(true).tameable(true).locomotion(LocomotionMode.WALK, 1.0F).locomotion(LocomotionMode.SWIM, 0.6F);
   }

   protected final void registerJawsAndBreath(FaunaComponentRegistry registry) {
      registry.register(OmniMeleeAttackHandler.fromGameplay());
      registry.register(new OmniAttackHandler());
   }

   public boolean isOnFire() {
      return false;
   }

   public void setRemainingFireTicks(int pTicks) {
   }

   public boolean fireImmune() {
      return true;
   }

   public boolean canFreeze() {
      return false;
   }

   public static EntityDataAccessor<Boolean> getIsFromTamedParentsData() {
      return DATA_IS_FROM_TAMED_PARENTS;
   }

   public static EntityDataAccessor<Integer> getShearingCooldownTicksData() {
      return DATA_SHEARING_COOLDOWN_TICKS;
   }

   public boolean isTame() {
      return this.getOwnerUUID() != null && this.isTamingRitualCompleted();
   }

   public void adopt(UUID courter) {
      if (!this.level().isClientSide()) {
         this.setTamingRitualCompleted(true);
      }

      super.adopt(courter);
   }

   public boolean isAwaitingTamingRitual() {
      return (Boolean)this.entityData.get(DATA_AWAITING_TAMING_RITUAL);
   }

   public void setAwaitingTamingRitual(boolean awaiting) {
      this.entityData.set(DATA_AWAITING_TAMING_RITUAL, awaiting);
   }

   public int getTamingRitualTimer() {
      return (Integer)this.entityData.get(DATA_TAMING_RITUAL_TIMER);
   }

   public void setTamingRitualTimer(int timer) {
      this.entityData.set(DATA_TAMING_RITUAL_TIMER, timer);
   }

   public boolean isTamingRitualCompleted() {
      return (Boolean)this.entityData.get(DATA_TAMING_RITUAL_COMPLETED);
   }

   public void setTamingRitualCompleted(boolean completed) {
      this.entityData.set(DATA_TAMING_RITUAL_COMPLETED, completed);
   }

   public String speciesId() {
      return this.getDragonType().getSerializedName();
   }

   public abstract DragonType getDragonType();

   public FaunaWeightClass getWeightClass() {
      SpeciesGameplayProfile gameplay = this.gameplay();
      return gameplay != null && gameplay.weightClass() != null ? gameplay.weightClass() : SpeciesGameplayRegistry.weightOf(this.speciesId());
   }

   @org.jetbrains.annotations.Nullable
   public SpeciesChannel speciesChannel() {
      SpeciesGameplayProfile profile = this.gameplay();
      return profile == null ? null : profile.channel();
   }

   public ItemStack getScaleItem() {
      return ItemStack.EMPTY;
   }

   @Nullable
   public AgeableMob getBreedOffspring(ServerLevel serverLevel, AgeableMob ageableMob) {
      return null;
   }

   public boolean isFavoriteFood(ItemStack stack) {
      SpeciesGameplayProfile profile = this.gameplay();
      return profile != null && !stack.isEmpty() && profile.favoriteFoods().contains(Regs.itemKey(stack.getItem()));
   }

   public boolean isSpecialFood(ItemStack stack) {
      SpeciesGameplayProfile profile = this.gameplay();
      return profile != null && !stack.isEmpty() && profile.specialFoods().contains(Regs.itemKey(stack.getItem()));
   }

   public boolean isGeneralFood(ItemStack stack) {
      if (stack.isEmpty()) {
         return false;
      }

      if (stack.is(STAPLE)) {
         return true;
      }

      SpeciesGameplayProfile profile = this.gameplay();
      return profile != null && profile.generalFoods().contains(Regs.itemKey(stack.getItem()));
   }

   public boolean isDislikedFood(ItemStack stack) {
      SpeciesGameplayProfile profile = this.gameplay();
      return profile != null && !stack.isEmpty() && profile.dislikedFoods().contains(Regs.itemKey(stack.getItem()));
   }

   public void refuse(Player who, ItemStack stack) {
      if (who instanceof ServerPlayer player) {
         RefusalTelegraph.emit(this);
         String item = BuiltInRegistries.ITEM.getKey(stack.getItem()).toString();
         DiscoveryManager.learn(player, this.getDragonType(), "dislike:" + item);
         ControlNetwork.sendToPlayer(new BookStripMessage(this.getId(), "refuses", item), player);
      }
   }

   protected float relish(ItemStack stack) {
      if (this.isSpecialFood(stack)) {
         return 2.0F;
      } else {
         return this.isFavoriteFood(stack) ? 1.5F : 1.0F;
      }
   }

   public boolean isHoardItem(ItemStack stack) {
      SpeciesGameplayProfile profile = this.gameplay();
      return profile != null && !stack.isEmpty() && profile.hoardItems().contains(Regs.itemKey(stack.getItem()));
   }

   public boolean isFood(ItemStack pStack) {
      return !this.isDislikedFood(pStack) && (this.isFavoriteFood(pStack) || this.isSpecialFood(pStack) || this.isGeneralFood(pStack));
   }

   protected float tierCraving(ItemStack stack) {
      if (this.isDislikedFood(stack)) {
         return 0.0F;
      } else if (this.isFavoriteFood(stack) || this.isHoardItem(stack)) {
         return 1.0F;
      } else if (this.isSpecialFood(stack)) {
         return 0.66F;
      } else {
         return this.isGeneralFood(stack) ? 0.33F : 0.12F;
      }
   }

   public Nature nature() {
      return DragonNatures.of(this.getDragonType());
   }

   public int getShearingCooldownTicks() {
      return ((ShearingComponent)this.componentRegistry.get(ShearingComponent.class)).getShearingCooldownTicks();
   }

   public void setShearingCooldownTicks(int ticks) {
      ((ShearingComponent)this.componentRegistry.get(ShearingComponent.class)).setShearingCooldownTicks(ticks);
   }

   protected void registerHostComponents(FaunaComponentRegistry registry) {
      registry.register(new InventoryComponent());
      registry.register(new TamingComponent());
      registry.register(new FeedingComponent());
      registry.register(new LineageComponent());
      registry.register(new RegistryComponent());
      registry.register(new ShearingComponent());
   }

   protected List<IFaunaInteractionHandler> buildInteractionHandlers() {
      List<IFaunaInteractionHandler> chain = new ArrayList<>();

      for (Supplier<IFaunaInteractionHandler> first : FIRST_HANDLERS) {
         chain.add(first.get());
      }

      chain.add(new ShearingInteractionHandler());
      if (this.supportsRiding()) {
         chain.add(new SaddleInteractionHandler());
      }

      chain.addAll(super.buildInteractionHandlers());
      chain.add(new FeedingInteractionHandler());
      chain.add(new SpeciesChannelInteractionHandler());
      this.touchHandler = new TamingInteractionHandler();
      chain.add(this.touchHandler);
      return chain;
   }

   protected void tickHostInteractions() {
      this.touchHandler.tickHolds(this);
   }

   protected void tickHost() {
      ((LineageComponent)this.componentRegistry.get(LineageComponent.class)).handleTamedLineage();
      if (!this.level().isClientSide()) {
         this.tickRideMilestones();
         this.tickWound();
         if (this.tickCount % 20 == 0) {
            this.refuseOnTheGround();
         }
      }
   }

   private void refuseOnTheGround() {
      if (this.refusedOnTheGround.size() > 32) {
         this.refusedOnTheGround.clear();
      }

      for (ItemEntity item : this.level().getEntitiesOfClass(ItemEntity.class, this.getBoundingBox().inflate(3.0))) {
         if (this.isDislikedFood(item.getItem()) && this.refusedOnTheGround.add(item.getUUID()) && item.getOwner() instanceof Player dropper) {
            this.refuse(dropper, item.getItem());
         }
      }
   }

   private void tickRideMilestones() {
      boolean flying = this.isFlying();
      RollComponent roll = (RollComponent)this.componentRegistry.get(RollComponent.class);
      boolean dodging = roll != null && roll.dodging();
      if (this.getControllingPassenger() instanceof Player rider) {
         if (flying && !this.wasFlying) {
            DragonMilestones.reach(rider, "takeoff", this);
         }

         if (dodging && !this.wasDodging) {
            DragonMilestones.reach(rider, "dodge", this);
         }
      }

      this.wasFlying = flying;
      this.wasDodging = dodging;
   }

   @Nullable
   private PartWounds.Wound wound() {
      return PartWounds.of(this.speciesId());
   }

   public boolean isGroundedByWound() {
      return (Boolean)this.entityData.get(DATA_WOUND_GROUNDED);
   }

   public boolean hurtThrough(FaunaPartEntity part, DamageSource source, float amount) {
      boolean hurt = super.hurtThrough(part, source, amount);
      if (hurt && !this.level().isClientSide() && !this.isGroundedByWound()) {
         PartWounds.Wound wound = this.wound();
         if (wound != null && part.partName.equals(wound.part())) {
            this.woundDamage += amount;
            if (this.woundDamage >= wound.limit()) {
               this.woundGroundedTicks = wound.groundedTicks();
               this.entityData.set(DATA_WOUND_GROUNDED, true);
               this.tellWound(wound, "lame");
            }

            return hurt;
         } else {
            return hurt;
         }
      } else {
         return hurt;
      }
   }

   private void tickWound() {
      if (this.isGroundedByWound()) {
         if (--this.woundGroundedTicks <= 0) {
            this.woundDamage = 0.0F;
            this.entityData.set(DATA_WOUND_GROUNDED, false);
            PartWounds.Wound wound = this.wound();
            if (wound != null) {
               this.tellWound(wound, "mended");
            }
         }
      } else if (this.woundDamage > 0.0F) {
         PartWounds.Wound wound = this.wound();
         this.woundDamage = wound == null ? 0.0F : Math.max(0.0F, this.woundDamage - wound.mendPerTick());
      }
   }

   private void tellWound(PartWounds.Wound wound, String state) {
      Component line = Component.translatable("book.bookofdragons.strip." + wound.line() + "." + state, new Object[]{this.getDisplayName()});
      Player told = this.getControllingPassenger() instanceof Player rider ? rider : null;
      if (told != null) {
         Strip.say(told, line);
      }

      if (this.getOwner() instanceof Player owner && owner != told) {
         Strip.say(owner, line);
      }
   }

   public void addAdditionalSaveData(CompoundTag tag) {
      super.addAdditionalSaveData(tag);
      tag.putFloat("WoundDamage", this.woundDamage);
      tag.putInt("WoundGroundedTicks", this.isGroundedByWound() ? this.woundGroundedTicks : 0);
   }

   public void readAdditionalSaveData(CompoundTag tag) {
      super.readAdditionalSaveData(tag);
      this.woundDamage = tag.getFloat("WoundDamage");
      this.woundGroundedTicks = tag.getInt("WoundGroundedTicks");
      this.entityData.set(DATA_WOUND_GROUNDED, this.woundGroundedTicks > 0);
   }

   public boolean isParalysed() {
      return Paralysis.is(this);
   }

   public void applyMove(@Nullable MoveIntent intent) {
      super.applyMove(this.isParalysed() ? null : intent);
   }

   public boolean isModeAvailable(LocomotionMode mode) {
      if (mode == LocomotionMode.FLY && this.isParalysed() && !this.isFlying()) {
         return false;
      } else {
         return mode == LocomotionMode.FLY && this.isGroundedByWound() ? false : super.isModeAvailable(mode);
      }
   }

   protected boolean heldStill() {
      return this.isParalysed();
   }

   public boolean strike(UUID targetId) {
      return !this.isParalysed() && super.strike(targetId);
   }

   public boolean hop(double rise) {
      return !this.isParalysed() && super.hop(rise);
   }

   protected boolean canCall() {
      return super.canCall() && !this.isParalysed();
   }

   public void holdTrigger(boolean pressed) {
      if (!pressed || !this.isParalysed()) {
         super.holdTrigger(pressed);
         if (pressed && !this.level().isClientSide() && this.getControllingPassenger() instanceof Player rider) {
            DragonMilestones.reach(rider, "breath", this);
         }
      }
   }

   public int touchHold(UUID courter) {
      return this.touchHandler.holdFor(courter);
   }

   public void startEating(ItemEntity foodItem) {
      ((FeedingComponent)this.componentRegistry.get(FeedingComponent.class)).startEating(foodItem);
   }

   public void startEatingCarried() {
      ((FeedingComponent)this.componentRegistry.get(FeedingComponent.class)).startEatingCarried();
   }

   public boolean isEating() {
      return ((FeedingComponent)this.componentRegistry.get(FeedingComponent.class)).isEating();
   }

   protected void finalizeHostSpawn() {
      if (((LineageComponent)this.componentRegistry.get(LineageComponent.class)).isFromTamedParents()) {
         ((LineageComponent)this.componentRegistry.get(LineageComponent.class))
            .getTamedLineageOwner()
            .ifPresent(ownerUUID -> this.setBond(ownerUUID, BondLevel.TOLERANT.getMinAffection()));
      }
   }

   public void setFromTamedParents(boolean fromTamedParents) {
      ((LineageComponent)this.componentRegistry.get(LineageComponent.class)).setFromTamedParents(fromTamedParents);
   }

   public void setTamedLineageOwner(UUID ownerUUID) {
      ((LineageComponent)this.componentRegistry.get(LineageComponent.class)).setTamedLineageOwner(ownerUUID);
   }

   protected void defineData(SyncedDataSink data) {
      super.defineData(data);
      data.define(DATA_AWAITING_TAMING_RITUAL, false);
      data.define(DATA_TAMING_RITUAL_TIMER, 0);
      data.define(DATA_TAMING_RITUAL_COMPLETED, false);
      data.define(DATA_IS_FROM_TAMED_PARENTS, false);
      data.define(DATA_TAMED_LINEAGE_OWNER, Optional.empty());
      data.define(DATA_SHEARING_COOLDOWN_TICKS, 0);
      data.define(DATA_HAS_CHEST, false);
      data.define(DATA_IS_SADDLED, false);
      data.define(DATA_WOUND_GROUNDED, false);
   }

   public static EntityDataAccessor<Boolean> getHasChestData() {
      return DATA_HAS_CHEST;
   }

   public static EntityDataAccessor<Boolean> getIsSaddledData() {
      return DATA_IS_SADDLED;
   }

   public boolean isSaddled() {
      return (Boolean)this.entityData.get(DATA_IS_SADDLED);
   }

   public void setSaddled(boolean saddled) {
      this.entityData.set(DATA_IS_SADDLED, saddled);
   }

   public boolean hasChest() {
      return (Boolean)this.entityData.get(DATA_HAS_CHEST);
   }

   public void setHasChest(boolean hasChest) {
      this.entityData.set(DATA_HAS_CHEST, hasChest);
   }

   public boolean isSaddleable() {
      return this.supportsRiding() && !this.refusesTack() ? this.isTame() && !this.isBaby() : false;
   }

   public void equipSaddle(@Nullable SoundSource pSource) {
      this.setSaddled(true);
      if (pSource != null) {
         this.level().playSound(null, this, SoundEvents.HORSE_SADDLE, pSource, 0.5F, 1.0F);
      }
   }

   public SimpleContainer getInventory() {
      return ((InventoryComponent)this.componentRegistry.get(InventoryComponent.class)).getInventory();
   }

   public void openGui(Player player) {
      if (player instanceof ServerPlayer serverPlayer) {
         ((InventoryComponent)this.componentRegistry.get(InventoryComponent.class)).openGui(serverPlayer);
      }
   }

   protected void shedTack() {
      ((InventoryComponent)this.componentRegistry.get(InventoryComponent.class)).dropSaddle();
   }

   protected void dropEquipment() {
      super.dropEquipment();
      ((InventoryComponent)this.componentRegistry.get(InventoryComponent.class)).dropEquipment();
   }

   public InteractionResult mobInteract(Player pPlayer, InteractionHand pHand) {
      ItemStack itemstack = pPlayer.getItemInHand(pHand);
      boolean isOwner = this.getOwnerUUID() != null && pPlayer.getUUID().equals(this.getOwnerUUID());
      if (pPlayer.isShiftKeyDown() && itemstack.is((Item)ModItems.BOOK_OF_DRAGONS.get()) && this.isTame() && isOwner) {
         return InteractionResult.PASS;
      } else {
         return itemstack.getItem() instanceof RattlestaffItem && this.isTame() && isOwner ? InteractionResult.PASS : super.mobInteract(pPlayer, pHand);
      }
   }

   protected void onOwnerChanged(UUID was, @Nullable UUID now) {
      LineageComponent lineage = (LineageComponent)this.componentRegistry.get(LineageComponent.class);
      RegistryComponent registry = (RegistryComponent)this.componentRegistry.get(RegistryComponent.class);
      if (now == null) {
         this.setTamingRitualCompleted(false);
         this.setAwaitingTamingRitual(false);
         lineage.forgetOwner();
         ((InventoryComponent)this.componentRegistry.get(InventoryComponent.class)).dropEquipment();
         registry.removeFromRegistry(was);
      } else {
         if (lineage.getTamedLineageOwner().isPresent()) {
            lineage.setTamedLineageOwner(now);
         }

         registry.handedOver(was);
      }
   }

   public void die(DamageSource pDamageSource) {
      ((RegistryComponent)this.componentRegistry.get(RegistryComponent.class)).removeFromRegistry();
      super.die(pDamageSource);
   }

   public void remove(RemovalReason pReason) {
      if (pReason == RemovalReason.KILLED || pReason == RemovalReason.DISCARDED) {
         ((RegistryComponent)this.componentRegistry.get(RegistryComponent.class)).removeFromRegistry();
      }

      super.remove(pReason);
   }
}
