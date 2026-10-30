package net.magister.liberanimalium.compat.v20;

import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.enchantment.EnchantmentHelper;

public final class MeleeImpl {
   private MeleeImpl() {
   }

   public static float damage(LivingEntity attacker, Entity target, DamageSource source, float base) {
      return target instanceof LivingEntity living ? base + EnchantmentHelper.getDamageBonus(attacker.getMainHandItem(), living.getMobType()) : base;
   }

   public static float knockback(LivingEntity attacker, Entity target, DamageSource source, float base) {
      return target instanceof LivingEntity ? base + EnchantmentHelper.getKnockbackBonus(attacker) : base;
   }

   public static void postAttack(LivingEntity attacker, Entity target, DamageSource source) {
      attacker.doEnchantDamageEffects(attacker, target);
   }
}
