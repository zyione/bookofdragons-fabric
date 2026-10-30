package net.magister.liberanimalium.fauna.behavior;

import org.jetbrains.annotations.Nullable;
import net.magister.liberanimalium.fauna.api.AnimalHandle;
import net.magister.liberanimalium.fauna.api.Gullet;
import net.magister.liberanimalium.fauna.api.Halter;
import net.magister.liberanimalium.fauna.api.MoveMode;
import net.magister.liberanimalium.fauna.api.PoseFlag;
import net.magister.liberanimalium.fauna.know.Bed;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.Vec3;

public final class Rest implements Behavior {
   @Nullable
   private Bed bed;
   private double tolerance = 1.0;
   private int settling;
   private boolean confirmed;
   @Nullable
   private Vec3 pack;
   private double huddle = 1.0;
   private boolean overcome;

   public void bedDownAt(@Nullable Bed bed, double tolerance) {
      this.bed = bed;
      this.tolerance = Math.max(1.0, tolerance);
   }

   public void huddleBy(@Nullable Vec3 pack, double huddle) {
      this.pack = pack;
      this.huddle = Math.max(0.5, huddle);
   }

   public void overcome(boolean overcome) {
      this.overcome = overcome;
   }

   public boolean wantsConfirmation(AnimalHandle handle) {
      return !this.confirmed && this.bed != null && this.at(handle, this.bed);
   }

   public void confirmed() {
      this.confirmed = true;
   }

   @Override
   public void start(AnimalHandle handle, MoveVerbs move, long now) {
      this.settling = 0;
      this.confirmed = false;
   }

   @Override
   public void tick(AnimalHandle handle, MoveVerbs move, long now) {
      if (Halter.of(handle).carrying()) {
         handle.setPose(PoseFlag.SLEEPING, false);
         move.stop();
      } else {
         boolean giveUp = ++this.settling >= 300;
         if (!this.overcome || handle.currentMode() != MoveMode.AIR && !handle.asleep()) {
            if (this.bed != null && !giveUp && !this.at(handle, this.bed)) {
               handle.setPose(PoseFlag.SLEEPING, false);
               move.goTo(this.bed.at(), 1.0F);
            } else if (this.bed == null
               && this.pack != null
               && !giveUp
               && !handle.asleep()
               && handle.bodyPosition().distanceToSqr(this.pack) > this.huddle * this.huddle) {
               handle.setPose(PoseFlag.SLEEPING, false);
               move.goTo(BlockPos.containing(this.pack), 1.0F);
            } else if (handle.currentMode() == MoveMode.AIR) {
               handle.setPose(PoseFlag.SLEEPING, false);
               if (move.idle()) {
                  move.alight(1.0F);
               }
            } else {
               move.stop();
               handle.endowed(Gullet.class).ifPresent(Gullet::stash);
               handle.setPose(PoseFlag.SLEEPING, true);
            }
         } else {
            move.stop();
            handle.setPose(PoseFlag.SLEEPING, true);
         }
      }
   }

   @Override
   public void stop(AnimalHandle handle, MoveVerbs move, long now) {
      handle.setPose(PoseFlag.SLEEPING, false);
      this.bed = null;
      this.pack = null;
      this.overcome = false;
      this.settling = 0;
      this.confirmed = false;
   }

   private boolean at(AnimalHandle handle, Bed bed) {
      return handle.bodyPosition().distanceToSqr(bed.centre()) <= this.tolerance * this.tolerance;
   }
}
