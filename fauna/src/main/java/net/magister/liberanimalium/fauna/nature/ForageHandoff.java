package net.magister.liberanimalium.fauna.nature;

import java.util.UUID;
import org.jetbrains.annotations.Nullable;
import net.magister.liberanimalium.fauna.behavior.Forage;
import net.magister.liberanimalium.fauna.drive.ForageDrive;
import net.magister.liberanimalium.fauna.know.Larder;

public final class ForageHandoff implements Handoff {
   private final Forage forage;
   @Nullable
   private UUID morselTarget;

   public ForageHandoff(Forage forage) {
      this.forage = forage;
   }

   @Override
   public void brief(HandoffContext ctx) {
      Larder.Morsel morsel = ForageDrive.bestMorsel(ctx.drives(), this.morselTarget);
      this.morselTarget = morsel == null ? null : morsel.id();
      this.forage
         .fetch(
            this.morselTarget,
            morsel == null ? null : morsel.at(),
            morsel == null ? null : morsel.pileAt(),
            morsel != null && morsel.edible(),
            morsel != null && !morsel.edible() && morsel.craving() > 0.12F
         );
      this.forage.noteFailure(ctx.routeFailed());
   }

   @Override
   public void debrief(HandoffContext ctx) {
      UUID refused = this.forage.takeRefused();
      if (refused != null) {
         ctx.knowledge().noteSnubbed(refused, ctx.age());
         if (refused.equals(this.morselTarget)) {
            this.morselTarget = null;
         }
      }
   }
}
