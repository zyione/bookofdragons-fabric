package net.magister.liberanimalium.fauna.behavior;

import java.util.UUID;
import org.jetbrains.annotations.Nullable;
import net.magister.liberanimalium.fauna.api.AnimalHandle;
import net.magister.liberanimalium.fauna.api.PoseFlag;
import net.minecraft.world.phys.Vec3;

public final class Shoo implements Behavior, Laddered {
   @Nullable
   private UUID crowderId;
   @Nullable
   private Vec3 estimate;
   private float crowding;
   private float tolerance;
   private double space;
   private boolean mayEscalate;
   private Shoo.Rung rung = Shoo.Rung.IDLE;
   private long rungStart = Long.MIN_VALUE;
   @Nullable
   private UUID rebuffed;

   public void objectTo(@Nullable UUID id, @Nullable Vec3 where, float crowding, float tolerance, double space, boolean mayEscalate) {
      if (id == null || !id.equals(this.crowderId)) {
         this.rung = Shoo.Rung.IDLE;
         this.rungStart = Long.MIN_VALUE;
      }

      this.crowderId = id;
      this.estimate = where;
      this.crowding = crowding;
      this.tolerance = tolerance;
      this.space = space;
      this.mayEscalate = mayEscalate;
   }

   @Override
   public void start(AnimalHandle handle, MoveVerbs move, long now) {
      this.rung = Shoo.Rung.IDLE;
      this.rungStart = Long.MIN_VALUE;
      this.rebuffed = null;
   }

   @Override
   public void tick(AnimalHandle handle, MoveVerbs move, long now) {
      this.rebuffed = null;
      if (this.crowderId != null && this.estimate != null) {
         if (this.rung == Shoo.Rung.IDLE) {
            this.enter(Shoo.Rung.STEP_OFF, now);
         }

         if (this.crowding < this.release()) {
            this.descend(now);
         } else if (this.rungStart != Long.MIN_VALUE && now - this.rungStart >= this.dwell()) {
            this.climb(now);
         }

         switch (this.rung) {
            case STEP_OFF:
               this.stepOff(handle, move);
               break;
            case WARN:
               this.warn(handle, move);
               break;
            case ESCALATE:
               this.escalate(handle, move);
               break;
            default:
               this.idle(handle, move);
         }
      } else {
         this.idle(handle, move);
      }
   }

   @Override
   public void stop(AnimalHandle handle, MoveVerbs move, long now) {
      handle.setPose(PoseFlag.POSTURING, false);
      this.rung = Shoo.Rung.IDLE;
      this.rungStart = Long.MIN_VALUE;
      this.rebuffed = null;
   }

   private void stepOff(AnimalHandle handle, MoveVerbs move) {
      handle.setPose(PoseFlag.POSTURING, false);
      move.back(this.crowderId, this.space, this.space + handle.bounds().getXsize(), 0.4F);
   }

   private void warn(AnimalHandle handle, MoveVerbs move) {
      move.stop();
      handle.setPose(PoseFlag.POSTURING, true);
      handle.applyLook(this.estimate);
   }

   private void escalate(AnimalHandle handle, MoveVerbs move) {
      this.warn(handle, move);
      this.rebuffed = this.crowderId;
   }

   private void idle(AnimalHandle handle, MoveVerbs move) {
      if (this.rung != Shoo.Rung.IDLE) {
         handle.setPose(PoseFlag.POSTURING, false);
         this.rung = Shoo.Rung.IDLE;
         this.rungStart = Long.MIN_VALUE;
      }
   }

   private void climb(long now) {
      Shoo.Rung next = switch (this.rung) {
         case STEP_OFF -> Shoo.Rung.WARN;
         case WARN -> this.mayEscalate ? Shoo.Rung.ESCALATE : Shoo.Rung.WARN;
         default -> this.rung;
      };
      if (next != this.rung) {
         this.enter(next, now);
      }
   }

   private void descend(long now) {
      Shoo.Rung next = switch (this.rung) {
         case WARN -> Shoo.Rung.STEP_OFF;
         case ESCALATE -> Shoo.Rung.WARN;
         default -> this.rung;
      };
      if (next != this.rung) {
         this.enter(next, now);
      }
   }

   private void enter(Shoo.Rung next, long now) {
      this.rung = next;
      this.rungStart = now;
   }

   private int dwell() {
      return this.rung == Shoo.Rung.STEP_OFF ? 40 : 80;
   }

   private float release() {
      return this.tolerance * 0.66F;
   }

   @Override
   public String rungName() {
      return this.rung.name();
   }

   public Shoo.Rung rung() {
      return this.rung;
   }

   @Nullable
   public UUID rebuffed() {
      UUID out = this.rebuffed;
      this.rebuffed = null;
      return out;
   }

   @Nullable
   public UUID crowder() {
      return this.crowderId;
   }

   public enum Rung {
      IDLE,
      STEP_OFF,
      WARN,
      ESCALATE;
   }
}
