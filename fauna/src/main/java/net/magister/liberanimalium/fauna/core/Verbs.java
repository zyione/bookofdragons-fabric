package net.magister.liberanimalium.fauna.core;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.jetbrains.annotations.Nullable;
import net.magister.liberanimalium.fauna.api.AnimalHandle;
import net.magister.liberanimalium.fauna.api.MoveMode;
import net.magister.liberanimalium.fauna.api.Wings;
import net.magister.liberanimalium.fauna.behavior.MoveVerbs;
import net.magister.liberanimalium.fauna.maneuver.Medium;
import net.magister.liberanimalium.fauna.maneuver.Situation;
import net.magister.liberanimalium.fauna.move.MoveTuning;
import net.magister.liberanimalium.fauna.move.RouteFollower;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

final class Verbs implements MoveVerbs {
   private final AnimalHandle handle;
   private final RouteFollower route;
   private final FaunaMind mind;
   private float urgency = 1.0F;
   private Verbs.FleeLeg fleeLeg = Verbs.FleeLeg.NONE;
   private Map<MoveMode, Float> mediumBias = Map.of();
   private boolean running;
   private long landingMissTick = Long.MIN_VALUE;
   private long landingNearMissTick = Long.MIN_VALUE;
   private boolean lastFailureCertified;

   Verbs(FaunaMind mind, AnimalHandle handle, RouteFollower route) {
      this.mind = mind;
      this.handle = handle;
      this.route = route;
   }

   float urgency() {
      return this.urgency;
   }

   @Override
   public void goTo(BlockPos destination, float urgency) {
      this.fleeLeg = Verbs.FleeLeg.NONE;
      this.running = false;
      this.order(destination, urgency);
   }

   @Override
   public void bolt(BlockPos destination, float urgency) {
      this.fleeLeg = Verbs.FleeLeg.NONE;
      this.running = true;
      this.order(destination, urgency);
   }

   boolean order(BlockPos destination, float urgency) {
      return this.order(destination, urgency, true);
   }

   @Override
   public void cruise(BlockPos destination, float urgency) {
      this.fleeLeg = Verbs.FleeLeg.NONE;
      this.order(destination, urgency, false);
   }

   boolean order(BlockPos destination, float urgency, boolean hold) {
      BlockPos pinned = this.mind.forcedDestination();
      if (pinned != null) {
         destination = pinned;
      }

      if (!destination.equals(this.route.debugGoal())) {
         this.mind.approach().reset();
      }

      this.urgency = Math.max(0.0F, Math.min(1.0F, urgency));
      return this.route.requestRoute(this.handle, destination, this.handle.modeAt(Vec3.atCenterOf(destination)), this.mediumBias, this.mind.age(), hold);
   }

   @Override
   public void flee(List<Vec3> from, float urgency) {
      if (!from.isEmpty()) {
         Vec3 at = this.handle.bodyPosition();
         Vec3 mean = Vec3.ZERO;

         for (Vec3 point : from) {
            mean = mean.add(point);
         }

         mean = mean.scale(1.0 / from.size());
         Vec3 away = at.subtract(mean);
         if (!(away.lengthSqr() < 1.0E-6)) {
            boolean flying = this.handle.currentMode() == MoveMode.AIR;
            boolean aloft = flying || this.mind.electedAir();
            double reach = aloft ? 48.0 : 24.0;
            Vec3 target = at.add(away.normalize().scale(reach));
            BlockPos destination = aloft
               ? Wings.of(this.handle).landingSiteNear(BlockPos.containing(target), (int)this.handle.navRange())
               : this.handle.findStandableNear(BlockPos.containing(target));
            if (destination != null) {
               BlockPos band = aloft && !flying ? this.mind.liftIntoBand(destination) : null;
               if (band != null) {
                  this.running = true;
                  this.order(band, urgency, false);
               } else {
                  this.bolt(destination, urgency);
               }

               this.fleeLeg = aloft ? Verbs.FleeLeg.AIR : Verbs.FleeLeg.GROUND;
            }
         }
      }
   }

   @Override
   public void fleeing(UUID from, float fear) {
      this.mind.stateFlight(from, fear);
   }

   void mediumChanged(Medium.Verdict now) {
      if (this.fleeLeg == Verbs.FleeLeg.GROUND && now == Medium.Verdict.AIR && !this.route.idle()) {
         this.route.clear();
         this.fleeLeg = Verbs.FleeLeg.NONE;
      }
   }

   @Override
   public void engage(UUID id, double reach, double ring, float urgency) {
      this.act(id, Situation.FIGHT, reach, ring, urgency);
   }

   @Override
   public void chase(UUID id, double reach, double ring, float urgency) {
      this.act(id, Situation.COURT, reach, ring, urgency);
   }

   @Override
   public void stalk(UUID id, double reach, double ring, float urgency) {
      this.act(id, Situation.HUNT, reach, ring, urgency);
   }

   @Override
   public void display(UUID id, double reach, double ring, float urgency) {
      this.act(id, Situation.POSTURE, reach, ring, urgency);
   }

   @Override
   public void back(UUID id, double reach, double ring, float urgency) {
      this.act(id, Situation.SPACE, reach, ring, urgency);
   }

   @Override
   public void escort(UUID id, double reach, double ring, float urgency) {
      this.act(id, Situation.SOCIAL, reach, ring, urgency);
   }

   private void act(UUID id, Situation situation, double reach, double ring, float urgency) {
      this.mind.requestBeat(id, situation, reach, ring, Math.max(0.0F, Math.min(1.0F, urgency)));
   }

   void bias(Map<MoveMode, Float> bias) {
      this.mediumBias = bias;
   }

   boolean followRibbon(List<Vec3> vertices) {
      return this.route.followRibbon(this.handle, vertices);
   }

   void orbit(Vec3 centre, double radius, int hand) {
      this.route.orbit(centre, radius, hand);
   }

   void pace(float urgency) {
      this.urgency = Math.max(0.0F, Math.min(1.0F, urgency));
   }

   void gait(boolean value) {
      this.running = value;
   }

   boolean running() {
      return this.running;
   }

   @Override
   public void alight(float urgency) {
      if (this.handle.currentMode() != MoveMode.GROUND) {
         boolean flying = this.handle.currentMode() == MoveMode.AIR;
         if (flying) {
            BlockPos descent = this.descentLeg();
            if (descent != null) {
               this.cruise(descent, urgency);
               return;
            }
         }

         BlockPos site = this.landingHere();
         if (site != null) {
            this.goTo(site, urgency);
         } else {
            if (flying) {
               BlockPos on = this.bandAhead(this.handle.navRange() * 0.5);
               if (on != null) {
                  this.cruise(on, urgency);
               }
            }
         }
      }
   }

   @Nullable
   private BlockPos descentLeg() {
      double width = this.handle.bounds().getXsize();
      Vec3 at = this.handle.bodyPosition();
      double over = at.y - this.surfaceUnder(at.x, at.z);
      int ceiling = MoveTuning.cruiseCeiling(width);
      if (over <= ceiling) {
         return null;
      }

      double drop = over - (MoveTuning.skyClearance(width) + ceiling) / 2.0;
      return this.bandAhead(Math.max(this.handle.navRange() * 0.5, drop * 1.25));
   }

   @Nullable
   private BlockPos bandAhead(double reach) {
      double width = this.handle.bounds().getXsize();
      Vec3 aim = this.handle.bodyPosition().add(this.travelHeading().scale(reach));
      int floor = MoveTuning.skyClearance(width);
      int ceiling = MoveTuning.cruiseCeiling(width);
      BlockPos aloft = new BlockPos(
         Mth.floor(aim.x), this.surfaceUnder(aim.x, aim.z) + (floor + ceiling) / 2, Mth.floor(aim.z)
      );
      return this.handle.isBlockedAt(Vec3.atCenterOf(aloft)) ? null : aloft;
   }

   private int surfaceUnder(double x, double z) {
      return Mth.floor(Wings.of(this.handle).landHeightUnder(new Vec3(x, this.handle.bodyPosition().y, z)));
   }

   @Nullable
   BlockPos landingHere() {
      long now = this.handle.gameTime();
      if (this.landingMissTick != Long.MIN_VALUE && now - this.landingMissTick < 10L) {
         return null;
      }

      BlockPos site = Wings.of(this.handle).landingSiteNear(BlockPos.containing(this.handle.bodyPosition()), (int)this.handle.navRange());
      if (site != null) {
         return site;
      }

      Vec3 ahead = this.handle.bodyPosition().add(this.travelHeading().scale(this.handle.navRange()));
      site = Wings.of(this.handle).landingSiteNear(BlockPos.containing(ahead), (int)this.handle.navRange());
      if (site == null) {
         this.landingMissTick = now;
      }

      return site;
   }

   @Nullable
   BlockPos landingNear(Vec3 point) {
      long now = this.handle.gameTime();
      boolean missHeld = this.landingNearMissTick != Long.MIN_VALUE && now - this.landingNearMissTick < 10L;
      if (!missHeld) {
         BlockPos site = Wings.of(this.handle).landingSiteNear(BlockPos.containing(point), (int)this.handle.navRange());
         if (site != null) {
            return site;
         }

         this.landingNearMissTick = now;
      }

      return this.landingHere();
   }

   private Vec3 travelHeading() {
      Vec3 velocity = this.handle.velocity();
      double flat = Math.sqrt(velocity.x * velocity.x + velocity.z * velocity.z);
      if (flat > 0.001) {
         return new Vec3(velocity.x / flat, 0.0, velocity.z / flat);
      }

      Vec3 look = this.handle.lookDirection();
      double facing = Math.sqrt(look.x * look.x + look.z * look.z);
      return facing > 0.001 ? new Vec3(look.x / facing, 0.0, look.z / facing) : new Vec3(0.0, 0.0, 1.0);
   }

   @Override
   public void standBy(UUID id, double radius) {
      if (this.handle.currentMode() != MoveMode.AIR) {
         this.stop();
      } else {
         Vec3 centre = this.mind.beliefAbout(id);
         if (centre == null) {
            this.alight(1.0F);
         } else if (Wings.of(this.handle).airGapBelow(centre) <= 4.0) {
            this.alight(1.0F);
         } else {
            this.route.hover(this.handle.bodyPosition());
         }
      }
   }

   @Override
   public void unstick() {
      this.route.clear();
   }

   @Override
   public void changeMode() {
      this.handle.requestMoveMode(this.alternateMode());
   }

   private MoveMode alternateMode() {
      return switch (this.handle.currentMode()) {
         case GROUND, WATER -> MoveMode.AIR;
         default -> MoveMode.GROUND;
      };
   }

   @Override
   public void stop() {
      this.mind.approach().reset();
      if (!this.route.landing(this.handle)) {
         this.route.clear();
      }

      this.running = false;
      this.urgency = 1.0F;
      this.fleeLeg = Verbs.FleeLeg.NONE;
   }

   @Override
   public boolean idle() {
      return this.route.idle();
   }

   @Override
   public boolean failed() {
      RouteFollower.Failure kind = this.route.takeFailure();
      this.lastFailureCertified = kind == RouteFollower.Failure.CERTIFIED;
      return kind != null;
   }

   boolean lastFailureCertified() {
      return this.lastFailureCertified;
   }

   @Override
   public double remaining() {
      Vec3 at = this.handle.bodyPosition();
      double distance = this.route.distanceToEnd(at);
      return this.route.hasRoute() ? distance : Double.NaN;
   }

   @Override
   public boolean doorAhead() {
      if (this.route.doorAhead()) {
         return true;
      }

      AABB box = this.handle.bounds();
      int y = Mth.floor(box.minY + 0.001);
      int x1 = Mth.floor(box.maxX - 0.001);
      int z1 = Mth.floor(box.maxZ - 0.001);

      for (int x = Mth.floor(box.minX); x <= x1; x++) {
         for (int z = Mth.floor(box.minZ); z <= z1; z++) {
            if (this.handle.passDoor(new BlockPos(x, y, z)) != null) {
               return true;
            }
         }
      }

      return false;
   }

   private enum FleeLeg {
      NONE,
      GROUND,
      AIR;
   }
}
