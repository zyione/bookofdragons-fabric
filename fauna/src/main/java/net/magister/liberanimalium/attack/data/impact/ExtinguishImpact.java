package net.magister.liberanimalium.attack.data.impact;

import com.mojang.serialization.Codec;
import net.magister.liberanimalium.attack.data.ImpactConfig;

public record ExtinguishImpact() implements ImpactConfig {
   public static final Codec<ExtinguishImpact> CODEC = Codec.unit(new ExtinguishImpact());

   @Override
   public String type() {
      return "extinguish";
   }
}
