package net.magister.liberanimalium.fauna.nature;

import net.magister.liberanimalium.fauna.behavior.Spar;
import net.magister.liberanimalium.fauna.drive.SparDrive;
import net.magister.liberanimalium.fauna.know.Knowledge;
import net.magister.liberanimalium.fauna.know.Representation;

public final class SparHandoff implements Handoff {
   private final Spar spar;

   public SparHandoff(Spar spar) {
      this.spar = spar;
   }

   @Override
   public void brief(HandoffContext ctx) {
      Knowledge knowledge = ctx.knowledge();
      Representation mate = SparDrive.partner(ctx.drives());
      this.spar
         .bout(
            mate == null ? null : mate.id(),
            mate == null ? null : knowledge.estimate(mate, ctx.age(), ctx.handle().species()),
            mate == null ? 0 : knowledge.courtship().hitsBy(mate.id()),
            ctx.fighter(),
            ctx.boutAgainst(mate)
         );
   }
}
