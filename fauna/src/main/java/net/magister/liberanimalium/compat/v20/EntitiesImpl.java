package net.magister.liberanimalium.compat.v20;

import org.jetbrains.annotations.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.SpawnPlacements;
import net.minecraft.world.entity.SpawnPlacements.Type;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.NaturalSpawner;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.phys.Vec3;

public final class EntitiesImpl {
   private static final double PLAYER_VEHICLE_ATTACHMENT = 0.6;

   private EntitiesImpl() {
   }

   public static void ignite(Entity entity, int seconds) {
      entity.setSecondsOnFire(seconds);
   }

   @Nullable
   public static SpawnGroupData finalizeSpawn(Mob mob, ServerLevelAccessor level, DifficultyInstance difficulty, MobSpawnType reason) {
      return mob.finalizeSpawn(level, difficulty, reason, null, null);
   }

   public static void damageHeld(ItemStack stack, int amount, LivingEntity holder, InteractionHand hand) {
      stack.hurtAndBreak(amount, holder, broken -> broken.broadcastBreakEvent(hand));
   }

   public static boolean spawnsOnGround(LevelReader level, BlockPos pos, @Nullable EntityType<?> type) {
      return NaturalSpawner.isSpawnPositionOk(Type.ON_GROUND, level, pos, type);
   }

   public static boolean spawnPositionOk(EntityType<?> type, LevelReader level, BlockPos pos) {
      return NaturalSpawner.isSpawnPositionOk(SpawnPlacements.getPlacementType(type), level, pos, type);
   }

   public static boolean spawnsInWater(LevelReader level, BlockPos pos, @Nullable EntityType<?> type) {
      return NaturalSpawner.isSpawnPositionOk(Type.IN_WATER, level, pos, type);
   }

   public static float stepHeight(Entity entity) {
      return entity.maxUpStep();
   }

   public static Vec3 seat(Entity vehicle, Entity passenger) {
      double attachment = passenger instanceof Player ? 0.6 : 0.0;
      return new Vec3(vehicle.getX(), vehicle.getY() + vehicle.getBbHeight() - attachment, vehicle.getZ());
   }
}
