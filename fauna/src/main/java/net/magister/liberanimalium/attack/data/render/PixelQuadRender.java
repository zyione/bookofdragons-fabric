package net.magister.liberanimalium.attack.data.render;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.List;
import java.util.Optional;
import net.magister.liberanimalium.attack.data.color.PaletteLUT;

public record PixelQuadRender(
   String blend, List<Float> size, Optional<PaletteLUT> palette, boolean spin, int snap, CurveSet curves, boolean emissive, float shaderAlpha
) implements RenderConfig {
   public static final Codec<PixelQuadRender> CODEC = RecordCodecBuilder.create(
      i -> i.group(
            Codec.STRING.optionalFieldOf("blend", "additive").forGetter(PixelQuadRender::blend),
            Codec.FLOAT.listOf().optionalFieldOf("size", List.of(0.25F, 0.0F)).forGetter(PixelQuadRender::size),
            PaletteLUT.CODEC.optionalFieldOf("palette").forGetter(PixelQuadRender::palette),
            Codec.BOOL.optionalFieldOf("spin", true).forGetter(PixelQuadRender::spin),
            Codec.INT.optionalFieldOf("snap", 0).forGetter(PixelQuadRender::snap),
            CurveSet.CODEC.optionalFieldOf("curves", CurveSet.NONE).forGetter(PixelQuadRender::curves),
            Codec.BOOL.optionalFieldOf("emissive", true).forGetter(PixelQuadRender::emissive),
            Codec.FLOAT.optionalFieldOf("shader_alpha", 1.0F).forGetter(PixelQuadRender::shaderAlpha)
         )
         .apply(i, PixelQuadRender::new)
   );

   public PixelQuadRender(String blend, List<Float> size, Optional<PaletteLUT> palette, boolean spin, int snap, CurveSet curves, boolean emissive) {
      this(blend, size, palette, spin, snap, curves, emissive, 1.0F);
   }

   public PixelQuadRender(String blend, List<Float> size, Optional<PaletteLUT> palette, boolean spin, int snap) {
      this(blend, size, palette, spin, snap, CurveSet.NONE);
   }

   public PixelQuadRender(String blend, List<Float> size, Optional<PaletteLUT> palette, boolean spin, int snap, CurveSet curves) {
      this(blend, size, palette, spin, snap, curves, true);
   }

   public float snapSize(float size) {
      return snapSize(this.snap, size);
   }

   public static float snapSize(int snap, float size) {
      if (snap <= 0) {
         return size;
      }

      float step = 1.0F / snap;
      return Math.max(step, Math.round(size / step) * step);
   }

   @Override
   public String type() {
      return "pixel_quad";
   }
}
