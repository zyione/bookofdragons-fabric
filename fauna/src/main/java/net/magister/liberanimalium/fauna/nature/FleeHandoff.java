package net.magister.liberanimalium.fauna.nature;

import net.magister.liberanimalium.fauna.behavior.Flee;
import net.magister.liberanimalium.fauna.drive.ThreatDrive;

public final class FleeHandoff implements Handoff {
   private final Flee flee;

   public FleeHandoff(Flee flee) {
      this.flee = flee;
   }

   @Override
   public void brief(HandoffContext ctx) {
      this.flee.avoid(ThreatDrive.threatPoints(ctx.drives()), ThreatDrive.leadThreat(ctx.drives()));
   }
}
