package net.magister.liberanimalium.attack.client.particle;

import com.mojang.serialization.Codec;
import net.magister.liberanimalium.platform.Services;
import net.magister.liberanimalium.platform.registry.RegEntry;
import net.magister.liberanimalium.platform.registry.Registrar;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.core.particles.ParticleOptions.Deserializer;
import net.minecraft.core.registries.Registries;

public final class FaunaParticles {
   public static final Registrar<ParticleType<?>> PARTICLE_TYPES = Services.registries().create(Registries.PARTICLE_TYPE, "liberanimalium");
   public static final RegEntry<ParticleType<AttackParticleOptions>> ATTACK_PARTICLE_TYPE = PARTICLE_TYPES.register(
      "attack_particle", () -> new ParticleType<AttackParticleOptions>(false, AttackParticleOptions.DESERIALIZER) {
         public Codec<AttackParticleOptions> codec() {
            return AttackParticleOptions.CODEC;
         }
      }
   );
   public static final RegEntry<SimpleParticleType> BREATH_PUFF = PARTICLE_TYPES.register("breath_puff", () -> new SimpleParticleType(false));
   public static final RegEntry<SimpleParticleType> WING_DUST = PARTICLE_TYPES.register("wing_dust", () -> new SimpleParticleType(false));
   public static final RegEntry<SimpleParticleType> WING_DEBRIS = PARTICLE_TYPES.register("wing_debris", () -> new SimpleParticleType(false));
   public static final RegEntry<SimpleParticleType> SLEEP_BUBBLE = PARTICLE_TYPES.register("sleep_bubble", () -> new SimpleParticleType(false));
   public static final RegEntry<SimpleParticleType> WATER_SPRAY = PARTICLE_TYPES.register("water_spray", () -> new SimpleParticleType(false));

   public static void register() {
   }
}
