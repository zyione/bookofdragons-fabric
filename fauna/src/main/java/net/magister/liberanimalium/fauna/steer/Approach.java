package net.magister.liberanimalium.fauna.steer;

public final class Approach {
   private double runway = -1.0;
   private static final double RELEASE_MULTIPLE = 2.0;

   public double runwayFor(double distanceToEnd, double liveRunway) {
      if (distanceToEnd > liveRunway && this.runway < 0.0) {
         return liveRunway;
      }

      if (this.runway < 0.0) {
         this.runway = liveRunway;
      }

      if (distanceToEnd > this.runway * 2.0) {
         this.runway = -1.0;
         return liveRunway;
      } else {
         return this.runway;
      }
   }

   public void reset() {
      this.runway = -1.0;
   }

   public double committed() {
      return this.runway;
   }
}
