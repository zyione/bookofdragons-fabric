package net.magister.liberanimalium.body.nav.pathfinding;

import org.jetbrains.annotations.Nullable;
import net.magister.liberanimalium.compat.Entities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.BlockPos.MutableBlockPos;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.PathNavigationRegion;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.pathfinder.BlockPathTypes;
import net.minecraft.world.level.pathfinder.Node;
import net.minecraft.world.level.pathfinder.WalkNodeEvaluator;
import net.minecraft.world.phys.Vec3;

public class FaunaWalkNodeEvaluator extends WalkNodeEvaluator {
   private static final int MAX_GROUND_DROP = 4;
   public static final double JUMP_SLACK = 0.125;
   private double hopHeight;
   private double hopLift;
   private int hopLimit;
   private boolean climbing;
   private int footprintRadius;
   @Nullable
   private Vec3 startAt;

   public void setHopHeight(double blocks) {
      this.hopHeight = Math.max(0.0, blocks);
   }

   static double hopLift(double hopHeight, double stepHeight) {
      double gate = Math.max(1.125, stepHeight);
      return Math.max(0.0, hopHeight + 0.125 - gate);
   }

   static boolean passesJumpGate(double to, double from, double lift, double stepHeight) {
      return to - (from + lift) <= Math.max(1.125, stepHeight);
   }

   private void prepareHop(double stepHeight) {
      this.hopLift = hopLift(this.hopHeight, stepHeight);
      this.hopLimit = Mth.floor(this.hopHeight);
      this.climbing = false;
   }

   public void prepare(PathNavigationRegion level, Mob mob) {
      super.prepare(level, mob);
      this.footprintRadius = BodyFit.radiusCells(mob.getBbWidth());
      this.entityWidth = 2 * this.footprintRadius + 1;
      this.entityDepth = this.entityWidth;
      this.entityHeight = Mth.ceil(mob.getBbHeight());
      this.prepareHop(Entities.stepHeight(mob));
   }

   public BlockPathTypes getBlockPathType(BlockGetter level, int x, int y, int z, Mob mob) {
      return super.getBlockPathType(level, x - this.footprintRadius, y, z - this.footprintRadius, mob);
   }

   public void startAt(@Nullable Vec3 at) {
      this.startAt = at;
   }

   private Vec3 from() {
      return this.startAt != null ? this.startAt : this.mob.position();
   }

   public Node getStart() {
      Vec3 at = this.from();
      int y;
      if (this.canFloat() && this.mob.isInWater()) {
         y = Mth.floor(at.y);
         MutableBlockPos mutable = new MutableBlockPos();
         BlockState state = this.level.getBlockState(mutable.set(at.x, y, at.z));

         while (state.getFluidState().is(FluidTags.WATER)) {
            state = this.level.getBlockState(mutable.set(at.x, ++y, at.z));
         }

         y--;
      } else if (this.mob.onGround()) {
         y = Mth.floor(at.y + 0.5);
      } else {
         BlockPos scan = BlockPos.containing(at);

         while (scan.getY() > this.mob.level().getMinBuildHeight() && this.level.getBlockState(scan).getCollisionShape(this.level, scan).isEmpty()) {
            scan = scan.below();
         }

         y = scan.above().getY();
      }

      return this.getStartNode(new BlockPos(Mth.floor(at.x), y, Mth.floor(at.z)));
   }

   public Node nodeAt(int x, int y, int z) {
      return this.getNode(x, y, z);
   }

   public boolean groundStandable(int x, int y, int z) {
      BlockPathTypes type = this.getCachedBlockType(this.mob, x, y, z);
      return this.mob.getPathfindingMalus(type) >= 0.0F && type != BlockPathTypes.OPEN;
   }

   @Nullable
   protected Node findAcceptedNode(int pX, int pY, int pZ, int pVerticalDeltaLimit, double pNodeFloorLevel, Direction pDirection, BlockPathTypes pPathType) {
      if (this.hopLift > 0.0 && !this.climbing && pVerticalDeltaLimit > 0) {
         this.climbing = true;

         try {
            return this.acceptNode(pX, pY, pZ, Math.max(pVerticalDeltaLimit, this.hopLimit), pNodeFloorLevel + this.hopLift, pDirection, pPathType);
         } finally {
            this.climbing = false;
         }
      } else {
         return this.acceptNode(pX, pY, pZ, pVerticalDeltaLimit, pNodeFloorLevel, pDirection, pPathType);
      }
   }

   @Nullable
   private Node acceptNode(int pX, int pY, int pZ, int pVerticalDeltaLimit, double pNodeFloorLevel, Direction pDirection, BlockPathTypes pPathType) {
      if (pPathType == BlockPathTypes.FENCE) {
         pPathType = BlockPathTypes.BLOCKED;
      }

      Node node = super.findAcceptedNode(pX, pY, pZ, pVerticalDeltaLimit, pNodeFloorLevel, pDirection, pPathType);
      if (node != null && node.type != BlockPathTypes.BLOCKED && pY - node.y > 4) {
         return null;
      } else {
         return node != null && refusesDoorway(node.type, this.mob.getBbWidth()) ? null : node;
      }
   }

   static boolean refusesDoorway(BlockPathTypes type, double width) {
      return (type == BlockPathTypes.WALKABLE_DOOR || type == BlockPathTypes.DOOR_OPEN) && !BodyFit.passesDoorway(width);
   }
}
