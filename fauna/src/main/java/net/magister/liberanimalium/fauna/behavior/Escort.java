package net.magister.liberanimalium.fauna.behavior;

import java.util.UUID;
import org.jetbrains.annotations.Nullable;
import net.magister.liberanimalium.fauna.api.AnimalHandle;
import net.minecraft.world.phys.Vec3;

public final class Escort implements Behavior {
   @Nullable
   private UUID friendId;
   @Nullable
   private Vec3 estimate;

   public void accompany(@Nullable UUID id, @Nullable Vec3 believedPosition) {
      this.friendId = id;
      this.estimate = believedPosition;
   }

   @Override
   public void tick(AnimalHandle handle, MoveVerbs move, long now) {
      if (this.friendId != null && this.estimate != null) {
         double reach = handle.bounds().getXsize() + 3.0;
         move.escort(this.friendId, reach, reach * 2.0, 1.0F);
      }
   }

   @Override
   public void stop(AnimalHandle handle, MoveVerbs move, long now) {
      this.friendId = null;
      this.estimate = null;
   }
}
