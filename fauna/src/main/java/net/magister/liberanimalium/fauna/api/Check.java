package net.magister.liberanimalium.fauna.api;

import java.util.Locale;
import java.util.Set;

public record Check(String id, Check.Engagement engages, int defaultHoldTicks, Set<Delivery> fedBy) {
   public Check(String id, Check.Engagement engages, int defaultHoldTicks, Set<Delivery> fedBy) {
      if (id != null && !id.isBlank() && id.equals(id.toLowerCase(Locale.ROOT))) {
         fedBy = Set.copyOf(fedBy);
         this.id = id;
         this.engages = engages;
         this.defaultHoldTicks = defaultHoldTicks;
         this.fedBy = fedBy;
      } else {
         throw new IllegalArgumentException("a check id is a lowercase word: " + id);
      }
   }

   public static Check of(String id, Check.Engagement engages) {
      return new Check(id, engages, 0, Set.of());
   }

   public Check holding(int defaultHoldTicks) {
      return new Check(this.id, this.engages, defaultHoldTicks, this.fedBy);
   }

   public Check fedBy(Delivery... deliveries) {
      return new Check(this.id, this.engages, this.defaultHoldTicks, Set.of(deliveries));
   }

   @Override
   public String toString() {
      return this.id.toUpperCase(Locale.ROOT);
   }

   public enum Engagement {
      APPROACH,
      FEEDING,
      TOUCH,
      TAG,
      SPAR,
      DEFEND,
      RODEO,
      REST,
      WALK,
      HUNT_TOGETHER,
      GIFT;
   }
}
