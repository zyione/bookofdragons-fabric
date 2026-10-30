package net.magister.liberanimalium.attack.movement;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

public record TurbulenceMod(float amount, float noiseScale) implements MovementModifier {
   public static final Codec<TurbulenceMod> CODEC = RecordCodecBuilder.create(
      i -> i.group(
            Codec.FLOAT.optionalFieldOf("amount", 0.03F).forGetter(TurbulenceMod::amount),
            Codec.FLOAT.optionalFieldOf("noise_scale", 0.5F).forGetter(TurbulenceMod::noiseScale)
         )
         .apply(i, TurbulenceMod::new)
   );
   private static final RandomSource RANDOM = RandomSource.create();

   @Override
   public String type() {
      return "turbulence";
   }

   @Override
   public Vec3 apply(Vec3 velocity, @Nullable Entity entity, int tick) {
      return this.apply(velocity, entity, tick, RANDOM);
   }

   @Override
   public Vec3 apply(Vec3 velocity, @Nullable Entity entity, int tick, RandomSource random) {
      double dx = (random.nextDouble() - 0.5) * this.amount;
      double dy = (random.nextDouble() - 0.5) * this.amount;
      double dz = (random.nextDouble() - 0.5) * this.amount;
      return velocity.add(dx, dy, dz);
   }
}
