package net.magister.bookofdragons.entity.custom.egg;

import com.mojang.logging.LogUtils;
import java.util.Optional;
import java.util.UUID;
import net.magister.bookofdragons.entity.ModEntities;
import net.magister.bookofdragons.entity.base.dragon.DragonBase;
import net.magister.bookofdragons.entity.data.DragonType;
import net.magister.bookofdragons.entity.data.EggHatchCondition;
import net.magister.bookofdragons.item.ModItems;
import net.magister.bookofdragons.network.Strip;
import net.magister.liberanimalium.compat.Entities;
import net.magister.liberanimalium.compat.StackData;
import net.magister.liberanimalium.genetics.GeneticsCrossover;
import net.magister.liberanimalium.genetics.GeneticsNBT;
import net.magister.liberanimalium.genetics.GeneticsSpawner;
import net.magister.liberanimalium.genetics.Genotype;
import net.magister.liberanimalium.genetics.GeneticsCrossover.CrossoverResult;
import net.magister.liberanimalium.genetics.GeneticsSpawner.SpawnResult;
import net.magister.liberanimalium.genetics.config.SpeciesGeneticsConfig;
import net.magister.liberanimalium.genetics.config.SpeciesGeneticsConfigRegistry;
import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.Level.ExplosionInteraction;
import net.minecraft.world.phys.Vec3;
import org.slf4j.Logger;

public class DragonEggEntity extends Entity {
   private static final Logger LOGGER = LogUtils.getLogger();
   private static final EntityDataAccessor<String> DRAGON_TYPE = SynchedEntityData.defineId(DragonEggEntity.class, EntityDataSerializers.STRING);
   private static final EntityDataAccessor<Integer> CURRENT_HATCH_TIME = SynchedEntityData.defineId(DragonEggEntity.class, EntityDataSerializers.INT);
   private static final EntityDataAccessor<Integer> REQUIREMENT_PROGRESS = SynchedEntityData.defineId(DragonEggEntity.class, EntityDataSerializers.INT);
   private static final EntityDataAccessor<Boolean> IS_ACTIVATED = SynchedEntityData.defineId(DragonEggEntity.class, EntityDataSerializers.BOOLEAN);
   private Genotype parent1Genotype = null;
   private Genotype parent2Genotype = null;
   private boolean isFromTamedParents = false;
   private Optional<UUID> ownerUUID = Optional.empty();
   private int totalHatchTime;
   private int requiredActivationTime = 1200;
   public final DragonEggDispatcher dispatcher;

   public DragonEggEntity(EntityType<? extends DragonEggEntity> entityType, Level level) {
      super(entityType, level);
      this.totalHatchTime = 2400;
      this.setDragonType(DragonType.NIGHTFURY);
      this.noPhysics = false;
      this.setNoGravity(false);
      this.dispatcher = new DragonEggDispatcher(this);
      this.blocksBuilding = true;
   }

   public DragonEggEntity(Level level, Vec3 position, DragonType dragonType) {
      this((EntityType<? extends DragonEggEntity>)ModEntities.DRAGON_EGG_ENTITY.get(), level);
      this.setPos(position);
      this.setDragonType(dragonType);
   }

   protected void defineSynchedData() {
      this.entityData.define(DRAGON_TYPE, DragonType.NIGHTFURY.getSerializedName());
      this.entityData.define(CURRENT_HATCH_TIME, 2400);
      this.entityData.define(REQUIREMENT_PROGRESS, 0);
      this.entityData.define(IS_ACTIVATED, false);
   }

   public boolean isPushable() {
      return true;
   }

   public boolean canBeCollidedWith() {
      return true;
   }

   public boolean isPickable() {
      return true;
   }

   public void onSyncedDataUpdated(EntityDataAccessor<?> key) {
      super.onSyncedDataUpdated(key);
      if (DRAGON_TYPE.equals(key)) {
         this.refreshDimensions();
      }
   }

   public EntityDimensions getDimensions(Pose pose) {
      DragonType type = this.getDragonType();
      float scale = type.getCollisionScale();
      return EntityDimensions.scalable(0.8F * scale, 1.0F * scale);
   }

   public void tick() {
      super.tick();
      if (!this.isNoGravity()) {
         this.setDeltaMovement(this.getDeltaMovement().add(0.0, -0.04, 0.0));
      }

      this.move(MoverType.SELF, this.getDeltaMovement());
      if (this.onGround()) {
         this.setDeltaMovement(this.getDeltaMovement().multiply(0.5, 0.0, 0.5));
      }

      if (this.level().isClientSide()) {
         this.dispatcher.dispatchAnimation();
      } else {
         this.tickHatching();
      }
   }

   private void tickHatching() {
      int currentTime = this.getCurrentHatchTime();
      if (currentTime < 0) {
         if (currentTime <= -60) {
            this.discard();
         } else {
            this.setCurrentHatchTime(currentTime - 1);
         }
      } else {
         boolean isActivated = this.getIsActivated();
         if (!isActivated) {
            DragonType dragonType = this.getDragonType();
            if (!dragonType.requiresHatchConditions()) {
               this.setIsActivated(true);
            } else {
               boolean conditionsMet = this.checkHatchingConditions();
               if (conditionsMet) {
                  int currentProgress = this.getRequirementProgress();
                  if (currentProgress < this.requiredActivationTime) {
                     this.setRequirementProgress(currentProgress + 1);
                  } else {
                     this.setIsActivated(true);
                  }
               }
            }
         } else {
            if (currentTime > 0) {
               this.setCurrentHatchTime(currentTime - 1);
            } else if (currentTime == 0) {
               this.hatch();
               this.setCurrentHatchTime(-1);
            }
         }
      }
   }

   private void hatch() {
      if (this.level() instanceof ServerLevel serverLevel) {
         DragonType dragonType = this.getDragonType();
         Mob babyDragon = (Mob)dragonType.getEntityType().create(serverLevel);
         if (babyDragon != null) {
            babyDragon.moveTo(this.getX(), this.getY(), this.getZ(), 0.0F, 0.0F);
            Entities.finalizeSpawn(babyDragon, serverLevel, serverLevel.getCurrentDifficultyAt(this.blockPosition()), MobSpawnType.BREEDING);
            if (babyDragon instanceof DragonBase dragonBase) {
               if (this.isFromTamedParents && this.ownerUUID.isPresent()) {
                  dragonBase.setFromTamedParents(true);
                  dragonBase.setTamedLineageOwner(this.ownerUUID.get());
               }

               dragonBase.setAge(-24000);
               dragonBase.setGrowthStage(0);
               String dragonTypeName = dragonBase.getDragonType().name().toLowerCase();
               SpeciesGeneticsConfig config = SpeciesGeneticsConfigRegistry.getConfig(dragonTypeName);
               if (this.parent1Genotype != null && this.parent2Genotype != null) {
                  GeneticsCrossover crossover = new GeneticsCrossover(this.random);
                  CrossoverResult result = crossover.cross(this.parent1Genotype, this.parent2Genotype, config);
                  dragonBase.setGenotypeAndDraws(result.getGenotype(), result.getPaletteDraws());
               } else {
                  GeneticsSpawner spawner = new GeneticsSpawner(this.random);
                  SpawnResult wildResult = spawner.generateWildGenotype(config, dragonTypeName, this.level().getBiome(this.blockPosition()));
                  dragonBase.setGenotypeAndDraws(wildResult.genotype, wildResult.paletteDraws);
               }

               dragonBase.startSpawnAnimation();
            }

            babyDragon.moveTo(this.getX(), this.getY(), this.getZ(), this.random.nextFloat() * 360.0F, 0.0F);
            serverLevel.addFreshEntity(babyDragon);
            if (dragonType == DragonType.GRONCKLE) {
               this.level().explode(this, this.getX(), this.getY() + 0.5, this.getZ(), 3.0F, true, ExplosionInteraction.TNT);
            } else {
               this.level().playSound(null, this.blockPosition(), SoundEvents.TURTLE_EGG_HATCH, SoundSource.NEUTRAL, 1.0F, 1.0F);
            }
         }
      }
   }

   public InteractionResult interact(Player player, InteractionHand hand) {
      if (this.level().isClientSide()) {
         return InteractionResult.SUCCESS;
      } else {
         return player.isCrouching() ? this.handlePickup(player) : this.handleInfoDisplay(player);
      }
   }

   private InteractionResult handlePickup(Player player) {
      ItemStack eggItem = this.createItemStack();
      if (!player.getInventory().add(eggItem)) {
         player.drop(eggItem, false);
      }

      this.level().playSound(null, this.blockPosition(), SoundEvents.ITEM_PICKUP, SoundSource.NEUTRAL, 0.8F, 1.2F);
      this.discard();
      Strip.say(player, Component.translatable("entity.bookofdragons.dragon_egg.picked_up", new Object[]{this.getDragonType().getEntityType().getDescription()}));
      return InteractionResult.SUCCESS;
   }

   private InteractionResult handleInfoDisplay(Player player) {
      DragonType type = this.getDragonType();
      boolean isActivated = this.getIsActivated();
      if (!isActivated && type.requiresHatchConditions()) {
         int currentProgress = this.getRequirementProgress();
         float reqProgress = (float)currentProgress / this.requiredActivationTime * 100.0F;
         Strip.say(
            player,
            Component.translatable("entity.bookofdragons.dragon_egg.activation", new Object[]{type.getEntityType().getDescription(), String.format("%.1f", reqProgress)})
               .withStyle(ChatFormatting.YELLOW)
         );
         boolean conditionsMet = this.areConditionsMet();
         if (conditionsMet) {
            player.displayClientMessage(Component.translatable("entity.bookofdragons.dragon_egg.conditions_met").withStyle(ChatFormatting.GREEN), false);
         } else {
            Component conditionDesc = type.getConditionsDescription();
            player.displayClientMessage(
               Component.translatable("entity.bookofdragons.dragon_egg.conditions_not_met", new Object[]{conditionDesc}).withStyle(ChatFormatting.RED), false
            );
         }
      } else {
         int currentTime = this.getCurrentHatchTime();
         int totalTime = this.getTotalHatchTime();
         float progress = (1.0F - (float)currentTime / totalTime) * 100.0F;
         Strip.say(
            player, Component.translatable("entity.bookofdragons.dragon_egg.info", new Object[]{type.getEntityType().getDescription(), String.format("%.1f", progress)})
         );
         if (type.requiresHatchConditions()) {
            player.displayClientMessage(Component.translatable("entity.bookofdragons.dragon_egg.activated").withStyle(ChatFormatting.AQUA), false);
         }
      }

      return InteractionResult.SUCCESS;
   }

   public boolean hurt(DamageSource source, float amount) {
      return source.is(DamageTypeTags.IS_FIRE) ? false : super.hurt(source, amount);
   }

   public boolean fireImmune() {
      return true;
   }

   protected void addAdditionalSaveData(CompoundTag tag) {
      tag.putInt("EggDataVersion", 1);
      tag.putString("DragonType", this.getDragonType().getSerializedName());
      if (this.parent1Genotype != null) {
         String speciesId = this.getDragonType().name().toLowerCase();
         tag.put("Parent1Genetics", GeneticsNBT.save(this.parent1Genotype, speciesId));
      }

      if (this.parent2Genotype != null) {
         String speciesId = this.getDragonType().name().toLowerCase();
         tag.put("Parent2Genetics", GeneticsNBT.save(this.parent2Genotype, speciesId));
      }

      tag.putBoolean("IsFromTamedParents", this.isFromTamedParents);
      this.ownerUUID.ifPresent(uuid -> tag.putUUID("OwnerUUID", uuid));
      tag.putInt("TotalHatchTime", this.totalHatchTime);
      tag.putInt("CurrentHatchTime", this.getCurrentHatchTime());
      tag.putInt("RequiredActivationTime", this.requiredActivationTime);
      tag.putInt("RequirementProgress", this.getRequirementProgress());
      tag.putBoolean("IsActivated", this.getIsActivated());
   }

   protected void readAdditionalSaveData(CompoundTag tag) {
      int dataVersion = tag.getInt("EggDataVersion");
      if (tag.contains("DragonType")) {
         this.setDragonType(DragonType.fromString(tag.getString("DragonType")));
      }

      if (dataVersion >= 1) {
         String speciesId = this.getDragonType().name().toLowerCase();
         SpeciesGeneticsConfig config = SpeciesGeneticsConfigRegistry.getConfig(speciesId);
         if (tag.contains("Parent1Genetics")) {
            try {
               this.parent1Genotype = GeneticsNBT.load(tag.getCompound("Parent1Genetics"), config);
            } catch (Exception e) {
               LOGGER.warn("[GENETICS] Failed to load parent1 genotype: {}", e.getMessage());
            }
         }

         if (tag.contains("Parent2Genetics")) {
            try {
               this.parent2Genotype = GeneticsNBT.load(tag.getCompound("Parent2Genetics"), config);
            } catch (Exception e) {
               LOGGER.warn("[GENETICS] Failed to load parent2 genotype: {}", e.getMessage());
            }
         }
      } else {
         this.parent1Genotype = null;
         this.parent2Genotype = null;
      }

      this.isFromTamedParents = tag.getBoolean("IsFromTamedParents");
      if (tag.hasUUID("OwnerUUID")) {
         this.ownerUUID = Optional.of(tag.getUUID("OwnerUUID"));
      }

      this.totalHatchTime = tag.contains("TotalHatchTime") ? tag.getInt("TotalHatchTime") : 2400;
      this.setCurrentHatchTime(tag.getInt("CurrentHatchTime"));
      this.requiredActivationTime = tag.contains("RequiredActivationTime") ? tag.getInt("RequiredActivationTime") : 1200;
      this.setRequirementProgress(tag.getInt("RequirementProgress"));
      this.setIsActivated(tag.getBoolean("IsActivated"));
   }

   public ItemStack createItemStack() {
      ItemStack stack = new ItemStack(ModItems.getEggItemForType(this.getDragonType()));
      CompoundTag entityTag = new CompoundTag();
      this.addAdditionalSaveData(entityTag);
      CompoundTag itemTag = new CompoundTag();
      itemTag.put("EntityTag", entityTag);
      StackData.write(stack, itemTag);
      return stack;
   }

   public void loadFromItemStack(ItemStack stack) {
      CompoundTag itemTag = StackData.read(stack);
      if (itemTag != null && itemTag.contains("EntityTag")) {
         CompoundTag entityTag = itemTag.getCompound("EntityTag");
         this.readAdditionalSaveData(entityTag);
      }
   }

   private boolean checkHatchingConditions() {
      DragonType dragonType = this.getDragonType();
      if (!dragonType.requiresHatchConditions()) {
         return true;
      }

      EggHatchCondition[] conditions = dragonType.getHatchConditions();
      int[] parameters = dragonType.getConditionParameters();

      for (int i = 0; i < conditions.length; i++) {
         if (!conditions[i].isMet(this.level(), this.blockPosition(), parameters[i])) {
            return false;
         }
      }

      return true;
   }

   public boolean areConditionsMet() {
      return this.checkHatchingConditions();
   }

   public DragonType getDragonType() {
      return DragonType.fromString((String)this.entityData.get(DRAGON_TYPE));
   }

   public void setDragonType(DragonType dragonType) {
      this.entityData.set(DRAGON_TYPE, dragonType.getSerializedName());
      this.requiredActivationTime = 1200;
      this.totalHatchTime = 2400;
      this.setCurrentHatchTime(this.totalHatchTime);
      this.setRequirementProgress(0);
      this.setIsActivated(false);
   }

   public Genotype getParent1Genotype() {
      return this.parent1Genotype;
   }

   public void setParent1Genotype(Genotype genotype) {
      this.parent1Genotype = genotype;
   }

   public Genotype getParent2Genotype() {
      return this.parent2Genotype;
   }

   public void setParent2Genotype(Genotype genotype) {
      this.parent2Genotype = genotype;
   }

   public boolean isFromTamedParents() {
      return this.isFromTamedParents;
   }

   public void setFromTamedParents(boolean fromTamedParents) {
      this.isFromTamedParents = fromTamedParents;
   }

   public Optional<UUID> getOwnerUUID() {
      return this.ownerUUID;
   }

   public void setOwnerUUID(UUID ownerUUID) {
      this.ownerUUID = Optional.of(ownerUUID);
   }

   public int getTotalHatchTime() {
      return this.totalHatchTime;
   }

   public void setTotalHatchTime(int totalHatchTime) {
      this.totalHatchTime = totalHatchTime;
   }

   public int getCurrentHatchTime() {
      return (Integer)this.entityData.get(CURRENT_HATCH_TIME);
   }

   public void setCurrentHatchTime(int currentHatchTime) {
      this.entityData.set(CURRENT_HATCH_TIME, currentHatchTime);
   }

   public int getRequirementProgress() {
      return (Integer)this.entityData.get(REQUIREMENT_PROGRESS);
   }

   public void setRequirementProgress(int progress) {
      this.entityData.set(REQUIREMENT_PROGRESS, progress);
   }

   public boolean getIsActivated() {
      return (Boolean)this.entityData.get(IS_ACTIVATED);
   }

   public void setIsActivated(boolean activated) {
      this.entityData.set(IS_ACTIVATED, activated);
   }

   public int getRequiredActivationTime() {
      return this.requiredActivationTime;
   }
}
