package net.magister.liberanimalium.fauna.maneuver.pattern;

import java.util.List;
import org.jetbrains.annotations.Nullable;
import net.magister.liberanimalium.fauna.api.AnimalHandle;
import net.magister.liberanimalium.fauna.maneuver.Ground;
import net.magister.liberanimalium.fauna.maneuver.Maneuver;
import net.magister.liberanimalium.fauna.maneuver.Subject;
import net.magister.liberanimalium.fauna.maneuver.Waypoints;
import net.minecraft.world.phys.Vec3;

public final class StoopManeuver extends TargetedManeuver {
   private final double ring;
   private final double standoffHeight;
   @Nullable
   private Vec3 exit;
   @Nullable
   private Vec3 approachDir;

   public StoopManeuver(Subject subject, double min, double max, float speed, double ring, double standoffHeight) {
      super(subject, min, max, speed);
      this.ring = ring;
      this.standoffHeight = standoffHeight;
   }

   @Override
   public String phase() {
      return this.exit == null ? "cut" : "pass";
   }

   @Override
   public boolean running(AnimalHandle handle) {
      return true;
   }

   @Override
   public void begin(AnimalHandle handle, Waypoints move) {
      this.budgetFor(Math.max(this.distance(handle), this.min) + this.ring);
      this.cut(handle, move);
   }

   @Override
   public Maneuver.Status tick(AnimalHandle handle, Waypoints move) {
      if (this.routeFailed(move)) {
         return Maneuver.Status.FAILED;
      } else if (this.exit != null && move.idle()) {
         return Maneuver.Status.SUCCEEDED;
      } else if (this.exit == null && !this.cut(handle, move)) {
         return Maneuver.Status.FAILED;
      } else {
         return this.stalled(this.distance(handle)) ? Maneuver.Status.FAILED : Maneuver.Status.RUNNING;
      }
   }

   private boolean cut(AnimalHandle handle, Waypoints move) {
      Vec3 at = handle.bodyPosition();
      double gap = this.distance(handle);
      double lead = Math.min(20.0, gap * 3.0);
      Vec3 ahead = this.subject.estimate().add(this.subject.velocity().scale(lead));
      Vec3 line = ahead.subtract(at);
      if (line.lengthSqr() < 1.0E-6) {
         return false;
      }

      Vec3 dir = line.normalize();
      double radius = Math.max(this.ring, handle.turnRadius(handle.velocity().length()));
      Vec3 flat = new Vec3(dir.x, 0.0, dir.z);
      Vec3 out = flat.lengthSqr() < 1.0E-6 ? new Vec3(0.0, 0.0, 1.0) : flat.normalize();
      double up = Math.max(radius, this.standoffHeight);

      for (double lift = 0.0; lift <= this.standoffHeight; lift += 4.0) {
         Vec3 through = Ground.legalise(handle, ahead.add(dir.scale(this.ring)).add(0.0, lift, 0.0), true);
         if (through != null) {
            Vec3 climb = Ground.legalise(handle, through.add(out.scale(radius)).add(0.0, up, 0.0), true);
            if (climb != null && move.fly(List.of(at, through, climb), this.speed)) {
               this.approachDir = dir;
               this.exit = climb;
               return true;
            }
         }
      }

      return false;
   }

   @Override
   public boolean premiseHolds(AnimalHandle handle) {
      return Ground.airborne(handle);
   }
}
