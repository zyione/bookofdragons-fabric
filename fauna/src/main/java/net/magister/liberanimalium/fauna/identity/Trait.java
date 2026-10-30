package net.magister.liberanimalium.fauna.identity;

import java.util.Locale;

public enum Trait {
   AGGRESSION,
   BRAVERY,
   DOMINANCE,
   ENERGY,
   NERVOUSNESS,
   SYMPATHY;

   private final String key = this.name().toLowerCase(Locale.ROOT);

   public String key() {
      return this.key;
   }
}
