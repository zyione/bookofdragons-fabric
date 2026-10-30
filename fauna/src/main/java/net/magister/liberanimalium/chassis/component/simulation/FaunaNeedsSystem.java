package net.magister.liberanimalium.chassis.component.simulation;

import net.magister.liberanimalium.body.data.CrippledFeature;
import net.magister.liberanimalium.chassis.FaunaBase;
import net.magister.liberanimalium.chassis.component.IFaunaComponent;
import net.magister.liberanimalium.compat.Food;
import net.magister.liberanimalium.fauna.api.PoseFlag;
import net.magister.liberanimalium.util.SaveKeys;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.Difficulty;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.phys.Vec3;

public class FaunaNeedsSystem implements IFaunaComponent {
   public static final int MAX_HUNGER = 100;
   public static final float MAX_SATURATION = 100.0F;
   public static final int SPAWN_FOOD_LEVEL = 70;
   private static final float WILD_DRAIN_PER_TICK = 5.5555557E-4F;
   private static final int WILD_DRAIN_FLOOR = 60;
   private static final float EXHAUSTION_THRESHOLD = 4.0F;
   private static final int REGEN_TICK_INTERVAL = 40;
   public static final float EXHAUSTION_SPRINT_PER_METER = 0.1F;
   public static final float EXHAUSTION_SWIM_PER_METER = 0.01F;
   public static final float EXHAUSTION_WALK_PER_METER = 0.005F;
   public static final float EXHAUSTION_JUMP = 0.05F;
   public static final float EXHAUSTION_SPRINT_JUMP = 0.2F;
   public static final float EXHAUSTION_ATTACK = 0.1F;
   public static final float EXHAUSTION_HEAL_PER_HP = 6.0F;
   public static final float EXHAUSTION_FLY_PER_METER = 0.04F;
   public static final float EXHAUSTION_BREATH_ATTACK = 0.15F;
   public static final float EXHAUSTION_PROJECTILE_ATTACK = 2.0F;
   public static final float EXHAUSTION_FLIGHT_POWERED_PER_TICK = 0.02F;
   public static final float EXHAUSTION_FLIGHT_BOOSTING_PER_TICK = 0.05F;
   public static final float EXHAUSTION_ABILITY_TOGGLE_PER_TICK = 0.04F;
   public static final float EXHAUSTION_ABILITY_LOW = 1.0F;
   public static final float EXHAUSTION_ABILITY_MEDIUM = 2.0F;
   public static final float EXHAUSTION_ABILITY_HIGH = 4.0F;
   private FaunaBase body;
   private int foodLevel = 70;
   private float saturationLevel = 0.0F;
   private float exhaustionLevel = 0.0F;
   private int tickTimer = 0;
   private Vec3 lastPosition = null;
   private boolean isHungry = false;
   private boolean isHunting = false;
   private float wildDrainAccumulator = 0.0F;

   @Override
   public void onAttach(FaunaBase host) {
      this.body = host;
   }

   @Override
   public void serverTick() {
      Difficulty difficulty = this.body.level().getDifficulty();
      this.processMovementExhaustion();
      if (this.exhaustionLevel >= 4.0F) {
         this.exhaustionLevel -= 4.0F;
         if (this.saturationLevel > 0.0F) {
            this.saturationLevel = Math.max(0.0F, this.saturationLevel - 1.0F);
         } else if (difficulty != Difficulty.PEACEFUL) {
            this.foodLevel = Math.max(0, this.foodLevel - 1);
         }
      }

      if (!this.isHunting && this.foodLevel <= 60) {
         this.isHunting = true;
      } else if (this.isHunting && this.foodLevel >= 100) {
         this.isHunting = false;
      }

      if (!this.body.isTame()) {
         this.wildDrainAccumulator += 5.5555557E-4F;
         if (this.wildDrainAccumulator >= 1.0F) {
            this.wildDrainAccumulator--;
            if (this.foodLevel > 60) {
               if (this.saturationLevel > 0.0F) {
                  this.saturationLevel = Math.max(0.0F, this.saturationLevel - 1.0F);
               } else {
                  this.foodLevel = Math.max(60, this.foodLevel - 1);
               }
            }
         }
      }

      boolean canNaturalRegen = this.body.level().getGameRules().getBoolean(GameRules.RULE_NATURAL_REGENERATION);
      if (canNaturalRegen && this.shouldHeal()) {
         this.tickTimer++;
         if (this.tickTimer >= 40) {
            boolean asleep = this.body.hasFaunaPose(PoseFlag.SLEEPING);
            float healAmount = this.body.getStatSheet().regen() * Vitals.healingFactor(this.foodLevel, asleep);
            if (healAmount > 0.0F) {
               this.body.heal(healAmount);
               this.addExhaustion(6.0F * healAmount);
            }

            this.tickTimer = 0;
         }
      } else {
         this.tickTimer = 0;
      }

      this.updateSyncedData();
   }

   private void processMovementExhaustion() {
      Vec3 currentPos = this.body.position();
      if (this.lastPosition == null) {
         this.lastPosition = currentPos;
      } else {
         double dx = currentPos.x - this.lastPosition.x;
         double dz = currentPos.z - this.lastPosition.z;
         double horizontalDistance = Math.sqrt(dx * dx + dz * dz);
         if (this.body.isFlying()) {
            if (this.body.isAccelerating()) {
               this.addExhaustion(0.02F);
            } else if (this.body.isBoosting() && !this.body.getStaminaSystem().isStaminaDepleted()) {
               this.addExhaustion(0.05F);
            }
         } else if (horizontalDistance > 0.01) {
            if (this.body.isInWater()) {
               this.addExhaustion((float)(horizontalDistance * 0.01F));
            } else if (this.body.isSprinting()) {
               this.addExhaustion((float)(horizontalDistance * 0.1F));
            } else {
               this.addExhaustion((float)(horizontalDistance * 0.005F));
            }
         }

         this.lastPosition = currentPos;
      }
   }

   public void onJump() {
      if (this.body.isSprinting()) {
         this.addExhaustion(0.2F);
      } else {
         this.addExhaustion(0.05F);
      }
   }

   public void onAttack() {
      this.addExhaustion(0.1F);
   }

   public void onBreathAttack() {
      this.addExhaustion(0.15F);
   }

   public void onProjectileAttack() {
      this.addExhaustion(2.0F);
   }

   public boolean canUseRangedAttack() {
      return true;
   }

   public boolean isEdibleKillDrop(ItemStack stack) {
      return !stack.isEmpty() && (Food.of(stack.getItem()) != null || this.body.isFood(stack));
   }

   public boolean eat(ItemStack stack) {
      if (stack.isEmpty()) {
         return false;
      }

      FoodProperties food = Food.of(stack.getItem());
      if (food == null) {
         return false;
      }

      int nutrition = Food.nutrition(food);
      float saturation = Food.saturation(food);
      float spent = Vitals.nutritionSpentHealing(nutrition, this.body.getHealth(), this.body.getMaxHealth());
      float healed = Vitals.healthFromNutrition(spent);
      if (healed > 0.0F) {
         this.body.heal(healed);
         this.showHealTell(healed);
      }

      this.foodLevel = Math.min(100, this.foodLevel + Math.round((nutrition - spent) * 5.0F));
      this.saturationLevel = Math.min(this.foodLevel, this.saturationLevel + saturation);
      this.updateSyncedData();
      return true;
   }

   private void showHealTell(float healed) {
      if (this.body.level() instanceof ServerLevel server) {
         int var7 = Mth.clamp((int)Math.ceil(healed / 2.0F), 1, 8);
         Vec3 at = this.body.moodPoint();
         double spread = FaunaBase.moodSpread();
         server.sendParticles(ParticleTypes.HEART, at.x, at.y, at.z, var7, spread, spread, spread, 0.02);
      }
   }

   public void feed(int amount) {
      this.foodLevel = Math.min(100, this.foodLevel + amount);
      this.saturationLevel = Math.min(this.foodLevel, this.saturationLevel + amount * 0.5F);
      this.updateSyncedData();
   }

   public void addExhaustion(float amount) {
      if (!this.body.level().isClientSide()) {
         float efficiency = this.body.getStatSheet().metabolism();
         float scaledAmount = amount * (1.0F / Math.max(0.1F, efficiency));
         this.exhaustionLevel = Math.min(this.exhaustionLevel + scaledAmount, 40.0F);
      }
   }

   public boolean canSprint() {
      return !this.body.getSyncedIsFeatureCrippled(CrippledFeature.RUNNING);
   }

   private boolean shouldHeal() {
      return this.body.getHealth() > 0.0F && this.body.getHealth() < this.body.getMaxHealth();
   }

   private void updateSyncedData() {
      this.isHungry = this.foodLevel < 40;
      this.body.getEntityData().set(FaunaBase.getHungerLevelData(), this.foodLevel);
   }

   public int getFoodLevel() {
      return this.foodLevel;
   }

   public float getSaturationLevel() {
      return this.saturationLevel;
   }

   public boolean isHunting() {
      return this.isHunting;
   }

   public boolean isFull() {
      return this.foodLevel >= 100;
   }

   public float getHungerPercentage() {
      return this.foodLevel / 100.0F;
   }

   public static int getHungerThreshold() {
      return 40;
   }

   public void setFoodLevel(int level) {
      this.foodLevel = Math.max(0, Math.min(100, level));
      this.updateSyncedData();
   }

   public void setSaturationLevel(float level) {
      this.saturationLevel = Math.max(0.0F, Math.min(100.0F, level));
   }

   @Override
   public void save(CompoundTag tag) {
      CompoundTag needsTag = new CompoundTag();
      needsTag.putInt("foodLevel", this.foodLevel);
      needsTag.putFloat("saturationLevel", this.saturationLevel);
      needsTag.putFloat("exhaustionLevel", this.exhaustionLevel);
      needsTag.putInt("tickTimer", this.tickTimer);
      needsTag.putFloat("wildDrainAcc", this.wildDrainAccumulator);
      tag.put("FaunaNeeds", needsTag);
   }

   @Override
   public void load(CompoundTag tag) {
      String key = SaveKeys.of(tag, "FaunaNeeds", "dragonNeeds");
      if (tag.contains(key)) {
         CompoundTag needsTag = tag.getCompound(key);
         this.foodLevel = needsTag.getInt("foodLevel");
         this.saturationLevel = needsTag.getFloat("saturationLevel");
         this.exhaustionLevel = needsTag.getFloat("exhaustionLevel");
         this.tickTimer = needsTag.getInt("tickTimer");
         this.wildDrainAccumulator = needsTag.getFloat("wildDrainAcc");
         this.updateSyncedData();
      }
   }
}
