package net.magister.liberanimalium.attack.data;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

public record LightConfig(
   int level, boolean source, boolean projectile, int everyTicks, int ttlTicks, float spacing, int maxPoints, int impactLevel, int impactTtlTicks
) {
   public static final Codec<LightConfig> CODEC = RecordCodecBuilder.create(
      i -> i.group(
            Codec.intRange(1, 15).optionalFieldOf("level", 15).forGetter(LightConfig::level),
            Codec.BOOL.optionalFieldOf("source", true).forGetter(LightConfig::source),
            Codec.BOOL.optionalFieldOf("projectile", true).forGetter(LightConfig::projectile),
            Codec.intRange(1, 20).optionalFieldOf("every_ticks", 2).forGetter(LightConfig::everyTicks),
            Codec.intRange(1, 40).optionalFieldOf("ttl_ticks", 4).forGetter(LightConfig::ttlTicks),
            Codec.FLOAT.optionalFieldOf("spacing", 2.0F).forGetter(LightConfig::spacing),
            Codec.intRange(1, 64).optionalFieldOf("max_points", 8).forGetter(LightConfig::maxPoints),
            Codec.intRange(0, 15).optionalFieldOf("impact_level", 0).forGetter(LightConfig::impactLevel),
            Codec.intRange(1, 100).optionalFieldOf("impact_ttl_ticks", 20).forGetter(LightConfig::impactTtlTicks)
         )
         .apply(i, LightConfig::new)
   );

   public LightConfig(int level, boolean source, boolean projectile, int everyTicks, int ttlTicks, float spacing, int maxPoints) {
      this(level, source, projectile, everyTicks, ttlTicks, spacing, maxPoints, 0, 20);
   }

   public LightConfig impactFlash() {
      return new LightConfig(
         Math.max(1, this.impactLevel), false, false, this.everyTicks, this.impactTtlTicks, 0.0F, this.maxPoints + 1, this.impactLevel, this.impactTtlTicks
      );
   }

   public int levelAt(float intensity) {
      return intensity <= 0.0F ? 0 : Math.max(1, Math.min(this.level, Math.round(this.level * intensity)));
   }
}
