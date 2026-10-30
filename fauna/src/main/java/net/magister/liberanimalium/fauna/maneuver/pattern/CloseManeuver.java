package net.magister.liberanimalium.fauna.maneuver.pattern;

import org.jetbrains.annotations.Nullable;
import net.magister.liberanimalium.fauna.api.AnimalHandle;
import net.magister.liberanimalium.fauna.maneuver.Ground;
import net.magister.liberanimalium.fauna.maneuver.Maneuver;
import net.magister.liberanimalium.fauna.maneuver.Subject;
import net.magister.liberanimalium.fauna.maneuver.Waypoints;
import net.minecraft.world.phys.Vec3;

public final class CloseManeuver extends TargetedManeuver {
   private final double ring;
   private final boolean driveThrough;
   private final int dwell;
   private int contactTicks;
   @Nullable
   private Vec3 line;
   @Nullable
   private Vec3 approachDir;

   public CloseManeuver(Subject subject, double min, double max, float speed, double ring, boolean driveThrough, int dwell) {
      super(subject, min, max, speed);
      this.ring = ring;
      this.driveThrough = driveThrough;
      this.dwell = Math.max(7, dwell);
   }

   @Override
   public String phase() {
      return this.contactTicks > 0 ? "contact" : "charge";
   }

   @Override
   public void begin(AnimalHandle handle, Waypoints move) {
      this.budgetFor(Math.max(this.distance(handle), this.min) + this.ring);
      Vec3 toTarget = this.subject.flatEstimate(handle.bodyPosition().y).subtract(handle.bodyPosition());
      if (toTarget.lengthSqr() > 1.0E-6) {
         this.approachDir = toTarget.normalize();
      }
   }

   @Override
   public boolean running(AnimalHandle handle) {
      return this.contactTicks == 0;
   }

   @Override
   public Maneuver.Status tick(AnimalHandle handle, Waypoints move) {
      if (this.routeFailed(move)) {
         return Maneuver.Status.FAILED;
      }

      double d = this.distance(handle);
      if (!this.driveThrough) {
         if (this.contactTicks > 0 ? d <= this.ring : d <= this.min) {
            if (this.contactTicks == 0) {
               this.releaseSteering();
            }

            move.stop();
            return ++this.contactTicks >= this.dwell ? Maneuver.Status.SUCCEEDED : Maneuver.Status.RUNNING;
         }

         if (this.contactTicks > 0) {
            this.contactTicks = 0;
            this.releaseSteering();
            this.budgetFor(Math.max(d, this.min) + this.ring);
         }
      } else if (this.approachDir != null) {
         Vec3 deltaFromTarget = handle.bodyPosition().subtract(this.subject.flatEstimate(handle.bodyPosition().y));
         double pastTarget = deltaFromTarget.dot(this.approachDir);
         if (pastTarget >= this.ring) {
            return Maneuver.Status.SUCCEEDED;
         }
      }

      if (this.driveThrough && this.line != null && this.at(handle, this.line)) {
         return Maneuver.Status.SUCCEEDED;
      }

      Vec3 want = this.cut(handle, d);
      Vec3 point = Ground.legalise(handle, want, Ground.airborne(handle));
      if (point == null) {
         return this.blocked(handle) ? Maneuver.Status.FAILED : Maneuver.Status.RUNNING;
      }

      this.unblocked();
      if (this.line == null || this.line.distanceToSqr(point) > 4.0) {
         this.line = point;
      }

      this.steer(move, this.line, this.speed);
      return this.stalled(d) ? Maneuver.Status.FAILED : Maneuver.Status.RUNNING;
   }

   private Vec3 cut(AnimalHandle handle, double d) {
      double lead = Math.min(20.0, d * 3.0);
      Vec3 ahead = this.subject.estimate().add(this.subject.velocity().scale(lead));
      Vec3 flat = new Vec3(ahead.x, handle.bodyPosition().y, ahead.z);
      Vec3 heading = Ground.bearing(handle, handle.bodyPosition(), flat);
      if (this.approachDir == null && heading.lengthSqr() > 1.0E-6) {
         this.approachDir = heading;
      }

      return flat.add(heading.scale(this.driveThrough ? this.ring : -this.min));
   }
}
