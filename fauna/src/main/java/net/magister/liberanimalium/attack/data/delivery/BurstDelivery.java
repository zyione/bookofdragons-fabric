package net.magister.liberanimalium.attack.data.delivery;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

public record BurstDelivery(int rounds, int roundIntervalTicks, int cooldownTicks) implements DeliveryConfig {
   public static final Codec<BurstDelivery> CODEC = RecordCodecBuilder.create(
      i -> i.group(
            Codec.INT.optionalFieldOf("rounds", 3).forGetter(BurstDelivery::rounds),
            Codec.INT.optionalFieldOf("round_interval_ticks", 4).forGetter(BurstDelivery::roundIntervalTicks),
            Codec.INT.optionalFieldOf("cooldown_ticks", 50).forGetter(BurstDelivery::cooldownTicks)
         )
         .apply(i, BurstDelivery::new)
   );

   @Override
   public String type() {
      return "burst";
   }
}
