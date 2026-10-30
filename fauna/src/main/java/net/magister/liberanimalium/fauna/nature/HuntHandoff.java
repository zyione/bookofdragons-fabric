package net.magister.liberanimalium.fauna.nature;

import java.util.UUID;
import org.jetbrains.annotations.Nullable;
import net.magister.liberanimalium.fauna.behavior.Hunt;
import net.magister.liberanimalium.fauna.drive.PreyDrive;
import net.magister.liberanimalium.fauna.know.Representation;

public final class HuntHandoff implements Handoff {
   private final Hunt hunt;
   @Nullable
   private Representation huntTarget;

   public HuntHandoff(Hunt hunt) {
      this.hunt = hunt;
   }

   @Override
   public void brief(HandoffContext ctx) {
      this.huntTarget = PreyDrive.bestPrey(ctx.drives(), this.huntTarget);
      this.hunt
         .pursue(
            this.huntTarget == null ? null : this.huntTarget.id(),
            this.huntTarget == null ? null : ctx.knowledge().estimate(this.huntTarget, ctx.age(), ctx.handle().species()),
            ctx.fighter(),
            ctx.boutAgainst(this.huntTarget)
         );
      this.hunt.patrolWithin(ctx.withinReach());
   }

   @Nullable
   @Override
   public UUID subject() {
      return this.huntTarget == null ? null : this.huntTarget.id();
   }

   @Override
   public void forget(UUID id) {
      if (this.huntTarget != null && this.huntTarget.id().equals(id)) {
         this.huntTarget = null;
      }
   }
}
