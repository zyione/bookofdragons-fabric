package net.magister.liberanimalium.fauna.maneuver.pattern;

import java.util.List;
import org.jetbrains.annotations.Nullable;
import net.magister.liberanimalium.fauna.api.AnimalHandle;
import net.magister.liberanimalium.fauna.maneuver.Ground;
import net.magister.liberanimalium.fauna.maneuver.Maneuver;
import net.magister.liberanimalium.fauna.maneuver.Subject;
import net.magister.liberanimalium.fauna.maneuver.Waypoints;
import net.minecraft.world.phys.Vec3;

public final class WithdrawManeuver extends TargetedManeuver {
   private final double demanded;
   private final double hopMax;
   @Nullable
   private Vec3 hop;

   public WithdrawManeuver(Subject subject, double demanded, float speed) {
      this(subject, demanded, speed, 16.0);
   }

   public WithdrawManeuver(Subject subject, double demanded, float speed, double hopMax) {
      super(subject, demanded, demanded, speed);
      this.demanded = demanded;
      this.hopMax = hopMax;
   }

   public double demanded() {
      return this.demanded;
   }

   public double hopMax() {
      return this.hopMax;
   }

   @Override
   public void begin(AnimalHandle handle, Waypoints move) {
      this.budgetFor(Math.min(this.demanded, this.hopMax));
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

      double d = this.distance(handle);
      if (d >= this.demanded + 1.5 && this.ticks >= 10) {
         return Maneuver.Status.SUCCEEDED;
      }

      if (this.hop == null || this.at(handle, this.hop)) {
         Vec3 next = this.fanAway(handle, d);
         if (next == null) {
            return this.blocked(handle) ? Maneuver.Status.FAILED : Maneuver.Status.RUNNING;
         }

         this.unblocked();
         this.hop = next;
         this.releaseSteering();
         this.budgetFor(Math.sqrt(Ground.flatDistSq(handle.bodyPosition(), next)));
      }

      this.steer(move, this.hop, this.speed);
      return this.stalled(Math.sqrt(Ground.flatDistSq(handle.bodyPosition(), this.hop))) ? Maneuver.Status.FAILED : Maneuver.Status.RUNNING;
   }

   @Nullable
   private Vec3 fanAway(AnimalHandle handle, double d) {
      Vec3 away = Ground.bearing(handle, this.subject.estimate(), handle.bodyPosition());
      double hopLength = Math.min(this.hopMax, Math.max(this.demanded - d, 1.5));
      List<Vec3> fan = Ground.fan(handle.bodyPosition(), away, hopLength, 4, 30.0);
      return Ground.bestFit(handle, fan, Ground.airborne(handle), List.of(this.subject.estimate()));
   }
}
