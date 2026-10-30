package net.magister.liberanimalium.fauna.behavior;

import java.util.UUID;
import org.jetbrains.annotations.Nullable;
import net.magister.liberanimalium.fauna.api.AnimalHandle;
import net.magister.liberanimalium.fauna.api.PoseFlag;
import net.minecraft.world.phys.Vec3;

public final class Solicit implements Behavior {
   @Nullable
   private UUID holder;
   @Nullable
   private Vec3 at;
   private double standoff;
   private boolean asking;
   private boolean present;

   public void ask(@Nullable UUID id, @Nullable Vec3 where, double distance) {
      this.ask(id, where, distance, false);
   }

   public void ask(@Nullable UUID id, @Nullable Vec3 where, double distance, boolean present) {
      this.holder = id;
      this.at = where;
      this.standoff = distance;
      this.present = present;
      if (id == null) {
         this.asking = false;
      }
   }

   public boolean beggingAt(UUID candidate) {
      return this.asking && this.holder != null && this.holder.equals(candidate);
   }

   public boolean asking() {
      return this.asking;
   }

   public double standoff() {
      return this.standoff;
   }

   @Override
   public void tick(AnimalHandle handle, MoveVerbs move, long now) {
      if (this.holder != null && this.at != null) {
         boolean arrived = this.at.distanceTo(handle.bodyPosition()) <= this.standoff + 0.25 + (this.asking ? 0.25 : 0.0);
         boolean turn = this.present && arrived;
         handle.applyLook(turn ? handle.bodyPosition().scale(2.0).subtract(this.at) : this.at);
         handle.setPose(PoseFlag.INVITING, turn);
         if (arrived) {
            this.asking = true;
            move.stop();
         } else {
            this.asking = false;
            move.chase(this.holder, this.standoff, this.standoff * 2.0, 0.3F);
         }
      } else {
         this.asking = false;
         handle.setPose(PoseFlag.INVITING, false);
      }
   }

   @Override
   public void stop(AnimalHandle handle, MoveVerbs move, long now) {
      handle.applyLook(null);
      handle.setPose(PoseFlag.INVITING, false);
      this.holder = null;
      this.at = null;
      this.asking = false;
      this.present = false;
   }
}
