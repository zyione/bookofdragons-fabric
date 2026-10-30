package net.magister.liberanimalium.attack.data.render;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.List;
import net.minecraft.resources.ResourceLocation;

public record BillboardRender(
   ResourceLocation texture,
   int frames,
   int frameTicks,
   String blend,
   List<Float> size,
   boolean spin,
   CurveSet curves,
   boolean flat,
   boolean emissive,
   float emissiveUntil,
   float shaderAlpha
) implements RenderConfig {
   public static final Codec<BillboardRender> CODEC = RecordCodecBuilder.create(
      i -> i.group(
            ResourceLocation.CODEC.fieldOf("texture").forGetter(BillboardRender::texture),
            Codec.INT.optionalFieldOf("frames", 1).forGetter(BillboardRender::frames),
            Codec.INT.optionalFieldOf("frame_ticks", 0).forGetter(BillboardRender::frameTicks),
            Codec.STRING.optionalFieldOf("blend", "alpha").forGetter(BillboardRender::blend),
            Codec.FLOAT.listOf().optionalFieldOf("size", List.of(0.5F, 0.0F)).forGetter(BillboardRender::size),
            Codec.BOOL.optionalFieldOf("spin", false).forGetter(BillboardRender::spin),
            CurveSet.CODEC.optionalFieldOf("curves", CurveSet.NONE).forGetter(BillboardRender::curves),
            Codec.BOOL.optionalFieldOf("flat", false).forGetter(BillboardRender::flat),
            Codec.BOOL.optionalFieldOf("emissive", true).forGetter(BillboardRender::emissive),
            Codec.FLOAT.optionalFieldOf("emissive_until", 1.0F).forGetter(BillboardRender::emissiveUntil),
            Codec.FLOAT.optionalFieldOf("shader_alpha", 1.0F).forGetter(BillboardRender::shaderAlpha)
         )
         .apply(i, BillboardRender::new)
   );

   public BillboardRender(ResourceLocation texture) {
      this(texture, 1, 0, "alpha", List.of(0.5F, 0.0F), false, CurveSet.NONE);
   }

   public BillboardRender(ResourceLocation texture, int frames, int frameTicks, String blend, List<Float> size, boolean spin, CurveSet curves) {
      this(texture, frames, frameTicks, blend, size, spin, curves, false, true, 1.0F, 1.0F);
   }

   public boolean litAt(float lifeFraction) {
      return this.emissive && lifeFraction <= this.emissiveUntil;
   }

   public ResourceLocation frame(int i) {
      return this.frames <= 1 ? this.texture : new ResourceLocation(this.texture.getNamespace(), this.texture.getPath() + "_" + i);
   }

   public ResourceLocation frameFile(int i) {
      ResourceLocation sprite = this.frame(i);
      return new ResourceLocation(sprite.getNamespace(), "textures/particle/" + sprite.getPath() + ".png");
   }

   public int frameAt(int age, int life) {
      if (this.frames <= 1) {
         return 0;
      }

      if (this.frameTicks > 0) {
         return age / this.frameTicks % this.frames;
      }

      float t = life <= 0 ? 0.0F : Math.min(1.0F, Math.max(0.0F, (float)age / life));
      return Math.min(this.frames - 1, (int)(t * this.frames));
   }

   @Override
   public String type() {
      return "billboard";
   }
}
