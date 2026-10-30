package net.magister.liberanimalium.chassis.component.melee.data;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.Optional;
import net.minecraft.resources.ResourceLocation;

public record MeleeAttackDefinition(
   int windupTicks,
   int activeTicks,
   int recoveryTicks,
   int cooldownTicks,
   float damageMultiplier,
   float knockback,
   float reach,
   float hitboxAngleDegrees,
   float staminaCost,
   Optional<String> attackAnimation,
   Optional<String> swingSound,
   Optional<String> hitSound,
   Optional<MeleeAttackDefinition.OnHit> onHit,
   Optional<Float> damage
) {
   public static final Codec<MeleeAttackDefinition> CODEC = RecordCodecBuilder.create(
      instance -> instance.group(
            Codec.INT.optionalFieldOf("windup_ticks", 8).forGetter(MeleeAttackDefinition::windupTicks),
            Codec.INT.optionalFieldOf("active_ticks", 4).forGetter(MeleeAttackDefinition::activeTicks),
            Codec.INT.optionalFieldOf("recovery_ticks", 10).forGetter(MeleeAttackDefinition::recoveryTicks),
            Codec.INT.optionalFieldOf("cooldown_ticks", 40).forGetter(MeleeAttackDefinition::cooldownTicks),
            Codec.FLOAT.optionalFieldOf("damage_multiplier", 1.0F).forGetter(MeleeAttackDefinition::damageMultiplier),
            Codec.FLOAT.optionalFieldOf("knockback", 0.5F).forGetter(MeleeAttackDefinition::knockback),
            Codec.FLOAT.optionalFieldOf("reach", 2.5F).forGetter(MeleeAttackDefinition::reach),
            Codec.FLOAT.optionalFieldOf("hitbox_angle_degrees", 90.0F).forGetter(MeleeAttackDefinition::hitboxAngleDegrees),
            Codec.FLOAT.optionalFieldOf("stamina_cost", 5.0F).forGetter(MeleeAttackDefinition::staminaCost),
            Codec.STRING.optionalFieldOf("attack_animation").forGetter(MeleeAttackDefinition::attackAnimation),
            Codec.STRING.optionalFieldOf("swing_sound").forGetter(MeleeAttackDefinition::swingSound),
            Codec.STRING.optionalFieldOf("hit_sound").forGetter(MeleeAttackDefinition::hitSound),
            MeleeAttackDefinition.OnHit.CODEC.optionalFieldOf("on_hit").forGetter(MeleeAttackDefinition::onHit),
            Codec.FLOAT.optionalFieldOf("damage").forGetter(MeleeAttackDefinition::damage)
         )
         .apply(instance, MeleeAttackDefinition::new)
   );

   public record OnHit(ResourceLocation effect, int durationTicks, int amplifier, float chance) {
      public static final Codec<MeleeAttackDefinition.OnHit> CODEC = RecordCodecBuilder.create(
         instance -> instance.group(
               ResourceLocation.CODEC.fieldOf("effect").forGetter(MeleeAttackDefinition.OnHit::effect),
               Codec.INT.fieldOf("duration_ticks").forGetter(MeleeAttackDefinition.OnHit::durationTicks),
               Codec.INT.optionalFieldOf("amplifier", 0).forGetter(MeleeAttackDefinition.OnHit::amplifier),
               Codec.FLOAT.optionalFieldOf("chance", 1.0F).forGetter(MeleeAttackDefinition.OnHit::chance)
            )
            .apply(instance, MeleeAttackDefinition.OnHit::new)
      );
   }
}
