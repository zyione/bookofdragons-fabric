package net.magister.liberanimalium.fauna.maneuver.pattern;

import java.util.List;
import org.jetbrains.annotations.Nullable;
import net.magister.liberanimalium.fauna.api.AnimalHandle;
import net.magister.liberanimalium.fauna.maneuver.Ground;
import net.magister.liberanimalium.fauna.maneuver.Maneuver;
import net.magister.liberanimalium.fauna.maneuver.Subject;
import net.magister.liberanimalium.fauna.maneuver.Waypoints;
import net.minecraft.world.phys.Vec3;

public final class AirFlankManeuver extends TargetedManeuver {
   private final int hand;
   private final double standoffHeight;
   @Nullable
   private Vec3 entry;

   public AirFlankManeuver(Subject subject, double min, double max, float speed, int hand, double standoffHeight) {
      super(subject, min, max, speed);
      this.hand = hand < 0 ? -1 : 1;
      this.standoffHeight = standoffHeight;
   }

   @Override
   public String phase() {
      return this.entry == null ? "cut" : "round";
   }

   @Override
   public boolean running(AnimalHandle handle) {
      return true;
   }

   @Override
   public void begin(AnimalHandle handle, Waypoints move) {
      this.budgetFor(this.max * 3.0);
      this.cut(handle, move);
   }

   @Override
   public Maneuver.Status tick(AnimalHandle handle, Waypoints move) {
      if (this.routeFailed(move)) {
         return Maneuver.Status.FAILED;
      }

      Vec3 toUs = Ground.bearing(handle, this.subject.estimate(), handle.bodyPosition());
      if (this.ticksRun() < 10 || !(this.subject.facing().dot(toUs) < -0.15) && !(this.distance(handle) <= this.min)) {
         if (this.entry != null && move.idle()) {
            return Maneuver.Status.SUCCEEDED;
         }

         if (this.entry == null) {
            if (!this.cut(handle, move)) {
               return this.blocked(handle) ? Maneuver.Status.FAILED : Maneuver.Status.RUNNING;
            }

            this.unblocked();
         }

         return this.stalled(this.distance(handle)) ? Maneuver.Status.FAILED : Maneuver.Status.RUNNING;
      } else {
         return Maneuver.Status.SUCCEEDED;
      }
   }

   private boolean cut(AnimalHandle handle, Waypoints move) {
      Vec3 at = handle.bodyPosition();
      Vec3 aim = this.subject.estimate();
      Vec3 rear = new Vec3(-this.subject.facing().x, 0.0, -this.subject.facing().z);
      if (rear.lengthSqr() < 1.0E-6) {
         rear = Ground.bearing(handle, aim, at);
      }

      Vec3 quarter = Ground.rotateY(rear.normalize(), Math.toRadians(60.0) * this.hand);
      double height = Ground.attackHeight(handle, aim, this.standoffHeight);
      Vec3 point = Ground.legalise(handle, new Vec3(aim.x, height, aim.z).add(quarter.scale(this.max)), true);
      if (point == null) {
         return false;
      }

      Vec3 line = Ground.legalise(handle, new Vec3(aim.x, height, aim.z).subtract(quarter.scale(this.min)), true);
      if (line == null) {
         return false;
      }

      if (!move.fly(List.of(at, point, line), this.speed)) {
         return false;
      }

      this.entry = point;
      return true;
   }

   @Override
   public boolean premiseHolds(AnimalHandle handle) {
      return Ground.airborne(handle);
   }
}
