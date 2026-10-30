package net.magister.liberanimalium.attack.data.render;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.List;

public record BeamRender(List<Integer> innerColor, List<Integer> outerColor) implements RenderConfig {
   public static final Codec<BeamRender> CODEC = RecordCodecBuilder.create(
      i -> i.group(
            Codec.INT.listOf().optionalFieldOf("inner_color", List.of(255, 255, 255)).forGetter(BeamRender::innerColor),
            Codec.INT.listOf().optionalFieldOf("outer_color", List.of(200, 200, 255)).forGetter(BeamRender::outerColor)
         )
         .apply(i, BeamRender::new)
   );

   @Override
   public String type() {
      return "beam";
   }
}
