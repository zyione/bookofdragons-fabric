package net.magister.liberanimalium.compat;

import org.jetbrains.annotations.Nullable;
import net.magister.liberanimalium.compat.v20.EntitiesImpl;
import net.minecraft.core.BlockPos;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.phys.Vec3;

public final class Entities {
   private Entities() {
   }

   public static void ignite(Entity entity, int seconds) {
      EntitiesImpl.ignite(entity, seconds);
   }

   @Nullable
   public static SpawnGroupData finalizeSpawn(Mob mob, ServerLevelAccessor level, DifficultyInstance difficulty, MobSpawnType reason) {
      return EntitiesImpl.finalizeSpawn(mob, level, difficulty, reason);
   }

   public static void damageHeld(ItemStack stack, int amount, LivingEntity holder, InteractionHand hand) {
      EntitiesImpl.damageHeld(stack, amount, holder, hand);
   }

   public static boolean spawnsOnGround(LevelReader level, BlockPos pos, @Nullable EntityType<?> type) {
      return EntitiesImpl.spawnsOnGround(level, pos, type);
   }

   public static boolean spawnPositionOk(EntityType<?> type, LevelReader level, BlockPos pos) {
      return EntitiesImpl.spawnPositionOk(type, level, pos);
   }

   public static boolean spawnsInWater(LevelReader level, BlockPos pos, @Nullable EntityType<?> type) {
      return EntitiesImpl.spawnsInWater(level, pos, type);
   }

   public static float stepHeight(Entity entity) {
      return EntitiesImpl.stepHeight(entity);
   }

   public static Vec3 seat(Entity vehicle, Entity passenger) {
      return EntitiesImpl.seat(vehicle, passenger);
   }
}
