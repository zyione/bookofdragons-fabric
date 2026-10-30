package net.magister.liberanimalium.attack.data.delivery;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

public record ChargedDelivery(int chargeTimeTicks, float minScale, float maxScale, int cooldownTicks) implements DeliveryConfig {
   public static final Codec<ChargedDelivery> CODEC = RecordCodecBuilder.create(
      i -> i.group(
            Codec.INT.optionalFieldOf("charge_time_ticks", 40).forGetter(ChargedDelivery::chargeTimeTicks),
            Codec.FLOAT.optionalFieldOf("min_scale", 0.15F).forGetter(ChargedDelivery::minScale),
            Codec.FLOAT.optionalFieldOf("max_scale", 1.0F).forGetter(ChargedDelivery::maxScale),
            Codec.INT.optionalFieldOf("cooldown_ticks", 60).forGetter(ChargedDelivery::cooldownTicks)
         )
         .apply(i, ChargedDelivery::new)
   );

   @Override
   public String type() {
      return "charged";
   }
}
