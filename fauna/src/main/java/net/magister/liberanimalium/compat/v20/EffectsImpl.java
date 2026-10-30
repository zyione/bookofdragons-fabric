package net.magister.liberanimalium.compat.v20;

import java.util.Optional;
import net.magister.liberanimalium.platform.registry.RegEntry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;

public final class EffectsImpl {
   private EffectsImpl() {
   }

   public static boolean has(LivingEntity entity, RegEntry<MobEffect> effect) {
      return entity.hasEffect(effect.get());
   }

   public static boolean is(MobEffectInstance instance, RegEntry<MobEffect> effect) {
      return instance.getEffect() == effect.get();
   }

   public static boolean remove(LivingEntity entity, RegEntry<MobEffect> effect) {
      return entity.removeEffect(effect.get());
   }

   public static MobEffectInstance instance(RegEntry<MobEffect> effect, int duration, int amplifier, boolean ambient, boolean visible, boolean showIcon) {
      return new MobEffectInstance(effect.get(), duration, amplifier, ambient, visible, showIcon);
   }

   public static Optional<MobEffectInstance> instance(ResourceLocation id, int duration, int amplifier) {
      return Optional.ofNullable((MobEffect)BuiltInRegistries.MOB_EFFECT.get(id))
         .map(effect -> new MobEffectInstance((MobEffect)effect, duration, amplifier));
   }
}
