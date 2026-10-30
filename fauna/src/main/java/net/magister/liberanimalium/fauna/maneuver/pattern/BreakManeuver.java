package net.magister.liberanimalium.fauna.maneuver.pattern;

import java.util.List;
import org.jetbrains.annotations.Nullable;
import net.magister.liberanimalium.fauna.api.AnimalHandle;
import net.magister.liberanimalium.fauna.maneuver.Ground;
import net.magister.liberanimalium.fauna.maneuver.Maneuver;
import net.magister.liberanimalium.fauna.maneuver.Subject;
import net.magister.liberanimalium.fauna.maneuver.Waypoints;
import net.minecraft.world.phys.Vec3;

public final class BreakManeuver extends TargetedManeuver {
   private final double demanded;
   @Nullable
   private Vec3 out;

   public BreakManeuver(Subject subject, double demanded, float speed) {
      super(subject, demanded, demanded, speed);
      this.demanded = demanded;
   }

   @Override
   public String phase() {
      return this.out == null ? "cut" : "extend";
   }

   @Override
   public boolean running(AnimalHandle handle) {
      return true;
   }

   @Override
   public void begin(AnimalHandle handle, Waypoints move) {
      this.budgetFor(this.demanded);
   }

   @Override
   public Maneuver.Status tick(AnimalHandle handle, Waypoints move) {
      if (this.routeFailed(move)) {
         return Maneuver.Status.FAILED;
      }

      if (this.distance(handle) >= this.demanded) {
         return Maneuver.Status.SUCCEEDED;
      }

      if (this.out == null) {
         if (!this.cut(handle, move)) {
            return this.blocked(handle) ? Maneuver.Status.FAILED : Maneuver.Status.RUNNING;
         }

         this.unblocked();
      }

      if (move.idle()) {
         return Maneuver.Status.SUCCEEDED;
      } else {
         return this.stalled(Math.max(0.0, this.demanded - this.distance(handle))) ? Maneuver.Status.FAILED : Maneuver.Status.RUNNING;
      }
   }

   private boolean cut(AnimalHandle handle, Waypoints move) {
      Vec3 at = handle.bodyPosition();
      Vec3 away = Ground.bearing(handle, this.subject.estimate(), at);
      double climb = handle.turnRadius(handle.velocity().length());
      Vec3 point = Ground.legalise(handle, at.add(away.scale(this.demanded)).add(0.0, climb, 0.0), true);
      if (point == null) {
         return false;
      }

      if (!move.fly(List.of(at, point), this.speed)) {
         return false;
      }

      this.out = point;
      return true;
   }

   @Override
   public boolean premiseHolds(AnimalHandle handle) {
      return Ground.airborne(handle);
   }
}
