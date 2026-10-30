package net.magister.liberanimalium.chassis.component;

import net.magister.liberanimalium.chassis.FaunaBase;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.Pose;

public class GrowthComponent implements IFaunaComponent {
   public static final int SPAWN_ANIMATION_DURATION = 30;
   protected FaunaBase body;
   private int progress;

   @Override
   public void onAttach(FaunaBase host) {
      this.body = host;
   }

   @Override
   public void serverTick() {
      this.handleGrowth();
      if (this.getSpawnScaleTicks() > 0) {
         this.setSpawnScaleTicks(this.getSpawnScaleTicks() - 1);
      }
   }

   @Override
   public void save(CompoundTag tag) {
      tag.putInt("GrowthStage", this.getGrowthStage());
      tag.putInt("GrowthProgress", this.getGrowthProgress());
      tag.putInt("SpawnScaleTicks", this.getSpawnScaleTicks());
   }

   @Override
   public void load(CompoundTag tag) {
      if (tag.contains("GrowthStage")) {
         this.setGrowthStage(tag.getInt("GrowthStage"));
      }

      if (tag.contains("GrowthProgress")) {
         this.setGrowthProgress(tag.getInt("GrowthProgress"));
      }

      if (tag.contains("SpawnScaleTicks")) {
         this.setSpawnScaleTicks(tag.getInt("SpawnScaleTicks"));
      }
   }

   public void handleGrowth() {
      if (this.getGrowthStage() < 2) {
         this.progress++;
         if (this.progress >= this.body.growthTicksPerStage()) {
            this.setGrowthStage(this.getGrowthStage() + 1);
            this.setGrowthProgress(0);
         } else if (this.progress % 20 == 0) {
            this.body.getEntityData().set(FaunaBase.getGrowthProgressData(), this.progress);
         }
      }
   }

   public int getGrowthStage() {
      return (Integer)this.body.getEntityData().get(FaunaBase.getGrowthStageData());
   }

   public void setGrowthStage(int stage) {
      this.body.getEntityData().set(FaunaBase.getGrowthStageData(), stage);
      this.body.refreshDimensions();
      this.body.recalculateStats();
   }

   public int getGrowthProgress() {
      return this.body.level().isClientSide() ? (Integer)this.body.getEntityData().get(FaunaBase.getGrowthProgressData()) : this.progress;
   }

   public void setGrowthProgress(int progress) {
      this.progress = progress;
      this.body.getEntityData().set(FaunaBase.getGrowthProgressData(), progress);
   }

   public int getSpawnScaleTicks() {
      return (Integer)this.body.getEntityData().get(FaunaBase.getSpawnScaleTicksData());
   }

   public void setSpawnScaleTicks(int ticks) {
      this.body.getEntityData().set(FaunaBase.getSpawnScaleTicksData(), ticks);
   }

   public void startSpawnAnimation() {
      this.setSpawnScaleTicks(30);
   }

   public boolean isBaby() {
      return this.getGrowthStage() < 2;
   }

   public EntityDimensions getDimensions(Pose pPose) {
      return this.body.dimensionsAtStage(this.getGrowthStage()).scale(this.body.bodyScale());
   }
}
