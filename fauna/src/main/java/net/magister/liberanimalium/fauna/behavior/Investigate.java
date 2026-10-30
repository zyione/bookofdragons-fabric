package net.magister.liberanimalium.fauna.behavior;

import org.jetbrains.annotations.Nullable;
import net.magister.liberanimalium.fauna.api.AnimalHandle;
import net.magister.liberanimalium.fauna.api.PoseFlag;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.Vec3;

public final class Investigate implements Behavior {
   @Nullable
   private Vec3 point;
   private boolean looked;

   public void lookAt(@Nullable Vec3 where) {
      this.point = where;
   }

   public boolean takeLooked() {
      boolean was = this.looked;
      this.looked = false;
      return was;
   }

   @Override
   public void tick(AnimalHandle handle, MoveVerbs move, long now) {
      if (this.point != null) {
         double gap = this.point.distanceTo(handle.bodyPosition());
         double seenRange = 9.0;
         boolean closeEnough = gap <= 3.0 || gap <= seenRange && handle.lineOfSight(handle.eyePosition(), this.point);
         if (closeEnough) {
            this.looked = true;
            handle.applyLook(this.point);
            handle.setPose(PoseFlag.ALERT, true);
            move.stop();
         } else {
            handle.setPose(PoseFlag.ALERT, true);
            handle.applyLook(this.point);
            move.goTo(BlockPos.containing(this.point), 1.0F);
         }
      }
   }

   @Override
   public void stop(AnimalHandle handle, MoveVerbs move, long now) {
      handle.setPose(PoseFlag.ALERT, false);
      handle.applyLook(null);
      this.point = null;
      this.looked = false;
   }
}
