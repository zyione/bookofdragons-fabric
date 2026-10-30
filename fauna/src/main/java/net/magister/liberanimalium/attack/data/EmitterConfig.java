package net.magister.liberanimalium.attack.data;

import com.mojang.serialization.Codec;
import net.magister.liberanimalium.attack.data.render.RenderConfig;

public sealed interface EmitterConfig permits ProjectileEmitter, VisualEmitter {
   Codec<EmitterConfig> CODEC = Codec.STRING.dispatch("kind", EmitterConfig::kind, key -> {
      return switch (key) {
         case "projectile" -> ProjectileEmitter.CODEC;
         case "visual" -> VisualEmitter.CODEC;
         default -> throw new IllegalArgumentException("Unknown emitter kind: " + key);
      };
   });

   String kind();

   EmissionConfig emission();

   EmitterSim sim();

   RenderConfig render();
}
