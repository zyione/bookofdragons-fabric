package net.magister.liberanimalium.fauna.maneuver;

import java.util.function.Function;
import org.jetbrains.annotations.Nullable;
import net.magister.liberanimalium.fauna.api.AnimalHandle;
import net.magister.liberanimalium.fauna.identity.Trait;
import net.magister.liberanimalium.fauna.maneuver.pattern.TargetedManeuver;

public final class ManeuverRunner {
   @Nullable
   private TargetedManeuver active;
   @Nullable
   private String goal;
   @Nullable
   private MoveStyles.Choice choice;
   private Posture posture = Posture.NONE;
   private int failedInARow;

   public void tick(AnimalHandle handle, Waypoints move, Brief brief, Posture.TraitReader traits, Function<MoveStyles.Primitive, Float> aptitude) {
      if (this.active != null && !brief.goal().equals(this.goal)) {
         this.end(handle, move);
      }

      this.posture = Posture.of(brief.situation(), traits, brief.health(), (float)brief.subject().attentionOn(handle.bodyPosition()), brief.strain());
      if (this.active != null) {
         this.active.refresh(brief.subject());
         if (this.broken(handle)) {
            this.end(handle, move);
         }
      }

      if (this.active == null) {
         this.select(handle, move, brief, traits, aptitude);
      }

      if (this.active != null) {
         Maneuver.Status status = this.run(handle, move);
         if (status != Maneuver.Status.RUNNING) {
            if (status == Maneuver.Status.SUCCEEDED && Ground.airborne(handle) && brief.quiet() >= 160) {
               status = Maneuver.Status.FAILED;
            }

            if (status == Maneuver.Status.FAILED) {
               this.choice = null;
            }

            this.failedInARow = status == Maneuver.Status.FAILED ? this.failedInARow + 1 : 0;
            this.end(handle, move);
            this.select(handle, move, brief, traits, aptitude);
            if (this.active != null) {
               this.run(handle, move);
            }
         }
      }
   }

   private Maneuver.Status run(AnimalHandle handle, Waypoints move) {
      Maneuver.Status status = this.active.tick(handle, move);
      move.pace(this.active.pace());
      move.gait(this.active.running(handle));
      this.active.advance();
      return status;
   }

   private boolean broken(AnimalHandle handle) {
      return this.active.ticksRun() < 10 ? false : !this.active.premiseHolds(handle);
   }

   private void select(AnimalHandle handle, Waypoints move, Brief brief, Posture.TraitReader traits, Function<MoveStyles.Primitive, Float> aptitude) {
      MoveStyles.Primitive incumbent = this.choice == null ? null : this.choice.primitive();
      MoveStyles.Choice picked = MoveStyles.select(handle, brief.subject(), brief.situation(), traits, this.posture, incumbent, aptitude);
      int dwellTicks = ManeuverTuning.dwellTicks(traits.get(Trait.ENERGY));
      TargetedManeuver next = MoveStyles.build(
         handle,
         brief.subject(),
         picked.primitive(),
         new double[]{brief.min(), brief.max()},
         brief.urgency(),
         this.posture.grip(),
         brief.hand(),
         dwellTicks,
         traits
      );
      this.choice = picked;
      this.active = next;
      this.goal = brief.goal();
      next.begin(handle, move);
   }

   public void clear(AnimalHandle handle, Waypoints move) {
      if (this.active != null) {
         this.end(handle, move);
         move.stop();
      }
   }

   private void end(AnimalHandle handle, Waypoints move) {
      if (this.active != null) {
         this.active.stop(handle, move);
         this.active = null;
         this.goal = null;
      }
   }

   @Nullable
   public TargetedManeuver active() {
      return this.active;
   }

   public int failedInARow() {
      return this.failedInARow;
   }

   public void resetFailures() {
      this.failedInARow = 0;
   }

   @Nullable
   public MoveStyles.Choice choice() {
      return this.choice;
   }

   public Posture posture() {
      return this.posture;
   }

   public boolean concealing() {
      return this.active != null && this.choice != null
         ? this.choice.primitive() == MoveStyles.Primitive.COVER || this.choice.primitive() == MoveStyles.Primitive.CREEP
         : false;
   }
}
