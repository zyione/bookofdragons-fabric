package net.magister.liberanimalium.fauna.maneuver.pattern;

import org.jetbrains.annotations.Nullable;
import net.magister.liberanimalium.fauna.api.AnimalHandle;
import net.magister.liberanimalium.fauna.core.Seeds;
import net.magister.liberanimalium.fauna.maneuver.Ground;
import net.magister.liberanimalium.fauna.maneuver.Maneuver;
import net.magister.liberanimalium.fauna.maneuver.Subject;
import net.magister.liberanimalium.fauna.maneuver.Waypoints;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

public final class OrbitManeuver extends TargetedManeuver {
   private final int hand;
   private final double standoffHeight;
   @Nullable
   private Vec3 centre;
   private double swept;
   private double sweepWanted;
   private double lastBearing = Double.NaN;

   public OrbitManeuver(Subject subject, double min, double max, float speed, int hand, double standoffHeight) {
      super(subject, min, max, speed);
      this.hand = hand < 0 ? -1 : 1;
      this.standoffHeight = standoffHeight;
   }

   @Override
   public String phase() {
      return this.hand > 0 ? "right" : "left";
   }

   @Override
   public boolean running(AnimalHandle handle) {
      return false;
   }

   @Override
   public void begin(AnimalHandle handle, Waypoints move) {
      this.budgetFor(this.max);
   }

   @Override
   public Maneuver.Status tick(AnimalHandle handle, Waypoints move) {
      if (this.routeFailed(move)) {
         return Maneuver.Status.FAILED;
      }

      Vec3 aim = this.subject.estimate();
      if (this.centre == null || Ground.flatDistSq(this.centre, aim) > 6.25) {
         if (!this.cutRing(handle, move, aim)) {
            return this.blocked(handle) ? Maneuver.Status.FAILED : Maneuver.Status.RUNNING;
         }

         this.unblocked();
      }

      double bearing = bearingFrom(this.centre, handle.bodyPosition());
      if (Double.isNaN(this.lastBearing)) {
         this.lastBearing = bearing;
      }

      this.swept = this.swept + Math.abs(Mth.wrapDegrees(bearing - this.lastBearing));
      this.lastBearing = bearing;
      return this.swept >= this.sweepWanted ? Maneuver.Status.SUCCEEDED : Maneuver.Status.RUNNING;
   }

   private boolean cutRing(AnimalHandle handle, Waypoints move, Vec3 aim) {
      double gap = this.distance(handle);
      double floor = handle.turnRadius(handle.velocity().length());
      double want = Mth.clamp(gap, Math.max(this.min, floor), Math.max(this.max, floor));
      double ease = gap < this.min ? 1.0 : 0.35;
      double radius = Math.max(floor, gap + (want - gap) * ease);
      Vec3 ring = Ground.legalise(handle, new Vec3(aim.x, this.ringHeight(handle, aim), aim.z), true);
      if (ring == null) {
         return false;
      }

      double spread = Seeds.unit(handle.faunaId(), this.ticks) * 30.0;
      this.sweepWanted = 45.0 + spread;
      this.swept = 0.0;
      this.lastBearing = Double.NaN;
      this.centre = aim;
      move.loiter(ring, radius, this.hand, this.speed);
      return true;
   }

   private static double bearingFrom(Vec3 from, Vec3 of) {
      return Math.toDegrees(Math.atan2(of.x - from.x, of.z - from.z));
   }

   private double ringHeight(AnimalHandle handle, Vec3 aim) {
      return Ground.attackHeight(handle, aim, this.standoffHeight);
   }

   @Override
   public boolean premiseHolds(AnimalHandle handle) {
      if (!Ground.airborne(handle)) {
         return false;
      }

      Vec3 toUs = Ground.bearing(handle, this.subject.estimate(), handle.bodyPosition());
      double closure = this.subject.velocity().dot(toUs);
      return closure > 0.45 ? false : !(-closure > 0.35) && !(this.distance(handle) > this.max + 3.0);
   }
}
