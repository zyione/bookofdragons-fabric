package net.magister.liberanimalium.attack.data;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.magister.liberanimalium.attack.data.render.RenderConfig;

public record VisualEmitter(
   String anchor, String phase, VisualEmitter.ChargeScale chargeScale, EmissionConfig emission, EmitterSim sim, RenderConfig render, float minPower
) implements EmitterConfig {
   public static final String PHASE_FIRE = "fire";
   public static final String PHASE_CHARGE = "charge";
   public static final String PHASE_RELEASE = "release";
   public static final Codec<VisualEmitter> CODEC = RecordCodecBuilder.create(
      i -> i.group(
            Codec.STRING.optionalFieldOf("anchor", "source").forGetter(VisualEmitter::anchor),
            Codec.STRING.optionalFieldOf("phase", "fire").forGetter(VisualEmitter::phase),
            VisualEmitter.ChargeScale.CODEC.optionalFieldOf("charge_scale", VisualEmitter.ChargeScale.NONE).forGetter(VisualEmitter::chargeScale),
            EmissionConfig.CODEC.optionalFieldOf("emission", EmissionConfig.POINT_SINGLE).forGetter(VisualEmitter::emission),
            EmitterSim.CODEC.optionalFieldOf("sim", EmitterSim.DEFAULT).forGetter(VisualEmitter::sim),
            RenderConfig.CODEC.fieldOf("render").forGetter(VisualEmitter::render),
            Codec.FLOAT.optionalFieldOf("min_power", 0.0F).forGetter(VisualEmitter::minPower)
         )
         .apply(i, VisualEmitter::new)
   );

   public VisualEmitter(String anchor, String phase, VisualEmitter.ChargeScale chargeScale, EmissionConfig emission, EmitterSim sim, RenderConfig render) {
      this(anchor, phase, chargeScale, emission, sim, render, 0.0F);
   }

   @Override
   public String kind() {
      return "visual";
   }

   public boolean isProjectileAnchored() {
      return "projectile".equals(this.anchor);
   }

   public boolean isSourceAnchored() {
      return "source".equals(this.anchor);
   }

   public boolean isImpactAnchored() {
      return "impact".equals(this.anchor);
   }

   public boolean isFirePhase() {
      return "fire".equals(this.phase);
   }

   public boolean isChargePhase() {
      return "charge".equals(this.phase);
   }

   public boolean isReleasePhase() {
      return "release".equals(this.phase);
   }

   public record ChargeScale(boolean count, boolean size) {
      public static final VisualEmitter.ChargeScale NONE = new VisualEmitter.ChargeScale(false, false);
      public static final Codec<VisualEmitter.ChargeScale> CODEC = RecordCodecBuilder.create(
         i -> i.group(
               Codec.BOOL.optionalFieldOf("count", false).forGetter(VisualEmitter.ChargeScale::count),
               Codec.BOOL.optionalFieldOf("size", false).forGetter(VisualEmitter.ChargeScale::size)
            )
            .apply(i, VisualEmitter.ChargeScale::new)
      );
   }
}
