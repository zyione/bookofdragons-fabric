package net.magister.liberanimalium.attack.data.render;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.List;

public record CubeRender(List<Float> size, List<Integer> color, float alpha, List<Float> elongation, boolean pulseColor, List<Integer> endColor, String blend)
   implements RenderConfig {
   private static final List<Float> DEFAULT_SIZE = List.of(0.5F, 1.0F);
   private static final List<Integer> DEFAULT_COLOR = List.of(255, 255, 255);
   private static final List<Float> DEFAULT_ELONGATION = List.of(1.0F, 1.0F);
   private static final List<Integer> DEFAULT_END_COLOR = List.of(255, 255, 255);
   public static final Codec<CubeRender> CODEC = RecordCodecBuilder.create(
      i -> i.group(
            Codec.FLOAT.listOf().optionalFieldOf("size", DEFAULT_SIZE).forGetter(CubeRender::size),
            Codec.INT.listOf().optionalFieldOf("color", DEFAULT_COLOR).forGetter(CubeRender::color),
            Codec.FLOAT.optionalFieldOf("alpha", 1.0F).forGetter(CubeRender::alpha),
            Codec.FLOAT.listOf().optionalFieldOf("elongation", DEFAULT_ELONGATION).forGetter(CubeRender::elongation),
            Codec.BOOL.optionalFieldOf("pulse_color", false).forGetter(CubeRender::pulseColor),
            Codec.INT.listOf().optionalFieldOf("end_color", DEFAULT_END_COLOR).forGetter(CubeRender::endColor),
            Codec.STRING.optionalFieldOf("blend", "additive").forGetter(CubeRender::blend)
         )
         .apply(i, CubeRender::new)
   );

   @Override
   public String type() {
      return "cube";
   }
}
