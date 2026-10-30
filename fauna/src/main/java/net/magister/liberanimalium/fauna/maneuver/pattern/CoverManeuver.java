package net.magister.liberanimalium.fauna.maneuver.pattern;

import java.util.List;
import org.jetbrains.annotations.Nullable;
import net.magister.liberanimalium.fauna.api.AnimalHandle;
import net.magister.liberanimalium.fauna.maneuver.Ground;
import net.magister.liberanimalium.fauna.maneuver.Maneuver;
import net.magister.liberanimalium.fauna.maneuver.MoveStyles;
import net.magister.liberanimalium.fauna.maneuver.Subject;
import net.magister.liberanimalium.fauna.maneuver.Waypoints;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

public final class CoverManeuver extends TargetedManeuver {
   @Nullable
   private Vec3 vantage;
   @Nullable
   private Vec3 chosenAgainst;
   private boolean hidden;
   private final double springMargin;

   public CoverManeuver(Subject subject, double min, double max, float speed) {
      this(subject, min, max, speed, 3.0);
   }

   public CoverManeuver(Subject subject, double min, double max, float speed, double springMargin) {
      super(subject, min, max, speed);
      this.springMargin = springMargin;
   }

   @Override
   public String phase() {
      return this.hidden ? "hidden" : "exposed";
   }

   @Override
   public void begin(AnimalHandle handle, Waypoints move) {
      this.budgetFor(this.vantage(handle));
   }

   @Override
   public Maneuver.Status tick(AnimalHandle handle, Waypoints move) {
      if (this.routeFailed(move)) {
         return Maneuver.Status.FAILED;
      }

      Vec3 aim = this.aim(handle);
      boolean drifted = this.chosenAgainst == null || Ground.flatDistSq(this.chosenAgainst, aim) > 9.0;
      if (this.vantage == null || drifted) {
         Vec3 taken = this.take(handle, aim);
         if (taken == null) {
            return this.blocked(handle) ? Maneuver.Status.FAILED : Maneuver.Status.RUNNING;
         }

         this.unblocked();
         this.vantage = taken;
         this.chosenAgainst = aim;
         this.releaseSteering();
         this.budgetFor(Math.sqrt(Ground.flatDistSq(handle.bodyPosition(), taken)));
      }

      if (this.at(handle, this.vantage)) {
         return Maneuver.Status.SUCCEEDED;
      }

      this.steer(move, this.vantage, this.speed);
      return this.stalled(Math.sqrt(Ground.flatDistSq(handle.bodyPosition(), this.vantage))) ? Maneuver.Status.FAILED : Maneuver.Status.RUNNING;
   }

   @Nullable
   private Vec3 take(AnimalHandle handle, Vec3 aim) {
      double radius = Mth.clamp(this.distance(handle), this.min, this.vantage(handle));
      double phase = Math.atan2(handle.bodyPosition().z - aim.z, handle.bodyPosition().x - aim.x);
      List<Vec3> ring = Ground.ring(aim, radius, 8, phase);
      Vec3 fallback = null;
      double fallbackAttention = Double.MAX_VALUE;
      Vec3 eye = this.subject.estimate().add(0.0, this.subject.eyeHeight(), 0.0);

      for (Vec3 raw : ring) {
         Vec3 point = Ground.legalise(handle, raw, Ground.airborne(handle));
         if (point != null) {
            Vec3 from = point.add(0.0, handle.bounds().getYsize() * 0.5, 0.0);
            if (!handle.lineOfSight(from, eye)) {
               this.hidden = true;
               return point;
            }

            double attention = this.subject.attentionOn(point);
            if (attention < fallbackAttention) {
               fallbackAttention = attention;
               fallback = point;
            }
         }
      }

      this.hidden = false;
      return fallback;
   }

   private double vantage(AnimalHandle handle) {
      return MoveStyles.springRange(handle, this.springMargin) * 2.0;
   }

   @Override
   public boolean premiseHolds(AnimalHandle handle) {
      return !MoveStyles.withinSpring(handle, this.subject, this.springMargin);
   }
}
