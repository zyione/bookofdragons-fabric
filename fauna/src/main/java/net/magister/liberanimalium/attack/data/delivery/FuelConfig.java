package net.magister.liberanimalium.attack.data.delivery;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

public record FuelConfig(int max, float drainPerTick, float regenPerSec, int rechargeTicks) {
   public static final FuelConfig DEFAULT = new FuelConfig(100, 1.0F, 20.0F, 100);
   public static final Codec<FuelConfig> CODEC = RecordCodecBuilder.create(
      i -> i.group(
            Codec.INT.optionalFieldOf("max", 100).forGetter(FuelConfig::max),
            Codec.FLOAT.optionalFieldOf("drain_per_tick", 1.0F).forGetter(FuelConfig::drainPerTick),
            Codec.FLOAT.optionalFieldOf("regen_per_sec", 20.0F).forGetter(FuelConfig::regenPerSec),
            Codec.INT.optionalFieldOf("recharge_ticks", 100).forGetter(FuelConfig::rechargeTicks)
         )
         .apply(i, FuelConfig::new)
   );
}
