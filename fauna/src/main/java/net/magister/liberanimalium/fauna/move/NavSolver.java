package net.magister.liberanimalium.fauna.move;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Set;
import org.jetbrains.annotations.Nullable;
import net.magister.liberanimalium.fauna.api.MoveMode;
import net.magister.liberanimalium.util.NavStats;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;

public final class NavSolver {
   private NavSolver() {
   }

   @Nullable
   public static NavPath solve(NavSolver.NeighborSource source, BlockPos goal, Map<MoveMode, Float> modeCost, float maxRange) {
      return solve(source, goal, MoveMode.GROUND, modeCost, maxRange);
   }

   @Nullable
   public static NavPath solve(NavSolver.NeighborSource source, BlockPos goal, MoveMode goalMode, Map<MoveMode, Float> modeCost, float maxRange) {
      return solve(source, goal, goalMode, modeCost, maxRange, NavSolver.DangerField.NONE);
   }

   public static NavPath solve(
      NavSolver.NeighborSource source, BlockPos goal, MoveMode goalMode, Map<MoveMode, Float> modeCost, float maxRange, NavSolver.DangerField danger
   ) {
      NavNode start = source.start();
      if (start == null) {
         return null;
      }

      NavSolver.Prices prices = NavSolver.Prices.of(modeCost);
      if (prices == null) {
         return null;
      }

      int accuracy = MoveTuning.accuracy(goalMode, source.arrivalCells());
      NavSolver.Tally tally = new NavSolver.Tally();
      NavSolver.Pass pass = null;
      if (start.mode() == MoveMode.GROUND && goalMode == MoveMode.GROUND && prices.mixed()) {
         pass = walkFirst(source, start, goal, prices, maxRange, danger, accuracy, tally);
      } else if (start.mode() == MoveMode.AIR && goalMode == MoveMode.GROUND) {
         pass = viaColumn(source, start, goal, prices, maxRange, danger, accuracy, tally);
      }

      if (pass == null) {
         pass = search(source, start, goal, goalMode, prices, maxRange, danger, accuracy, Float.POSITIVE_INFINITY, null, false, tally);
      }

      report(tally, pass, goalMode, start, goal, prices, source, accuracy);
      return ribbon(pass.path(), source);
   }

   private static NavSolver.Pass walkFirst(
      NavSolver.NeighborSource source,
      NavNode start,
      BlockPos goal,
      NavSolver.Prices prices,
      float maxRange,
      NavSolver.DangerField danger,
      int accuracy,
      NavSolver.Tally tally
   ) {
      NavSolver.Pass walk = search(
         source, start, goal, MoveMode.GROUND, prices.only(MoveMode.GROUND), maxRange, danger, accuracy, Float.POSITIVE_INFINITY, null, true, tally
      );
      if (!walk.reached()) {
         Set<NavNode> sealed = walk.path().outcome() == NavPath.Outcome.EXHAUSTED ? walk.closed() : null;
         NavSolver.Pass mixed = search(source, start, goal, MoveMode.GROUND, prices, maxRange, danger, accuracy, Float.POSITIVE_INFINITY, sealed, false, tally);
         if (mixed.reached()) {
            return mixed;
         }

         float walkTotal = walk.cost() + prices.estimate(walk.end(), goal, MoveMode.GROUND, null);
         float mixedTotal = mixed.cost() + prices.estimate(mixed.end(), goal, MoveMode.GROUND, null);
         return mixedTotal < walkTotal ? mixed : walk;
      } else {
         NavSolver.Pass mixed = search(source, start, goal, MoveMode.GROUND, prices, maxRange, danger, accuracy, walk.cost(), null, false, tally);
         return mixed.reached() && mixed.cost() < walk.cost() ? mixed : walk;
      }
   }

   @Nullable
   private static NavSolver.Pass viaColumn(
      NavSolver.NeighborSource source,
      NavNode start,
      BlockPos goal,
      NavSolver.Prices prices,
      float maxRange,
      NavSolver.DangerField danger,
      int accuracy,
      NavSolver.Tally tally
   ) {
      if (prices.active(MoveMode.AIR) && prices.active(MoveMode.GROUND)) {
         if (prices.factor(MoveMode.AIR) >= prices.factor(MoveMode.GROUND)) {
            return null;
         }

         NavNode top = source.landingColumn(start, goal);
         if (top == null) {
            return null;
         }

         NavSolver.Pass air = search(
            source,
            start,
            top.pos(),
            MoveMode.AIR,
            prices.only(MoveMode.AIR),
            maxRange,
            danger,
            MoveTuning.accuracy(MoveMode.AIR, source.arrivalCells()),
            Float.POSITIVE_INFINITY,
            null,
            false,
            tally
         );
         if (!air.reached()) {
            return null;
         }

         List<NavPath.Step> steps = new ArrayList<>(air.path().steps());
         float cost = air.cost();
         NavNode over = air.end();
         if (!over.equals(top)) {
            if (!source.segmentClear(over, top)) {
               return null;
            }

            steps.add(new NavPath.Step(top, null));
            cost += dist(over, top) * prices.factor(MoveMode.AIR);
         }

         for (NavNode site : source.transitionTargets(top, ModeTransition.LAND)) {
            if (site.mode() == MoveMode.GROUND && manhattan(site, goal) <= accuracy) {
               steps.add(new NavPath.Step(site, ModeTransition.LAND));
               cost += MoveTuning.transitionCost(ModeTransition.LAND) + prices.travel(ModeTransition.LAND, dist(top, site), site.mode(), source.band());
               return new NavSolver.Pass(new NavPath(List.copyOf(steps), NavPath.Outcome.REACHED, air.path().reach()), site, cost, null);
            }
         }

         return null;
      } else {
         return null;
      }
   }

   private static NavSolver.Pass search(
      NavSolver.NeighborSource source,
      NavNode start,
      BlockPos goal,
      MoveMode goalMode,
      NavSolver.Prices prices,
      float maxRange,
      NavSolver.DangerField danger,
      int accuracy,
      float bound,
      @Nullable Set<NavNode> sealed,
      boolean keepClosed,
      NavSolver.Tally tally
   ) {
      Map<NavNode, NavSolver.State> states = new HashMap<>();
      PriorityQueue<NavSolver.Entry> open = new PriorityQueue<>();
      NavSolver.State s0 = new NavSolver.State();
      s0.g = 0.0F;
      s0.walked = 0.0F;
      s0.f = prices.estimate(start, goal, goalMode, sealed);
      states.put(start, s0);
      open.add(new NavSolver.Entry(start, s0.f, straightLine(start, goal)));
      NavSolver.Progress progress = new NavSolver.Progress(start);
      NavNode bestInMode = start.mode() == goalMode ? start : null;
      NavNode reached = null;
      int visited = 0;
      boolean cappedNodes = false;
      float reach = 0.0F;
      int band = source.band();
      float maxWalked = MoveTuning.detourAllowance(maxRange);

      while (!open.isEmpty()) {
         if (++visited >= 5000) {
            cappedNodes = true;
            break;
         }

         NavSolver.Entry entry = open.poll();
         NavSolver.State cs = states.get(entry.node());
         if (cs != null && !cs.closed && !(entry.f() > cs.f)) {
            if (cs.f > bound) {
               break;
            }

            cs.closed = true;
            NavNode node = entry.node();
            reach = Math.max(reach, dist(node, start));
            if (node.mode() == goalMode && manhattan(node, goal) <= accuracy) {
               reached = node;
               break;
            }

            if (!(dist(node, start) >= maxRange)) {
               for (NavSolver.Neighbor nb : source.sameModeNeighbors(node)) {
                  if (prices.active(nb.to().mode())) {
                     float d = dist(node, nb.to());
                     float c = nb.costMalus();
                     float m = danger.maluAt(nb.to());
                     float step = (d + c + m) * prices.edge(nb);
                     tally.edgeDist += d;
                     tally.edgeMalus += c + m;
                     relax(node, nb.to(), cs, step, d, null, states, open, goal, goalMode, prices, sealed, maxWalked, progress);
                     if (nb.to().mode() == goalMode && states.containsKey(nb.to()) && closer(nb.to(), bestInMode, goal)) {
                        bestInMode = nb.to();
                     }
                  }
               }

               for (ModeTransition transition : transitionsFrom(node.mode())) {
                  if (prices.active(transition.destination())) {
                     for (NavNode target : source.transitionTargets(node, transition)) {
                        if (prices.active(target.mode())) {
                           float travel = prices.travel(transition, dist(node, target), target.mode(), band);
                           relax(
                              node,
                              target,
                              cs,
                              MoveTuning.transitionCost(transition) + travel,
                              dist(node, target),
                              transition,
                              states,
                              open,
                              goal,
                              goalMode,
                              prices,
                              sealed,
                              maxWalked,
                              progress
                           );
                           if (target.mode() == goalMode && states.containsKey(target) && closer(target, bestInMode, goal)) {
                              bestInMode = target;
                           }
                        }
                     }
                  }
               }
            }
         }
      }

      tally.visited += visited;
      tally.reach = Math.max(tally.reach, reach);
      NavNode end;
      NavPath.Outcome outcome;
      if (reached != null) {
         end = reached;
         outcome = NavPath.Outcome.REACHED;
      } else {
         end = bestInMode != null ? bestInMode : progress.best;
         outcome = why(cappedNodes, progress, source);
      }

      Set<NavNode> closed = null;
      if (keepClosed) {
         closed = new HashSet<>();

         for (Map.Entry<NavNode, NavSolver.State> e : states.entrySet()) {
            if (e.getValue().closed) {
               closed.add(e.getKey());
            }
         }
      }

      return new NavSolver.Pass(reconstruct(end, states, outcome, reach), end, states.get(end).g, closed);
   }

   private static void report(
      NavSolver.Tally tally,
      NavSolver.Pass pass,
      MoveMode goalMode,
      NavNode start,
      BlockPos goal,
      NavSolver.Prices prices,
      NavSolver.NeighborSource source,
      int accuracy
   ) {
      NavNode end = pass.end();
      NavPath.Outcome outcome = pass.path().outcome();
      float goalFactor = prices.factor(goalMode);
      float hRatio = Float.isFinite(goalFactor) ? goalFactor / prices.minFactor : Float.NaN;
      float malusPerBlock = tally.edgeDist > 0.0F ? tally.edgeMalus / tally.edgeDist : Float.NaN;
      float dx = goal.getX() - end.x();
      float dz = goal.getZ() - end.z();
      float closestXz = Mth.sqrt(dx * dx + dz * dz);
      float closestY = Math.abs(goal.getY() - end.y());
      Boolean standable = goalMode == MoveMode.GROUND ? source.goalStandable(goal, accuracy) : null;
      int pocket = -2;
      if (outcome == NavPath.Outcome.NODE_CAP && goalMode == MoveMode.GROUND && Boolean.TRUE.equals(standable)) {
         pocket = source.goalPocket(goal, accuracy, 256);
      }

      NavStats.searched(
         new NavStats.Search(
            tally.visited,
            outcome.name(),
            goalMode.name(),
            straightLine(start, goal),
            tally.reach,
            straightLine(end, goal),
            closestXz,
            closestY,
            standable,
            pocket,
            hRatio,
            malusPerBlock
         )
      );
   }

   private static NavPath ribbon(NavPath solved, NavSolver.NeighborSource source) {
      return Ribbon.prune(solved, source::segmentClear);
   }

   private static NavPath.Outcome why(boolean cappedNodes, NavSolver.Progress progress, NavSolver.NeighborSource source) {
      if (cappedNodes) {
         return NavPath.Outcome.NODE_CAP;
      } else if (progress.walkedCapped) {
         return NavPath.Outcome.WALKED_CAP;
      } else {
         return source.reachedHorizon() ? NavPath.Outcome.HORIZON : NavPath.Outcome.EXHAUSTED;
      }
   }

   private static List<ModeTransition> transitionsFrom(MoveMode mode) {
      return switch (mode) {
         case GROUND -> List.of(ModeTransition.TAKEOFF, ModeTransition.WATER_ENTER, ModeTransition.CLIMB_ON);
         case AIR -> List.of(ModeTransition.LAND, ModeTransition.SPLASH_DOWN);
         case WATER -> List.of(ModeTransition.WATER_EXIT, ModeTransition.TAKEOFF);
         case CLIMB -> List.of(ModeTransition.CLIMB_OFF);
      };
   }

   private static void relax(
      NavNode from,
      NavNode to,
      NavSolver.State fromState,
      float gInc,
      float walkedInc,
      @Nullable ModeTransition marker,
      Map<NavNode, NavSolver.State> states,
      PriorityQueue<NavSolver.Entry> open,
      BlockPos goal,
      MoveMode goalMode,
      NavSolver.Prices prices,
      @Nullable Set<NavNode> sealed,
      float maxWalked,
      NavSolver.Progress progress
   ) {
      float walked = fromState.walked + walkedInc;
      if (walked >= maxWalked) {
         progress.walkedCapped = true;
      } else {
         float g = fromState.g + gInc;
         NavSolver.State ts = states.get(to);
         if (ts == null) {
            ts = new NavSolver.State();
            states.put(to, ts);
         } else if (ts.closed || g >= ts.g) {
            return;
         }

         ts.g = g;
         ts.walked = walked;
         ts.cameFrom = from;
         ts.transitionBefore = marker;
         ts.f = g + prices.estimate(to, goal, goalMode, sealed);
         ts.closed = false;
         open.add(new NavSolver.Entry(to, ts.f, straightLine(to, goal)));
         if (straightLine(to, goal) < straightLine(progress.best, goal)) {
            progress.best = to;
         }
      }
   }

   private static NavPath reconstruct(NavNode target, Map<NavNode, NavSolver.State> states, NavPath.Outcome outcome, double reach) {
      List<NavPath.Step> steps = new ArrayList<>();
      NavNode cur = target;

      while (cur != null) {
         NavSolver.State st = states.get(cur);
         steps.add(0, new NavPath.Step(cur, st.transitionBefore));
         cur = st.cameFrom;
      }

      return new NavPath(List.copyOf(steps), outcome, reach);
   }

   private static boolean closer(NavNode candidate, @Nullable NavNode incumbent, BlockPos goal) {
      return incumbent == null || straightLine(candidate, goal) < straightLine(incumbent, goal);
   }

   static float straightLine(NavNode node, BlockPos goal) {
      float dx = goal.getX() - node.x();
      float dy = goal.getY() - node.y();
      float dz = goal.getZ() - node.z();
      return Mth.sqrt(dx * dx + dy * dy + dz * dz);
   }

   static float dist(NavNode a, NavNode b) {
      float dx = b.x() - a.x();
      float dy = b.y() - a.y();
      float dz = b.z() - a.z();
      return Mth.sqrt(dx * dx + dy * dy + dz * dz);
   }

   static int manhattan(NavNode node, BlockPos goal) {
      return Math.abs(goal.getX() - node.x()) + Math.abs(goal.getY() - node.y()) + Math.abs(goal.getZ() - node.z());
   }

   @FunctionalInterface
   public interface DangerField {
      NavSolver.DangerField NONE = node -> 0.0F;

      float maluAt(NavNode var1);
   }

   private record Entry(NavNode node, float f, float h) implements Comparable<NavSolver.Entry> {
      public int compareTo(NavSolver.Entry o) {
         int byF = Float.compare(this.f, o.f);
         return byF != 0 ? byF : Float.compare(this.h, o.h);
      }
   }

   public record Neighbor(NavNode to, float costMalus, boolean low) {
      public Neighbor(NavNode to, float costMalus) {
         this(to, costMalus, false);
      }
   }

   public interface NeighborSource {
      @Nullable
      NavNode start();

      List<NavSolver.Neighbor> sameModeNeighbors(NavNode var1);

      @Nullable
      NavNode transitionTarget(NavNode var1, ModeTransition var2);

      default List<NavNode> transitionTargets(NavNode node, ModeTransition transition) {
         NavNode target = this.transitionTarget(node, transition);
         return target == null ? List.of() : List.of(target);
      }

      default int arrivalCells() {
         return 0;
      }

      default boolean reachedHorizon() {
         return false;
      }

      @Nullable
      default Boolean goalStandable(BlockPos goal, int accuracy) {
         return null;
      }

      default int goalPocket(BlockPos goal, int accuracy, int budget) {
         return -2;
      }

      default boolean segmentClear(NavNode from, NavNode to) {
         return false;
      }

      default int band() {
         return 0;
      }

      @Nullable
      default NavNode landingColumn(NavNode from, BlockPos goal) {
         return null;
      }
   }

   private record Pass(NavPath path, NavNode end, float cost, @Nullable Set<NavNode> closed) {
      boolean reached() {
         return this.path.reachesGoal();
      }
   }

   private static final class Prices {
      final EnumMap<MoveMode, Float> factor;
      final float minFactor;
      final float[][] toll;
      final float lowAir;

      private Prices(EnumMap<MoveMode, Float> factor, float lowAir) {
         this.factor = factor;
         float min = Float.MAX_VALUE;

         for (float f : factor.values()) {
            min = Math.min(min, f);
         }

         this.minFactor = min;
         this.lowAir = lowAir;
         this.toll = tolls(factor.keySet());
      }

      @Nullable
      static NavSolver.Prices of(Map<MoveMode, Float> modeCost) {
         EnumMap<MoveMode, Float> factor = new EnumMap<>(MoveMode.class);

         for (MoveMode m : MoveMode.values()) {
            float cost = modeCost.getOrDefault(m, Float.POSITIVE_INFINITY);
            if (Float.isFinite(cost)) {
               factor.put(m, Math.max(cost, 1.0E-4F));
            }
         }

         if (factor.isEmpty()) {
            return null;
         }

         Float ground = factor.get(MoveMode.GROUND);
         return new NavSolver.Prices(factor, ground == null ? 0.0F : MoveTuning.lowAirFactor(ground));
      }

      NavSolver.Prices only(MoveMode mode) {
         EnumMap<MoveMode, Float> one = new EnumMap<>(MoveMode.class);
         one.put(mode, this.factor.get(mode));
         return new NavSolver.Prices(one, this.lowAir);
      }

      boolean active(MoveMode mode) {
         return this.factor.containsKey(mode);
      }

      boolean mixed() {
         return this.active(MoveMode.GROUND) && this.factor.size() > 1;
      }

      float factor(MoveMode mode) {
         return this.factor.getOrDefault(mode, Float.POSITIVE_INFINITY);
      }

      float edge(NavSolver.Neighbor nb) {
         float f = this.factor.get(nb.to().mode());
         return nb.low() ? Math.max(f, this.lowAir) : f;
      }

      float travel(ModeTransition transition, float distance, MoveMode into, int band) {
         float air = this.factor.getOrDefault(MoveMode.AIR, 0.0F);
         return MoveTuning.transitionTravel(transition, distance, air, this.factor.get(into), this.lowAir, band);
      }

      float estimate(NavNode node, BlockPos goal, MoveMode goalMode, @Nullable Set<NavNode> sealed) {
         float d = NavSolver.straightLine(node, goal);
         int from = node.mode().ordinal();
         int to = goalMode.ordinal();
         boolean grounded = sealed != null && node.mode() == MoveMode.GROUND && goalMode == MoveMode.GROUND && sealed.contains(node);
         float best = Float.POSITIVE_INFINITY;

         for (Map.Entry<MoveMode, Float> e : this.factor.entrySet()) {
            int via = e.getKey().ordinal();
            if (!grounded || e.getKey() != MoveMode.GROUND) {
               float rate = grounded ? Math.min(e.getValue(), this.factor.get(MoveMode.GROUND)) : e.getValue();
               best = Math.min(best, this.toll[from][via] + this.toll[via][to] + rate * d);
            }
         }

         return Float.isFinite(best) ? best : d * this.minFactor;
      }

      private static float[][] tolls(Set<MoveMode> active) {
         int n = MoveMode.values().length;
         float[][] t = new float[n][n];

         for (int a = 0; a < n; a++) {
            for (int b = 0; b < n; b++) {
               t[a][b] = a == b ? 0.0F : Float.POSITIVE_INFINITY;
            }
         }

         for (MoveMode m : active) {
            for (ModeTransition transition : NavSolver.transitionsFrom(m)) {
               if (active.contains(transition.destination())) {
                  int a = m.ordinal();
                  int b = transition.destination().ordinal();
                  t[a][b] = Math.min(t[a][b], MoveTuning.transitionCost(transition));
               }
            }
         }

         for (int k = 0; k < n; k++) {
            for (int a = 0; a < n; a++) {
               for (int b = 0; b < n; b++) {
                  if (t[a][k] + t[k][b] < t[a][b]) {
                     t[a][b] = t[a][k] + t[k][b];
                  }
               }
            }
         }

         return t;
      }
   }

   private static final class Progress {
      NavNode best;
      boolean walkedCapped;

      Progress(NavNode start) {
         this.best = start;
      }
   }

   private static final class State {
      float g = Float.MAX_VALUE;
      float f;
      float walked;
      @Nullable
      NavNode cameFrom;
      @Nullable
      ModeTransition transitionBefore;
      boolean closed;
   }

   private static final class Tally {
      int visited;
      float reach;
      float edgeDist;
      float edgeMalus;
   }
}
