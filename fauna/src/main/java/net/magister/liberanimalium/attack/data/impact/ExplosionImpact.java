package net.magister.liberanimalium.attack.data.impact;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.Optional;
import net.magister.liberanimalium.attack.data.ImpactConfig;

public record ExplosionImpact(
   float minPower, float maxPower, boolean breaksBlocks, boolean setsFire, float speedGain, Optional<ExplosionImpact.Airburst> airburst
) implements ImpactConfig {
   public static final Codec<ExplosionImpact> CODEC = RecordCodecBuilder.create(
      i -> i.group(
            Codec.FLOAT.optionalFieldOf("min_power", 0.0F).forGetter(ExplosionImpact::minPower),
            Codec.FLOAT.optionalFieldOf("max_power", 0.0F).forGetter(ExplosionImpact::maxPower),
            Codec.BOOL.optionalFieldOf("breaks_blocks", false).forGetter(ExplosionImpact::breaksBlocks),
            Codec.BOOL.optionalFieldOf("sets_fire", false).forGetter(ExplosionImpact::setsFire),
            Codec.FLOAT.optionalFieldOf("speed_gain", 0.0F).forGetter(ExplosionImpact::speedGain),
            ExplosionImpact.Airburst.CODEC.optionalFieldOf("airburst").forGetter(ExplosionImpact::airburst)
         )
         .apply(i, ExplosionImpact::new)
   );

   public ExplosionImpact(float minPower, float maxPower, boolean breaksBlocks, boolean setsFire) {
      this(minPower, maxPower, breaksBlocks, setsFire, 0.0F);
   }

   public ExplosionImpact(float minPower, float maxPower, boolean breaksBlocks, boolean setsFire, float speedGain) {
      this(minPower, maxPower, breaksBlocks, setsFire, speedGain, Optional.empty());
   }

   public float power(float charge, float speedRatio) {
      float charged = this.minPower + (this.maxPower - this.minPower) * charge;
      return charged * (1.0F + this.speedGain * Math.max(0.0F, speedRatio - 1.0F));
   }

   public float share(float power) {
      return this.maxPower > this.minPower ? Math.max(0.0F, (power - this.minPower) / (this.maxPower - this.minPower)) : 1.0F;
   }

   @Override
   public String type() {
      return "explosion";
   }

   public record Airburst(float minDistance, float maxDistance) {
      public static final Codec<ExplosionImpact.Airburst> CODEC = RecordCodecBuilder.create(
         i -> i.group(
               Codec.FLOAT.fieldOf("min_distance").forGetter(ExplosionImpact.Airburst::minDistance),
               Codec.FLOAT.fieldOf("max_distance").forGetter(ExplosionImpact.Airburst::maxDistance)
            )
            .apply(i, ExplosionImpact.Airburst::new)
      );

      public float distance(float charge) {
         return this.minDistance + (this.maxDistance - this.minDistance) * Math.max(0.0F, Math.min(1.0F, charge));
      }
   }
}
