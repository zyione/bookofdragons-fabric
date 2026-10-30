package net.magister.liberanimalium.body.nav.pathfinding;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import org.jetbrains.annotations.Nullable;
import net.magister.liberanimalium.body.SpeciesAssets;
import net.magister.liberanimalium.body.locomotion.transition.TakeoffTransition;
import net.magister.liberanimalium.fauna.api.MoveMode;
import net.magister.liberanimalium.fauna.move.ModeTransition;
import net.magister.liberanimalium.fauna.move.MoveTuning;
import net.magister.liberanimalium.fauna.move.NavNode;
import net.magister.liberanimalium.fauna.move.NavSolver;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.BlockPos.MutableBlockPos;
import net.minecraft.core.Direction.Plane;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.PathNavigationRegion;
import net.minecraft.world.level.pathfinder.BlockPathTypes;
import net.minecraft.world.level.pathfinder.Node;
import net.minecraft.world.level.pathfinder.NodeEvaluator;
import net.minecraft.world.phys.Vec3;

public final class FaunaNeighborSource implements NavSolver.NeighborSource {
   private static final int SPLASH_REACH = 8;
   private final FaunaWalkNodeEvaluator walkEval;
   @Nullable
   private final FaunaFlyNodeEvaluator flyEval;
   @Nullable
   private final AirLattice lattice;
   @Nullable
   private final SwimNodeEvaluator swimEval;
   @Nullable
   private final WaterLattice waterLattice;
   private final Vec3 bodyAt;
   private final PathNavigationRegion region;
   private final MoveMode startMode;
   private final BlockPos min;
   private final BlockPos max;
   private boolean horizon;
   private final BodyFit fit;
   private final int headroomTop;
   private final Node[] buffer = new Node[32];
   private final MutableBlockPos scan = new MutableBlockPos();

   public FaunaNeighborSource(
      Mob mob,
      PathNavigationRegion region,
      BlockPos min,
      BlockPos max,
      MoveMode startMode,
      boolean canFly,
      boolean canSwim,
      boolean opensDoors,
      double hopHeight,
      Map<BlockPathTypes, Float> hazards
   ) {
      this(mob, region, min, max, startMode, canFly, canSwim, false, opensDoors, hopHeight, hazards);
   }

   public FaunaNeighborSource(
      Mob mob,
      PathNavigationRegion region,
      BlockPos min,
      BlockPos max,
      MoveMode startMode,
      boolean canFly,
      boolean canSwim,
      boolean canDive,
      boolean opensDoors,
      double hopHeight,
      Map<BlockPathTypes, Float> hazards
   ) {
      this(mob, region, min, max, startMode, canFly, canSwim, canDive, opensDoors, hopHeight, hazards, mob.position());
   }

   public FaunaNeighborSource(
      Mob mob,
      PathNavigationRegion region,
      BlockPos min,
      BlockPos max,
      MoveMode startMode,
      boolean canFly,
      boolean canSwim,
      boolean canDive,
      boolean opensDoors,
      double hopHeight,
      Map<BlockPathTypes, Float> hazards,
      Vec3 bodyAt
   ) {
      this.region = region;
      this.min = min;
      this.max = max;
      this.startMode = startMode;
      this.fit = new BodyFit(region, mob.getBbWidth(), mob.getBbHeight());
      this.headroomTop = TakeoffTransition.headroomTop(mob.getBbHeight(), TakeoffTransition.headroom(SpeciesAssets.hostOf(mob)));

      for (Entry<BlockPathTypes, Float> entry : hazards.entrySet()) {
         mob.setPathfindingMalus(entry.getKey(), entry.getValue());
      }

      this.walkEval = new FaunaWalkNodeEvaluator();
      this.walkEval.setHopHeight(hopHeight);
      prepare(this.walkEval, region, mob, opensDoors);
      this.walkEval.startAt(bodyAt);
      this.flyEval = canFly ? new FaunaFlyNodeEvaluator() : null;
      if (this.flyEval != null) {
         prepare(this.flyEval, region, mob, opensDoors);
         this.flyEval.startAt(bodyAt);
      }

      this.lattice = canFly ? new AirLattice(this.fit, mob.getBbWidth()) : null;
      this.swimEval = canSwim ? new SwimNodeEvaluator() : null;
      if (this.swimEval != null) {
         prepare(this.swimEval, region, mob, opensDoors);
         this.swimEval.startAt(bodyAt);
      }

      this.waterLattice = canSwim && canDive ? new WaterLattice(this.fit, mob.getBbWidth()) : null;
      this.bodyAt = bodyAt;
   }

   private static void prepare(NodeEvaluator eval, PathNavigationRegion region, Mob mob, boolean opensDoors) {
      eval.setCanPassDoors(true);
      eval.setCanOpenDoors(opensDoors);
      eval.setCanFloat(true);
      eval.prepare(region, mob);
   }

   public void done() {
      this.walkEval.done();
      if (this.flyEval != null) {
         this.flyEval.done();
      }

      if (this.swimEval != null) {
         this.swimEval.done();
      }
   }

   @Nullable
   @Override
   public NavNode start() {
      Node s = switch (this.startMode) {
         case AIR -> this.flyEval != null ? this.flyEval.getStart() : this.walkEval.getStart();
         case WATER -> this.swimEval != null ? this.swimEval.getStart() : this.walkEval.getStart();
         default -> this.walkEval.getStart();
      };
      if (this.startMode == MoveMode.WATER && this.waterLattice != null) {
         int x = Mth.floor(this.bodyAt.x);
         int y = Mth.floor(this.bodyAt.y);
         int z = Mth.floor(this.bodyAt.z);
         if (this.waterLattice.swims(x, y, z)) {
            return new NavNode(x, y, z, MoveMode.WATER);
         }
      }

      return s == null ? null : new NavNode(s.x, s.y, s.z, this.startMode);
   }

   @Override
   public int arrivalCells() {
      return this.fit.radius();
   }

   @Override
   public boolean reachedHorizon() {
      return this.horizon;
   }

   @Override
   public Boolean goalStandable(BlockPos goal, int accuracy) {
      return this.standableInBall(goal, accuracy) != null;
   }

   @Nullable
   private NavNode standableInBall(BlockPos goal, int accuracy) {
      for (int dy = -accuracy; dy <= accuracy; dy++) {
         int budget = accuracy - Math.abs(dy);

         for (int dx = -budget; dx <= budget; dx++) {
            int rest = budget - Math.abs(dx);

            for (int dz = -rest; dz <= rest; dz++) {
               int x = goal.getX() + dx;
               int y = goal.getY() + dy;
               int z = goal.getZ() + dz;
               if (this.inBounds(x, y, z) && this.walkEval.groundStandable(x, y, z)) {
                  return new NavNode(x, y, z, MoveMode.GROUND);
               }
            }
         }
      }

      return null;
   }

   @Override
   public int goalPocket(BlockPos goal, int accuracy, int budget) {
      NavNode seed = this.standableInBall(goal, accuracy);
      if (seed == null) {
         return -2;
      }

      boolean horizonBefore = this.horizon;
      HashSet<NavNode> seen = new HashSet<>();
      ArrayDeque<NavNode> frontier = new ArrayDeque<>();
      seen.add(seed);
      frontier.add(seed);

      int result;
      while (true) {
         if (seen.size() >= budget) {
            result = -1;
            break;
         }

         NavNode node = frontier.poll();
         if (node == null) {
            result = seen.size();
            break;
         }

         for (NavSolver.Neighbor nb : this.sameModeNeighbors(node)) {
            if (seen.add(nb.to())) {
               frontier.add(nb.to());
            }
         }
      }

      this.horizon = horizonBefore;
      return result;
   }

   @Override
   public int band() {
      return this.lattice == null ? 0 : this.lattice.band();
   }

   @Nullable
   @Override
   public NavNode landingColumn(NavNode from, BlockPos goal) {
      if (this.lattice == null) {
         return null;
      }

      int x = goal.getX();
      int y = goal.getY();
      int z = goal.getZ();
      int step = this.lattice.step();
      int top = Math.max(y, Math.min(y + this.lattice.band(), from.y()));
      top -= Math.floorMod(top - from.y(), step);
      if (top < y) {
         top += step;
      }

      int off = offLattice(x - from.x(), step) + offLattice(z - from.z(), step);
      if (off > MoveTuning.accuracy(MoveMode.AIR, this.arrivalCells())) {
         return null;
      } else {
         int head = top + this.fit.height() - 1;
         if (!this.inBounds(x, y, z) || !this.inBounds(x, head, z)) {
            return null;
         } else if (!this.walkEval.groundStandable(x, y, z)) {
            return null;
         } else {
            return this.fit.columnClear(x, y, head, z) ? new NavNode(x, top, z, MoveMode.AIR) : null;
         }
      }
   }

   private static int offLattice(int delta, int step) {
      int m = Math.floorMod(delta, step);
      return Math.min(m, step - m);
   }

   @Override
   public boolean segmentClear(NavNode from, NavNode to) {
      if (from.mode() == MoveMode.WATER && to.mode() == MoveMode.WATER) {
         return this.waterLattice != null && this.waterLattice.segmentClear(from, to);
      } else if (this.lattice == null) {
         return false;
      } else {
         return from.mode() == MoveMode.AIR && to.mode() == MoveMode.AIR ? this.lattice.segmentClear(from, to) : false;
      }
   }

   private boolean inBounds(int x, int y, int z) {
      return x >= this.min.getX()
         && x <= this.max.getX()
         && y >= this.min.getY()
         && y <= this.max.getY()
         && z >= this.min.getZ()
         && z <= this.max.getZ();
   }

   @Override
   public List<NavSolver.Neighbor> sameModeNeighbors(NavNode node) {
      int count;
      switch (node.mode()) {
         case AIR:
            if (this.lattice == null) {
               return List.of();
            }

            List<NavSolver.Neighbor> air = new ArrayList<>();

            for (NavSolver.Neighbor nb : this.lattice.neighbors(node)) {
               if (!this.inBounds(nb.to().x(), nb.to().y(), nb.to().z())) {
                  this.horizon = true;
               } else {
                  air.add(nb);
               }
            }

            return air;
         case WATER:
            if (this.swimEval == null) {
               return List.of();
            }

            if (this.waterLattice != null) {
               List<NavSolver.Neighbor> water = new ArrayList<>();

               for (NavSolver.Neighbor nb : this.waterLattice.neighbors(node)) {
                  if (!this.inBounds(nb.to().x(), nb.to().y(), nb.to().z())) {
                     this.horizon = true;
                  } else {
                     water.add(nb);
                  }
               }

               int surface = this.swimEval.getNeighbors(this.buffer, this.swimEval.nodeAt(node.x(), node.y(), node.z()));

               for (int i = 0; i < surface; i++) {
                  Node n = this.buffer[i];
                  if (!this.inBounds(n.x, n.y, n.z)) {
                     this.horizon = true;
                  } else {
                     water.add(new NavSolver.Neighbor(new NavNode(n.x, n.y, n.z, MoveMode.WATER), n.costMalus));
                  }
               }

               return water;
            }

            count = this.swimEval.getNeighbors(this.buffer, this.swimEval.nodeAt(node.x(), node.y(), node.z()));
            break;
         case GROUND:
            count = this.walkEval.getNeighbors(this.buffer, this.walkEval.nodeAt(node.x(), node.y(), node.z()));
            break;
         case CLIMB:
            return List.of();
         default:
            return List.of();
      }

      List<NavSolver.Neighbor> out = new ArrayList<>(count);

      for (int i = 0; i < count; i++) {
         Node nb = this.buffer[i];
         if (!BodyFit.isImpossible(nb.costMalus) && !(nb.costMalus < 0.0F)) {
            if (!this.inBounds(nb.x, nb.y, nb.z)) {
               this.horizon = true;
            } else {
               float malus = nb.costMalus;
               if (node.mode() == MoveMode.GROUND) {
                  malus += this.supportMalus(nb);
               }

               out.add(new NavSolver.Neighbor(new NavNode(nb.x, nb.y, nb.z, node.mode()), malus));
            }
         }
      }

      return out;
   }

   private float supportMalus(Node nb) {
      return this.fit.floorAt(nb.x, nb.y, nb.z) ? 0.0F : 2.5F;
   }

   @Nullable
   @Override
   public NavNode transitionTarget(NavNode node, ModeTransition transition) {
      return switch (transition) {
         case TAKEOFF -> this.takeoff(node);
         case LAND -> this.land(node);
         case WATER_ENTER -> this.waterEnter(node);
         case WATER_EXIT -> this.waterExit(node);
         case SPLASH_DOWN -> this.splashDown(node);
         case CLIMB_ON, CLIMB_OFF -> null;
      };
   }

   @Override
   public List<NavNode> transitionTargets(NavNode node, ModeTransition transition) {
      if (transition == ModeTransition.TAKEOFF && this.lattice != null) {
         NavNode here = this.takeoff(node);
         NavNode out = this.climbOut(node);
         if (out == null) {
            return here == null ? List.of() : List.of(here);
         } else {
            return here != null && this.lattice.besideDrop(node.x(), node.y(), node.z()) ? List.of(out, here) : List.of(out);
         }
      } else {
         return NavSolver.NeighborSource.super.transitionTargets(node, transition);
      }
   }

   @Nullable
   private NavNode takeoff(NavNode node) {
      int ceiling = node.y() + this.headroomTop;
      if (!this.inBounds(node.x(), ceiling, node.z())) {
         this.horizon = true;
         return null;
      } else {
         return !this.fit.columnClear(node.x(), node.y() + 1, ceiling, node.z()) ? null : new NavNode(node.x(), node.y(), node.z(), MoveMode.AIR);
      }
   }

   @Nullable
   private NavNode climbOut(NavNode node) {
      int top = node.y() + this.lattice.band();
      int head = top + this.fit.height() - 1;
      if (!this.inBounds(node.x(), head, node.z())) {
         this.horizon = true;
         return null;
      } else {
         return !this.fit.columnClear(node.x(), node.y() + 1, head, node.z()) ? null : new NavNode(node.x(), top, node.z(), MoveMode.AIR);
      }
   }

   @Nullable
   private NavNode land(NavNode node) {
      int floor = Math.max(this.region.getMinBuildHeight(), this.min.getY());

      for (int y = node.y() - 1; y >= floor; y--) {
         this.scan.set(node.x(), y, node.z());
         if (!this.region.getFluidState(this.scan).isEmpty()) {
            return null;
         }

         if (!this.region.getBlockState(this.scan).getCollisionShape(this.region, this.scan).isEmpty()) {
            int stand = y + 1;
            if (!this.fit.columnClear(node.x(), stand, node.y(), node.z())) {
               return null;
            }

            if (!this.walkEval.groundStandable(node.x(), stand, node.z())) {
               return null;
            }

            return new NavNode(node.x(), stand, node.z(), MoveMode.GROUND);
         }
      }

      if (floor > this.region.getMinBuildHeight()) {
         this.horizon = true;
      }

      return null;
   }

   @Nullable
   private NavNode waterEnter(NavNode node) {
      if (this.swimEval == null) {
         return null;
      } else {
         return !this.fit.fitsWet(node.x(), node.y(), node.z()) ? null : new NavNode(node.x(), node.y(), node.z(), MoveMode.WATER);
      }
   }

   @Nullable
   private NavNode waterExit(NavNode node) {
      for (Direction dir : Plane.HORIZONTAL) {
         int bx = node.x() + dir.getStepX();
         int bz = node.z() + dir.getStepZ();
         if (!this.inBounds(bx, node.y(), bz)) {
            this.horizon = true;
         } else {
            this.scan.set(bx, node.y(), bz);
            if (!this.region.getBlockState(this.scan).getCollisionShape(this.region, this.scan).isEmpty()) {
               int stand = node.y() + 1;
               if (this.walkEval.groundStandable(bx, stand, bz)) {
                  return new NavNode(bx, stand, bz, MoveMode.GROUND);
               }
            }
         }
      }

      return null;
   }

   @Nullable
   private NavNode splashDown(NavNode node) {
      if (this.swimEval == null) {
         return null;
      }

      for (int i = 0; i <= 8; i++) {
         int y = node.y() - i;
         if (!this.inBounds(node.x(), y, node.z())) {
            this.horizon = true;
            return null;
         }

         this.scan.set(node.x(), y, node.z());
         if (!this.region.getFluidState(this.scan).isEmpty()) {
            if (i > 0 && !this.fit.columnClear(node.x(), y + 1, node.y(), node.z())) {
               return null;
            }

            if (!this.fit.fitsWet(node.x(), y, node.z())) {
               return null;
            }

            return new NavNode(node.x(), y, node.z(), MoveMode.WATER);
         }
      }

      return null;
   }
}
