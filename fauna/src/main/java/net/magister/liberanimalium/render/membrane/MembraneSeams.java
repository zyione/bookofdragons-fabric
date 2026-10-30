package net.magister.liberanimalium.render.membrane;

import java.util.Arrays;
import java.util.List;
import java.util.function.Function;
import org.joml.Vector3fc;

public final class MembraneSeams {
   static final float SAME_POINT = 1.0E-4F;
   private static final int PEAK_SAMPLES = 64;

   private MembraneSeams() {
   }

   public static MembraneSeams.Pins of(List<MembranePanel> panels, Function<String, Vector3fc> rest) {
      int n = panels.size();
      MembraneSeams.Pins pins = MembraneSeams.Pins.none(n);

      for (int i = 0; i < n; i++) {
         MembranePanel p = panels.get(i);

         for (int edge = 0; edge < 2; edge++) {
            float taper = taperAt(p, edge);
            if (!(taper <= 0.0F)) {
               Vector3fc a = corner(p, rest, edge == 0 ? 0 : 1);
               Vector3fc b = corner(p, rest, edge == 0 ? 3 : 2);
               if (a != null && b != null && !near(a, b) && sharedUnwelded(panels, i, edge, taper, a, b, rest)) {
                  if (edge == 0) {
                     pins.u0()[i] = true;
                  } else {
                     pins.u1()[i] = true;
                  }
               }
            }
         }

         pins.scale()[i] = peakScale(p, pins.u0()[i], pins.u1()[i]);
      }

      return pins;
   }

   private static boolean sharedUnwelded(
      List<MembranePanel> panels, int self, int edge, float taper, Vector3fc a, Vector3fc b, Function<String, Vector3fc> rest
   ) {
      MembranePanel p = panels.get(self);

      for (int j = 0; j < panels.size(); j++) {
         if (j != self) {
            MembranePanel q = panels.get(j);
            int[][] edges = new int[][]{{0, 3}, {1, 2}, {0, 1}, {3, 2}};

            for (int k = 0; k < edges.length; k++) {
               Vector3fc c = corner(q, rest, edges[k][0]);
               Vector3fc d = corner(q, rest, edges[k][1]);
               if (c != null && d != null) {
                  boolean same = near(a, c) && near(b, d) || near(a, d) && near(b, c);
                  if (same) {
                     boolean welded = k < 2 && Math.abs(taperAt(q, k) - taper) < 0.001F && sameFold(p, q);
                     if (!welded) {
                        return true;
                     }
                  }
               }
            }
         }
      }

      return false;
   }

   static float taperAt(MembranePanel p, int edge) {
      return taper(p, edge == 0 ? 0.0F : 1.0F);
   }

   static float taper(MembranePanel p, float u) {
      float span = p.uStart() + (p.uEnd() - p.uStart()) * u;
      return p.creasePeak() == 1 ? span : 1.0F - span;
   }

   public static float pinned(boolean u0, boolean u1, float u) {
      return (u0 ? u : 1.0F) * (u1 ? 1.0F - u : 1.0F);
   }

   private static float peakScale(MembranePanel p, boolean u0, boolean u1) {
      if (!u0 && !u1) {
         return 1.0F;
      }

      float free = 0.0F;
      float kept = 0.0F;

      for (int s = 0; s <= 64; s++) {
         float u = s / 64.0F;
         float t = taper(p, u);
         free = Math.max(free, t);
         kept = Math.max(kept, t * pinned(u0, u1, u));
      }

      return kept > 1.0E-4F ? free / kept : 1.0F;
   }

   private static boolean sameFold(MembranePanel p, MembranePanel q) {
      return Arrays.equals(p.foldRef(), q.foldRef()) && Float.compare(p.restRatio(), q.restRatio()) == 0;
   }

   private static Vector3fc corner(MembranePanel p, Function<String, Vector3fc> rest, int k) {
      return rest.apply(p.corner()[k]);
   }

   private static boolean near(Vector3fc a, Vector3fc b) {
      return a.distanceSquared(b) < 9.999999E-9F;
   }

   public record Pins(boolean[] u0, boolean[] u1, float[] scale) {
      static MembraneSeams.Pins none(int n) {
         float[] one = new float[n];
         Arrays.fill(one, 1.0F);
         return new MembraneSeams.Pins(new boolean[n], new boolean[n], one);
      }
   }
}
