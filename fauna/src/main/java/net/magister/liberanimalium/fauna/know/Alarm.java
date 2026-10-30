package net.magister.liberanimalium.fauna.know;

import net.magister.liberanimalium.fauna.api.RelationType;
import net.minecraft.util.Mth;

public final class Alarm {
   private Alarm() {
   }

   public static float spike(RelationType type, float intensity, float find, double distance, float sightRadius, float manner) {
      return spike(type, intensity, find, distance, sightRadius, manner, 0.0F);
   }

   public static float spike(RelationType type, float intensity, float find, double distance, float sightRadius, float manner, float shade) {
      if (!Bend.isNegative(type)) {
         return 0.0F;
      }

      if (intensity <= 0.0F) {
         return 0.0F;
      }

      float confidence = (float)Math.pow(Mth.clamp(find, 0.0F, 1.0F), 0.75);
      return confidence <= 0.0F
         ? 0.0F
         : Mth.clamp(
            1.0F
               * intensity
               * confidence
               * nearness(distance, sightRadius * Manner.reach(manner))
               * Manner.gainFactor(manner)
               * (1.0F - Mth.clamp(shade, 0.0F, 1.0F)),
            0.0F,
            1.0F
         );
   }

   public static float nearness(double distance, float sightRadius) {
      double scale = Math.max(0.001, sightRadius);
      double ratio = Math.max(0.0, distance) / scale;
      return (float)(1.0 / (1.0 + ratio * ratio));
   }

   public static float envelope(float previous, float spike, long elapsed, double retentionPerTick) {
      float decayed = previous;
      if (elapsed > 0L && previous > 0.0F) {
         decayed = previous * (float)Math.pow(retentionPerTick, elapsed);
      }

      return Mth.clamp(Math.max(decayed, spike), 0.0F, 1.0F);
   }
}
