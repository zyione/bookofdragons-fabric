package net.magister.liberanimalium.fauna.maneuver.pattern;

import org.jetbrains.annotations.Nullable;
import net.magister.liberanimalium.fauna.api.AnimalHandle;
import net.magister.liberanimalium.fauna.maneuver.Ground;
import net.magister.liberanimalium.fauna.maneuver.Maneuver;
import net.magister.liberanimalium.fauna.maneuver.MoveStyles;
import net.magister.liberanimalium.fauna.maneuver.Subject;
import net.magister.liberanimalium.fauna.maneuver.Waypoints;
import net.minecraft.world.phys.Vec3;

public final class CreepManeuver extends TargetedManeuver {
   private final double springMargin;
   @Nullable
   private Vec3 destination;

   public CreepManeuver(Subject subject, double min, double max, float speed) {
      this(subject, min, max, speed, 3.0);
   }

   public CreepManeuver(Subject subject, double min, double max, float speed, double springMargin) {
      super(subject, min, max, speed * 0.35F);
      this.springMargin = springMargin;
   }

   @Override
   public float pace() {
      return this.speed;
   }

   @Override
   public boolean running(AnimalHandle handle) {
      return false;
   }

   private double arrive(AnimalHandle handle) {
      return MoveStyles.springRange(handle, this.springMargin) * 0.8;
   }

   @Override
   public void begin(AnimalHandle handle, Waypoints move) {
      this.budgetFor(Math.max(this.distance(handle) - this.arrive(handle), 1.0));
   }

   @Override
   public Maneuver.Status tick(AnimalHandle handle, Waypoints move) {
      if (this.routeFailed(move)) {
         return Maneuver.Status.FAILED;
      }

      double d = this.distance(handle);
      if (d <= this.arrive(handle)) {
         return Maneuver.Status.SUCCEEDED;
      }

      Vec3 aim = this.aim(handle);
      Vec3 toward = Ground.bearing(handle, handle.bodyPosition(), aim);
      Vec3 targetPoint = aim.subtract(toward.scale(this.arrive(handle)));
      Vec3 legal = Ground.legalise(handle, targetPoint, Ground.airborne(handle));
      if (legal == null) {
         return this.blocked(handle) ? Maneuver.Status.FAILED : Maneuver.Status.RUNNING;
      }

      this.unblocked();
      if (this.destination == null || this.destination.distanceToSqr(legal) > 4.0) {
         this.destination = legal;
      }

      this.steer(move, this.destination, this.pace());
      return this.stalled(d) ? Maneuver.Status.FAILED : Maneuver.Status.RUNNING;
   }

   @Override
   public boolean premiseHolds(AnimalHandle handle) {
      return !MoveStyles.withinSpring(handle, this.subject, this.springMargin);
   }
}
