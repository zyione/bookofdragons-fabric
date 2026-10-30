package net.magister.liberanimalium.fauna.behavior;

import java.util.UUID;
import org.jetbrains.annotations.Nullable;
import net.magister.liberanimalium.fauna.api.AnimalHandle;
import net.magister.liberanimalium.fauna.api.Arms;
import net.magister.liberanimalium.fauna.core.PairSession;
import net.minecraft.world.phys.Vec3;

public final class Play implements Behavior {
   @Nullable
   private UUID partnerId;
   @Nullable
   private Vec3 estimate;
   private float partnerGirth;
   private Play.Phase phase = Play.Phase.IDLE;
   private long phaseStart = Long.MIN_VALUE;
   private long boutStart = Long.MIN_VALUE;
   private boolean tagArmed;
   private boolean person;
   private boolean personArmed;
   private boolean wasLeader;
   private boolean oneSided;
   private boolean shoves;
   private boolean backing;
   private long swungAt = Long.MIN_VALUE;
   private boolean batted;
   private boolean tagGame;
   private int catches;
   private int seenCatches;
   private int games;
   private int seenGames;
   private long waitSince = Long.MIN_VALUE;
   private int teaseHops;
   private double hopTarget = Double.NaN;
   private long lookUntil = Long.MIN_VALUE;
   private boolean legging;
   private boolean caughtHere;
   private boolean tagBout;
   @Nullable
   private UUID finishedWith;
   private boolean finishedPlayed;

   public void romp(@Nullable UUID id, @Nullable Vec3 believedPosition, float girth, boolean tag, int catches) {
      this.romp(id, believedPosition, girth, tag, catches, false);
   }

   public void romp(@Nullable UUID id, @Nullable Vec3 believedPosition, float girth, boolean tag, int catches, boolean person) {
      this.romp(id, believedPosition, girth, tag, catches, person, false, false);
   }

   public void romp(@Nullable UUID id, @Nullable Vec3 believedPosition, float girth, boolean tag, int catches, boolean person, boolean oneSided, boolean shoves) {
      this.romp(id, believedPosition, girth, tag, catches, this.games, person, oneSided, shoves);
   }

   public void romp(
      @Nullable UUID id, @Nullable Vec3 believedPosition, float girth, boolean tag, int catches, int games, boolean person, boolean oneSided, boolean shoves
   ) {
      this.games = games;
      this.oneSided = oneSided;
      this.shoves = shoves;
      this.person = person;
      this.partnerId = id;
      this.estimate = believedPosition;
      this.partnerGirth = girth;
      this.tagGame = tag;
      this.catches = catches;
   }

   public Play.Phase phase() {
      return this.phase;
   }

   @Override
   public boolean romping() {
      return this.phase == Play.Phase.ROMP && (!this.oneSided || this.batted);
   }

   @Nullable
   public UUID partner() {
      return this.partnerId;
   }

   @Nullable
   public UUID takeFinished() {
      UUID out = this.finishedWith;
      this.finishedWith = null;
      return out;
   }

   public boolean finishedPlayed() {
      return this.finishedPlayed;
   }

   @Override
   public void start(AnimalHandle handle, MoveVerbs move, long now) {
      this.phase = Play.Phase.IDLE;
      this.phaseStart = Long.MIN_VALUE;
      this.boutStart = Long.MIN_VALUE;
      this.finishedWith = null;
      this.finishedPlayed = false;
      this.resetTag();
   }

   @Override
   public void tick(AnimalHandle handle, MoveVerbs move, long now) {
      if (this.partnerId != null && this.estimate != null) {
         PairSession session = livePlay(handle, this.partnerId);
         if (session == null && this.phase != Play.Phase.IDLE) {
            this.end(handle, move, now, this.phase == Play.Phase.ROMP);
         } else {
            long shared = handle.gameTime();
            if (session == null) {
               session = PairSession.open(handle.faunaId(), this.partnerId, "play", shared);
            }

            session.noteProgress(shared);
            session.join(handle.faunaId());
            if (this.oneSided) {
               session.join(this.partnerId);
            }

            if (this.phase == Play.Phase.IDLE) {
               this.begin(now);
            }

            if (!this.tagGame && !this.oneSided && now - this.boutStart > 300L) {
               this.end(handle, move, now, true);
            } else {
               long elapsed = this.phaseStart == Long.MIN_VALUE ? 0L : now - this.phaseStart;
               handle.applyLook(this.estimate);
               switch (this.phase) {
                  case INVITE:
                     this.invite(handle, move, now, elapsed, session);
                     break;
                  case ROMP:
                     this.romp(handle, move, now, session);
                     break;
                  default:
                     this.idle();
               }
            }
         }
      } else {
         this.idle();
      }
   }

   private void invite(AnimalHandle handle, MoveVerbs move, long now, long elapsed, PairSession session) {
      if (this.tagGame) {
         this.tease(handle, move, now, session);
      } else if (this.oneSided) {
         session.setLeader(handle.faunaId());
         this.enter(Play.Phase.ROMP, now);
         this.romp(handle, move, now, session);
      } else if (elapsed >= 240L) {
         this.end(handle, move, now, false);
      } else {
         double tag = this.tagDistance(handle);
         if (elapsed >= 60L && session.joined(this.partnerId) && this.estimate.distanceTo(handle.bodyPosition()) <= tag * 2.5) {
            if (session.leader() == null) {
               session.setLeader(handle.faunaId());
            }

            this.enter(Play.Phase.ROMP, now);
            this.romp(handle, move, now, session);
         } else {
            move.escort(this.partnerId, tag * 1.25, tag * 2.5, 1.0F);
         }
      }
   }

   private void romp(AnimalHandle handle, MoveVerbs move, long now, PairSession session) {
      double tag = this.tagDistance(handle);
      double gap = this.estimate.distanceTo(handle.bodyPosition());
      if (this.tagGame) {
         this.tagTick(handle, move, now, tag, gap);
      } else if (this.tagBout) {
         this.end(handle, move, now, this.caughtHere);
      } else if (this.oneSided) {
         this.bat(handle, move, now, session, tag, gap);
      } else {
         boolean leading = session.isLeader(handle.faunaId());
         if (leading != this.wasLeader) {
            this.wasLeader = leading;
            this.tagArmed = false;
         }

         if (leading) {
            move.chase(this.partnerId, 0.0, tag, 1.0F);
            if (!this.tagArmed && gap >= tag * 3.75) {
               this.tagArmed = true;
            }

            if (this.tagArmed && gap <= tag) {
               this.tagArmed = false;
               if (this.shoves) {
                  Arms.of(handle).shove(this.partnerId);
               }

               session.completeRound(handle.gameTime());
               session.swapLeader();
               if (session.rounds() >= roundTarget(session)) {
                  this.end(handle, move, now, true);
               }
            }
         } else {
            if (this.person) {
               if (!this.personArmed && gap >= tag * 3.75) {
                  this.personArmed = true;
               }

               if (this.personArmed && gap <= tag) {
                  this.personArmed = false;
                  session.completeRound(handle.gameTime());
                  session.swapLeader();
                  if (session.rounds() >= roundTarget(session)) {
                     this.end(handle, move, now, true);
                     return;
                  }
               }
            }

            move.display(this.partnerId, tag * 3.75, tag * 3.75 * 1.25, 1.0F);
         }
      }
   }

   private void bat(AnimalHandle handle, MoveVerbs move, long now, PairSession session, double tag, double gap) {
      if (now - Math.max(this.swungAt, this.boutStart) > 240L) {
         this.end(handle, move, now, this.batted);
      } else {
         double ring = tag * 1.25;
         if (this.backing) {
            if (now - this.swungAt <= 20L) {
               move.chase(this.partnerId, 0.0, tag, 1.0F);
               return;
            }

            if (gap < ring) {
               move.back(this.partnerId, ring, ring * 1.25, 1.0F);
               return;
            }

            this.backing = false;
         }

         move.chase(this.partnerId, 0.0, tag, 1.0F);
         if (gap <= tag && Arms.of(handle).shove(this.partnerId)) {
            session.completeRound(handle.gameTime());
            this.backing = true;
            this.batted = true;
            this.swungAt = now;
         }
      }
   }

   private void tagTick(AnimalHandle handle, MoveVerbs move, long now, double tag, double gap) {
      this.tagBout = true;
      if (this.games != this.seenGames) {
         this.end(handle, move, now, true);
      } else if (this.catches != this.seenCatches) {
         this.seenCatches = this.catches;
         this.caughtHere = true;
         this.beginTease(now);
      } else {
         double ring = tag * 3.75;
         if (this.legging && gap < ring) {
            move.back(this.partnerId, ring, ring * 1.25, 1.0F);
         } else {
            this.legging = false;
            if (this.waitSince == Long.MIN_VALUE) {
               this.waitSince = now;
            }

            if (now - this.waitSince > 240L) {
               this.end(handle, move, now, true);
            } else {
               move.display(this.partnerId, ring, ring * 1.25, 1.0F);
            }
         }
      }
   }

   private void tease(AnimalHandle handle, MoveVerbs move, long now, PairSession session) {
      double tag = this.tagDistance(handle);
      double gap = this.estimate.distanceTo(handle.bodyPosition());
      if (this.lookUntil != Long.MIN_VALUE) {
         if (now < this.lookUntil) {
            move.stop();
            return;
         }

         this.lookUntil = Long.MIN_VALUE;
         this.teaseHops++;
      }

      if (this.teaseHops >= 2) {
         session.setLeader(this.partnerId);
         this.legging = true;
         this.waitSince = Long.MIN_VALUE;
         this.enter(Play.Phase.ROMP, now);
         this.tagTick(handle, move, now, tag, gap);
      } else {
         if (Double.isNaN(this.hopTarget)) {
            this.hopTarget = gap + tag;
         }

         if (gap >= this.hopTarget) {
            this.hopTarget = Double.NaN;
            this.lookUntil = now + 20L;
            move.stop();
         } else {
            move.back(this.partnerId, this.hopTarget, this.hopTarget * 1.25, 1.0F);
         }
      }
   }

   private void beginTease(long now) {
      this.teaseHops = 0;
      this.hopTarget = Double.NaN;
      this.lookUntil = Long.MIN_VALUE;
      this.legging = false;
      this.waitSince = Long.MIN_VALUE;
      this.enter(Play.Phase.INVITE, now);
   }

   private double tagDistance(AnimalHandle handle) {
      double halves = handle.clearanceTo(this.partnerGirth);
      return halves + 3.75;
   }

   private static int roundTarget(PairSession session) {
      float draw = session.draw();
      return 2 + (int)(draw * 2.0F);
   }

   @Nullable
   private static PairSession livePlay(AnimalHandle handle, UUID other) {
      PairSession open = PairSession.find(handle.faunaId(), other);
      return open != null && "play".equals(open.kind()) ? open : null;
   }

   private void begin(long now) {
      this.boutStart = now;
      this.tagArmed = false;
      this.personArmed = false;
      this.wasLeader = false;
      this.resetTag();
      this.enter(Play.Phase.INVITE, now);
   }

   private void resetTag() {
      this.tagBout = false;
      this.caughtHere = false;
      this.legging = false;
      this.waitSince = Long.MIN_VALUE;
      this.teaseHops = 0;
      this.hopTarget = Double.NaN;
      this.lookUntil = Long.MIN_VALUE;
      this.seenCatches = this.catches;
      this.seenGames = this.games;
      this.backing = false;
      this.batted = false;
      this.swungAt = Long.MIN_VALUE;
   }

   private void enter(Play.Phase next, long now) {
      this.phase = next;
      this.phaseStart = now;
   }

   private void end(AnimalHandle handle, MoveVerbs move, long now, boolean played) {
      if (this.partnerId != null) {
         this.finishedWith = this.partnerId;
         this.finishedPlayed = played;
         PairSession.noteSettled(handle.faunaId(), this.partnerId, "play", handle.gameTime());
         PairSession.close(handle.faunaId(), this.partnerId);
      }

      this.partnerId = null;
      this.estimate = null;
      this.phase = Play.Phase.IDLE;
      this.phaseStart = Long.MIN_VALUE;
      this.boutStart = Long.MIN_VALUE;
      this.resetTag();
   }

   private void idle() {
      this.phase = Play.Phase.IDLE;
      this.phaseStart = Long.MIN_VALUE;
   }

   @Override
   public void stop(AnimalHandle handle, MoveVerbs move, long now) {
      if (this.partnerId != null) {
         PairSession session = livePlay(handle, this.partnerId);
         if (!this.oneSided && this.phase == Play.Phase.ROMP && session != null && (session.rounds() >= 1 || this.caughtHere)) {
            this.finishedWith = this.partnerId;
            this.finishedPlayed = true;
            PairSession.noteSettled(handle.faunaId(), this.partnerId, "play", handle.gameTime());
         }

         PairSession.close(handle.faunaId(), this.partnerId);
      }

      this.partnerId = null;
      this.estimate = null;
      this.partnerGirth = 0.0F;
      this.phase = Play.Phase.IDLE;
      this.phaseStart = Long.MIN_VALUE;
      this.boutStart = Long.MIN_VALUE;
      this.resetTag();
   }

   public enum Phase {
      IDLE,
      INVITE,
      ROMP;
   }
}
