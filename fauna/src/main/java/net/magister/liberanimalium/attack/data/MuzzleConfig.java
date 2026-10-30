package net.magister.liberanimalium.attack.data;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.List;
import java.util.Optional;
import net.magister.liberanimalium.attack.data.color.PaletteLUT;
import net.minecraft.resources.ResourceLocation;

public record MuzzleConfig(String bone, List<Float> offset, Optional<ResourceLocation> glowTexture, float glowAlpha, Optional<MuzzleConfig.Glow> glow) {
   public static final String DEFAULT_BONE = "tongue";
   public static final MuzzleConfig DEFAULT = new MuzzleConfig("tongue", List.of(0.0F, 0.0F, 0.0F), Optional.empty(), 1.0F, Optional.empty());
   public static final Codec<MuzzleConfig> CODEC = RecordCodecBuilder.create(
      i -> i.group(
            Codec.STRING.optionalFieldOf("bone", "tongue").forGetter(MuzzleConfig::bone),
            Codec.FLOAT.listOf().optionalFieldOf("offset", List.of(0.0F, 0.0F, 0.0F)).forGetter(MuzzleConfig::offset),
            ResourceLocation.CODEC.optionalFieldOf("glow_texture").forGetter(MuzzleConfig::glowTexture),
            Codec.FLOAT.optionalFieldOf("glow_alpha", 1.0F).forGetter(MuzzleConfig::glowAlpha),
            MuzzleConfig.Glow.CODEC.optionalFieldOf("glow").forGetter(MuzzleConfig::glow)
         )
         .apply(i, MuzzleConfig::new)
   );

   public float offsetX() {
      return this.offset.size() > 0 ? this.offset.get(0) : 0.0F;
   }

   public float offsetY() {
      return this.offset.size() > 1 ? this.offset.get(1) : 0.0F;
   }

   public float offsetZ() {
      return this.offset.size() > 2 ? this.offset.get(2) : 0.0F;
   }

   public boolean hasTextureGlow() {
      return this.glowTexture.isPresent() && this.glowAlpha > 0.0F;
   }

   public float glowAlphaAt(float progress) {
      return !this.hasTextureGlow() ? 0.0F : this.glowAlpha * Math.max(0.0F, Math.min(1.0F, progress));
   }

   public record Glow(List<Float> size, PaletteLUT palette) {
      public static final Codec<MuzzleConfig.Glow> CODEC = RecordCodecBuilder.create(
         i -> i.group(
               Codec.FLOAT.listOf().optionalFieldOf("size", List.of(0.2F, 0.8F)).forGetter(MuzzleConfig.Glow::size),
               PaletteLUT.CODEC.fieldOf("palette").forGetter(MuzzleConfig.Glow::palette)
            )
            .apply(i, MuzzleConfig.Glow::new)
      );

      public float sizeAt(float progress) {
         float min = this.size.size() >= 1 ? this.size.get(0) : 0.2F;
         float max = this.size.size() >= 2 ? this.size.get(1) : min;
         return min + (max - min) * progress;
      }
   }
}
