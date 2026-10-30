package net.magister.liberanimalium.fauna.behavior;

import java.util.UUID;
import org.jetbrains.annotations.Nullable;
import net.magister.liberanimalium.fauna.api.AnimalHandle;
import net.magister.liberanimalium.fauna.api.PoseFlag;
import net.minecraft.world.phys.Vec3;

public final class Warn implements Behavior {
   @Nullable
   private UUID at;
   @Nullable
   private Vec3 where;

   public void faceUp(@Nullable UUID id, @Nullable Vec3 estimate) {
      this.at = id;
      this.where = estimate;
   }

   @Nullable
   public UUID facing() {
      return this.at;
   }

   @Override
   public void tick(AnimalHandle handle, MoveVerbs move, long now) {
      if (this.at != null && this.where != null) {
         move.stop();
         handle.setPose(PoseFlag.POSTURING, true);
         handle.applyLook(this.where);
      }
   }

   @Override
   public void stop(AnimalHandle handle, MoveVerbs move, long now) {
      handle.setPose(PoseFlag.POSTURING, false);
      handle.applyLook(null);
      this.at = null;
      this.where = null;
   }
}
