package net.magister.liberanimalium.fauna.core;

import java.util.List;
import net.magister.liberanimalium.fauna.maneuver.Waypoints;
import net.magister.liberanimalium.fauna.move.RouteFollower;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.Vec3;

final class Legs implements Waypoints {
   private final Verbs verbs;
   private final RouteFollower route;
   private boolean failedThisTick;

   Legs(Verbs verbs, RouteFollower route) {
      this.verbs = verbs;
      this.route = route;
   }

   void noteFailure(boolean failed) {
      this.failedThisTick = failed;
   }

   @Override
   public boolean goTo(BlockPos destination, float urgency) {
      return this.verbs.order(destination, urgency, false);
   }

   @Override
   public void pace(float urgency) {
      this.verbs.pace(urgency);
   }

   @Override
   public void gait(boolean running) {
      this.verbs.gait(running);
   }

   @Override
   public boolean fly(List<Vec3> vertices, float urgency) {
      this.verbs.pace(urgency);
      return this.verbs.followRibbon(vertices);
   }

   @Override
   public void loiter(Vec3 centre, double radius, int hand, float urgency) {
      this.verbs.pace(urgency);
      this.verbs.orbit(centre, radius, hand);
   }

   @Override
   public void stop() {
      this.verbs.stop();
   }

   @Override
   public boolean idle() {
      return this.route.idle();
   }

   @Override
   public boolean failed() {
      boolean was = this.failedThisTick;
      this.failedThisTick = false;
      return was;
   }

   @Override
   public boolean solving() {
      return this.route.solving();
   }
}
