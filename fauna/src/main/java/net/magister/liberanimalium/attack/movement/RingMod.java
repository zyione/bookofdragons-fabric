package net.magister.liberanimalium.attack.movement;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

public record RingMod(float radius, float speed) implements MovementModifier {
   public static final Codec<RingMod> CODEC = RecordCodecBuilder.create(
      i -> i.group(Codec.FLOAT.optionalFieldOf("radius", 1.0F).forGetter(RingMod::radius), Codec.FLOAT.optionalFieldOf("speed", 1.0F).forGetter(RingMod::speed))
         .apply(i, RingMod::new)
   );

   @Override
   public String type() {
      return "ring";
   }

   @Override
   public Vec3 apply(Vec3 velocity, @Nullable Entity entity, int tick) {
      Vec3 fwd = velocity.normalize();
      Vec3 up = Math.abs(fwd.y) > 0.99 ? new Vec3(1.0, 0.0, 0.0) : new Vec3(0.0, 1.0, 0.0);
      Vec3 right = fwd.cross(up).normalize();
      Vec3 perpUp = right.cross(fwd).normalize();
      double angle = tick * this.speed;
      Vec3 ring = right.scale(Math.cos(angle)).add(perpUp.scale(Math.sin(angle))).scale(this.radius * 0.05);
      return velocity.add(ring);
   }
}
