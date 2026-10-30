package net.magister.liberanimalium.anim.rig;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public final class GaitPlan {
   private static final double PAIR_WITHIN_PX = 16.0;
   public final int[] pair;
   public final int[][] adjacent;
   public final int[] order;
   public final int[] diagonal;
   public final int[] rank;

   private GaitPlan(int[] pair, int[][] adjacent, int[] order, int[] diagonal, int[] rank) {
      this.pair = pair;
      this.adjacent = adjacent;
      this.order = order;
      this.diagonal = diagonal;
      this.rank = rank;
   }

   public boolean isFront(int i) {
      return i >= 0 && i < this.rank.length && this.rank[i] == 0;
   }

   public static GaitPlan of(double[] x, double[] y, double[] z) {
      int n = x.length;
      int[] pair = new int[n];

      for (int a = 0; a < n; a++) {
         pair[a] = -1;
         if (!Double.isNaN(x[a])) {
            double best = 16.0;

            for (int b = 0; b < n; b++) {
               if (b != a && !Double.isNaN(x[b])) {
                  double d = Math.hypot(Math.hypot(-x[a] - x[b], y[a] - y[b]), z[a] - z[b]);
                  if (d < best) {
                     best = d;
                     pair[a] = b;
                  }
               }
            }
         }
      }

      int[] side = new int[n];
      int[] rank = new int[n];

      for (int a = 0; a < n; a++) {
         if (!Double.isNaN(x[a])) {
            side[a] = x[a] >= 0.0 ? 0 : 1;

            for (int b = 0; b < n; b++) {
               if (b != a && !Double.isNaN(x[b]) && (x[b] >= 0.0 ? 0 : 1) == side[a] && (z[b] < z[a] || z[b] == z[a] && b < a)) {
                  rank[a]++;
               }
            }
         }
      }

      int[][] adjacent = new int[n][];
      int[] diagonal = new int[n];

      for (int a = 0; a < n; a++) {
         List<Integer> near = new ArrayList<>();
         if (pair[a] >= 0) {
            near.add(pair[a]);
         }

         if (!Double.isNaN(x[a])) {
            for (int b = 0; b < n; b++) {
               if (b != a && b != pair[a] && !Double.isNaN(x[b]) && side[b] == side[a] && Math.abs(rank[b] - rank[a]) == 1) {
                  near.add(b);
               }
            }
         }

         adjacent[a] = near.stream().mapToInt(Integer::intValue).toArray();
         diagonal[a] = rank[a] + side[a] & 1;
      }

      Integer[] boxed = new Integer[n];

      for (int i = 0; i < n; i++) {
         boxed[i] = i;
      }

      Arrays.sort(
         boxed,
         (a, b) -> diagonal[a] != diagonal[b]
            ? Integer.compare(diagonal[a], diagonal[b])
            : (rank[a] != rank[b] ? Integer.compare(rank[a], rank[b]) : Integer.compare(a, b))
      );
      int[] order = new int[n];

      for (int i = 0; i < n; i++) {
         order[i] = boxed[i];
      }

      return new GaitPlan(pair, adjacent, order, diagonal, rank);
   }

   public boolean down(int i, FootStep[] feet) {
      for (int b : this.adjacent[i]) {
         if (!feet[b].grounded()) {
            return false;
         }
      }

      return true;
   }

   public boolean pairHalfway(int i, FootStep[] feet) {
      int p = this.pair[i];
      return p >= 0 && feet[p].grounded() ? feet[p].sinceLanded >= feet[i].trotWait() : true;
   }

   public boolean rested(int i, FootStep[] feet) {
      for (int b : this.adjacent[i]) {
         if (feet[b].sinceLanded < 1) {
            return false;
         }
      }

      return true;
   }
}
