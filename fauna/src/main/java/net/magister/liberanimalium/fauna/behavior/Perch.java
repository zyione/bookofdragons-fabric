package net.magister.liberanimalium.fauna.behavior;

import java.util.UUID;
import org.jetbrains.annotations.Nullable;
import net.magister.liberanimalium.fauna.api.AnimalHandle;
import net.magister.liberanimalium.fauna.api.Halter;
import net.magister.liberanimalium.fauna.api.MoveMode;
import net.magister.liberanimalium.fauna.api.PoseFlag;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.Vec3;

public final class Perch implements Behavior {
   @Nullable
   private UUID owner;
   @Nullable
   private Vec3 ownerAt;
   private int refused;
   private boolean gaveUp;

   public void onto(@Nullable UUID owner, @Nullable Vec3 ownerAt) {
      this.owner = owner;
      this.ownerAt = ownerAt;
   }

   public boolean takeGaveUp() {
      boolean was = this.gaveUp;
      this.gaveUp = false;
      return was;
   }

   @Override
   public void start(AnimalHandle handle, MoveVerbs move, long now) {
      this.refused = 0;
      this.gaveUp = false;
   }

   @Override
   public void tick(AnimalHandle handle, MoveVerbs move, long now) {
      if (Halter.of(handle).perched()) {
         this.refused = 0;
         move.stop();
         handle.setPose(PoseFlag.PERCHED, true);
         handle.applyLook(null);
      } else {
         handle.setPose(PoseFlag.PERCHED, false);
         if (this.owner != null && this.ownerAt != null) {
            double reach = handle.bounds().getXsize() * 0.5 + 1.5;
            if (handle.bodyPosition().distanceToSqr(this.ownerAt) > reach * reach) {
               move.goTo(BlockPos.containing(this.ownerAt), 1.0F);
            } else if (handle.currentMode() == MoveMode.AIR) {
               if (move.idle()) {
                  move.alight(1.0F);
               }
            } else {
               move.stop();
               if (Halter.of(handle).perchOn(this.owner)) {
                  this.refused = 0;
                  handle.setPose(PoseFlag.PERCHED, true);
               } else if (++this.refused >= 40) {
                  this.refused = 0;
                  this.gaveUp = true;
               }
            }
         } else {
            move.stop();
         }
      }
   }

   @Override
   public void stop(AnimalHandle handle, MoveVerbs move, long now) {
      if (Halter.of(handle).perched()) {
         Halter.of(handle).leavePerch();
      }

      handle.setPose(PoseFlag.PERCHED, false);
      handle.applyLook(null);
      this.owner = null;
      this.ownerAt = null;
      this.refused = 0;
   }
}
