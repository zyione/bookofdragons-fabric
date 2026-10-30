package net.magister.liberanimalium.body.data;

import java.util.Locale;

public enum CrippledFeature {
   BREATH,
   SECONDARY_ABILITY,
   RUNNING,
   FLYING;

   public static String key(CrippledFeature feature) {
      return "liberanimalium.injury." + feature.name().toLowerCase(Locale.ROOT);
   }

   public static String refusalKey(CrippledFeature feature) {
      return "liberanimalium.injury.refused." + feature.name().toLowerCase(Locale.ROOT);
   }
}
