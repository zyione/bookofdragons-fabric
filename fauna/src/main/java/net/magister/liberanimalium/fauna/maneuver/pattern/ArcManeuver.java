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

public final class ArcManeuver extends TargetedManeuver {
   private final int hand;
   private final double retreatOpeningThreshold;
   private final double retreatDistanceExtra;
   @Nullable
   private Vec3 leg;
   @Nullable
   private Vec3 centre;

   public ArcManeuver(Subject subject, double min, double max, float speed, int hand) {
      this(subject, min, max, speed, hand, 0.35, 3.0);
   }

   public ArcManeuver(Subject subject, double min, double max, float speed, int hand, double retreatOpeningThreshold, double retreatDistanceExtra) {
      super(subject, min, max, speed);
      this.hand = hand < 0 ? -1 : 1;
      this.retreatOpeningThreshold = retreatOpeningThreshold;
      this.retreatDistanceExtra = retreatDistanceExtra;
   }

   @Override
   public String phase() {
      return this.hand > 0 ? "right" : "left";
   }

   @Override
   public void begin(AnimalHandle handle, Waypoints move) {
      this.budgetFor(this.max);
   }

   @Override
   public boolean running(AnimalHandle handle) {
      return this.distance(handle) < this.min;
   }

   @Override
   public Maneuver.Status tick(AnimalHandle handle, Waypoints move) {
      if (this.routeFailed(move)) {
         return Maneuver.Status.FAILED;
      }

      Vec3 aim = this.aim(handle);
      if (this.leg == null || this.centre == null || Ground.flatDistSq(this.centre, aim) > 6.25) {
         Vec3 cut = this.cutLeg(handle, aim);
         if (cut == null) {
            return this.blocked(handle) ? Maneuver.Status.FAILED : Maneuver.Status.RUNNING;
         }

         this.unblocked();
         this.leg = cut;
         this.centre = aim;
         this.releaseSteering();
      }

      if (this.at(handle, this.leg)) {
         return Maneuver.Status.SUCCEEDED;
      }

      this.steer(move, this.leg, this.speed);
      return this.stalled(Math.sqrt(Ground.flatDistSq(handle.bodyPosition(), this.leg))) ? Maneuver.Status.FAILED : Maneuver.Status.RUNNING;
   }

   @Nullable
   private Vec3 cutLeg(AnimalHandle handle, Vec3 aim) {
      Vec3 out = Ground.bearing(handle, aim, handle.bodyPosition());
      double d = this.distance(handle);
      double want = Mth.clamp(d, this.min, this.max);
      double ease = d < this.min ? 1.0 : 0.35;
      double radius = d + (want - d) * ease;
      double spread = Seeds.unit(handle.faunaId(), this.ticks) * 30.0;
      double degrees = 45.0 + spread;
      Vec3 turned = Ground.rotateY(out, Math.toRadians(degrees) * this.hand);
      return Ground.legalise(handle, aim.add(turned.scale(radius)), Ground.airborne(handle));
   }

   @Override
   public boolean premiseHolds(AnimalHandle handle) {
      Vec3 toUs = Ground.bearing(handle, this.subject.estimate(), handle.bodyPosition());
      double closure = this.subject.velocity().dot(toUs);
      if (closure > 0.45) {
         return false;
      }

      double opening = -closure;
      double d = this.distance(handle);
      return !(opening > this.retreatOpeningThreshold) && !(d > this.max + this.retreatDistanceExtra);
   }
}
