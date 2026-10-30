package net.magister.liberanimalium.attack.data.render;

import com.mojang.serialization.Codec;

public sealed interface RenderConfig permits BillboardRender, CubeRender, BeamRender, PixelQuadRender, ModelRender {
   Codec<RenderConfig> CODEC = Codec.STRING.dispatch("type", RenderConfig::type, key -> {
      return switch (key) {
         case "billboard" -> BillboardRender.CODEC;
         case "cube" -> CubeRender.CODEC;
         case "beam" -> BeamRender.CODEC;
         case "pixel_quad" -> PixelQuadRender.CODEC;
         case "model" -> ModelRender.CODEC;
         default -> throw new IllegalArgumentException("Unknown render type: " + key);
      };
   });

   String type();
}
