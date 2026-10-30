package net.magister.liberanimalium.fauna.nature;

import java.util.UUID;
import org.jetbrains.annotations.Nullable;
import net.magister.liberanimalium.fauna.behavior.Hunt;
import net.magister.liberanimalium.fauna.drive.EnmityDrive;
import net.magister.liberanimalium.fauna.know.Representation;

public final class FightHandoff implements Handoff {
   private final Hunt fight;
   @Nullable
   private Representation enemyTarget;

   public FightHandoff(Hunt fight) {
      this.fight = fight;
   }

   @Override
   public void brief(HandoffContext ctx) {
      this.enemyTarget = EnmityDrive.worstEnemy(ctx.drives(), this.enemyTarget);
      this.fight
         .pursue(
            this.enemyTarget == null ? null : this.enemyTarget.id(),
            this.enemyTarget == null ? null : ctx.knowledge().estimate(this.enemyTarget, ctx.age(), ctx.handle().species()),
            ctx.fighter(),
            ctx.boutAgainst(this.enemyTarget),
            false
         );
      this.fight.contact(this.enemyTarget == null || this.enemyTarget.visible() || this.enemyTarget.age(ctx.age()) < 80L);
   }

   @Nullable
   @Override
   public UUID subject() {
      return this.enemyTarget == null ? null : this.enemyTarget.id();
   }

   @Override
   public void forget(UUID id) {
      if (this.enemyTarget != null && this.enemyTarget.id().equals(id)) {
         this.enemyTarget = null;
      }
   }
}
