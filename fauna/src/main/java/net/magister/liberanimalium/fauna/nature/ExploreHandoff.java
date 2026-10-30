package net.magister.liberanimalium.fauna.nature;

import java.util.function.Predicate;
import net.magister.liberanimalium.fauna.api.AnimalHandle;
import net.magister.liberanimalium.fauna.behavior.Explore;
import net.magister.liberanimalium.fauna.know.Herd;
import net.magister.liberanimalium.fauna.know.Representation;
import net.magister.liberanimalium.fauna.know.Territory;
import net.magister.liberanimalium.fauna.move.MoveTuning;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.Vec3;

public final class ExploreHandoff implements Handoff {
   private final Explore explore;

   public ExploreHandoff(Explore explore) {
      this.explore = explore;
   }

   @Override
   public void brief(HandoffContext ctx) {
      AnimalHandle handle = ctx.handle();
      long age = ctx.age();
      Territory terr = ctx.knowledge().faculty(Territory.class).orElse(null);
      Iterable<Representation> known = ctx.knowledge().all();
      Vec3 herd = Herd.heading(known, age, 0.05);
      float herdMoving = Herd.movingShare(known, age, 0.05);
      Vec3 bias = herd;
      if (terr == null) {
         this.explore
            .configure(
               herd,
               ctx.withinReach(),
               ctx.regard(),
               ctx.ambientRange(),
               ctx.aloft(),
               MoveTuning.skyClearance(handle.bounds().getXsize()),
               MoveTuning.cruiseCeiling(handle.bounds().getXsize()),
               herdMoving
            );
      } else {
         if (terr.homeCenter() != null && terr.radius() > 0.0F) {
            double dist = handle.bodyPosition().distanceTo(terr.homeCenter());
            if (dist > terr.radius() * 0.7) {
               bias = terr.homeCenter().subtract(handle.bodyPosition());
            } else if (herd == null) {
               bias = ctx.patrolHeading(terr.homeCenter());
            }
         } else if (terr.avoidedCenter() != null && terr.isExileActive(age)) {
            bias = handle.bodyPosition().subtract(terr.avoidedCenter());
         }

         Predicate<BlockPos> filter = terr.isExileActive(age) ? pos -> !terr.isExiledFrom(Vec3.atCenterOf(pos), age) : null;
         Predicate<BlockPos> fence = ctx.withinReach();
         if (fence != null) {
            filter = filter == null ? fence : filter.and(fence);
         }

         this.explore
            .configure(
               bias,
               filter,
               ctx.regard(),
               ctx.ambientRange(),
               ctx.aloft(),
               MoveTuning.skyClearance(handle.bounds().getXsize()),
               MoveTuning.cruiseCeiling(handle.bounds().getXsize()),
               herdMoving
            );
      }
   }
}
