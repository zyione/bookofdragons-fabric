package net.magister.liberanimalium.fauna.behavior;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.jetbrains.annotations.Nullable;
import net.magister.liberanimalium.fauna.api.AnimalHandle;
import net.magister.liberanimalium.fauna.api.Arms;
import net.magister.liberanimalium.fauna.api.PoseFlag;
import net.magister.liberanimalium.fauna.api.Relation;
import net.magister.liberanimalium.fauna.api.RelationType;
import net.magister.liberanimalium.fauna.core.PairSession;
import net.magister.liberanimalium.fauna.drive.SocialTuning;
import net.magister.liberanimalium.fauna.know.Bend;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

public final class Contest implements Behavior, Fighting, Laddered {
   @Nullable
   private UUID rivalId;
   @Nullable
   private Vec3 estimate;
   private float domGap;
   private float exchangeDeficit;
   private float standing;
   private final Flee withdrawal = new Flee();
   private Fighter self = Fighter.NONE;
   private Bout bout = Bout.NONE;
   private boolean rangedAllowed;
   private final Engage engage = new Engage();
   private Contest.Rung rung = Contest.Rung.IDLE;
   private long rungStart = Long.MIN_VALUE;
   private final Map<UUID, Long> shownAt = new HashMap<>();
   private boolean quietDisplay;
   private int hand = 1;
   @Nullable
   private Contest.Yield yield;
   @Nullable
   private UUID settledWith;
   private float healthAtStart = 1.0F;
   private int unseenTicks;
   @Nullable
   private UUID lostSightOf;
   @Nullable
   private UUID wonAgainst;

   public void confront(@Nullable UUID id, @Nullable Vec3 believedPosition, float domGap, float exchangeDeficit, boolean rangedAllowed, Fighter self, Bout bout) {
      this.confront(id, believedPosition, domGap, exchangeDeficit, rangedAllowed, self, bout, 0.0F);
   }

   public void confront(
      @Nullable UUID id, @Nullable Vec3 believedPosition, float domGap, float exchangeDeficit, boolean rangedAllowed, Fighter self, Bout bout, float standing
   ) {
      this.rivalId = id;
      this.estimate = believedPosition;
      this.domGap = domGap;
      this.exchangeDeficit = exchangeDeficit;
      this.rangedAllowed = rangedAllowed;
      this.self = self;
      this.bout = bout;
      this.standing = standing;
   }

   @Override
   public String rungName() {
      return this.rung.name();
   }

   public Contest.Rung rung() {
      return this.rung;
   }

   @Nullable
   public UUID rival() {
      return this.rivalId;
   }

   @Nullable
   public Contest.Yield takeYield() {
      Contest.Yield out = this.yield;
      this.yield = null;
      return out;
   }

   @Nullable
   public UUID takeLostSight() {
      UUID out = this.lostSightOf;
      this.lostSightOf = null;
      return out;
   }

   @Nullable
   public UUID takeWon() {
      UUID out = this.wonAgainst;
      this.wonAgainst = null;
      return out;
   }

   @Nullable
   public UUID takeSettled() {
      UUID out = this.settledWith;
      this.settledWith = null;
      return out;
   }

   @Override
   public void start(AnimalHandle handle, MoveVerbs move, long now) {
      this.engage.reset(handle);
      this.withdrawal.stop(handle, move, now);
      this.rung = Contest.Rung.IDLE;
      this.rungStart = Long.MIN_VALUE;
      this.healthAtStart = handle.healthFraction();
      this.yield = null;
      this.settledWith = null;
   }

   @Override
   public void tick(AnimalHandle handle, MoveVerbs move, long now) {
      if (this.rivalId != null && this.estimate != null) {
         long shared = handle.gameTime();
         PairSession session = PairSession.open(handle.faunaId(), this.rivalId, "territory_contest", shared);
         session.noteProgress(shared);
         this.hand = session.hand();
         if (this.rung == Contest.Rung.IDLE) {
            this.enter(Contest.Rung.DISPLAY, now, handle);
         }

         UUID leader = session.leader();
         if (leader != null && this.rung != Contest.Rung.SUBMIT) {
            if (leader.equals(handle.faunaId())) {
               this.won(handle, move, now);
               return;
            }

            this.enter(Contest.Rung.SUBMIT, now, handle);
         }

         long elapsed = this.rungStart == Long.MIN_VALUE ? 0L : now - this.rungStart;
         switch (this.rung) {
            case DISPLAY:
               this.display(handle, move, now, elapsed);
               break;
            case DUEL:
               this.duel(handle, move, now, elapsed);
               break;
            case SUBMIT:
               this.submit(handle, move, now, elapsed);
               break;
            default:
               this.idle(handle, move);
         }
      } else {
         this.idle(handle, move);
      }
   }

   private void display(AnimalHandle handle, MoveVerbs move, long now, long elapsed) {
      if (Duel.backsDown(this.domGap)) {
         this.concede(handle, move, now, Contest.Reason.BACKDOWN);
      } else if (elapsed >= 60L) {
         this.enter(Contest.Rung.DUEL, now, handle);
         this.duel(handle, move, now, 0L);
      } else {
         handle.setPose(PoseFlag.POSTURING, true);
         handle.applyLook(this.estimate);
         double ring = Mth.lerp(confidence(handle), 8.0, 4.0);
         move.display(this.rivalId, ring, Math.max(8.0, ring), 1.0F);
      }
   }

   private void duel(AnimalHandle handle, MoveVerbs move, long now, long elapsed) {
      handle.setPose(PoseFlag.POSTURING, false);
      if (Duel.yields(handle.healthFraction(), this.self.bravery(), this.exchangeDeficit)) {
         this.concede(handle, move, now, Contest.Reason.MORALE);
      } else {
         if (elapsed >= 6000L) {
            if (this.exchangeDeficit > 0.0F) {
               this.concede(handle, move, now, Contest.Reason.LAPSE);
               return;
            }

            if (elapsed >= 6040L) {
               this.lapse(handle, move, now);
               return;
            }
         }

         handle.applyLook(this.estimate);
         this.unseenTicks = this.engage.sawTarget() ? 0 : this.unseenTicks + 1;
         if (this.unseenTicks > 30) {
            this.lostSightOf = this.rivalId;
            this.idle(handle, move);
            this.rivalId = null;
            this.estimate = null;
            this.unseenTicks = 0;
         } else {
            double contact = Arms.of(handle).strikeRange();
            double halves = handle.clearanceTo(this.bout.girth());
            Engage.Weapon elected = this.engage
               .electWeapon(handle, this.rivalId, this.estimate, contact, false, this.rangedAllowed, this.self.bravery(), this.self.nervousness());
            float commit = this.engage
               .commitment(this.self.aggression(), this.self.bravery(), this.self.nervousness(), handle.healthFraction(), this.bout.observed());
            this.engage.tick(handle, this.rivalId, this.estimate, contact, elected, false, this.self, this.bout, now);
            this.engage.strike(handle, this.rivalId, this.estimate, elected, contact, this.self.energy());
            double[] band = this.engage.band(elected, contact, halves, commit);
            move.engage(this.rivalId, band[0], band[1], this.engage.pace(1.0F));
         }
      }
   }

   private void submit(AnimalHandle handle, MoveVerbs move, long now, long elapsed) {
      handle.setPose(PoseFlag.POSTURING, false);
      this.withdraw(handle, move, now);
      if (this.estimate.distanceTo(handle.bodyPosition()) >= SocialTuning.withdrawDistance() || elapsed >= 6000L) {
         this.finish(handle, move, now);
      }
   }

   private void won(AnimalHandle handle, MoveVerbs move, long now) {
      this.rung = Contest.Rung.WON;
      this.wonAgainst = this.rivalId;
      move.stop();
      this.finish(handle, move, now);
   }

   private void lapse(AnimalHandle handle, MoveVerbs move, long now) {
      if (this.rivalId == null) {
         this.finish(handle, move, now);
      } else if (this.domGap < 0.0F) {
         this.concede(handle, move, now, Contest.Reason.LAPSE);
      } else {
         PairSession session = PairSession.find(handle.faunaId(), this.rivalId);
         if (session == null) {
            this.won(handle, move, now);
         } else {
            session.setLeader(handle.faunaId());
         }
      }
   }

   private void concede(AnimalHandle handle, MoveVerbs move, long now, Contest.Reason reason) {
      if (this.rivalId != null) {
         PairSession session = PairSession.find(handle.faunaId(), this.rivalId);
         if (session != null) {
            session.setLeader(this.rivalId);
         }

         this.yield = new Contest.Yield(this.rivalId, Mth.clamp(this.healthAtStart - handle.healthFraction(), 0.0F, 1.0F), reason);
         this.withdraw(handle, move, now);
         this.enter(Contest.Rung.SUBMIT, now, handle);
      }
   }

   private void withdraw(AnimalHandle handle, MoveVerbs move, long now) {
      this.withdrawal.avoid(List.of(this.estimate), this.rivalId, this.defeat(handle));
      this.withdrawal.tick(handle, move, now);
   }

   private float defeat(AnimalHandle handle) {
      float lost = Mth.clamp(this.healthAtStart - handle.healthFraction(), 0.0F, 1.0F);
      return Bend.afterLostFight(new Relation(RelationType.RIVAL, this.standing), lost).intensity();
   }

   private void idle(AnimalHandle handle, MoveVerbs move) {
      if (this.rung != Contest.Rung.IDLE) {
         handle.setPose(PoseFlag.POSTURING, false);
         this.rung = Contest.Rung.IDLE;
      }
   }

   private void finish(AnimalHandle handle, MoveVerbs move, long now) {
      this.engage.reset(handle);
      this.withdrawal.stop(handle, move, now);
      if (this.rivalId != null) {
         this.settledWith = this.rivalId;
         PairSession.noteSettled(handle.faunaId(), this.rivalId, "territory_contest", handle.gameTime());
         PairSession.close(handle.faunaId(), this.rivalId);
      }

      handle.setPose(PoseFlag.POSTURING, false);
      this.rivalId = null;
      this.estimate = null;
      this.rung = Contest.Rung.IDLE;
      this.rungStart = Long.MIN_VALUE;
   }

   private void enter(Contest.Rung next, long now, AnimalHandle handle) {
      this.rung = next;
      this.rungStart = now;
      if (next == Contest.Rung.DISPLAY) {
         this.healthAtStart = handle.healthFraction();
         long time = handle.gameTime();
         this.shownAt.values().removeIf(at -> time - at >= 6000L);
         this.quietDisplay = this.shownAt.containsKey(this.rivalId);
         this.shownAt.put(this.rivalId, time);
      }
   }

   @Override
   public boolean quietRung() {
      return this.rung == Contest.Rung.DISPLAY && this.quietDisplay;
   }

   @Override
   public void stop(AnimalHandle handle, MoveVerbs move, long now) {
      this.engage.reset(handle);
      this.withdrawal.stop(handle, move, now);
      if (this.rivalId != null) {
         float lost = Mth.clamp(this.healthAtStart - handle.healthFraction(), 0.0F, 1.0F);
         boolean calledOff = PairSession.settledRecently(handle.faunaId(), this.rivalId, "territory_contest", handle.gameTime(), 1200L);
         if (this.rung == Contest.Rung.DUEL && lost > 0.0F && !calledOff) {
            this.yield = new Contest.Yield(this.rivalId, lost, Contest.Reason.PREEMPTED);
            PairSession session = PairSession.find(handle.faunaId(), this.rivalId);
            if (session != null) {
               session.setLeader(this.rivalId);
            }
         }

         PairSession.close(handle.faunaId(), this.rivalId);
      }

      handle.setPose(PoseFlag.POSTURING, false);
      this.rivalId = null;
      this.estimate = null;
      this.rung = Contest.Rung.IDLE;
      this.rungStart = Long.MIN_VALUE;
      this.domGap = 0.0F;
      this.exchangeDeficit = 0.0F;
      this.standing = 0.0F;
      this.unseenTicks = 0;
   }

   @Override
   public Engage engagement() {
      return this.engage;
   }

   @Nullable
   @Override
   public UUID engagedWith() {
      return this.rivalId;
   }

   @Override
   public boolean armed() {
      return this.rangedAllowed;
   }

   public int hand() {
      return this.hand;
   }

   private static float confidence(AnimalHandle handle) {
      return handle.healthFraction();
   }

   public enum Reason {
      MORALE,
      BACKDOWN,
      LAPSE,
      PREEMPTED;
   }

   public enum Rung {
      IDLE,
      DISPLAY,
      DUEL,
      SUBMIT,
      WON;
   }

   public record Yield(UUID rivalId, float fear, Contest.Reason reason) {
   }
}
