package net.magister.bookofdragons.entity.base.dragon;

import java.util.Locale;
import net.magister.liberanimalium.body.gameplay.SpeciesGameplayRegistry;

public enum DragonClass {
   BOULDER,
   MYSTERY,
   SHARP,
   STOKER,
   STRIKE,
   TIDAL,
   TRACKER;

   public String getDisplayName() {
      return this.name().charAt(0) + this.name().substring(1).toLowerCase(Locale.ROOT);
   }

   public static DragonClass of(String word) {
      if (word == null) {
         return STRIKE;
      }

      for (DragonClass c : values()) {
         if (c.name().equalsIgnoreCase(word)) {
            return c;
         }
      }

      return STRIKE;
   }

   public static DragonClass ofSpecies(String speciesId) {
      return of(SpeciesGameplayRegistry.speciesClassOf(speciesId));
   }
}
