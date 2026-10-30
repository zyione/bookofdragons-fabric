package net.magister.liberanimalium.attack.data.impact;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.magister.liberanimalium.attack.data.ImpactConfig;

public record SlideImpact(float bounciness, float friction, int maxBounces, int lifetimeTicks, float fadeRate, boolean setsFire) implements ImpactConfig {
   public static final Codec<SlideImpact> CODEC = RecordCodecBuilder.create(
      i -> i.group(
            Codec.FLOAT.optionalFieldOf("bounciness", 0.35F).forGetter(SlideImpact::bounciness),
            Codec.FLOAT.optionalFieldOf("friction", 0.88F).forGetter(SlideImpact::friction),
            Codec.INT.optionalFieldOf("max_bounces", 3).forGetter(SlideImpact::maxBounces),
            Codec.INT.optionalFieldOf("lifetime_ticks", 60).forGetter(SlideImpact::lifetimeTicks),
            Codec.FLOAT.optionalFieldOf("fade_rate", 0.025F).forGetter(SlideImpact::fadeRate),
            Codec.BOOL.optionalFieldOf("sets_fire", false).forGetter(SlideImpact::setsFire)
         )
         .apply(i, SlideImpact::new)
   );

   @Override
   public String type() {
      return "slide";
   }
}
