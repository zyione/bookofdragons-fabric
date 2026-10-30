package net.magister.liberanimalium.body;

public final class ReporterElection {
   public static final int NONE = -1;

   private ReporterElection() {
   }

   public static int elect(int current, int rider, Iterable<ReporterElection.Candidate> candidates) {
      if (rider != -1) {
         return rider;
      }

      double keepSqr = 3136.0;
      double rangeSqr = 2304.0;
      int nearest = -1;
      double nearestSqr = Double.MAX_VALUE;

      for (ReporterElection.Candidate candidate : candidates) {
         if (candidate.id() == current && candidate.distanceSqr() <= keepSqr) {
            return current;
         }

         if (candidate.distanceSqr() <= rangeSqr && candidate.distanceSqr() < nearestSqr) {
            nearest = candidate.id();
            nearestSqr = candidate.distanceSqr();
         }
      }

      return nearest;
   }

   public record Candidate(int id, double distanceSqr) {
   }
}
