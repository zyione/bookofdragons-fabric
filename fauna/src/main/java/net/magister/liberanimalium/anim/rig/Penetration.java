package net.magister.liberanimalium.anim.rig;

import java.util.List;
import java.util.function.Function;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

public final class Penetration {
   static final int MAX_ROUNDS = 4;
   static final double SKIN = 0.001;
   public static final int NO_AXIS = 0;
   static final double[] INTERNAL = new double[3];
   static final double SEAM = 0.001;

   private Penetration() {
   }

   public static AABB sphereQuery(Vec3 c, double r) {
      return new AABB(c.x - r, c.y - r, c.z - r, c.x + r, c.y + r, c.z + r);
   }

   public static Vec3 sphere(Vec3 c, double r, List<AABB> boxes) {
      return sphere(null, c, r, 0, query -> boxes).push();
   }

   public static Penetration.Push sphere(Vec3 from, Vec3 c, double r, int prefer, Function<AABB, List<AABB>> boxesFor) {
      Vec3 at = c;
      int axis = 0;

      for (int round = 0; round < 4; round++) {
         List<AABB> boxes = boxesFor.apply(sphereQuery(at, r));
         boolean moved = false;

         for (AABB box : boxes) {
            double[] d = contact(box, at, r, boxes);
            if (d != INTERNAL) {
               if (d != null) {
                  double dist = Math.sqrt(d[0] * d[0] + d[1] * d[1] + d[2] * d[2]);
                  if (!(dist >= r - 0.001)) {
                     double k = (r - dist) / dist;
                     at = new Vec3(at.x + d[0] * k, at.y + d[1] * k, at.z + d[2] * k);
                     moved = true;
                  }
               } else {
                  int exit = exitAxis(box, at, r, from, prefer, boxesFor);
                  at = throughFace(box, at, r, exit);
                  axis = exit;
                  moved = true;
               }
            }
         }

         if (!moved) {
            break;
         }
      }

      Vec3 push = at.subtract(c);
      return push.lengthSqr() == 0.0 ? Penetration.Push.NONE : new Penetration.Push(push, axis);
   }

   static double[] contact(AABB box, Vec3 at, double r, List<AABB> boxes) {
      return contact(box, at, r, of(boxes));
   }

   public static Penetration.Solids of(final List<AABB> boxes) {
      return new Penetration.Solids() {
         @Override
         public int count() {
            return boxes.size();
         }

         @Override
         public AABB box(int k) {
            return boxes.get(k);
         }

         @Override
         public boolean holds(AABB box, double qx, double qy, double qz) {
            for (AABB other : boxes) {
               if (other != box
                  && other.minX < qx
                  && qx < other.maxX
                  && other.minY < qy
                  && qy < other.maxY
                  && other.minZ < qz
                  && qz < other.maxZ) {
                  return true;
               }
            }

            return false;
         }
      };
   }

   static double[] contact(AABB box, Vec3 at, double r, Penetration.Solids boxes) {
      double cx = Mth.clamp(at.x, box.minX, box.maxX);
      double cy = Mth.clamp(at.y, box.minY, box.maxY);
      double cz = Mth.clamp(at.z, box.minZ, box.maxZ);
      double dx = at.x - cx;
      double dy = at.y - cy;
      double dz = at.z - cz;
      if (dx == 0.0 && dy == 0.0 && dz == 0.0) {
         return null;
      }

      if (dx * dx + dy * dy + dz * dz >= r * r) {
         return new double[]{dx, dy, dz};
      }

      int clamped = (dx != 0.0 ? 1 : 0) + (dy != 0.0 ? 1 : 0) + (dz != 0.0 ? 1 : 0);
      if (clamped == 1) {
         return new double[]{dx, dy, dz};
      }

      boolean ix = dx != 0.0 && across(box, cx, cy, cz, dx, dy, dz, 0, boxes);
      boolean iy = dy != 0.0 && across(box, cx, cy, cz, dx, dy, dz, 1, boxes);
      boolean iz = dz != 0.0 && across(box, cx, cy, cz, dx, dy, dz, 2, boxes);
      if (ix) {
         dx = 0.0;
      }

      if (iy) {
         dy = 0.0;
      }

      if (iz) {
         dz = 0.0;
      }

      return dx == 0.0 && dy == 0.0 && dz == 0.0 ? INTERNAL : new double[]{dx, dy, dz};
   }

   static boolean across(AABB box, double cx, double cy, double cz, double dx, double dy, double dz, int a, List<AABB> boxes) {
      return across(box, cx, cy, cz, dx, dy, dz, a, of(boxes));
   }

   static boolean across(AABB box, double cx, double cy, double cz, double dx, double dy, double dz, int a, Penetration.Solids boxes) {
      double qx = cx;
      double qy = cy;
      double qz = cz;
      if (a == 0) {
         qx += Math.signum(dx) * 0.001;
      } else if (dx != 0.0) {
         qx -= Math.signum(dx) * 0.001;
      }

      if (a == 1) {
         qy += Math.signum(dy) * 0.001;
      } else if (dy != 0.0) {
         qy -= Math.signum(dy) * 0.001;
      }

      if (a == 2) {
         qz += Math.signum(dz) * 0.001;
      } else if (dz != 0.0) {
         qz -= Math.signum(dz) * 0.001;
      }

      return boxes.holds(box, qx, qy, qz);
   }

   public static double clearance(Vec3 c, double r, double far, List<AABB> boxes, Vec3 heading) {
      return clearance(c, r, far, of(boxes), heading);
   }

   public static double clearance(Vec3 c, double r, double far, Penetration.Solids boxes, Vec3 heading) {
      double best = far;
      int k = 0;

      for (int n = boxes.count(); k < n; k++) {
         AABB box = boxes.box(k);
         double[] d = contact(box, c, Double.POSITIVE_INFINITY, boxes);
         if (d != INTERNAL && (d == null || !receding(d, heading))) {
            double gap = d == null ? -r : Math.sqrt(d[0] * d[0] + d[1] * d[1] + d[2] * d[2]) - r;
            best = Math.min(best, gap);
         }
      }

      return best;
   }

   static boolean receding(double[] d, Vec3 heading) {
      if (heading == null) {
         return false;
      }

      double dot = d[0] * heading.x + d[1] * heading.y + d[2] * heading.z;
      return dot >= -1.0E-9 * Math.sqrt((d[0] * d[0] + d[1] * d[1] + d[2] * d[2]) * heading.lengthSqr());
   }

   public static boolean sphereOverlaps(Vec3 c, double r, List<AABB> boxes) {
      return sphereDepth(c, r, boxes) > 0.001;
   }

   public static boolean segmentCrosses(Vec3 a, Vec3 b, List<AABB> boxes) {
      return segmentCrosses(a, b, of(boxes));
   }

   public static boolean segmentCrosses(Vec3 a, Vec3 b, Penetration.Solids boxes) {
      double dx = b.x - a.x;
      double dy = b.y - a.y;
      double dz = b.z - a.z;
      double[] a3 = new double[]{a.x, a.y, a.z};
      double[] d3 = new double[]{dx, dy, dz};
      double[] lo = new double[3];
      double[] hi = new double[3];
      double minX = Math.min(a.x, b.x);
      double maxX = Math.max(a.x, b.x);
      double minY = Math.min(a.y, b.y);
      double maxY = Math.max(a.y, b.y);
      double minZ = Math.min(a.z, b.z);
      double maxZ = Math.max(a.z, b.z);
      int n = boxes.count();

      for (int q = 0; q < n; q++) {
         AABB box = boxes.box(q);
         lo[0] = box.minX + 0.001;
         lo[1] = box.minY + 0.001;
         lo[2] = box.minZ + 0.001;
         hi[0] = box.maxX - 0.001;
         hi[1] = box.maxY - 0.001;
         hi[2] = box.maxZ - 0.001;
         if (!(maxX <= lo[0]) && !(minX >= hi[0]) && !(maxY <= lo[1]) && !(minY >= hi[1]) && !(maxZ <= lo[2]) && !(minZ >= hi[2])) {
            double tEnter = 0.0;
            double tExit = 1.0;
            boolean miss = false;

            for (int k = 0; k < 3 && !miss; k++) {
               if (Math.abs(d3[k]) < 1.0E-12) {
                  if (a3[k] <= lo[k] || a3[k] >= hi[k]) {
                     miss = true;
                  }
               } else {
                  double t0 = (lo[k] - a3[k]) / d3[k];
                  double t1 = (hi[k] - a3[k]) / d3[k];
                  if (t0 > t1) {
                     double s = t0;
                     t0 = t1;
                     t1 = s;
                  }

                  tEnter = Math.max(tEnter, t0);
                  tExit = Math.min(tExit, t1);
                  if (tEnter >= tExit) {
                     miss = true;
                  }
               }
            }

            if (!miss) {
               if (tEnter > 0.0) {
                  return true;
               }

               if (tExit < 1.0) {
                  int nearest = -1;
                  double nearestGap = Double.POSITIVE_INFINITY;

                  for (int k = 0; k < 3; k++) {
                     double gapLo = a3[k] - lo[k];
                     double gapHi = hi[k] - a3[k];
                     if (gapLo < nearestGap) {
                        nearestGap = gapLo;
                        nearest = 2 * k;
                     }

                     if (gapHi < nearestGap) {
                        nearestGap = gapHi;
                        nearest = 2 * k + 1;
                     }
                  }

                  double x = a3[0] + dx * tExit;
                  double y = a3[1] + dy * tExit;
                  double z = a3[2] + dz * tExit;
                  double[] at = new double[]{x, y, z};
                  int exit = -1;
                  double exitGap = Double.POSITIVE_INFINITY;

                  for (int k = 0; k < 3; k++) {
                     if (Math.abs(at[k] - lo[k]) < exitGap) {
                        exitGap = Math.abs(at[k] - lo[k]);
                        exit = 2 * k;
                     }

                     if (Math.abs(at[k] - hi[k]) < exitGap) {
                        exitGap = Math.abs(at[k] - hi[k]);
                        exit = 2 * k + 1;
                     }
                  }

                  if (exit != nearest) {
                     return true;
                  }
               }
            }
         }
      }

      return false;
   }

   public static double signedDepth(Vec3 c, double r, double far, List<AABB> boxes) {
      return signedDepth(c, r, far, of(boxes));
   }

   public static double signedDepth(Vec3 c, double r, double far, Penetration.Solids boxes) {
      double deep = sphereDepth(c, r, boxes);
      return deep > 0.0 ? deep : -clearance(c, r, far, boxes, null);
   }

   public static boolean clear(Vec3 c, double r, List<AABB> boxes) {
      return clear(c, r, of(boxes));
   }

   public static boolean clear(Vec3 c, double r, Penetration.Solids boxes) {
      double rr = r * r;
      int n = boxes.count();

      for (int k = 0; k < n; k++) {
         AABB box = boxes.box(k);
         double dx = c.x - Mth.clamp(c.x, box.minX, box.maxX);
         double dy = c.y - Mth.clamp(c.y, box.minY, box.maxY);
         double dz = c.z - Mth.clamp(c.z, box.minZ, box.maxZ);
         if (dx == 0.0 && dy == 0.0 && dz == 0.0) {
            return false;
         }

         double sum = dx * dx + dy * dy + dz * dz;
         if (sum >= rr) {
            if (r - Math.sqrt(sum) > 0.0) {
               return false;
            }
         } else {
            double[] d = contact(box, c, r, boxes);
            if (d != INTERNAL && r - Math.sqrt(d[0] * d[0] + d[1] * d[1] + d[2] * d[2]) > 0.0) {
               return false;
            }
         }
      }

      double past = r + 1.0E-9 * (1.0 + r);

      for (int k = 0; k < n; k++) {
         AABB box = boxes.box(k);
         double cx = Mth.clamp(c.x, box.minX, box.maxX);
         double cy = Mth.clamp(c.y, box.minY, box.maxY);
         double cz = Mth.clamp(c.z, box.minZ, box.maxZ);
         double dx = c.x - cx;
         double dy = c.y - cy;
         double dz = c.z - cz;
         double least = Double.POSITIVE_INFINITY;
         if (dx != 0.0) {
            least = Math.abs(dx);
         }

         if (dy != 0.0) {
            least = Math.min(least, Math.abs(dy));
         }

         if (dz != 0.0) {
            least = Math.min(least, Math.abs(dz));
         }

         if (!(least > past)
            && (
               (dx != 0.0 ? 1 : 0) + (dy != 0.0 ? 1 : 0) + (dz != 0.0 ? 1 : 0) < 2
                  || (!(Math.abs(dx) > past) || across(box, cx, cy, cz, dx, dy, dz, 0, boxes))
                     && (!(Math.abs(dy) > past) || across(box, cx, cy, cz, dx, dy, dz, 1, boxes))
                     && (!(Math.abs(dz) > past) || across(box, cx, cy, cz, dx, dy, dz, 2, boxes))
            )) {
            double[] d = contact(box, c, Double.POSITIVE_INFINITY, boxes);
            if (d != INTERNAL) {
               double gap = d == null ? -r : Math.sqrt(d[0] * d[0] + d[1] * d[1] + d[2] * d[2]) - r;
               if (gap < 0.0) {
                  return false;
               }
            }
         }
      }

      return true;
   }

   public static double sphereDepth(Vec3 c, double r, List<AABB> boxes) {
      return sphereDepth(c, r, of(boxes));
   }

   public static double sphereDepth(Vec3 c, double r, Penetration.Solids boxes) {
      double worst = 0.0;
      int k = 0;

      for (int n = boxes.count(); k < n; k++) {
         AABB box = boxes.box(k);
         double[] d = contact(box, c, r, boxes);
         if (d != INTERNAL) {
            double depth;
            if (d != null) {
               depth = r - Math.sqrt(d[0] * d[0] + d[1] * d[1] + d[2] * d[2]);
            } else {
               double in = Math.min(
                  Math.min(Math.min(c.x - box.minX, box.maxX - c.x), Math.min(c.y - box.minY, box.maxY - c.y)),
                  Math.min(c.z - box.minZ, box.maxZ - c.z)
               );
               depth = r + in;
            }

            worst = Math.max(worst, depth);
         }
      }

      return worst;
   }

   static int exitAxis(AABB box, Vec3 at, double r, Vec3 from, int prefer, Function<AABB, List<AABB>> boxesFor) {
      int[] order = new int[7];
      int n = 0;
      if (from != null && !contains(box, from)) {
         int entered = entryAxis(box, from, at);
         if (entered != 0) {
            order[n++] = entered;
         }
      }

      if (prefer != 0) {
         order[n++] = prefer;
      }

      double[] dist = new double[]{
         at.x - box.minX, box.maxX - at.x, box.maxY - at.y, at.z - box.minZ, box.maxZ - at.z
      };
      int[] faces = new int[]{-1, 1, 2, -3, 3};

      for (int a = 0; a < faces.length; a++) {
         int best = -1;

         for (int b = 0; b < faces.length; b++) {
            if (faces[b] != 0 && (best < 0 || dist[b] < dist[best])) {
               best = b;
            }
         }

         order[n++] = faces[best];
         faces[best] = 0;
      }

      for (int k = 0; k < n; k++) {
         Vec3 out = throughFace(box, at, r, order[k]);
         boolean open = true;

         for (AABB other : boxesFor.apply(sphereQuery(out, 0.001))) {
            if (other != box && contains(other, out)) {
               open = false;
               break;
            }
         }

         if (open) {
            return order[k];
         }
      }

      return order[0];
   }

   private static boolean contains(AABB box, Vec3 p) {
      return box.minX < p.x
         && p.x < box.maxX
         && box.minY < p.y
         && p.y < box.maxY
         && box.minZ < p.z
         && p.z < box.maxZ;
   }

   static int entryAxis(AABB box, Vec3 from, Vec3 to) {
      double enter = Double.NEGATIVE_INFINITY;
      double leave = Double.POSITIVE_INFINITY;
      int axis = 0;
      double[] f = new double[]{from.x, from.y, from.z};
      double[] t = new double[]{to.x, to.y, to.z};
      double[] lo = new double[]{box.minX, box.minY, box.minZ};
      double[] hi = new double[]{box.maxX, box.maxY, box.maxZ};

      for (int a = 0; a < 3; a++) {
         double d = t[a] - f[a];
         if (Math.abs(d) < 1.0E-12) {
            if (f[a] < lo[a] || f[a] > hi[a]) {
               return 0;
            }
         } else {
            double t0 = (lo[a] - f[a]) / d;
            double t1 = (hi[a] - f[a]) / d;
            int face = d > 0.0 ? -(a + 1) : a + 1;
            if (t0 > t1) {
               double s = t0;
               t0 = t1;
               t1 = s;
            }

            if (t0 > enter) {
               enter = t0;
               axis = face;
            }

            leave = Math.min(leave, t1);
         }
      }

      return enter <= leave && enter <= 1.0 ? axis : 0;
   }

   static Vec3 throughFace(AABB box, Vec3 at, double r, int axis) {
      return switch (axis) {
         case -3 -> new Vec3(at.x, at.y, box.minZ - r);
         case -2 -> new Vec3(at.x, box.minY - r, at.z);
         case -1 -> new Vec3(box.minX - r, at.y, at.z);
         default -> new Vec3(at.x, at.y, box.maxZ + r);
         case 1 -> new Vec3(box.maxX + r, at.y, at.z);
         case 2 -> new Vec3(at.x, box.maxY + r, at.z);
      };
   }

   public static Vec3 sideways(Vec3 c, double r, AABB box) {
      if (box.minX < c.x + r
         && box.maxX > c.x - r
         && box.minY < c.y + r
         && box.maxY > c.y - r
         && box.minZ < c.z + r
         && box.maxZ > c.z - r) {
         double west = c.x + r - box.minX + 0.001;
         double east = box.maxX - (c.x - r) + 0.001;
         double north = c.z + r - box.minZ + 0.001;
         double south = box.maxZ - (c.z - r) + 0.001;
         double best = west;
         Vec3 push = new Vec3(-west, 0.0, 0.0);
         if (east < best) {
            best = east;
            push = new Vec3(east, 0.0, 0.0);
         }

         if (north < best) {
            best = north;
            push = new Vec3(0.0, 0.0, -north);
         }

         if (south < best) {
            push = new Vec3(0.0, 0.0, south);
         }

         return push;
      } else {
         return Vec3.ZERO;
      }
   }

   public static boolean overlaps(Vec3 c, double r, List<AABB> boxes) {
      return overlaps(c, r, r, boxes);
   }

   public static boolean overlaps(Vec3 c, double hx, double hy, List<AABB> boxes) {
      for (AABB box : boxes) {
         if (inside(box, c, hx, hy)) {
            return true;
         }
      }

      return false;
   }

   static boolean inside(AABB box, Vec3 c, double hx, double hy) {
      return box.minX < c.x + hx - 0.001
         && box.maxX > c.x - hx + 0.001
         && box.minY < c.y + hy - 0.001
         && box.maxY > c.y - hy + 0.001
         && box.minZ < c.z + hx - 0.001
         && box.maxZ > c.z - hx + 0.001;
   }

   public record Push(Vec3 push, int axis) {
      public static final Penetration.Push NONE = new Penetration.Push(Vec3.ZERO, 0);
   }

   public interface Solids {
      int count();

      AABB box(int var1);

      boolean holds(AABB var1, double var2, double var4, double var6);
   }
}
