package net.magister.liberanimalium.fauna.move;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiPredicate;
import java.util.function.IntToDoubleFunction;
import net.magister.liberanimalium.fauna.api.MoveMode;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

public final class Ribbon {
   private Ribbon() {
   }

   public static Vec3 at(List<Vec3> poly, Ribbon.Cursor cursor) {
      if (poly.isEmpty()) {
         return Vec3.ZERO;
      }

      int s = Math.min(cursor.segment(), poly.size() - 1);
      if (s >= poly.size() - 1) {
         return poly.get(poly.size() - 1);
      }

      Vec3 a = poly.get(s);
      Vec3 b = poly.get(s + 1);
      return a.add(b.subtract(a).scale(Mth.clamp(cursor.t(), 0.0, 1.0)));
   }

   public static Ribbon.Cursor project(List<Vec3> poly, Vec3 body, Ribbon.Cursor previous) {
      if (poly.size() < 2) {
         return new Ribbon.Cursor(0, 0.0);
      }

      int first = Math.min(Math.max(0, previous.segment()), poly.size() - 2);
      int last = Math.min(first + 2, poly.size() - 2);
      Ribbon.Cursor best = new Ribbon.Cursor(first, Mth.clamp(previous.t(), 0.0, 1.0));
      double bestDistanceSq = body.distanceToSqr(at(poly, best));

      for (int s = first; s <= last; s++) {
         double t = parameterOn(poly.get(s), poly.get(s + 1), body);
         if (s == first) {
            t = Math.max(t, Mth.clamp(previous.t(), 0.0, 1.0));
         }

         Ribbon.Cursor candidate = new Ribbon.Cursor(s, t);
         double distanceSq = body.distanceToSqr(at(poly, candidate));
         if (distanceSq < bestDistanceSq) {
            bestDistanceSq = distanceSq;
            best = candidate;
         }
      }

      return best;
   }

   public static double parameterOn(Vec3 a, Vec3 b, Vec3 p) {
      Vec3 ab = b.subtract(a);
      double lengthSq = ab.lengthSqr();
      return lengthSq < 1.0E-9 ? 0.0 : Mth.clamp(p.subtract(a).dot(ab) / lengthSq, 0.0, 1.0);
   }

   public static double distanceToSegment(Vec3 p, Vec3 a, Vec3 b) {
      Vec3 ab = b.subtract(a);
      double lengthSq = ab.lengthSqr();
      return lengthSq < 1.0E-9 ? p.distanceTo(a) : p.distanceTo(a.add(ab.scale(Mth.clamp(p.subtract(a).dot(ab) / lengthSq, 0.0, 1.0))));
   }

   public static Vec3 reference(List<Vec3> poly, Ribbon.Cursor cursor, double lookahead, IntToDoubleFunction cutAt) {
      if (poly.isEmpty()) {
         return Vec3.ZERO;
      }

      if (poly.size() == 1) {
         return poly.get(0);
      }

      double remaining = Math.max(0.0, lookahead);
      Vec3 from = at(poly, cursor);
      int segment = Math.min(Math.max(0, cursor.segment()), poly.size() - 2);

      while (true) {
         Vec3 corner = poly.get(segment + 1);
         double leg = from.distanceTo(corner);
         if (leg >= remaining) {
            return leg < 1.0E-6 ? corner : from.add(corner.subtract(from).scale(remaining / leg));
         }

         remaining -= leg;
         from = corner;
         if (segment + 2 > poly.size() - 1) {
            return from;
         }

         double cut = cutAt.applyAsDouble(segment + 1);
         if (cut <= 0.0) {
            return corner;
         }

         remaining = Math.min(remaining, cut);
         segment++;
      }
   }

   public static double radius(double speedPerTick, double yawDegreesPerTick) {
      double omega = Math.toRadians(Math.max(1.0E-4, yawDegreesPerTick));
      return Math.max(0.0, speedPerTick) / omega;
   }

   public static double anticipation(double radius, double turnRadians) {
      double half = Math.max(0.0, turnRadians) * 0.5;
      return half >= 1.5706963267948966 ? Double.MAX_VALUE : radius * Math.tan(half);
   }

   public static double turnAngle(Vec3 a, Vec3 b, Vec3 c) {
      Vec3 in = b.subtract(a);
      Vec3 out = c.subtract(b);
      if (!(in.lengthSqr() < 1.0E-9) && !(out.lengthSqr() < 1.0E-9)) {
         double dot = in.normalize().dot(out.normalize());
         return Math.acos(Math.max(-1.0, Math.min(1.0, dot)));
      } else {
         return 0.0;
      }
   }

   public static List<Vec3> arcSamples(Vec3 previous, Vec3 corner, Vec3 next, double anticipation) {
      List<Vec3> out = new ArrayList<>(3);
      Vec3 in = corner.subtract(previous);
      Vec3 leave = next.subtract(corner);
      if (!(in.lengthSqr() < 1.0E-9) && !(leave.lengthSqr() < 1.0E-9)) {
         Vec3 inDir = in.normalize();
         Vec3 outDir = leave.normalize();
         Vec3 entry = corner.subtract(inDir.scale(anticipation));
         Vec3 exit = corner.add(outDir.scale(anticipation));
         out.add(entry);
         out.add(arcMidpoint(previous, corner, next, anticipation, inDir, outDir));
         out.add(exit);
         return out;
      } else {
         return out;
      }
   }

   private static Vec3 arcMidpoint(Vec3 previous, Vec3 corner, Vec3 next, double anticipation, Vec3 inDir, Vec3 outDir) {
      Vec3 bisector = outDir.subtract(inDir);
      if (bisector.lengthSqr() < 1.0E-9) {
         return corner;
      }

      double halfTurn = turnAngle(previous, corner, next) * 0.5;
      return corner.add(bisector.normalize().scale(anticipation * Math.tan(halfTurn * 0.5)));
   }

   public static NavPath prune(NavPath path, BiPredicate<NavNode, NavNode> clear) {
      List<NavPath.Step> steps = path.steps();
      int n = steps.size();
      if (n < 3) {
         return path;
      }

      boolean[] keep = new boolean[n];
      keep[0] = true;
      keep[n - 1] = true;

      for (int i = 0; i < n; i++) {
         boolean anchor = steps.get(i).transitionBefore() != null || steps.get(i).node().mode() != MoveMode.AIR;
         if (anchor) {
            keep[i] = true;
            if (i > 0) {
               keep[i - 1] = true;
            }

            if (i < n - 1) {
               keep[i + 1] = true;
            }
         }
      }

      keepCeilings(steps, keep);
      int low = 0;

      for (int high = 1; high < n; high++) {
         if (keep[high]) {
            if (high - low >= 2) {
               bisect(steps, low, high, clear, keep);
            }

            low = high;
         }
      }

      List<NavPath.Step> out = new ArrayList<>();

      for (int i = 0; i < n; i++) {
         if (keep[i]) {
            out.add(steps.get(i));
         }
      }

      return out.size() == n ? path : new NavPath(List.copyOf(out), path.outcome(), path.reach());
   }

   private static void keepCeilings(List<NavPath.Step> steps, boolean[] keep) {
      int n = steps.size();
      int start = -1;

      for (int i = 0; i <= n; i++) {
         boolean air = i < n && steps.get(i).node().mode() == MoveMode.AIR;
         if (air && start < 0) {
            start = i;
         }

         if ((!air || i >= n) && start >= 0) {
            int top = Integer.MIN_VALUE;

            for (int k = start; k < i; k++) {
               top = Math.max(top, steps.get(k).node().y());
            }

            int first = -1;
            int last = -1;

            for (int k = start; k < i; k++) {
               if (steps.get(k).node().y() == top) {
                  if (first < 0) {
                     first = k;
                  }

                  last = k;
               }
            }

            keep[first] = true;
            keep[last] = true;
            start = -1;
         }
      }
   }

   private static void bisect(List<NavPath.Step> steps, int low, int high, BiPredicate<NavNode, NavNode> clear, boolean[] keep) {
      if (high - low >= 2) {
         if (!clear.test(steps.get(low).node(), steps.get(high).node())) {
            int mid = (low + high) / 2;
            keep[mid] = true;
            bisect(steps, low, mid, clear, keep);
            bisect(steps, mid, high, clear, keep);
         }
      }
   }

   public record Cursor(int segment, double t) {
      public static final Ribbon.Cursor START = new Ribbon.Cursor(0, 0.0);
   }
}
