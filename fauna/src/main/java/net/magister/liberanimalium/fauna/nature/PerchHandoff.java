package net.magister.liberanimalium.fauna.nature;

import java.util.UUID;
import net.magister.liberanimalium.fauna.behavior.Perch;
import net.magister.liberanimalium.fauna.drive.DriveContext;
import net.magister.liberanimalium.fauna.know.Beckons;

public final class PerchHandoff implements Handoff {
   private final Perch perch;

   public PerchHandoff(Perch perch) {
      this.perch = perch;
   }

   @Override
   public void brief(HandoffContext ctx) {
      DriveContext drives = ctx.drives();
      UUID owner = drives.perchInvitation();
      this.perch.onto(owner, owner == null ? null : drives.whereIs(owner));
   }

   @Override
   public void debrief(HandoffContext ctx) {
      if (this.perch.takeGaveUp()) {
         ctx.knowledge().faculty(Beckons.class).ifPresent(Beckons::declined);
      }
   }
}
