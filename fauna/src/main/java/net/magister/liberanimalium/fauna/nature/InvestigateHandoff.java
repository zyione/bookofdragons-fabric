package net.magister.liberanimalium.fauna.nature;

import net.magister.liberanimalium.fauna.behavior.Investigate;
import net.magister.liberanimalium.fauna.know.Noises;

public final class InvestigateHandoff implements Handoff {
   private final Investigate investigate;

   public InvestigateHandoff(Investigate investigate) {
      this.investigate = investigate;
   }

   @Override
   public void brief(HandoffContext ctx) {
      this.investigate.lookAt(ctx.knowledge().faculty(Noises.class).map(Noises::point).orElse(null));
   }

   @Override
   public void debrief(HandoffContext ctx) {
      if (this.investigate.takeLooked()) {
         ctx.knowledge().faculty(Noises.class).ifPresent(Noises::cleared);
      }
   }
}
