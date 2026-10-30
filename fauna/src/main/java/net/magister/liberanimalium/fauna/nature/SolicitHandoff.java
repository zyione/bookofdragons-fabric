package net.magister.liberanimalium.fauna.nature;

import java.util.UUID;
import org.jetbrains.annotations.Nullable;
import net.magister.liberanimalium.fauna.api.Halter;
import net.magister.liberanimalium.fauna.behavior.Solicit;
import net.magister.liberanimalium.fauna.drive.BegDrive;
import net.magister.liberanimalium.fauna.know.Knowledge;
import net.magister.liberanimalium.fauna.know.Larder;
import net.magister.liberanimalium.fauna.know.Representation;

public final class SolicitHandoff implements Handoff {
   private final Solicit solicit;
   @Nullable
   private UUID offerTarget;

   public SolicitHandoff(Solicit solicit) {
      this.solicit = solicit;
   }

   @Override
   public void brief(HandoffContext ctx) {
      Knowledge knowledge = ctx.knowledge();
      Larder.Offer offer = BegDrive.bestOffer(ctx.drives(), this.offerTarget);
      this.offerTarget = offer == null ? null : offer.holder();
      Representation holder = this.offerTarget == null ? null : knowledge.about(this.offerTarget);
      this.solicit
         .ask(
            holder == null ? null : holder.id(),
            holder == null ? null : knowledge.estimate(holder, ctx.age(), ctx.handle().species()),
            holder == null ? 0.0 : ctx.standoffFor(holder),
            holder != null && knowledge.courting(ctx.handle(), holder) && knowledge.courtship().atMount(holder.id(), Halter.of(ctx.handle()).rideable())
         );
   }

   @Nullable
   @Override
   public UUID subject() {
      return this.offerTarget;
   }

   @Override
   public void forget(UUID id) {
      if (id.equals(this.offerTarget)) {
         this.offerTarget = null;
      }
   }
}
