package net.magister.liberanimalium.attack.data;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

public record CollisionConfig(float damage, int fireSeconds, int pierce) {
   public static final Codec<CollisionConfig> CODEC = RecordCodecBuilder.create(
      i -> i.group(
            Codec.FLOAT.optionalFieldOf("damage", 2.0F).forGetter(CollisionConfig::damage),
            Codec.INT.optionalFieldOf("fire_seconds", 0).forGetter(CollisionConfig::fireSeconds),
            Codec.INT.optionalFieldOf("pierce", 0).forGetter(CollisionConfig::pierce)
         )
         .apply(i, CollisionConfig::new)
   );
}
