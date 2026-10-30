package net.magister.liberanimalium.fauna.maneuver.pattern;

import java.util.List;
import org.jetbrains.annotations.Nullable;
import net.magister.liberanimalium.fauna.api.AnimalHandle;
import net.magister.liberanimalium.fauna.maneuver.Ground;
import net.magister.liberanimalium.fauna.maneuver.Maneuver;
import net.magister.liberanimalium.fauna.maneuver.Subject;
import net.magister.liberanimalium.fauna.maneuver.Waypoints;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

public final class FlankManeuver extends TargetedManeuver {
   @Nullable
   private Vec3 point;
   @Nullable
   private Vec3 tookAt;

   public FlankManeuver(Subject subject, double min, double max, float speed) {
      super(subject, min, max, speed);
   }

   @Override
   public void begin(AnimalHandle handle, Waypoints move) {
      this.budgetFor(this.max);
   }

   @Override
   public boolean running(AnimalHandle handle) {
      return true;
   }

   @Override
   public Maneuver.Status tick(AnimalHandle handle, Waypoints move) {
      if (this.routeFailed(move)) {
         return Maneuver.Status.FAILED;
      }

      if (this.ticks < 10 || !this.behind(handle) && !(this.distance(handle) <= this.min + 1.5)) {
         if (this.point == null || this.tookAt == null || this.turned() || this.at(handle, this.point)) {
            Vec3 taken = this.take(handle);
            if (taken == null) {
               return this.blocked(handle) ? Maneuver.Status.FAILED : Maneuver.Status.RUNNING;
            }

            this.unblocked();
            this.point = taken;
            this.tookAt = this.subject.facing();
            this.releaseSteering();
         }

         this.steer(move, this.point, this.speed);
         return this.stalled(Math.sqrt(Ground.flatDistSq(handle.bodyPosition(), this.point))) ? Maneuver.Status.FAILED : Maneuver.Status.RUNNING;
      } else {
         return Maneuver.Status.SUCCEEDED;
      }
   }

   private boolean behind(AnimalHandle handle) {
      Vec3 toUs = Ground.bearing(handle, this.subject.estimate(), handle.bodyPosition());
      return this.subject.facing().dot(toUs) < -0.15;
   }

   private boolean turned() {
      double dot = Mth.clamp(this.subject.facing().dot(this.tookAt), -1.0, 1.0);
      return Math.toDegrees(Math.acos(dot)) > 35.0;
   }

   @Nullable
   private Vec3 take(AnimalHandle handle) {
      Vec3 aim = this.aim(handle);
      Vec3 back = this.subject.facing().scale(-1.0);
      Vec3 out = Ground.bearing(handle, aim, handle.bodyPosition());
      double d = this.distance(handle);
      double want = this.min;
      double ease = d < this.min ? 1.0 : 0.35;
      double radius = d + (want - d) * ease;
      double toGo = Math.toDegrees(Math.acos(Mth.clamp(out.dot(back), -1.0, 1.0)));
      double step = Math.min(45.0, toGo);
      Vec3 left = Ground.rotateY(out, Math.toRadians(step));
      Vec3 right = Ground.rotateY(out, -Math.toRadians(step));
      Vec3 turned = left.dot(back) >= right.dot(back) ? left : right;
      List<Vec3> fan = Ground.fan(aim, turned, radius, 4, 60.0);
      return Ground.firstFit(handle, fan, Ground.airborne(handle));
   }
}
