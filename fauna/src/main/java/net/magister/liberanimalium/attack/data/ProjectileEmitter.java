package net.magister.liberanimalium.attack.data;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.List;
import java.util.Optional;
import net.magister.liberanimalium.attack.data.render.PixelQuadRender;
import net.magister.liberanimalium.attack.data.render.RenderConfig;

public record ProjectileEmitter(EmissionConfig emission, EmitterSim sim, RenderConfig render) implements EmitterConfig {
   private static final RenderConfig DEFAULT_RENDER = new PixelQuadRender("additive", List.of(0.3F, 0.0F), Optional.empty(), true, 0);
   public static final Codec<ProjectileEmitter> CODEC = RecordCodecBuilder.create(
      i -> i.group(
            EmissionConfig.CODEC.optionalFieldOf("emission", EmissionConfig.POINT_SINGLE).forGetter(ProjectileEmitter::emission),
            EmitterSim.CODEC.optionalFieldOf("sim", EmitterSim.DEFAULT).forGetter(ProjectileEmitter::sim),
            RenderConfig.CODEC.optionalFieldOf("render", DEFAULT_RENDER).forGetter(ProjectileEmitter::render)
         )
         .apply(i, ProjectileEmitter::new)
   );

   @Override
   public String kind() {
      return "projectile";
   }
}
