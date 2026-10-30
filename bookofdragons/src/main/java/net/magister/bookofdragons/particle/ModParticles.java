package net.magister.bookofdragons.particle;

import net.magister.liberanimalium.platform.Services;
import net.magister.liberanimalium.platform.registry.RegEntry;
import net.magister.liberanimalium.platform.registry.Registrar;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.core.registries.Registries;

public class ModParticles {
   public static final Registrar<ParticleType<?>> PARTICLE_TYPES = Services.registries().create(Registries.PARTICLE_TYPE, "bookofdragons");
   public static final RegEntry<SimpleParticleType> FURY_TRAIL = PARTICLE_TYPES.register("fury_trail", () -> net.fabricmc.fabric.api.particle.v1.FabricParticleTypes.simple(true));
   public static final RegEntry<SimpleParticleType> BLASTWAVE = PARTICLE_TYPES.register("blastwave", () -> net.fabricmc.fabric.api.particle.v1.FabricParticleTypes.simple(false));
   public static final RegEntry<SimpleParticleType> FIREBALL = PARTICLE_TYPES.register("fireball", () -> net.fabricmc.fabric.api.particle.v1.FabricParticleTypes.simple(false));
   public static final RegEntry<SimpleParticleType> NADDERSPARK = PARTICLE_TYPES.register("nadder_spark", () -> net.fabricmc.fabric.api.particle.v1.FabricParticleTypes.simple(false));
   public static final RegEntry<SimpleParticleType> NIGHTMARE_JACKET_FLAME = PARTICLE_TYPES.register(
      "nightmare_jacket_flame", () -> net.fabricmc.fabric.api.particle.v1.FabricParticleTypes.simple(false)
   );

   public static void register() {
   }
}
