package net.magister.liberanimalium.fauna.nature;

import net.magister.liberanimalium.fauna.behavior.Warn;
import net.magister.liberanimalium.fauna.drive.WarnDrive;
import net.magister.liberanimalium.fauna.know.Representation;

public final class WarnHandoff implements Handoff {
   private final Warn warn;

   public WarnHandoff(Warn warn) {
      this.warn = warn;
   }

   @Override
   public void brief(HandoffContext ctx) {
      Representation subject = WarnDrive.subject(ctx.drives());
      this.warn.faceUp(subject == null ? null : subject.id(), subject == null ? null : ctx.knowledge().estimate(subject, ctx.age(), ctx.handle().species()));
   }
}
