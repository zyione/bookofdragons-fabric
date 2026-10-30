package net.magister.liberanimalium.fauna.maneuver.pattern;

import net.magister.liberanimalium.fauna.api.AnimalHandle;
import net.magister.liberanimalium.fauna.maneuver.Maneuver;
import net.magister.liberanimalium.fauna.maneuver.MoveStyles;
import net.magister.liberanimalium.fauna.maneuver.Subject;
import net.magister.liberanimalium.fauna.maneuver.Waypoints;

public final class StillManeuver extends TargetedManeuver {
   private final int dwell;
   private final boolean concealed;
   private final double springMargin;

   public StillManeuver(Subject subject, int dwell, boolean concealed) {
      this(subject, dwell, concealed, 3.0);
   }

   public StillManeuver(Subject subject, int dwell, boolean concealed, double springMargin) {
      super(subject, 0.0, 0.0, 0.0F);
      this.dwell = Math.max(7, dwell);
      this.concealed = concealed;
      this.springMargin = springMargin;
   }

   @Override
   public String name() {
      return this.concealed ? "FREEZE" : "HOLD";
   }

   @Override
   public void begin(AnimalHandle handle, Waypoints move) {
      move.stop();
      this.releaseSteering();
   }

   @Override
   public Maneuver.Status tick(AnimalHandle handle, Waypoints move) {
      this.routeFailed(move);
      move.stop();
      return this.ticks >= this.dwell ? Maneuver.Status.SUCCEEDED : Maneuver.Status.RUNNING;
   }

   @Override
   public boolean premiseHolds(AnimalHandle handle) {
      return !this.concealed || !MoveStyles.withinSpring(handle, this.subject, this.springMargin);
   }
}
