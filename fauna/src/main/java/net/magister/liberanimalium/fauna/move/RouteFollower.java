package net.magister.liberanimalium.fauna.move;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.function.Consumer;
import java.util.function.UnaryOperator;
import org.jetbrains.annotations.Nullable;
import net.magister.liberanimalium.fauna.api.AnimalHandle;
import net.magister.liberanimalium.fauna.api.MoveMode;
import net.magister.liberanimalium.util.NavStats;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

public final class RouteFollower {
   @Nullable
   private NavPath path;
   private int index;
   @Nullable
   private BlockPos goal;
   private MoveMode goalMode = MoveMode.GROUND;
   private volatile long pendingGeneration;
   private long requestedAt;
   private boolean arrived;
   @Nullable
   private Vec3 stuckAnchor;
   private long stuckSince;
   private int segments;
   private int landingAttempts;
   private int replans;
   private boolean invalidated;
   private int groundBreakTicks;
   private boolean resubmitted;
   private NavSolver.DangerField danger = NavSolver.DangerField.NONE;
   @Nullable
   private String lastFailure;
   @Nullable
   private BlockPos deckGoal;
   private UnaryOperator<Vec3> frame = UnaryOperator.identity();
   @Nullable
   private List<Vec3> poly;
   private Ribbon.Cursor cursor = Ribbon.Cursor.START;
   private double[] cuts = new double[0];
   @Nullable
   private Vec3 orbitCentre;
   private double orbitRadius;
   private int orbitHand = 1;
   private Map<MoveMode, Float> mediumBias = Map.of();
   @Nullable
   private Vec3 hoverPoint;
   private boolean holdAtEnd;
   private boolean searched;
   @Nullable
   private RouteFollower.Failure failureKind;
   @Nullable
   private Consumer<RouteFollower.Reach> reachSink;
   @Nullable
   private Consumer<BlockPos> unreachableSink;
   @Nullable
   private NavPath doorwayPath;
   private int doorwayStep;
   @Nullable
   private Vec3 doorwayPoint;

   public void rememberDangerVia(NavSolver.DangerField field) {
      this.danger = field == null ? NavSolver.DangerField.NONE : field;
   }

   private Vec3 at(NavNode node) {
      Vec3 centre = node.center();
      if (this.deckGoal == null) {
         return centre;
      }

      Vec3 now = this.frame.apply(centre);
      return now == null ? centre : now;
   }

   private Map<MoveMode, Float> biasedModeCost(AnimalHandle handle) {
      Map<MoveMode, Float> base = handle.effectiveModeCost();
      if (this.mediumBias.isEmpty()) {
         return base;
      }

      EnumMap<MoveMode, Float> out = new EnumMap<>(MoveMode.class);

      for (Entry<MoveMode, Float> entry : base.entrySet()) {
         float bias = this.mediumBias.getOrDefault(entry.getKey(), 1.0F);
         out.put(entry.getKey(), entry.getValue() * bias);
      }

      return out;
   }

   @Nullable
   public Vec3 currentNodePoint() {
      return this.path != null && this.index < this.path.size() ? this.at(this.path.step(this.index).node()) : null;
   }

   @Nullable
   public Vec3 pursuitTarget(AnimalHandle handle) {
      if (this.path != null && this.index < this.path.size()) {
         if (handle.currentMode() == MoveMode.AIR && this.poly != null && this.poly.size() >= 2) {
            MoveMode wanted = this.targetMode();
            if (this.pendingTransition() != null && wanted != null && wanted != handle.currentMode()) {
               Vec3 site = this.currentNodePoint();
               if (site != null) {
                  return site;
               }
            }

            return this.flyable(handle, Ribbon.reference(this.poly, this.cursor, this.lookahead(handle), v -> this.cutAt(handle, v)));
         } else {
            double width = handle.bounds().getXsize();
            double lookahead = MoveTuning.lookahead(width);
            double clearance = MoveTuning.cornerClearance(width);
            Vec3 at = handle.bodyPosition();
            MoveMode mode = this.path.step(this.index).node().mode();
            Vec3 doorway = this.alongDoorway(at, lookahead, width);
            if (doorway != null) {
               return doorway;
            }

            Vec3 nextNode = this.at(this.path.step(this.index).node());
            if (at.distanceTo(nextNode) > clearance + lookahead) {
               return nextNode;
            }

            double remaining = lookahead;
            Vec3 from = at;

            for (int i = this.index; i < this.path.size(); i++) {
               NavPath.Step step = this.path.step(i);
               if (i > this.index && (step.transitionBefore() != null || step.node().mode() != mode)) {
                  return from;
               }

               Vec3 corner = this.at(step.node());
               double leg = from.distanceTo(corner);
               if (!(leg < 1.0E-6)) {
                  if (leg >= remaining) {
                     return from.add(corner.subtract(from).scale(remaining / leg));
                  }

                  if (i + 1 < this.path.size()) {
                     NavPath.Step next = this.path.step(i + 1);
                     boolean sameRun = next.transitionBefore() == null && next.node().mode() == mode;
                     if (sameRun) {
                        Vec3 inDir = corner.subtract(from);
                        Vec3 outDir = this.at(next.node()).subtract(corner);
                        if (inDir.lengthSqr() > 1.0E-6 && outDir.lengthSqr() > 1.0E-6) {
                           Vec3 inH = new Vec3(inDir.x, 0.0, inDir.z).normalize();
                           Vec3 outH = new Vec3(outDir.x, 0.0, outDir.z).normalize();
                           double dot = inH.dot(outH);
                           Vec3 fromRay = at.add(0.0, 0.5, 0.0);
                           Vec3 cornerRay = this.at(next.node()).add(0.0, 0.5, 0.0);
                           if (dot < 0.35 || dot < 0.85 && !handle.lineOfSight(fromRay, cornerRay)) {
                              return corner;
                           }
                        }

                        Vec3 out = this.at(next.node()).subtract(corner);
                        if (out.lengthSqr() > 1.0E-9) {
                           Vec3 candidate = corner.add(out.normalize().scale(remaining - leg));
                           Vec3 fromRay = at.add(0.0, 0.5, 0.0);
                           Vec3 toRay = candidate.add(0.0, 0.5, 0.0);
                           if (offsetFromChord(corner, at, candidate) > clearance || handle.isBlockedAt(candidate) || !handle.lineOfSight(fromRay, toRay)) {
                              return corner;
                           }
                        }
                     }
                  }

                  remaining -= leg;
                  from = corner;
               }
            }

            return from;
         }
      } else {
         return null;
      }
   }

   private double lookahead(AnimalHandle handle) {
      double floor = MoveTuning.lookahead(handle.bounds().getXsize());
      double speed = handle.velocity().length();
      double turn = handle.turnRateDegrees(speed);
      return turn <= 0.0 ? floor : Math.max(floor, speed * 90.0 / turn);
   }

   private double cutAt(AnimalHandle handle, int vertex) {
      if (this.path != null && this.poly != null) {
         if (vertex <= 0 || vertex >= this.path.size() - 1) {
            return 0.0;
         }

         if (this.path.step(vertex).transitionBefore() != null) {
            return 0.0;
         }

         if (this.path.step(vertex + 1).transitionBefore() != null) {
            return 0.0;
         }

         if (vertex < this.cuts.length && !Double.isNaN(this.cuts[vertex])) {
            return this.cuts[vertex];
         }

         Vec3 a = this.poly.get(vertex - 1);
         Vec3 b = this.poly.get(vertex);
         Vec3 c = this.poly.get(vertex + 1);
         double speed = handle.velocity().length();
         double anticipation = Ribbon.anticipation(Ribbon.radius(speed, handle.turnRateDegrees(speed)), Ribbon.turnAngle(a, b, c));
         if (Double.isFinite(anticipation) && !(anticipation <= 0.0)) {
            double share = 0.5;
            double certified = Math.min(anticipation, Math.min(a.distanceTo(b), b.distanceTo(c)) * share);

            for (Vec3 sample : Ribbon.arcSamples(a, b, c, certified)) {
               if (handle.isBlockedAt(sample)) {
                  return 0.0;
               }
            }

            return Double.POSITIVE_INFINITY;
         } else {
            return 0.0;
         }
      } else {
         return 0.0;
      }
   }

   private void freezeEnteredCorner(AnimalHandle handle) {
      if (this.path != null && this.poly != null) {
         int vertex = this.cursor.segment() + 1;
         if (vertex > 0 && vertex < this.cuts.length) {
            if (Double.isNaN(this.cuts[vertex])) {
               double anticipation = this.cutAt(handle, vertex);
               double remaining = Ribbon.at(this.poly, this.cursor).distanceTo(this.poly.get(vertex));
               if (remaining <= anticipation) {
                  this.cuts[vertex] = anticipation;
               }
            }
         }
      }
   }

   private void advanceAlongRibbon(AnimalHandle handle) {
      if (this.path != null && this.poly != null && this.poly.size() >= 2) {
         Ribbon.Cursor next = Ribbon.project(this.poly, handle.bodyPosition(), this.cursor);
         int limit = this.cursorLimit(handle);
         if (next.segment() > limit) {
            next = new Ribbon.Cursor(limit, 1.0);
         }

         this.cursor = next;
         this.freezeEnteredCorner(handle);
         int wanted = Math.min(this.cursor.segment() + 1, this.path.size() - 1);
         int settling = this.nextTransitionAfter(this.index);
         if (settling >= 0) {
            this.index = Math.min(Math.max(this.index, wanted), settling);
            if (this.index == settling - 1
               && (this.arrivedAt(handle, this.at(this.path.step(this.index).node())) || this.overColumnOf(handle, this.at(this.path.step(settling).node())))) {
               this.index = settling;
            }
         } else {
            if (wanted > this.index) {
               this.index = wanted;
            }

            boolean past = this.cursor.segment() >= this.poly.size() - 2 && this.cursor.t() >= 0.999;
            if (this.arrivedAt(handle, this.at(this.path.last())) || past) {
               this.index = this.path.size();
            }
         }
      }
   }

   private int nextTransitionAfter(int from) {
      if (this.path == null) {
         return -1;
      }

      for (int i = from + 1; i < this.path.size(); i++) {
         if (this.path.step(i).transitionBefore() != null) {
            return i;
         }
      }

      return -1;
   }

   private int cursorLimit(AnimalHandle handle) {
      int last = this.path.size() - 2;

      for (int i = Math.max(1, this.index); i < this.path.size(); i++) {
         if (this.path.step(i).transitionBefore() != null && (i != this.index || handle.currentMode() != this.path.step(i).node().mode())) {
            return Math.max(0, Math.min(last, i - 1));
         }
      }

      return Math.max(0, last);
   }

   private void projectRibbon() {
      List<Vec3> points = new ArrayList<>(this.path.size());

      for (NavPath.Step step : this.path.steps()) {
         points.add(this.at(step.node()));
      }

      this.poly = List.copyOf(points);
   }

   private void buildRibbon() {
      if (this.path == null) {
         this.poly = null;
         this.cuts = new double[0];
      } else {
         List<Vec3> points = new ArrayList<>(this.path.size());

         for (NavPath.Step step : this.path.steps()) {
            points.add(this.at(step.node()));
         }

         this.poly = List.copyOf(points);
         this.cuts = new double[this.path.size()];
         Arrays.fill(this.cuts, Double.NaN);
         this.cursor = Ribbon.Cursor.START;
      }
   }

   public void orbit(Vec3 centre, double radius, int hand) {
      this.clear();
      this.orbitCentre = centre;
      this.orbitRadius = Math.max(1.0, radius);
      this.orbitHand = hand < 0 ? -1 : 1;
   }

   public boolean orbiting() {
      return this.orbitCentre != null;
   }

   public void hover(Vec3 point) {
      if (!this.hovering()) {
         this.clear();
         this.hoverPoint = point;
      }
   }

   public boolean hovering() {
      return this.hoverPoint != null;
   }

   @Nullable
   public Vec3 hoverPoint() {
      return this.hoverPoint;
   }

   private Vec3 hoverTarget(AnimalHandle handle) {
      Vec3 at = handle.bodyPosition();
      return this.arrivedAt(handle, this.hoverPoint) ? new Vec3(at.x, this.hoverPoint.y, at.z) : this.hoverPoint;
   }

   @Nullable
   public Vec3 orbitCentre() {
      return this.orbitCentre;
   }

   public double orbitRadius() {
      return this.orbitRadius;
   }

   public int orbitHand() {
      return this.orbitHand;
   }

   @Nullable
   private Vec3 orbitTarget(AnimalHandle handle) {
      if (this.orbitCentre == null) {
         return null;
      } else {
         Vec3 at = handle.bodyPosition();
         double dx = at.x - this.orbitCentre.x;
         double dz = at.z - this.orbitCentre.z;
         double bearing = dx * dx + dz * dz < 1.0E-6 ? 0.0 : Math.atan2(dx, dz);
         double ahead = bearing + this.orbitHand * (this.lookahead(handle) / this.orbitRadius);
         Vec3 point = new Vec3(
            this.orbitCentre.x + Math.sin(ahead) * this.orbitRadius,
            this.orbitCentre.y,
            this.orbitCentre.z + Math.cos(ahead) * this.orbitRadius
         );
         if (handle.isBlockedAt(point)) {
            this.orbitCentre = null;
            this.lastFailure = "nowhere to circle";
            this.failureKind = RouteFollower.Failure.TACTICAL;
            return null;
         } else {
            return point;
         }
      }
   }

   private static Vec3 closestPointOnSegment(Vec3 p, Vec3 a, Vec3 b) {
      Vec3 ab = b.subtract(a);
      double lengthSq = ab.lengthSqr();
      if (lengthSq < 1.0E-9) {
         return a;
      }

      double t = Mth.clamp(p.subtract(a).dot(ab) / lengthSq, 0.0, 1.0);
      return a.add(ab.scale(t));
   }

   private static double offsetFromChord(Vec3 p, Vec3 a, Vec3 b) {
      return p.distanceTo(closestPointOnSegment(p, a, b));
   }

   @Nullable
   public MoveMode targetMode() {
      return this.path != null && this.index < this.path.size() ? this.path.step(this.index).node().mode() : null;
   }

   @Nullable
   public ModeTransition pendingTransition() {
      return this.path != null && this.index < this.path.size() ? this.path.step(this.index).transitionBefore() : null;
   }

   public boolean hasRoute() {
      return this.path != null && this.index < this.path.size();
   }

   public boolean planned() {
      return this.searched && this.hasRoute();
   }

   public boolean solving() {
      return this.pendingGeneration != 0L;
   }

   @Nullable
   public String lastFailure() {
      return this.lastFailure;
   }

   @Nullable
   public RouteFollower.Failure takeFailure() {
      RouteFollower.Failure out = this.failureKind;
      this.failureKind = null;
      return out;
   }

   public String progress() {
      if (this.path == null) {
         return this.solving() ? "solving" : "idle";
      }

      String state = this.index + "/" + this.path.size();
      return this.path.reachesGoal() ? state : state + " ->" + this.path.outcome().name().toLowerCase() + (this.segments > 1 ? " seg" + this.segments : "");
   }

   @Nullable
   public NavPath.Outcome outcome() {
      return this.path == null ? null : this.path.outcome();
   }

   public int segments() {
      return this.segments;
   }

   public boolean requestRoute(AnimalHandle handle, BlockPos destination, long now) {
      return this.requestRoute(handle, destination, MoveMode.GROUND, now);
   }

   public boolean requestRoute(AnimalHandle handle, BlockPos destination, MoveMode mode, long now) {
      return this.requestRoute(handle, destination, mode, Map.of(), now);
   }

   public boolean requestRoute(AnimalHandle handle, BlockPos destination, MoveMode mode, Map<MoveMode, Float> mediumBias, long now) {
      return this.requestRoute(handle, destination, mode, mediumBias, now, true);
   }

   public boolean requestRoute(AnimalHandle handle, BlockPos destination, MoveMode mode, Map<MoveMode, Float> mediumBias, long now, boolean hold) {
      if (!destination.equals(this.goal) || !this.hasRoute() && !this.solving()) {
         if (this.arrived && destination.equals(this.goal) && this.standingAt(handle, destination)) {
            return false;
         }

         if (this.solving()) {
            return false;
         }

         BlockPos onDeck = null;
         if (mode == MoveMode.GROUND && handle.onDeck()) {
            onDeck = handle.deckGoal(destination);
            if (onDeck == null) {
               this.lastFailure = "no ground on the deck there";
               this.failureKind = RouteFollower.Failure.TACTICAL;
               return false;
            }

            if (this.deckGoal != null && onDeck.distManhattan(this.deckGoal) <= 1 && (this.hasRoute() || this.arrived && this.standingAt(handle, this.goal))) {
               this.goal = destination;
               return false;
            }
         }

         BlockPos solveFor = onDeck != null ? onDeck : destination;
         NavSolver.NeighborSource source = handle.navSource(solveFor);
         if (source == null) {
            this.lastFailure = "no start node";
            this.failureKind = RouteFollower.Failure.TACTICAL;
            return false;
         } else {
            this.lastFailure = null;
            this.failureKind = null;
            this.arrived = false;
            this.goal = destination;
            this.goalMode = mode;
            this.deckGoal = onDeck;
            this.frame = onDeck == null ? UnaryOperator.identity() : handle::placeNow;
            this.orbitCentre = null;
            this.hoverPoint = null;
            this.holdAtEnd = hold && mode == MoveMode.AIR;
            this.mediumBias = Map.copyOf(mediumBias);
            this.replans = 0;
            this.segments = 1;
            this.invalidated = false;
            this.requestedAt = now;
            this.pendingGeneration = AsyncSolver.nextGeneration();
            long generation = this.pendingGeneration;
            AsyncSolver.submit(
               new SolveRequest(source, solveFor, mode, this.biasedModeCost(handle), handle.navRange(), generation, this.danger),
               result -> this.accept(result, generation),
               () -> this.pendingGeneration == generation
            );
            return true;
         }
      } else {
         return false;
      }
   }

   private void accept(AsyncSolver.Result result, long generation) {
      if (generation != this.pendingGeneration) {
         NavStats.stale();
      } else {
         this.pendingGeneration = 0L;
         if (result.path() != null && !result.path().isEmpty()) {
            NavPath solved = result.path();
            if (solved.outcome() == NavPath.Outcome.EXHAUSTED && this.reachSink != null && Double.isFinite(solved.reach())) {
               this.reachSink.accept(new RouteFollower.Reach(solved.first().center(), solved.reach()));
            }

            if (!solved.reachesGoal() && solved.span() < 5.0) {
               this.lastFailure = "no way forward (" + solved.outcome().name().toLowerCase() + ")";
               this.failureKind = this.verdictOn(solved.outcome());
               this.clear();
            } else {
               this.path = solved;
               this.searched = true;
               this.index = 0;
               this.landingAttempts = 0;
               this.groundBreakTicks = 0;
               this.resubmitted = false;
               this.buildRibbon();
               this.lastFailure = null;
            }
         } else {
            this.lastFailure = "unsolvable";
            this.failureKind = RouteFollower.Failure.TACTICAL;
            this.clear();
         }
      }
   }

   private RouteFollower.Failure verdictOn(NavPath.Outcome outcome) {
      if (outcome != NavPath.Outcome.EXHAUSTED) {
         return RouteFollower.Failure.TACTICAL;
      }

      BlockPos proved = this.deckGoal != null ? this.deckGoal : this.goal;
      if (proved != null && this.unreachableSink != null) {
         this.unreachableSink.accept(proved);
      }

      return RouteFollower.Failure.CERTIFIED;
   }

   public void reportReachTo(@Nullable Consumer<RouteFollower.Reach> sink) {
      this.reachSink = sink;
   }

   public void touchedDown() {
      if (this.orbiting() || this.hovering()) {
         this.clear();
      } else if (this.goal != null) {
         if (this.goalMode == MoveMode.AIR) {
            this.clear();
         } else if (this.hasRoute()) {
            this.invalidated = true;
            this.lastFailure = "touched down short of the site - walking the rest";
         } else {
            if (this.solving()) {
               this.pendingGeneration = 0L;
               this.lastFailure = "touched down mid-solve - asking again on foot";
            }
         }
      }
   }

   public void reportUnreachableTo(@Nullable Consumer<BlockPos> sink) {
      this.unreachableSink = sink;
   }

   public RouteFollower.Status tick(AnimalHandle handle, long now) {
      if (this.deckGoal == null || handle.onDeck() && handle.placeNow(Vec3.atBottomCenterOf(this.deckGoal)) != null) {
         if (this.deckGoal != null && this.path != null) {
            this.projectRibbon();
         }

         if (this.solving()) {
            if (now - this.requestedAt <= 60L) {
               return RouteFollower.Status.SOLVING;
            } else if (!this.resubmitted) {
               this.resubmitted = true;
               this.lastFailure = "solve timed out - resubmitting";
               this.resolveSameGoal(handle, now);
               return RouteFollower.Status.SOLVING;
            } else {
               this.pendingGeneration = 0L;
               this.lastFailure = "solve timed out";
               this.failureKind = RouteFollower.Failure.TACTICAL;
               return RouteFollower.Status.IDLE;
            }
         } else {
            if (this.orbiting() || this.hovering()) {
               return RouteFollower.Status.FOLLOWING;
            }

            if (!this.hasRoute()) {
               return RouteFollower.Status.IDLE;
            }

            if (this.isGroundBreakStalled(handle)) {
               return RouteFollower.Status.IDLE;
            }

            if (this.invalidated) {
               this.resolveSameGoal(handle, now);
               return RouteFollower.Status.SOLVING;
            }

            this.actuatePendingTransition(handle);
            this.advanceIfArrived(handle);
            if (!this.hasRoute()) {
               return this.finishSegment(handle, now);
            }

            this.passDoorsAhead(handle);
            this.hopLedgeAhead(handle);
            return this.isStuck(handle, now) ? RouteFollower.Status.IDLE : RouteFollower.Status.FOLLOWING;
         }
      } else {
         this.lastFailure = "left the deck";
         this.failureKind = RouteFollower.Failure.TACTICAL;
         this.clear();
         return RouteFollower.Status.IDLE;
      }
   }

   private void hopLedgeAhead(AnimalHandle handle) {
      if (this.path != null && this.index < this.path.size() && !(handle.hopHeight() <= 0.0)) {
         if (handle.currentMode() == MoveMode.GROUND && handle.bodyOnGround()) {
            NavPath.Step step = this.path.step(this.index);
            if (step.transitionBefore() == null && step.node().mode() == MoveMode.GROUND) {
               Vec3 at = handle.bodyPosition();
               Vec3 ledge = this.at(step.node());
               if (aboveStep(handle, at, ledge)) {
                  double reach = Math.max(1.0, handle.bounds().getXsize());
                  double dx = ledge.x - at.x;
                  double dz = ledge.z - at.z;
                  if (!(dx * dx + dz * dz > reach * reach)) {
                     handle.hop(ledge.y - at.y);
                  }
               }
            }
         }
      }
   }

   private static boolean aboveStep(AnimalHandle handle, Vec3 at, Vec3 node) {
      return handle.hopHeight() > 0.0 && node.y - at.y > handle.stepHeight();
   }

   private void passDoorsAhead(AnimalHandle handle) {
      if (this.path != null && handle.currentMode() == MoveMode.GROUND) {
         int end = Math.min(this.path.size(), this.index + 4);
         boolean found = false;

         for (int i = this.index; i < end; i++) {
            NavNode node = this.path.step(i).node();
            if (node.mode() != MoveMode.GROUND) {
               break;
            }

            Vec3 passage = handle.passDoor(node.pos());
            if (passage != null && this.deckGoal != null) {
               passage = this.frame.apply(passage);
            }

            if (passage != null && !found) {
               found = true;
               this.doorwayPath = this.path;
               this.doorwayStep = i;
               this.doorwayPoint = passage;
            }
         }

         if (!found && this.doorwayPath == this.path && this.doorwayStep < this.index - 1) {
            Vec3 axis = this.doorAxis();
            if (axis == null || this.alongDoor(handle.bodyPosition(), axis) > 1.0) {
               this.doorwayPoint = null;
            }
         }
      }
   }

   public boolean doorAhead() {
      return this.path != null && this.doorwayPath == this.path && this.doorwayPoint != null;
   }

   @Nullable
   private Vec3 alongDoorway(Vec3 at, double lookahead, double width) {
      if (this.doorwayPoint != null && this.doorwayPath == this.path && this.path != null && this.doorwayStep < this.path.size()) {
         Vec3 axis = this.doorAxis();
         if (axis == null) {
            return null;
         } else {
            Vec3 offset = new Vec3(at.x - this.doorwayPoint.x, 0.0, at.z - this.doorwayPoint.z);
            double along = this.alongDoor(at, axis);
            double side = offset.subtract(axis.scale(along)).length();
            if (along > 1.0) {
               this.doorwayPoint = null;
               return null;
            } else if (!(along < -3.0) && !(side > 1.0)) {
               double ahead = side > 0.05 ? Math.min(lookahead, MoveTuning.doorAlignLookahead(width)) : lookahead;
               return this.doorwayPoint.add(axis.scale(along + ahead));
            } else {
               return null;
            }
         }
      } else {
         return null;
      }
   }

   @Nullable
   private Vec3 doorAxis() {
      if (this.doorwayPoint != null && this.doorwayPath == this.path && this.path != null && this.doorwayStep < this.path.size()) {
         Vec3 door = this.at(this.path.step(this.doorwayStep).node());
         Vec3 axis = this.doorwayStep > 0
            ? door.subtract(this.at(this.path.step(this.doorwayStep - 1).node()))
            : (this.doorwayStep + 1 < this.path.size() ? this.at(this.path.step(this.doorwayStep + 1).node()).subtract(door) : Vec3.ZERO);
         axis = new Vec3(axis.x, 0.0, axis.z);
         return axis.lengthSqr() < 1.0E-6 ? null : axis.normalize();
      } else {
         return null;
      }
   }

   private double alongDoor(Vec3 at, Vec3 axis) {
      return (at.x - this.doorwayPoint.x) * axis.x + (at.z - this.doorwayPoint.z) * axis.z;
   }

   private RouteFollower.Status finishSegment(AnimalHandle handle, long now) {
      NavPath finished = this.path;
      this.path = null;
      this.index = 0;
      this.buildRibbon();
      if (finished == null || !finished.worthContinuing()) {
         this.arrived = true;
         if (this.holdAtEnd && finished != null && finished.last().mode() == MoveMode.AIR) {
            this.hoverPoint = this.at(finished.last());
            return RouteFollower.Status.FOLLOWING;
         } else {
            return RouteFollower.Status.IDLE;
         }
      } else if (this.goal != null && this.segments < 6) {
         this.segments++;
         this.lastFailure = null;
         this.resolveSameGoal(handle, now);
         return RouteFollower.Status.SOLVING;
      } else {
         this.lastFailure = "gave up after " + this.segments + " segments (" + finished.outcome().name().toLowerCase() + ")";
         this.failureKind = RouteFollower.Failure.TACTICAL;
         this.clear();
         return RouteFollower.Status.IDLE;
      }
   }

   public double distanceToEnd(Vec3 from) {
      if (this.hovering()) {
         return from.distanceTo(this.hoverPoint);
      }

      boolean passing = this.hasRoute() && this.goalMode == MoveMode.AIR && !this.holdAtEnd && this.nextTransitionAfter(this.index - 1) < 0;
      if (passing) {
         return Double.MAX_VALUE;
      }

      if (this.hasRoute()) {
         for (int i = Math.max(0, this.index); i < this.path.size(); i++) {
            NavPath.Step step = this.path.step(i);
            if (step.transitionBefore() != null && step.node().mode() != MoveMode.AIR) {
               return from.distanceTo(this.at(step.node()));
            }
         }

         return from.distanceTo(this.at(this.path.last()));
      } else {
         return this.goal != null && !this.arrived ? from.distanceTo(Vec3.atCenterOf(this.goal)) : Double.MAX_VALUE;
      }
   }

   private boolean standingAt(AnimalHandle handle, BlockPos destination) {
      if (this.deckGoal != null && destination.equals(this.goal)) {
         Vec3 place = this.frame.apply(Vec3.atBottomCenterOf(this.deckGoal));
         return place != null && this.arrivedAt(handle, place);
      } else {
         return this.arrivedAt(handle, Vec3.atBottomCenterOf(destination));
      }
   }

   public boolean arrived() {
      return this.arrived && !this.hasRoute() && !this.solving();
   }

   public boolean idle() {
      return !this.hasRoute() && !this.solving() && !this.orbiting() && !this.hovering();
   }

   public List<NavPath.Step> debugSteps() {
      return this.path == null ? List.of() : this.path.steps();
   }

   public List<Vec3> debugRibbon() {
      return this.poly == null ? List.of() : this.poly;
   }

   public Ribbon.Cursor debugCursor() {
      return this.cursor;
   }

   public double debugCut(AnimalHandle handle, int vertex) {
      return vertex >= 0 && vertex < this.cuts.length && !Double.isNaN(this.cuts[vertex]) ? this.cuts[vertex] : this.cutAt(handle, vertex);
   }

   public boolean followRibbon(AnimalHandle handle, List<Vec3> vertices) {
      if (vertices.size() < 2) {
         return false;
      }

      double stride = MoveTuning.airLatticeStep(handle.bounds().getXsize());

      for (int i = 0; i + 1 < vertices.size(); i++) {
         if (!this.chordClear(handle, vertices.get(i), vertices.get(i + 1), stride)) {
            return false;
         }
      }

      List<NavPath.Step> steps = new ArrayList<>(vertices.size());

      for (Vec3 point : vertices) {
         steps.add(new NavPath.Step(NavNode.of(BlockPos.containing(point), MoveMode.AIR), null));
      }

      this.clear();
      this.path = new NavPath(List.copyOf(steps), NavPath.Outcome.REACHED);
      this.index = 0;
      this.arrived = false;
      this.goal = this.path.last().pos();
      this.goalMode = MoveMode.AIR;
      this.holdAtEnd = false;
      this.segments = 1;
      this.buildRibbon();
      return true;
   }

   private Vec3 flyable(AnimalHandle handle, Vec3 aim) {
      Vec3 at = handle.bodyPosition();
      double stride = MoveTuning.airLatticeStep(handle.bounds().getXsize());
      if (this.chordClear(handle, at, aim, stride)) {
         return aim;
      }

      Vec3 near = aim;

      for (int i = 0; i < 3; i++) {
         near = at.add(near.subtract(at).scale(0.5));
         if (this.chordClear(handle, at, near, stride)) {
            return near;
         }
      }

      Vec3 node = this.currentNodePoint();
      return node != null ? node : aim;
   }

   private boolean chordClear(AnimalHandle handle, Vec3 from, Vec3 to, double stride) {
      double length = from.distanceTo(to);
      int samples = (int)Math.ceil(length / Math.max(1.0, stride));

      for (int i = 0; i <= samples; i++) {
         Vec3 point = from.add(to.subtract(from).scale((double)i / Math.max(1, samples)));
         if (handle.isBlockedAt(point)) {
            return false;
         }
      }

      return true;
   }

   public int debugIndex() {
      return this.index;
   }

   @Nullable
   public BlockPos debugGoal() {
      return this.goal;
   }

   private void actuatePendingTransition(AnimalHandle handle) {
      if (this.pendingTransition() != null) {
         MoveMode wanted = this.targetMode();
         if (wanted != null && handle.currentMode() != wanted) {
            Vec3 target = this.currentNodePoint();
            if (target != null) {
               boolean ready = this.overColumnOf(handle, target) || handle.bodyOnGround() && this.settledOver(handle, target);
               if (ready) {
                  handle.requestMoveMode(wanted);
               }
            }
         }
      }
   }

   public boolean landing(AnimalHandle handle) {
      return handle.currentMode() == MoveMode.AIR && this.goalMode == MoveMode.GROUND && (this.hasRoute() || this.solving());
   }

   private boolean settledOver(AnimalHandle handle, Vec3 target) {
      Vec3 at = handle.bodyPosition();
      double reach = MoveTuning.aerialArrivalReach(handle.bounds().getXsize());
      double dx = at.x - target.x;
      double dz = at.z - target.z;
      return dx * dx + dz * dz <= reach * reach;
   }

   private boolean overColumnOf(AnimalHandle handle, Vec3 target) {
      Vec3 at = handle.bodyPosition();
      double reach = handle.bounds().getXsize() * 0.5 + 0.5;
      double dx = at.x - target.x;
      double dz = at.z - target.z;
      return dx * dx + dz * dz <= reach * reach;
   }

   private boolean arrivedAt(AnimalHandle handle, Vec3 target) {
      Vec3 at = handle.bodyPosition();
      double width = handle.bounds().getXsize();
      MoveMode moving = handle.currentMode();
      double reach = moving == MoveMode.AIR ? MoveTuning.aerialArrivalReach(width) : width * 0.5 + 0.5;
      double dx = at.x - target.x;
      double dz = at.z - target.z;
      if (dx * dx + dz * dz > reach * reach) {
         return false;
      }

      return switch (moving) {
         case AIR -> Math.abs(at.y - target.y) <= MoveTuning.aerialArrivalReach(width);
         case WATER -> Math.abs(at.y - target.y) <= 2.0;
         default -> at.y - target.y > -2.0 && at.y - target.y < handle.bounds().getYsize() + 1.0 && !aboveStep(handle, at, target);
      };
   }

   private void advanceIfArrived(AnimalHandle handle) {
      if (this.path != null && this.index < this.path.size()) {
         if (this.pendingTransition() == null || handle.currentMode() == this.targetMode()) {
            if (handle.currentMode() == MoveMode.AIR && this.poly != null) {
               this.advanceAlongRibbon(handle);
            } else {
               int reached = -1;

               for (int i = this.index; i < this.path.size() && (i <= this.index || this.path.step(i).transitionBefore() == null); i++) {
                  if (this.arrivedAt(handle, this.at(this.path.step(i).node()))) {
                     reached = i;
                  }
               }

               if (reached >= 0) {
                  this.index = reached + 1;
               }
            }
         }
      }
   }

   private boolean isGroundBreakStalled(AnimalHandle handle) {
      if (handle.currentMode() == MoveMode.GROUND || handle.currentMode() == MoveMode.WATER || !handle.bodyOnGround()) {
         this.groundBreakTicks = 0;
         return false;
      }

      if (++this.groundBreakTicks < 40) {
         return false;
      }

      this.lastFailure = "could not leave the ground";
      this.failureKind = RouteFollower.Failure.TACTICAL;
      handle.requestMoveMode(MoveMode.GROUND);
      this.clear();
      return true;
   }

   @Nullable
   public Vec3 coastTarget() {
      return this.solving() && this.goal != null ? Vec3.atCenterOf(this.goal) : null;
   }

   @Nullable
   public Vec3 effectiveTarget(AnimalHandle handle) {
      if (this.orbiting()) {
         return this.orbitTarget(handle);
      }

      if (this.hovering()) {
         return this.hoverTarget(handle);
      }

      if (this.hasRoute()) {
         this.advanceIfArrived(handle);
      }

      Vec3 aim = this.pursuitTarget(handle);
      return aim != null ? aim : this.coastTarget();
   }

   private boolean isStuck(AnimalHandle handle, long now) {
      Vec3 at = this.deckGoal != null ? handle.keepPlace(handle.bodyPosition()) : handle.bodyPosition();
      if (this.stuckAnchor != null && !handle.stoodOn() && !handle.held()) {
         if (now - this.stuckSince < 80L) {
            return false;
         } else {
            double moved = at.distanceTo(this.stuckAnchor);
            this.stuckAnchor = at;
            this.stuckSince = now;
            if (moved >= 2.5) {
               return false;
            } else if (this.pendingTransition() == ModeTransition.LAND && ++this.landingAttempts < 3) {
               this.index = Math.max(0, this.index - 1);
               return false;
            } else if (this.pendingTransition() == ModeTransition.LAND) {
               this.lastFailure = "nowhere to land";
               this.failureKind = RouteFollower.Failure.TACTICAL;
               this.clear();
               return true;
            } else if (++this.replans <= 2) {
               this.lastFailure = "stuck - replanning " + this.replans + "/2";
               this.resolveSameGoal(handle, now);
               return false;
            } else {
               this.lastFailure = "stuck";
               this.failureKind = RouteFollower.Failure.TACTICAL;
               this.clear();
               return true;
            }
         }
      } else {
         this.stuckAnchor = at;
         this.stuckSince = now;
         return false;
      }
   }

   public void onBlockChanged(BlockPos changed, long now) {
      if (this.path != null && !this.invalidated) {
         double range = 8.0;
         double sq = range * range;
         Vec3 at = Vec3.atCenterOf(changed);

         for (int i = this.index; i < this.path.size(); i++) {
            if (this.path.step(i).node().pos().distSqr(changed) <= sq) {
               this.invalidated = true;
               this.lastFailure = "route invalidated by block change";
               return;
            }

            if (this.poly != null && i + 1 < this.poly.size() && Ribbon.distanceToSegment(at, this.poly.get(i), this.poly.get(i + 1)) <= range) {
               this.invalidated = true;
               this.lastFailure = "route invalidated by block change";
               return;
            }
         }
      }
   }

   public boolean invalidated() {
      return this.invalidated;
   }

   private void resolveSameGoal(AnimalHandle handle, long now) {
      BlockPos keep = this.deckGoal != null ? this.deckGoal : this.goal;
      if (keep == null) {
         this.clear();
      } else {
         NavSolver.NeighborSource source = handle.navSource(keep);
         if (source == null) {
            this.clear();
         } else {
            this.path = null;
            this.index = 0;
            this.buildRibbon();
            this.invalidated = false;
            this.stuckAnchor = null;
            this.pendingGeneration = AsyncSolver.nextGeneration();
            long generation = this.pendingGeneration;
            AsyncSolver.submit(
               new SolveRequest(source, keep, this.goalMode, this.biasedModeCost(handle), handle.navRange(), generation, this.danger),
               result -> this.accept(result, generation),
               () -> this.pendingGeneration == generation
            );
         }
      }
   }

   public void clear() {
      this.path = null;
      this.searched = false;
      this.index = 0;
      this.poly = null;
      this.cuts = new double[0];
      this.cursor = Ribbon.Cursor.START;
      this.orbitCentre = null;
      this.hoverPoint = null;
      this.holdAtEnd = false;
      this.pendingGeneration = 0L;
      this.goal = null;
      this.deckGoal = null;
      this.frame = UnaryOperator.identity();
      this.arrived = false;
      this.stuckAnchor = null;
      this.landingAttempts = 0;
      this.replans = 0;
      this.segments = 0;
      this.invalidated = false;
      this.groundBreakTicks = 0;
      this.resubmitted = false;
   }

   public enum Failure {
      TACTICAL,
      CERTIFIED;
   }

   public record Reach(Vec3 centre, double radius) {
   }

   public enum Status {
      IDLE,
      SOLVING,
      FOLLOWING;
   }
}
