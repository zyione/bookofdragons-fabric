package net.magister.liberanimalium.fauna.nature;

import java.util.UUID;
import net.magister.liberanimalium.fauna.api.AnimalHandle;
import net.magister.liberanimalium.fauna.api.Halter;
import net.magister.liberanimalium.fauna.behavior.Contest;
import net.magister.liberanimalium.fauna.behavior.Duel;
import net.magister.liberanimalium.fauna.core.PairSession;
import net.magister.liberanimalium.fauna.drive.DriveContext;
import net.magister.liberanimalium.fauna.drive.RivalDrive;
import net.magister.liberanimalium.fauna.drive.SocialTuning;
import net.magister.liberanimalium.fauna.identity.Trait;
import net.magister.liberanimalium.fauna.know.Knowledge;
import net.magister.liberanimalium.fauna.know.Representation;
import net.magister.liberanimalium.fauna.know.Territory;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

public final class ContestHandoff implements Handoff {
   private final Contest contest;

   public ContestHandoff(Contest contest) {
      this.contest = contest;
   }

   @Override
   public void brief(HandoffContext ctx) {
      Knowledge knowledge = ctx.knowledge();
      AnimalHandle handle = ctx.handle();
      long age = ctx.age();
      Representation rival = RivalDrive.worstRival(ctx.drives());
      boolean housemate = rival != null && Knowledge.household(handle, rival);
      this.contest
         .confront(
            rival == null ? null : rival.id(),
            rival == null ? null : knowledge.estimate(rival, age, handle.species()),
            rival == null ? 0.0F : dominanceGap(ctx, rival),
            rival == null ? 0.0F : this.exchangeDeficit(),
            rival != null && rival.bent(),
            ctx.fighter(),
            ctx.boutAgainst(rival),
            rival != null && !housemate ? rival.relation().intensity() : 0.0F
         );
   }

   @Override
   public void debrief(HandoffContext ctx) {
      Knowledge knowledge = ctx.knowledge();
      long age = ctx.age();
      UUID lost = this.contest.takeLostSight();
      if (lost != null) {
         knowledge.noteUnreachable(lost, age);
      }
   }

   @Override
   public void collectYield(HandoffContext ctx) {
      Knowledge knowledge = ctx.knowledge();
      AnimalHandle handle = ctx.handle();
      DriveContext context = ctx.drives();
      long age = ctx.age();
      Contest.Yield yield = this.contest.takeYield();
      if (yield != null && knowledge.of(yield.rivalId()) != null) {
         knowledge.noteLostFight(yield.rivalId(), yield.fear());
      }

      if (yield != null && knowledge.of(yield.rivalId()) != null && !Knowledge.household(handle, knowledge.of(yield.rivalId()))) {
         Representation winnerRep = knowledge.of(yield.rivalId());
         Vec3 winnerCenter = winnerRep != null ? knowledge.estimate(winnerRep, age, handle.species()) : handle.bodyPosition();
         float winnerRadius = handle.species().social().territoryRadius();
         float bravery = context.trait(Trait.BRAVERY);
         long duration = (long)Mth.lerp(bravery, 168000.0F, 72000.0F);
         knowledge.faculty(Territory.class).ifPresent(t -> t.setExile(winnerCenter, winnerRadius, age + duration));
      }

      UUID beaten = this.contest.takeWon();
      if (beaten != null) {
         Halter.of(handle).wonAgainst(beaten);
      }

      UUID settled = this.contest.takeSettled();
      if (settled != null) {
         knowledge.noteSettledWith(settled);
      }
   }

   private static float dominanceGap(HandoffContext ctx, Representation rival) {
      AnimalHandle handle = ctx.handle();
      DriveContext context = ctx.drives();
      float ownSize = (float)(handle.bounds().getXsize() * handle.bounds().getYsize());
      float theirSize = rival.apparentSize();
      float ratio = theirSize <= 0.0F ? 1.0F : ownSize / theirSize;
      float mine = SocialTuning.dominanceScore(context.trait(Trait.DOMINANCE), ratio, context.healthFraction());
      float theirs = SocialTuning.dominanceScore(0.5F, ratio <= 0.0F ? 1.0F : 1.0F / ratio, 0.5F);
      PairSession session = PairSession.find(handle.faunaId(), rival.id());
      long seed = session == null ? handle.gameTime() : session.startedTick();
      return mine - theirs + Duel.orientedJitter(handle.faunaId(), rival.id(), seed);
   }

   private float exchangeDeficit() {
      return this.contest.engagement().frustration();
   }
}
