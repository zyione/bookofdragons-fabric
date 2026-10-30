package net.magister.liberanimalium.fauna.behavior;

import java.util.UUID;
import org.jetbrains.annotations.Nullable;
import net.magister.liberanimalium.fauna.api.AnimalHandle;
import net.magister.liberanimalium.fauna.api.PoseFlag;
import net.magister.liberanimalium.fauna.core.PairSession;
import net.minecraft.world.phys.Vec3;

public final class Spar implements Behavior {
   private static final double SPAR_RING = 6.0;
   private final Hunt fight = new Hunt();
   @Nullable
   private UUID partnerId;
   @Nullable
   private Vec3 estimate;
   @Nullable
   private Fighter self;
   @Nullable
   private Bout bout;
   private int blows;
   private int seenBlows;
   private boolean open;
   private long displayStart = Long.MIN_VALUE;
   private long boutStart = Long.MIN_VALUE;
   private long lastBlow = Long.MIN_VALUE;

   public void bout(@Nullable UUID id, @Nullable Vec3 believedPosition, int blows, @Nullable Fighter self, @Nullable Bout bout) {
      this.partnerId = id;
      this.estimate = believedPosition;
      this.blows = blows;
      this.self = self;
      this.bout = bout;
   }

   public boolean open() {
      return this.open;
   }

   @Override
   public boolean romping() {
      return this.open && this.boutStart != Long.MIN_VALUE;
   }

   @Override
   public void start(AnimalHandle handle, MoveVerbs move, long now) {
      this.open = false;
      this.seenBlows = 0;
      this.displayStart = Long.MIN_VALUE;
      this.boutStart = Long.MIN_VALUE;
      this.lastBlow = Long.MIN_VALUE;
      this.fight.start(handle, move, now);
   }

   @Override
   public void tick(AnimalHandle handle, MoveVerbs move, long now) {
      if (this.partnerId != null && this.estimate != null && this.self != null && this.bout != null) {
         long shared = handle.gameTime();
         PairSession session = PairSession.find(handle.faunaId(), this.partnerId);
         boolean spar = session != null && "spar".equals(session.kind());
         if (this.open && !spar) {
            this.end(handle, shared);
         } else {
            if (!this.open) {
               if (session != null) {
                  return;
               }

               session = PairSession.open(handle.faunaId(), this.partnerId, "spar", shared);
               this.open = true;
               this.displayStart = now;
               this.boutStart = Long.MIN_VALUE;
               this.seenBlows = this.blows;
            }

            session.noteProgress(shared);
            session.join(handle.faunaId());
            if (this.boutStart == Long.MIN_VALUE) {
               if (this.blows == this.seenBlows && now - this.displayStart < 200L) {
                  handle.setPose(PoseFlag.POSTURING, true);
                  handle.applyLook(this.estimate);
                  move.display(this.partnerId, 6.0, 8.0, 1.0F);
                  return;
               }

               handle.setPose(PoseFlag.POSTURING, false);
               this.boutStart = now;
               this.lastBlow = now;
            }

            if (this.blows != this.seenBlows) {
               this.seenBlows = this.blows;
               this.lastBlow = now;
            }

            if (now - this.lastBlow <= 240L && now - this.boutStart <= 1200L) {
               this.fight.pursue(this.partnerId, this.estimate, this.self, this.bout, false);
               this.fight.tick(handle, move, now);
            } else {
               this.end(handle, shared);
            }
         }
      }
   }

   @Override
   public void stop(AnimalHandle handle, MoveVerbs move, long now) {
      handle.setPose(PoseFlag.POSTURING, false);
      if (this.open) {
         this.end(handle, handle.gameTime());
      }

      this.fight.stop(handle, move, now);
   }

   private void end(AnimalHandle handle, long shared) {
      this.open = false;
      if (this.partnerId != null) {
         PairSession session = PairSession.find(handle.faunaId(), this.partnerId);
         if (session != null && "spar".equals(session.kind())) {
            PairSession.close(handle.faunaId(), this.partnerId);
         }

         PairSession.noteSettled(handle.faunaId(), this.partnerId, "spar", shared);
      }
   }
}
