package net.magister.liberanimalium.fauna.move;

import java.util.List;
import org.jetbrains.annotations.Nullable;

public record NavPath(List<NavPath.Step> steps, NavPath.Outcome outcome, double reach) {
   public NavPath(List<NavPath.Step> steps, NavPath.Outcome outcome) {
      this(steps, outcome, Double.NaN);
   }

   public boolean reachesGoal() {
      return this.outcome == NavPath.Outcome.REACHED;
   }

   public boolean worthContinuing() {
      return this.outcome != NavPath.Outcome.REACHED && this.outcome != NavPath.Outcome.EXHAUSTED;
   }

   public boolean isEmpty() {
      return this.steps.isEmpty();
   }

   public int size() {
      return this.steps.size();
   }

   public NavPath.Step step(int index) {
      return this.steps.get(index);
   }

   public NavNode first() {
      return this.steps.get(0).node();
   }

   public NavNode last() {
      return this.steps.get(this.steps.size() - 1).node();
   }

   public double span() {
      if (this.steps.size() < 2) {
         return 0.0;
      }

      NavNode a = this.first();
      NavNode b = this.last();
      double dx = b.x() - a.x();
      double dy = b.y() - a.y();
      double dz = b.z() - a.z();
      return Math.sqrt(dx * dx + dy * dy + dz * dz);
   }

   public enum Outcome {
      REACHED,
      HORIZON,
      NODE_CAP,
      WALKED_CAP,
      EXHAUSTED;
   }

   public record Step(NavNode node, @Nullable ModeTransition transitionBefore) {
   }
}
