package net.magister.liberanimalium.fauna.api;

import java.util.Set;

public record Kind(String typeKey, Set<String> tags, boolean sameSpecies) {
   public static final String SAME_SPECIES_SELECTOR = "#fauna:same_species";
   public static final Kind UNKNOWN = new Kind("fauna:unseen", Set.of(), false);
   public static final String PLAYER = "minecraft:player";

   public boolean isPlayer() {
      return "minecraft:player".equals(this.typeKey);
   }

   public boolean matches(String selector) {
      if (selector == null || selector.isEmpty()) {
         return false;
      } else if ("#fauna:same_species".equals(selector)) {
         return this.sameSpecies;
      } else {
         return selector.charAt(0) == '#' ? this.tags.contains(selector) : selector.equals(this.typeKey);
      }
   }
}
