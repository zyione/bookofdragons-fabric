package net.magister.liberanimalium.fauna.nature;

import java.util.UUID;
import net.magister.liberanimalium.fauna.api.AnimalHandle;
import net.magister.liberanimalium.fauna.behavior.Shoo;
import net.magister.liberanimalium.fauna.drive.DriveContext;
import net.magister.liberanimalium.fauna.drive.SpaceDrive;
import net.magister.liberanimalium.fauna.identity.Trait;
import net.magister.liberanimalium.fauna.know.Crowding;
import net.magister.liberanimalium.fauna.know.Representation;

public final class ShooHandoff implements Handoff {
   private final Shoo shoo;

   public ShooHandoff(Shoo shoo) {
      this.shoo = shoo;
   }

   @Override
   public void brief(HandoffContext ctx) {
      AnimalHandle handle = ctx.handle();
      DriveContext context = ctx.drives();
      Representation crowder = SpaceDrive.worstCrowder(context);
      this.shoo
         .objectTo(
            crowder == null ? null : crowder.id(),
            crowder == null ? null : ctx.knowledge().estimate(crowder, ctx.age(), handle.species()),
            crowder == null ? 0.0F : crowder.crowding(),
            SpaceDrive.threshold(context),
            Crowding.space(handle.bounds().getXsize(), context.trait(Trait.SYMPATHY), context.trait(Trait.NERVOUSNESS)),
            crowder != null && crowder.sameSpecies()
         );
   }

   @Override
   public void debrief(HandoffContext ctx) {
      UUID rebuffed = this.shoo.rebuffed();
      if (rebuffed != null) {
         ctx.knowledge().noteRebuffed(rebuffed);
      }
   }
}
