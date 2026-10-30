package net.magister.liberanimalium.chassis.component;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import net.magister.liberanimalium.body.data.CrippledFeature;
import net.magister.liberanimalium.body.effect.FaunaEffects;
import net.magister.liberanimalium.chassis.FaunaBase;
import net.magister.liberanimalium.chassis.Notices;
import net.magister.liberanimalium.chassis.component.ranged.OmniAttackHandler;
import net.magister.liberanimalium.compat.Effects;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.network.chat.Component;
import net.minecraft.util.RandomSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.player.Player;

public class IncapacitationComponent implements IFaunaComponent {
   private static final float DOWN_THRESHOLD_PCT = 0.15F;
   private static final float RECOVERY_THRESHOLD_PCT = 0.35F;
   private static final int STAGE_TICKS_BASE = 24000;
   private static final int STAGE_TICKS_INCAP_BONUS = 12000;
   private int incapCount = 0;
   private boolean isIncapacitated = false;
   private final List<CrippledFeature> crippledFeaturesOrdered = new ArrayList<>();
   private int currentStage = 0;
   private int originalStageCount = 0;
   private int stageTimerTicks = 0;
   private FaunaBase body;
   private static final int REFUSAL_TOLD_EVERY_TICKS = 40;
   private final int[] refusalToldAt = new int[CrippledFeature.values().length];

   public IncapacitationComponent() {
      Arrays.fill(this.refusalToldAt, -1073741824);
   }

   @Override
   public void onAttach(FaunaBase host) {
      this.body = host;
   }

   @Override
   public void serverTick() {
      if (this.body != null && !this.body.level().isClientSide() && this.body.isTame()) {
         if (this.isIncapacitated) {
            if (this.body.getHealth() >= this.body.getMaxHealth() * 0.35F) {
               this.exitIncapacitation(this.body.getRandom());
            }
         } else {
            if (this.body.getHealth() > 0.0F && this.body.getHealth() <= this.body.getMaxHealth() * 0.15F) {
               this.enterIncapacitation();
            }

            if (this.currentStage > 0) {
               this.tickInjuryStageTimer();
            }
         }
      }
   }

   @Override
   public void clientTick() {
   }

   public void enterIncapacitation() {
      if (!this.isIncapacitated) {
         this.isIncapacitated = true;
         this.body.setNoGravity(false);
         this.body.ejectPassengers();
         this.stopActiveAttacks();
         this.pushSyncData();
      }
   }

   public void exitIncapacitation(RandomSource rng) {
      if (this.isIncapacitated) {
         this.isIncapacitated = false;
         this.incapCount++;
         if (this.currentStage > 0) {
            this.stageTimerTicks = 0;
            this.pushSyncData();
            this.triggerStatRecalculation();
            this.reapplyInjuryEffect();
         } else {
            this.crippledFeaturesOrdered.clear();
            this.crippledFeaturesOrdered.addAll(this.rollCrippledFeatures(rng));
            this.currentStage = this.crippledFeaturesOrdered.size();
            this.originalStageCount = this.currentStage;
            this.stageTimerTicks = 0;
            this.pushSyncData();
            if (this.currentStage > 0) {
               this.triggerStatRecalculation();
               this.reapplyInjuryEffect();
            }
         }
      }
   }

   private void tickInjuryStageTimer() {
      this.stageTimerTicks++;
      int stageTicks = 24000 + this.incapCount * 12000;
      if (this.stageTimerTicks >= stageTicks) {
         this.stageTimerTicks = 0;
         this.advanceStage();
      }
   }

   private void advanceStage() {
      if (this.currentStage > 0) {
         int healedIndex = this.crippledFeaturesOrdered.size() - this.currentStage;
         if (healedIndex >= 0 && healedIndex < this.crippledFeaturesOrdered.size()) {
            this.crippledFeaturesOrdered.remove(healedIndex);
         }

         this.currentStage--;
         this.pushSyncData();
         if (this.currentStage > 0) {
            this.triggerStatRecalculation();
            this.reapplyInjuryEffect();
         } else {
            this.triggerStatRecalculation();
            Effects.remove(this.body, FaunaEffects.INJURY);
         }
      }
   }

   private List<CrippledFeature> rollCrippledFeatures(RandomSource rng) {
      int count = this.rollCrippledCount(rng);
      List<CrippledFeature> pool = new ArrayList<>(List.of(CrippledFeature.values()));

      for (int i = pool.size() - 1; i > 0; i--) {
         int j = rng.nextInt(i + 1);
         CrippledFeature tmp = pool.get(i);
         pool.set(i, pool.get(j));
         pool.set(j, tmp);
      }

      return new ArrayList<>(pool.subList(0, count));
   }

   private int rollCrippledCount(RandomSource rng) {
      int tier = Math.min(this.incapCount, 5);
      float roll = rng.nextFloat();

      return switch (tier) {
         case 1 -> roll < 0.7F ? 1 : 2;
         case 2 -> roll < 0.5F ? 1 : (roll < 0.9F ? 2 : 3);
         case 3 -> roll < 0.3F ? 1 : (roll < 0.7F ? 2 : 3);
         case 4 -> roll < 0.1F ? 1 : (roll < 0.4F ? 2 : 3);
         default -> 3;
      };
   }

   private float computeBaseDebuffFraction() {
      return 0.2F + Math.min(4, this.incapCount - 1) * 0.1F;
   }

   private float computeActiveDebuffFraction() {
      return this.computeBaseDebuffFraction() * ((float)this.currentStage / Math.max(1, this.originalStageCount));
   }

   public float getDebuffFraction() {
      return this.currentStage <= 0 ? 0.0F : this.computeActiveDebuffFraction();
   }

   private void triggerStatRecalculation() {
      StatsComponent stats = this.body.componentRegistry.get(StatsComponent.class);
      if (stats != null) {
         stats.recalculateStats();
      }
   }

   private void reapplyInjuryEffect() {
      int amplifier = Math.max(0, this.currentStage - 1);
      int remainingTicks = Math.max(20, this.ticksToMend());
      Effects.remove(this.body, FaunaEffects.INJURY);
      this.body.addEffect(Effects.instance(FaunaEffects.INJURY, remainingTicks, amplifier, false, true, true));
   }

   public boolean shouldBlockDamage(DamageSource source) {
      return !this.isIncapacitated ? false : !(source.getEntity() instanceof Player);
   }

   private void stopActiveAttacks() {
      OmniAttackHandler handler = this.body.componentRegistry.get(OmniAttackHandler.class);
      if (handler != null) {
         handler.stopBreathing();
         handler.setCharging(false);
      }
   }

   public boolean isIncapacitated() {
      return this.isIncapacitated;
   }

   public static boolean refuses(FaunaBase body, Player player, CrippledFeature feature) {
      if (!body.getSyncedIsFeatureCrippled(feature)) {
         return false;
      }

      IncapacitationComponent incap = body.componentRegistry.get(IncapacitationComponent.class);
      if (incap != null && body.tickCount - incap.refusalToldAt[feature.ordinal()] >= 40) {
         incap.refusalToldAt[feature.ordinal()] = body.tickCount;
         Notices.tell(player, Component.translatable(CrippledFeature.refusalKey(feature), new Object[]{body.getDisplayName()}));
      }

      return true;
   }

   public List<CrippledFeature> crippledFeatures() {
      return List.copyOf(this.crippledFeaturesOrdered);
   }

   public int ticksToMend() {
      if (this.currentStage <= 0) {
         return 0;
      }

      int stageTicks = 24000 + this.incapCount * 12000;
      return stageTicks - this.stageTimerTicks + (this.currentStage - 1) * stageTicks;
   }

   @Override
   public void save(CompoundTag tag) {
      tag.putBoolean("Incap_IsIncapacitated", this.isIncapacitated);
      tag.putInt("Incap_Count", this.incapCount);
      tag.putInt("Incap_CurrentStage", this.currentStage);
      tag.putInt("Incap_OriginalStageCount", this.originalStageCount);
      tag.putInt("Incap_StageTimerTicks", this.stageTimerTicks);
      ListTag featureList = new ListTag();

      for (CrippledFeature f : this.crippledFeaturesOrdered) {
         featureList.add(StringTag.valueOf(f.name()));
      }

      tag.put("Incap_CrippledFeatures", featureList);
   }

   @Override
   public void load(CompoundTag tag) {
      this.isIncapacitated = tag.getBoolean("Incap_IsIncapacitated");
      this.incapCount = tag.getInt("Incap_Count");
      this.currentStage = tag.getInt("Incap_CurrentStage");
      this.originalStageCount = tag.getInt("Incap_OriginalStageCount");
      this.stageTimerTicks = tag.getInt("Incap_StageTimerTicks");
      this.crippledFeaturesOrdered.clear();
      ListTag featureList = tag.getList("Incap_CrippledFeatures", 8);

      for (int i = 0; i < featureList.size(); i++) {
         try {
            this.crippledFeaturesOrdered.add(CrippledFeature.valueOf(featureList.getString(i)));
         } catch (IllegalArgumentException var5) {
         }
      }

      this.pushSyncData();
      if (this.currentStage > 0 && !this.body.level().isClientSide()) {
         this.triggerStatRecalculation();
         if (!Effects.has(this.body, FaunaEffects.INJURY)) {
            this.reapplyInjuryEffect();
         }
      }
   }

   private void pushSyncData() {
      if (!this.body.level().isClientSide()) {
         this.body.updateIncapSyncData(this.isIncapacitated, this.incapCount, this.getDebuffFraction(), this.crippledFeaturesOrdered);
      }
   }
}
