package net.magister.liberanimalium.chassis.component.simulation;

import net.magister.liberanimalium.chassis.FaunaBase;
import net.magister.liberanimalium.fauna.api.PoseFlag;
import net.magister.liberanimalium.util.Quantum;
import net.magister.liberanimalium.util.SaveKeys;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.Mth;

public class FaunaStaminaSystem {
   private static final float DEFAULT_MAX_STAMINA = 100.0F;
   private static final float DEFAULT_REGEN_RATE = 0.5F;
   private static final int HUNGER_REGEN_BLOCK_THRESHOLD = 20;
   private static final int REGEN_PAUSE_TICKS = 40;
   private static final int REGEN_PAUSE_ON_EMPTY = 200;
   public static final float COST_BOOST_PER_TICK = 0.5F;
   public static final float COST_BRAKE_PER_TICK = 0.3F;
   public static final float COST_STRAFE_PER_TICK = 0.3F;
   public static final float COST_TAKEOFF = 8.0F;
   public static final float COST_THROTTLE_ACCEL_PER_TICK = 0.1F;
   public static final float COST_BREATH_PER_TICK = 1.4F;
   public static final float COST_PROJECTILE = 10.0F;
   public static final float COST_SECONDARY_ABILITY = 15.0F;
   private final FaunaBase body;
   private float currentStamina;
   private float maxStamina;
   private float regenRate;
   private boolean isDraining = false;
   private int regenPauseTicks = 0;

   public FaunaStaminaSystem(FaunaBase body) {
      this.body = body;
      this.maxStamina = 100.0F;
      this.regenRate = 0.5F;
      this.currentStamina = this.maxStamina;
   }

   public void tick() {
      if (!this.body.level().isClientSide()) {
         if (this.body.getNeedsSystem().getFoodLevel() <= 0) {
            this.currentStamina = Math.max(0.0F, this.currentStamina - this.regenRate);
         } else if (!this.isDraining && this.regenPauseTicks <= 0) {
            int foodLevel = this.body.getNeedsSystem().getFoodLevel();
            boolean isHungry = foodLevel <= 20;
            boolean isSleeping = this.body.hasFaunaPose(PoseFlag.SLEEPING);
            if (!isHungry || isSleeping) {
               float hungerPercent = this.body.getNeedsSystem().getHungerPercentage();
               float effectiveRegen = this.regenRate * hungerPercent;
               this.currentStamina = Math.min(this.maxStamina, this.currentStamina + effectiveRegen);
            }
         }

         if (this.regenPauseTicks > 0) {
            this.regenPauseTicks--;
         }

         boolean wasDraining = this.isDraining;
         this.isDraining = false;
         if (wasDraining && this.currentStamina > 0.0F) {
            this.regenPauseTicks = Math.max(this.regenPauseTicks, 40);
         }

         this.updateSyncedData();
      }
   }

   public boolean drainContinuous(float cost) {
      if (this.body.level().isClientSide()) {
         return this.currentStamina > 0.0F;
      }

      if (this.currentStamina <= 0.0F) {
         return false;
      }

      float oldStamina = this.currentStamina;
      this.currentStamina = Math.max(0.0F, this.currentStamina - cost);
      this.isDraining = true;
      if (this.currentStamina <= 0.0F && oldStamina > 0.0F) {
         this.regenPauseTicks = 200;
      }

      return true;
   }

   public boolean tryConsume(float cost) {
      if (this.body.level().isClientSide()) {
         return this.currentStamina >= cost;
      }

      if (this.currentStamina < cost) {
         return false;
      }

      this.currentStamina -= cost;
      this.regenPauseTicks = 40;
      return true;
   }

   public boolean hasStamina(float amount) {
      return this.currentStamina >= amount;
   }

   public float getStaminaPercentage() {
      return this.maxStamina > 0.0F ? this.currentStamina / this.maxStamina : 0.0F;
   }

   public float getCurrentStamina() {
      return this.currentStamina;
   }

   public boolean isStaminaDepleted() {
      if (this.body.level().isClientSide()) {
         float syncedStamina = (Float)this.body.getEntityData().get(FaunaBase.getStaminaData());
         return syncedStamina <= 0.0F;
      } else {
         return this.currentStamina <= 0.0F;
      }
   }

   public void setMaxStamina(float max) {
      this.maxStamina = Math.max(1.0F, max);
      this.currentStamina = Math.min(this.currentStamina, this.maxStamina);
   }

   public void setRegenRate(float rate) {
      this.regenRate = Math.max(0.0F, rate);
   }

   private void updateSyncedData() {
      this.body.getEntityData().set(FaunaBase.getStaminaData(), Quantum.step(this.currentStamina, 0.5F));
      this.body.getEntityData().set(FaunaBase.getMaxStaminaData(), Quantum.step(this.maxStamina, 0.5F));
   }

   public CompoundTag save(CompoundTag tag) {
      CompoundTag staminaTag = new CompoundTag();
      staminaTag.putInt("regenPauseTicks", this.regenPauseTicks);
      staminaTag.putFloat("current", this.currentStamina);
      tag.put("FaunaStamina", staminaTag);
      return tag;
   }

   public void load(CompoundTag tag) {
      String key = SaveKeys.of(tag, "FaunaStamina", "dragonStamina");
      if (tag.contains(key)) {
         CompoundTag staminaTag = tag.getCompound(key);
         this.regenPauseTicks = staminaTag.getInt("regenPauseTicks");
         this.regenPauseTicks = Math.max(0, this.regenPauseTicks);
         if (staminaTag.contains("current")) {
            this.currentStamina = Mth.clamp(staminaTag.getFloat("current"), 0.0F, this.maxStamina);
         }

         this.updateSyncedData();
      }
   }
}
