package net.magister.liberanimalium.body.nav.pathfinding;

import org.jetbrains.annotations.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.PathNavigationRegion;
import net.minecraft.world.level.pathfinder.FlyNodeEvaluator;
import net.minecraft.world.level.pathfinder.Node;
import net.minecraft.world.phys.Vec3;

public class FaunaFlyNodeEvaluator extends FlyNodeEvaluator {
   @Nullable
   private Vec3 startAt;

   public void prepare(PathNavigationRegion level, Mob mob) {
      super.prepare(level, mob);
      this.entityWidth = Mth.ceil(mob.getBbWidth()) + 1;
      this.entityHeight = Mth.ceil(mob.getBbHeight()) + 1;
      this.entityDepth = Mth.ceil(mob.getBbWidth()) + 1;
   }

   public void startAt(@Nullable Vec3 at) {
      this.startAt = at;
   }

   private Vec3 from() {
      return this.startAt != null ? this.startAt : this.mob.position();
   }

   public Node getStart() {
      Vec3 at = this.from();
      int y = Mth.floor(at.y + 0.5);
      return this.getStartNode(new BlockPos(Mth.floor(at.x), y, Mth.floor(at.z)));
   }

   public Node nodeAt(int x, int y, int z) {
      return this.getNode(x, y, z);
   }
}
