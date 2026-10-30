package net.magister.liberanimalium.fauna.identity;

import java.util.UUID;
import net.magister.liberanimalium.fauna.api.SpeciesProfile;
import net.magister.liberanimalium.fauna.core.Seeds;
import net.minecraft.util.Mth;

public final class Temper {
   private final float[] values;

   private Temper(float[] values) {
      this.values = values;
   }

   public static Temper of(UUID id, SpeciesProfile species) {
      float bravery = extreme(Seeds.unit(id, 3030900749L));
      float energy = extreme(Seeds.unit(id, 3840310289L));
      float sympathy = extreme(Seeds.unit(id, 1394055697L));
      float aggression = extreme(pull(Seeds.unit(id, 2791564625L), (energy + bravery) * (1.0F - sympathy) / 2.0F));
      float dominance = moderate(pull(Seeds.unit(id, 3492813335L), mean(energy, bravery, aggression)));
      float nervousness = extreme(pull(Seeds.unit(id, 1588004178L), (energy - bravery + 1.0F) / 2.0F));
      float[] raw = new float[Trait.values().length];
      raw[Trait.AGGRESSION.ordinal()] = aggression;
      raw[Trait.BRAVERY.ordinal()] = bravery;
      raw[Trait.DOMINANCE.ordinal()] = dominance;
      raw[Trait.ENERGY.ordinal()] = energy;
      raw[Trait.NERVOUSNESS.ordinal()] = nervousness;
      raw[Trait.SYMPATHY.ordinal()] = sympathy;

      for (Trait trait : Trait.values()) {
         raw[trait.ordinal()] = inBand(raw[trait.ordinal()], species.traitBand(trait.key()));
      }

      return new Temper(raw);
   }

   public float get(Trait trait) {
      return this.values[trait.ordinal()];
   }

   private static float inBand(float raw, float[] band) {
      float lo = Mth.clamp(Math.min(band[0], band[1]), 0.0F, 1.0F);
      float hi = Mth.clamp(Math.max(band[0], band[1]), 0.0F, 1.0F);
      return Mth.clamp(Mth.lerp(Mth.clamp(raw, 0.0F, 1.0F), lo, hi), lo, hi);
   }

   private static float mean(float... traits) {
      float sum = 0.0F;

      for (float trait : traits) {
         sum += trait;
      }

      return sum / traits.length;
   }

   private static float pull(float draw, float target) {
      return Mth.lerp(0.6F, draw, Mth.clamp(target, 0.0F, 1.0F));
   }

   private static float extreme(float value) {
      return skew(value, 0.6F);
   }

   private static float moderate(float value) {
      return skew(value, 1.6666666F);
   }

   private static float skew(float value, float exponent) {
      float clamped = Mth.clamp(value, 0.0F, 1.0F);
      double distance = Math.abs(clamped - 0.5F) * 2.0F;
      float moved = (float)Math.pow(distance, exponent) / 2.0F;
      return clamped < 0.5F ? 0.5F - moved : 0.5F + moved;
   }

   public float[] readings() {
      return (float[])this.values.clone();
   }

   @Override
   public String toString() {
      StringBuilder out = new StringBuilder();

      for (Trait trait : Trait.values()) {
         if (out.length() > 0) {
            out.append(' ');
         }

         out.append(trait.key()).append('=').append(String.format("%.2f", this.get(trait)));
      }

      return out.toString();
   }
}
