package net.magister.liberanimalium.fauna.nature;

import java.util.UUID;
import net.magister.liberanimalium.fauna.api.Check;
import net.magister.liberanimalium.fauna.behavior.Play;
import net.magister.liberanimalium.fauna.drive.PlayDrive;
import net.magister.liberanimalium.fauna.know.Knowledge;
import net.magister.liberanimalium.fauna.know.Representation;
import net.magister.liberanimalium.fauna.maneuver.Subject;

public final class PlayHandoff implements Handoff {
   private final Play play;

   public PlayHandoff(Play play) {
      this.play = play;
   }

   @Override
   public void brief(HandoffContext ctx) {
      Knowledge knowledge = ctx.knowledge();
      Representation mate = PlayDrive.partner(ctx.drives());
      Subject mateSubject = mate == null ? null : ctx.subjectOf(mate.id());
      boolean tag = mate != null
         && knowledge.courting(ctx.handle(), mate)
         && knowledge.courtship().playing(mate.id())
         && ctx.ladderFor().recognises(Check.Engagement.TAG);
      boolean rough = PlayDrive.playsRough(ctx.drives());
      boolean thing = mate != null && !mate.answers();
      boolean oneSided = !tag && (thing || mate != null && mate.isPlayer() && rough);
      this.play
         .romp(
            mate == null ? null : mate.id(),
            mate == null ? null : knowledge.estimate(mate, ctx.age(), ctx.handle().species()),
            mateSubject == null ? 0.0F : (float)mateSubject.girth(),
            tag,
            mate == null ? 0 : knowledge.courtship().catchesBy(mate.id()),
            mate == null ? 0 : knowledge.courtship().gamesBy(mate.id()),
            mate != null && mate.isPlayer(),
            oneSided,
            !tag && (thing || rough)
         );
   }

   @Override
   public void debrief(HandoffContext ctx) {
      this.collectRomp(ctx);
   }

   @Override
   public void collectYield(HandoffContext ctx) {
      this.collectRomp(ctx);
   }

   private void collectRomp(HandoffContext ctx) {
      UUID played = this.play.takeFinished();
      if (played != null && this.play.finishedPlayed()) {
         ctx.knowledge().notePlayedWith(played);
      }
   }
}
