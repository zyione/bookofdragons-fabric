package net.magister.liberanimalium.compat;

import net.magister.liberanimalium.compat.v20.MeleeImpl;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;

public final class Melee {
   private Melee() {
   }

   public static float damage(LivingEntity attacker, Entity target, DamageSource source, float base) {
      return MeleeImpl.damage(attacker, target, source, base);
   }

   public static float knockback(LivingEntity attacker, Entity target, DamageSource source, float base) {
      return MeleeImpl.knockback(attacker, target, source, base);
   }

   public static void postAttack(LivingEntity attacker, Entity target, DamageSource source) {
      MeleeImpl.postAttack(attacker, target, source);
   }
}
