package net.magister.liberanimalium.attack.data.render;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.List;
import net.minecraft.resources.ResourceLocation;

public record ModelRender(ResourceLocation geo, ResourceLocation texture, int frames, int frameTicks, List<Float> size, String blend, float rollSpeed)
   implements RenderConfig {
   public static final String BLEND_EMISSIVE = "emissive";
   public static final String BLEND_ADDITIVE = "additive";
   public static final String BLEND_CUTOUT = "cutout";
   public static final String BLEND_GLOW = "glow";
   public static final Codec<ModelRender> CODEC = RecordCodecBuilder.create(
      i -> i.group(
            ResourceLocation.CODEC.fieldOf("geo").forGetter(ModelRender::geo),
            ResourceLocation.CODEC.fieldOf("texture").forGetter(ModelRender::texture),
            Codec.INT.optionalFieldOf("frames", 1).forGetter(ModelRender::frames),
            Codec.INT.optionalFieldOf("frame_ticks", 2).forGetter(ModelRender::frameTicks),
            Codec.FLOAT.listOf().optionalFieldOf("size", List.of(1.0F, 1.0F)).forGetter(ModelRender::size),
            Codec.STRING.optionalFieldOf("blend", "emissive").forGetter(ModelRender::blend),
            Codec.FLOAT.optionalFieldOf("roll_speed", 0.0F).forGetter(ModelRender::rollSpeed)
         )
         .apply(i, ModelRender::new)
   );

   public int frameAt(int age) {
      int n = Math.max(1, this.frames);
      int hold = Math.max(1, this.frameTicks);
      return age / hold % n;
   }

   public ResourceLocation frameTexture(int frame) {
      return new ResourceLocation(this.texture.getNamespace(), this.texture.getPath() + frame + ".png");
   }

   @Override
   public String type() {
      return "model";
   }
}
