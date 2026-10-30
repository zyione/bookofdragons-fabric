package net.magister.liberanimalium.fauna.maneuver.pattern;

import net.magister.liberanimalium.fauna.api.AnimalHandle;
import net.magister.liberanimalium.fauna.maneuver.Ground;
import net.magister.liberanimalium.fauna.maneuver.Maneuver;
import net.magister.liberanimalium.fauna.maneuver.Subject;
import net.magister.liberanimalium.fauna.maneuver.Waypoints;
import net.minecraft.world.phys.Vec3;

public abstract class TargetedManeuver extends Maneuver {
   protected Subject subject;
   protected final double min;
   protected final double max;
   protected final float speed;
   private double best = Double.MAX_VALUE;
   private int allowance;

   public final double reach() {
      return this.min;
   }

   public float pace() {
      return this.speed;
   }

   protected TargetedManeuver(Subject subject, double min, double max, float speed) {
      this.subject = subject;
      this.min = min;
      this.max = max;
      this.speed = speed;
   }

   public Subject subject() {
      return this.subject;
   }

   public final void refresh(Subject fresh) {
      if (fresh.id().equals(this.subject.id())) {
         this.subject = fresh;
      }
   }

   protected double distance(AnimalHandle handle) {
      return Math.sqrt(Ground.flatDistSq(handle.bodyPosition(), this.subject.estimate()));
   }

   protected Vec3 aim(AnimalHandle handle) {
      return this.subject.flatEstimate(handle.bodyPosition().y);
   }

   protected void budgetFor(double legBlocks) {
      this.best = Double.MAX_VALUE;
      this.allowance = Math.max(10, (int)Math.ceil(legBlocks * 6.0));
   }

   protected boolean stalled(double distanceToGoal) {
      if (distanceToGoal < this.best - 0.35) {
         this.best = distanceToGoal;
         return false;
      } else {
         return --this.allowance <= 0;
      }
   }

   protected boolean at(AnimalHandle handle, Vec3 point) {
      return Ground.flatDistSq(handle.bodyPosition(), point) <= Ground.arrivalTolerance(handle) * Ground.arrivalTolerance(handle);
   }

   protected boolean routeFailed(Waypoints move) {
      return move.failed();
   }
}
