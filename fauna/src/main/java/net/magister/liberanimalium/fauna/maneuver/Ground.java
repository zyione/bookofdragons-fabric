package net.magister.liberanimalium.fauna.maneuver;

import java.util.ArrayList;
import java.util.List;
import org.jetbrains.annotations.Nullable;
import net.magister.liberanimalium.fauna.api.AnimalHandle;
import net.magister.liberanimalium.fauna.api.MoveMode;
import net.magister.liberanimalium.fauna.api.Wings;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.Vec3;

public final class Ground {
   private Ground() {
   }

   @Nullable
   public static Vec3 firstFit(AnimalHandle handle, List<Vec3> offered, boolean airborne) {
      for (Vec3 raw : offered) {
         if (raw != null) {
            Vec3 point = legalise(handle, raw, airborne);
            if (point != null) {
               return point;
            }
         }
      }

      return null;
   }

   @Nullable
   public static Vec3 bestFit(AnimalHandle handle, List<Vec3> offered, boolean airborne, List<Vec3> away) {
      Vec3 best = null;
      double bestScore = Double.NEGATIVE_INFINITY;

      for (Vec3 raw : offered) {
         if (raw != null) {
            Vec3 point = legalise(handle, raw, airborne);
            if (point != null) {
               if (away.isEmpty()) {
                  return point;
               }

               double score = 0.0;

               for (Vec3 source : away) {
                  score += point.distanceTo(source);
               }

               if (score > bestScore) {
                  best = point;
                  bestScore = score;
               }
            }
         }
      }

      return best;
   }

   @Nullable
   public static Vec3 legalise(AnimalHandle handle, Vec3 raw, boolean airborne) {
      if (airborne) {
         Vec3 lifted = flightFloor(handle, raw);
         return handle.isBlockedAt(lifted) ? null : lifted;
      }

      BlockPos approx = BlockPos.containing(raw);
      BlockPos standable = handle.findStandableNear(approx);
      if (standable == null && handle.species().movement().allows(MoveMode.WATER)) {
         standable = handle.findSwimmableNear(approx);
      }

      return standable == null ? null : Vec3.atBottomCenterOf(standable);
   }

   public static Vec3 flightFloor(AnimalHandle handle, Vec3 p) {
      double ground = handle.groundHeightNear(p, p.y, 8.0);
      if (Double.isNaN(ground)) {
         return p;
      }

      double minY = ground + 4.0;
      return p.y < minY ? new Vec3(p.x, minY, p.z) : p;
   }

   public static double attackHeight(AnimalHandle handle, Vec3 aim, double standoffHeight) {
      return aim.y + Math.max(0.0, standoffHeight - Wings.of(handle).airGapBelow(aim));
   }

   public static boolean airborne(AnimalHandle handle) {
      return handle.currentMode() == MoveMode.AIR;
   }

   public static double arrivalTolerance(AnimalHandle handle) {
      return 1.5 * Math.max(1.0, handle.bounds().getXsize()) + 1.5;
   }

   public static double flatDistSq(Vec3 a, Vec3 b) {
      double dx = a.x - b.x;
      double dz = a.z - b.z;
      return dx * dx + dz * dz;
   }

   public static Vec3 bearing(AnimalHandle handle, Vec3 from, Vec3 to) {
      Vec3 delta = new Vec3(to.x - from.x, 0.0, to.z - from.z);
      if (delta.lengthSqr() > 1.0E-4) {
         return delta.normalize();
      }

      Vec3 look = handle.lookDirection();
      Vec3 flat = new Vec3(look.x, 0.0, look.z);
      return flat.lengthSqr() > 1.0E-4 ? flat.normalize() : new Vec3(0.0, 0.0, 1.0);
   }

   public static Vec3 rotateY(Vec3 v, double radians) {
      double cos = Math.cos(radians);
      double sin = Math.sin(radians);
      return new Vec3(v.x * cos - v.z * sin, v.y, v.x * sin + v.z * cos);
   }

   public static List<Vec3> ring(Vec3 centre, double radius, int samples, double phase) {
      List<Vec3> out = new ArrayList<>(samples);

      for (int i = 0; i < samples; i++) {
         double angle = phase + i * ((Math.PI * 2) / samples);
         out.add(centre.add(Math.cos(angle) * radius, 0.0, Math.sin(angle) * radius));
      }

      return out;
   }

   public static List<Vec3> fan(Vec3 origin, Vec3 heading, double distance, int samples, double spreadDegrees) {
      List<Vec3> out = new ArrayList<>(samples);
      double spread = Math.toRadians(spreadDegrees);

      for (int i = 0; i < samples; i++) {
         int step = (i + 1) / 2;
         double sign = i % 2 == 0 ? 1.0 : -1.0;
         double angle = step == 0 ? 0.0 : sign * spread * step / Math.max(1, samples / 2);
         out.add(origin.add(rotateY(heading, angle).scale(distance)));
      }

      return out;
   }
}
