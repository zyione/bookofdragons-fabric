package net.magister.bookofdragons.effect;

import org.jetbrains.annotations.Nullable;
import net.magister.liberanimalium.chassis.FaunaBase;
import net.magister.liberanimalium.compat.Effects;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.phys.Vec3;

public final class Paralysis {
   public static final TagKey<EntityType<?>> IMMUNE = TagKey.create(
      Registries.ENTITY_TYPE, new ResourceLocation("bookofdragons", "paralysis_immune")
   );
   private static final String IMMUNE_UNTIL = "bookofdragons:paralysis_immune_until";
   private static final String HELD_AI = "bookofdragons:paralysis_held_ai";

   private Paralysis() {
   }

   public static boolean is(LivingEntity entity) {
      return Effects.has(entity, ModEffects.PARALYSIS);
   }

   public static boolean is(@Nullable MobEffectInstance instance) {
      return instance != null && Effects.is(instance, ModEffects.PARALYSIS);
   }

   public static boolean mayTake(LivingEntity target) {
      return target.getType().is(IMMUNE)
         ? false
         : target.level().getGameTime() >= net.magister.liberanimalium.platform.fabric.EntityData.get(target).getLong("bookofdragons:paralysis_immune_until");
   }

   public static void take(LivingEntity target, int durationTicks) {
      CompoundTag data = net.magister.liberanimalium.platform.fabric.EntityData.get(target);
      data.putLong("bookofdragons:paralysis_immune_until", immuneUntil(target.level().getGameTime(), durationTicks));
      Vec3 velocity = target.getDeltaMovement();
      target.setDeltaMovement(0.0, Math.min(velocity.y, 0.0), 0.0);
      target.hurtMarked = true;
      target.stopUsingItem();
      if (target instanceof Mob mob && !(target instanceof FaunaBase) && !mob.isNoAi()) {
         mob.setNoAi(true);
         data.putBoolean("bookofdragons:paralysis_held_ai", true);
      }
   }

   public static void release(LivingEntity entity) {
      CompoundTag data = net.magister.liberanimalium.platform.fabric.EntityData.get(entity);
      if (data.getBoolean("bookofdragons:paralysis_held_ai")) {
         data.remove("bookofdragons:paralysis_held_ai");
         if (entity instanceof Mob mob) {
            mob.setNoAi(false);
         }
      }
   }

   public static void releaseIfOver(LivingEntity entity) {
      if (!is(entity)) {
         release(entity);
      }
   }

   static long immuneUntil(long now, int durationTicks) {
      return now + 2L * durationTicks;
   }

   public static boolean byParalysed(DamageSource source) {
      return source.getEntity() instanceof LivingEntity cause && is(cause);
   }

   public static boolean sentByParalysed(Entity entity) {
      return entity instanceof Projectile projectile && projectile.getOwner() instanceof LivingEntity owner && is(owner);
   }

   public static void stopJump(LivingEntity entity) {
      Vec3 velocity = entity.getDeltaMovement();
      entity.setDeltaMovement(velocity.x, Math.min(velocity.y, 0.0), velocity.z);
   }
}
