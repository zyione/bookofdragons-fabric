package net.magister.liberanimalium.fauna.nature;

import net.magister.liberanimalium.fauna.api.MoveMode;
import net.magister.liberanimalium.fauna.behavior.Buck;
import net.magister.liberanimalium.fauna.identity.Trait;

public final class BuckHandoff implements Handoff {
   private final Buck buck;

   public BuckHandoff(Buck buck) {
      this.buck = buck;
   }

   @Override
   public void brief(HandoffContext ctx) {
      this.buck.ridden(ctx.drives().trait(Trait.ENERGY), ctx.handle().species().movement().allows(MoveMode.AIR));
   }
}
