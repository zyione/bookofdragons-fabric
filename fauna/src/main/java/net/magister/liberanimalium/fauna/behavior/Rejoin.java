package net.magister.liberanimalium.fauna.behavior;

import org.jetbrains.annotations.Nullable;
import net.magister.liberanimalium.fauna.api.AnimalHandle;
import net.magister.liberanimalium.fauna.api.MoveMode;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.Vec3;

public final class Rejoin implements Behavior {
   @Nullable
   private Vec3 centroid;
   private double handover = 1.0;

   public void gatherAt(@Nullable Vec3 packCentroid, double handoverRadius) {
      this.centroid = packCentroid;
      this.handover = Math.max(1.0, handoverRadius);
   }

   @Override
   public void tick(AnimalHandle handle, MoveVerbs move, long now) {
      if (this.centroid != null) {
         double gap = this.centroid.distanceTo(handle.bodyPosition());
         if (gap <= this.handover) {
            if (handle.currentMode() == MoveMode.AIR) {
               move.alight(1.0F);
            } else {
               move.stop();
            }
         } else {
            move.goTo(BlockPos.containing(this.centroid), 1.0F);
         }
      }
   }

   @Override
   public void stop(AnimalHandle handle, MoveVerbs move, long now) {
      this.centroid = null;
   }
}
