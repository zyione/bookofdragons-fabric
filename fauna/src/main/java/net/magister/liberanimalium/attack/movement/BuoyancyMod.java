package net.magister.liberanimalium.attack.movement;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

public record BuoyancyMod(float amount) implements MovementModifier {
   public static final Codec<BuoyancyMod> CODEC = RecordCodecBuilder.create(
      i -> i.group(Codec.FLOAT.optionalFieldOf("amount", 0.02F).forGetter(BuoyancyMod::amount)).apply(i, BuoyancyMod::new)
   );

   @Override
   public String type() {
      return "buoyancy";
   }

   @Override
   public Vec3 apply(Vec3 velocity, @Nullable Entity entity, int tick) {
      return velocity.add(0.0, this.amount, 0.0);
   }
}
