package net.magister.liberanimalium.fauna.nature;

import net.magister.liberanimalium.fauna.api.AnimalHandle;
import net.magister.liberanimalium.fauna.behavior.Wander;
import net.magister.liberanimalium.fauna.know.Herd;
import net.magister.liberanimalium.fauna.know.Representation;
import net.magister.liberanimalium.fauna.move.MoveTuning;

public final class WanderHandoff implements Handoff {
   private final Wander wander;

   public WanderHandoff(Wander wander) {
      this.wander = wander;
   }

   @Override
   public void brief(HandoffContext ctx) {
      AnimalHandle handle = ctx.handle();
      Iterable<Representation> known = ctx.knowledge().all();
      this.wander
         .configure(
            ctx.regard(),
            ctx.ambientRange(),
            ctx.aloft(),
            MoveTuning.skyClearance(handle.bounds().getXsize()),
            MoveTuning.cruiseCeiling(handle.bounds().getXsize()),
            ctx.withinReach(),
            Herd.heading(known, ctx.age(), 0.05),
            Herd.movingShare(known, ctx.age(), 0.05)
         );
   }
}
