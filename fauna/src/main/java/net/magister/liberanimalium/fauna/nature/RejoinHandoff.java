package net.magister.liberanimalium.fauna.nature;

import net.magister.liberanimalium.fauna.behavior.Rejoin;
import net.magister.liberanimalium.fauna.drive.PackDrive;

public final class RejoinHandoff implements Handoff {
   private final Rejoin rejoin;

   public RejoinHandoff(Rejoin rejoin) {
      this.rejoin = rejoin;
   }

   @Override
   public void brief(HandoffContext ctx) {
      this.rejoin.gatherAt(PackDrive.centroid(ctx.drives()), ctx.drives().flockRadius());
   }
}
