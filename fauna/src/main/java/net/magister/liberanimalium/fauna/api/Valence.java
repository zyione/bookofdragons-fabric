package net.magister.liberanimalium.fauna.api;

import java.util.Locale;
import org.jetbrains.annotations.Nullable;

public enum Valence {
   CRAVES,
   LIKES,
   NEUTRAL,
   DISLIKES,
   HATES,
   RESPECTS;

   public float weight() {
      return switch (this) {
         case CRAVES -> 1.0F;
         case LIKES -> 0.75F;
         case NEUTRAL, RESPECTS -> 0.5F;
         case DISLIKES -> 0.25F;
         case HATES -> 0.0F;
      };
   }

   public static float weightOf(@Nullable Valence valence) {
      return (valence == null ? NEUTRAL : valence).weight();
   }

   public static Valence parse(String raw) {
      if (raw == null) {
         return NEUTRAL;
      }

      try {
         return valueOf(raw.trim().toUpperCase(Locale.ROOT));
      } catch (IllegalArgumentException e) {
         return NEUTRAL;
      }
   }
}
