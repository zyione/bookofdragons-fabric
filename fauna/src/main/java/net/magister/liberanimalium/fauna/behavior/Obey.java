package net.magister.liberanimalium.fauna.behavior;

import java.util.UUID;
import org.jetbrains.annotations.Nullable;
import net.magister.liberanimalium.fauna.api.AnimalHandle;
import net.magister.liberanimalium.fauna.api.Order;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.Vec3;

public final class Obey implements Behavior {
   private Order order = Order.FREE;
   @Nullable
   private UUID ownerId;
   @Nullable
   private Vec3 ownerEstimate;
   @Nullable
   private Vec3 post;
   private boolean ownerRiding;
   private boolean askedForPost;
   private boolean catchingUp;
   private boolean sprinting;
   @Nullable
   private Vec3 walledOffAt;
   private long hiddenSince = Long.MIN_VALUE;
   private boolean ownerCrouching;
   private double postTolerance;

   public void ordered(Order order, @Nullable UUID ownerId, @Nullable Vec3 ownerEstimate, @Nullable Vec3 post, boolean ownerRiding) {
      this.order = order;
      this.ownerId = ownerId;
      this.ownerEstimate = ownerEstimate;
      this.post = post;
      this.ownerRiding = ownerRiding;
   }

   public void ownerCrouching(boolean crouching) {
      this.ownerCrouching = crouching;
   }

   public boolean creeping() {
      return this.order == Order.FOLLOW && this.ownerCrouching && this.ownerId != null && !this.ownerRiding;
   }

   @Override
   public void start(AnimalHandle handle, MoveVerbs move, long now) {
      this.askedForPost = false;
      this.catchingUp = false;
      this.sprinting = false;
      this.walledOffAt = null;
      this.hiddenSince = Long.MIN_VALUE;
   }

   @Override
   public void tick(AnimalHandle handle, MoveVerbs move, long now) {
      switch (this.order) {
         case STAY:
            this.stay(handle, move);
            break;
         case FOLLOW:
            this.follow(handle, move, now);
         case FREE:
      }
   }

   public void holdPostWithin(double tolerance) {
      this.postTolerance = tolerance;
   }

   private void stay(AnimalHandle handle, MoveVerbs move) {
      if (this.post == null) {
         this.post = handle.bodyPosition();
      }

      double reach = Math.max(handle.clearanceTo(0.0), this.postTolerance);
      if (handle.bodyPosition().distanceTo(this.post) > reach) {
         move.goTo(BlockPos.containing(this.post.x, this.post.y, this.post.z), 1.0F);
         this.askedForPost = true;
      } else {
         if (!this.askedForPost || move.idle()) {
            move.stop();
         }
      }
   }

   private void follow(AnimalHandle handle, MoveVerbs move, long now) {
      if (this.ownerId != null && this.ownerEstimate != null && !this.ownerRiding) {
         double reach = handle.bounds().getXsize() + 3.0;
         double ring = reach * 2.0;
         double gap = handle.bodyPosition().distanceTo(this.ownerEstimate);
         boolean far = gap > ring;
         boolean hidden = !far && !handle.lineOfSight(handle.eyePosition(), this.ownerEstimate.add(0.0, 1.0, 0.0));
         if (!hidden) {
            this.hiddenSince = Long.MIN_VALUE;
         }

         if (this.walledOffAt != null && (!hidden || this.walledOffAt.distanceTo(this.ownerEstimate) > reach)) {
            this.walledOffAt = null;
            this.hiddenSince = Long.MIN_VALUE;
         }

         if (hidden && this.walledOffAt == null) {
            if (this.hiddenSince == Long.MIN_VALUE) {
               this.hiddenSince = now;
            } else if (now - this.hiddenSince > 200L) {
               this.walledOffAt = this.ownerEstimate;
               this.catchingUp = false;
            }
         }

         if (!far && (!hidden || this.walledOffAt != null)) {
            if (gap <= reach && !hidden && !move.doorAhead()) {
               this.catchingUp = false;
            }
         } else {
            this.catchingUp = true;
         }

         double sprintLine = BehaviorTuning.followSprintLine(ring);
         if (!this.catchingUp || gap <= sprintLine * 0.75) {
            this.sprinting = false;
         } else if (gap >= sprintLine) {
            this.sprinting = true;
         }

         boolean creep = this.creeping();
         if (creep) {
            this.sprinting = false;
         }

         if (this.catchingUp) {
            BlockPos beside = BlockPos.containing(this.ownerEstimate.x, this.ownerEstimate.y, this.ownerEstimate.z);
            if (this.sprinting) {
               move.bolt(beside, 1.0F);
            } else {
               double span = sprintLine - reach;
               double ramp = span > 0.0 ? Math.max(0.0, Math.min(1.0, (gap - reach) / span)) : 1.0;
               float share = 0.3F + 0.7F * (float)ramp;
               if (creep) {
                  share = Math.min(share, 0.3F);
               }

               move.goTo(beside, share * 1.0F);
            }
         } else {
            move.standBy(this.ownerId, ring);
         }
      }
   }

   @Override
   public void stop(AnimalHandle handle, MoveVerbs move, long now) {
      this.askedForPost = false;
      this.catchingUp = false;
      this.sprinting = false;
      this.walledOffAt = null;
      this.hiddenSince = Long.MIN_VALUE;
   }
}
