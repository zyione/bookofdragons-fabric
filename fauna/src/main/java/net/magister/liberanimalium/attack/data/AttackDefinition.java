package net.magister.liberanimalium.attack.data;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.List;
import java.util.Optional;
import net.magister.liberanimalium.attack.data.delivery.DeliveryConfig;

public record AttackDefinition(
   DeliveryConfig delivery,
   CollisionConfig collision,
   List<EmitterConfig> emitters,
   List<ImpactConfig> impact,
   MuzzleConfig muzzle,
   Optional<LightConfig> light,
   Optional<Float> reach
) {
   public static final Codec<AttackDefinition> CODEC = RecordCodecBuilder.create(
      i -> i.group(
            DeliveryConfig.CODEC.fieldOf("delivery").forGetter(AttackDefinition::delivery),
            CollisionConfig.CODEC.optionalFieldOf("collision", new CollisionConfig(2.0F, 0, 0)).forGetter(AttackDefinition::collision),
            EmitterConfig.CODEC.listOf().optionalFieldOf("emitters", List.of()).forGetter(AttackDefinition::emitters),
            ImpactConfig.CODEC.listOf().optionalFieldOf("impact", List.of()).forGetter(AttackDefinition::impact),
            MuzzleConfig.CODEC.optionalFieldOf("muzzle", MuzzleConfig.DEFAULT).forGetter(AttackDefinition::muzzle),
            LightConfig.CODEC.optionalFieldOf("light").forGetter(AttackDefinition::light),
            Codec.FLOAT.optionalFieldOf("reach").forGetter(AttackDefinition::reach)
         )
         .apply(i, AttackDefinition::new)
   );

   public AttackDefinition(
      DeliveryConfig delivery,
      CollisionConfig collision,
      List<EmitterConfig> emitters,
      List<ImpactConfig> impact,
      MuzzleConfig muzzle,
      Optional<LightConfig> light
   ) {
      this(delivery, collision, emitters, impact, muzzle, light, Optional.empty());
   }

   public AttackDefinition(DeliveryConfig delivery, CollisionConfig collision, List<EmitterConfig> emitters, List<ImpactConfig> impact) {
      this(delivery, collision, emitters, impact, MuzzleConfig.DEFAULT, Optional.empty(), Optional.empty());
   }
}
