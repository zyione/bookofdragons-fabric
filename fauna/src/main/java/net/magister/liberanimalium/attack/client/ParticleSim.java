package net.magister.liberanimalium.attack.client;

import net.magister.liberanimalium.attack.data.EmitterSim;
import net.magister.liberanimalium.attack.movement.MovementModifier;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

public class ParticleSim {
   public static Vec3 step(Vec3 velocity, EmitterSim sim, int tick, @Nullable Entity entity) {
      Vec3 v = velocity;

      for (MovementModifier mod : sim.movement()) {
         v = mod.apply(v, entity, tick);
      }

      return v;
   }

   public static Vec3 step(Vec3 velocity, EmitterSim sim, int tick, @Nullable Entity entity, RandomSource random) {
      Vec3 v = velocity;

      for (MovementModifier mod : sim.movement()) {
         v = mod.apply(v, entity, tick, random);
      }

      return v;
   }

   public static float cool(float temperature, EmitterSim sim) {
      return Math.max(0.0F, temperature - sim.coolingRate());
   }

   public static float shrinkSize(float baseSize, EmitterSim sim, float temperature) {
      return sim.coolingShrink() ? baseSize * temperature : baseSize;
   }

   public static float growScale(int age, int growInTicks) {
      if (growInTicks <= 0) {
         return 1.0F;
      }

      float t = Mth.clamp((float)age / growInTicks, 0.0F, 1.0F);
      return Mth.lerp(t, 0.1F, 1.0F);
   }
}
