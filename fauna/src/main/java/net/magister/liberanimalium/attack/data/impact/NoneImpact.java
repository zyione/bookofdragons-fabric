package net.magister.liberanimalium.attack.data.impact;

import com.mojang.serialization.Codec;
import net.magister.liberanimalium.attack.data.ImpactConfig;

public record NoneImpact() implements ImpactConfig {
   public static final Codec<NoneImpact> CODEC = Codec.unit(new NoneImpact());

   @Override
   public String type() {
      return "none";
   }
}
