package net.magister.liberanimalium.fauna.know;

import java.util.UUID;
import org.jetbrains.annotations.Nullable;
import net.magister.liberanimalium.fauna.api.Beckoned;
import net.magister.liberanimalium.fauna.api.Halter;

public final class Beckons implements Faculty {
   @Nullable
   private UUID invitedBy;
   private long invitedAt = Long.MIN_VALUE;
   private boolean wasPerched;

   @Override
   public String id() {
      return "beckons";
   }

   @Override
   public void integrate(FacultyContext ctx) {
      boolean perched = Halter.of(ctx.handle()).perched();

      for (Beckoned beckoned : ctx.stimuli(Beckoned.class)) {
         if (Halter.of(ctx.handle()).ownedBy(beckoned.by())) {
            if (this.invitedBy == null && !perched) {
               this.invitedBy = beckoned.by();
               this.invitedAt = ctx.now();
            } else {
               this.invitedBy = null;
            }
         }
      }

      if (this.wasPerched && !perched) {
         this.invitedBy = null;
      }

      if (this.invitedBy != null && !perched && ctx.now() - this.invitedAt > 200L) {
         this.invitedBy = null;
      }

      this.wasPerched = perched;
   }

   @Nullable
   public UUID invitation() {
      return this.invitedBy;
   }

   public void declined() {
      this.invitedBy = null;
   }
}
