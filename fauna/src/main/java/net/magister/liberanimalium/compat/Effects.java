package net.magister.liberanimalium.compat;

import java.util.Optional;
import net.magister.liberanimalium.compat.v20.EffectsImpl;
import net.magister.liberanimalium.platform.registry.RegEntry;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;

public final class Effects {
   private Effects() {
   }

   public static boolean has(LivingEntity entity, RegEntry<MobEffect> effect) {
      return EffectsImpl.has(entity, effect);
   }

   public static boolean is(MobEffectInstance instance, RegEntry<MobEffect> effect) {
      return EffectsImpl.is(instance, effect);
   }

   public static boolean remove(LivingEntity entity, RegEntry<MobEffect> effect) {
      return EffectsImpl.remove(entity, effect);
   }

   public static MobEffectInstance instance(RegEntry<MobEffect> effect, int duration, int amplifier, boolean ambient, boolean visible, boolean showIcon) {
      return EffectsImpl.instance(effect, duration, amplifier, ambient, visible, showIcon);
   }

   public static Optional<MobEffectInstance> instance(ResourceLocation id, int duration, int amplifier) {
      return EffectsImpl.instance(id, duration, amplifier);
   }
}
