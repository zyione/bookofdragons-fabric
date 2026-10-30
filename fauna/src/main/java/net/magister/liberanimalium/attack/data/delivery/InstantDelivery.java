package net.magister.liberanimalium.attack.data.delivery;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

public record InstantDelivery(int cooldownTicks) implements DeliveryConfig {
   public static final Codec<InstantDelivery> CODEC = RecordCodecBuilder.create(
      i -> i.group(Codec.INT.optionalFieldOf("cooldown_ticks", 20).forGetter(InstantDelivery::cooldownTicks)).apply(i, InstantDelivery::new)
   );

   @Override
   public String type() {
      return "instant";
   }
}
