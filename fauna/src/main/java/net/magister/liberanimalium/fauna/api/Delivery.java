package net.magister.liberanimalium.fauna.api;

public enum Delivery {
   HAND(1.0F),
   DROPPED(0.5F),
   PREY(2.0F),
   GIFT(1.0F);

   private final float share;

   Delivery(float share) {
      this.share = share;
   }

   public float share() {
      return this.share;
   }
}
