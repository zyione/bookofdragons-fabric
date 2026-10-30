package net.magister.liberanimalium.fauna.core;

import java.util.UUID;

public record PairKey(UUID lo, UUID hi) {
   public PairKey(UUID lo, UUID hi) {
      if (lo != null && hi != null) {
         this.lo = lo;
         this.hi = hi;
      } else {
         throw new IllegalArgumentException("a pair needs two parties");
      }
   }

   public static PairKey of(UUID a, UUID b) {
      int cmp = a.compareTo(b);
      if (cmp == 0) {
         throw new IllegalArgumentException("a creature cannot pair with itself: " + a);
      } else {
         return cmp < 0 ? new PairKey(a, b) : new PairKey(b, a);
      }
   }

   public boolean involves(UUID id) {
      return this.lo.equals(id) || this.hi.equals(id);
   }

   public UUID other(UUID id) {
      if (this.lo.equals(id)) {
         return this.hi;
      } else if (this.hi.equals(id)) {
         return this.lo;
      } else {
         throw new IllegalArgumentException(id + " is not part of " + this);
      }
   }
}
