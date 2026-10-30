package net.magister.liberanimalium.attack.data.delivery;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

public record StreamDelivery(int windupTicks, int maxDurationTicks, int fireRate, int cooldownTicks, FuelConfig fuel) implements DeliveryConfig {
   public static final Codec<StreamDelivery> CODEC = RecordCodecBuilder.create(
      i -> i.group(
            Codec.INT.optionalFieldOf("windup_ticks", 10).forGetter(StreamDelivery::windupTicks),
            Codec.INT.optionalFieldOf("max_duration_ticks", 200).forGetter(StreamDelivery::maxDurationTicks),
            Codec.INT.optionalFieldOf("fire_rate", 2).forGetter(StreamDelivery::fireRate),
            Codec.INT.optionalFieldOf("cooldown_ticks", 40).forGetter(StreamDelivery::cooldownTicks),
            FuelConfig.CODEC.optionalFieldOf("fuel", FuelConfig.DEFAULT).forGetter(StreamDelivery::fuel)
         )
         .apply(i, StreamDelivery::new)
   );

   @Override
   public String type() {
      return "stream";
   }
}
