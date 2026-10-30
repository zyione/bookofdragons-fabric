package net.magister.liberanimalium.fauna.nature;

import net.magister.liberanimalium.fauna.behavior.Unstick;
import net.magister.liberanimalium.fauna.know.Places;

public final class UnstickHandoff implements Handoff {
   private final Unstick unstick;

   public UnstickHandoff(Unstick unstick) {
      this.unstick = unstick;
   }

   @Override
   public void brief(HandoffContext ctx) {
      this.unstick.noteFailure(ctx.routeFailed());
   }

   @Override
   public void debrief(HandoffContext ctx) {
      if (this.unstick.takeMark()) {
         ctx.knowledge().faculty(Places.class).ifPresent(p -> p.mark(ctx.handle().bodyPosition(), 1.0F, ctx.age()));
      }
   }
}
