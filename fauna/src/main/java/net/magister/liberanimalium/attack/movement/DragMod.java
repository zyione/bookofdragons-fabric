package net.magister.liberanimalium.attack.movement;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

public record DragMod(float factor) implements MovementModifier {
   public static final Codec<DragMod> CODEC = RecordCodecBuilder.create(
      i -> i.group(Codec.FLOAT.optionalFieldOf("factor", 0.98F).forGetter(DragMod::factor)).apply(i, DragMod::new)
   );

   @Override
   public String type() {
      return "drag";
   }

   @Override
   public Vec3 apply(Vec3 velocity, @Nullable Entity entity, int tick) {
      return velocity.scale(this.factor);
   }
}
