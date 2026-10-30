package net.magister.liberanimalium.fauna.behavior;

import java.util.UUID;
import java.util.function.Predicate;
import org.jetbrains.annotations.Nullable;
import net.magister.liberanimalium.fauna.api.AnimalHandle;
import net.magister.liberanimalium.fauna.api.Arms;
import net.magister.liberanimalium.fauna.api.PoseFlag;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

public final class Hunt implements Behavior, Fighting {
   @Nullable
   private Vec3 estimate;
   @Nullable
   private UUID targetId;
   private Fighter self = Fighter.NONE;
   private Bout bout = Bout.NONE;
   private boolean asPrey = true;
   private final Engage engage = new Engage();
   private long nextPatrolAt;
   @Nullable
   private Predicate<BlockPos> patrolFilter;
   private boolean inContact = true;
   private boolean searching;

   public void patrolWithin(@Nullable Predicate<BlockPos> filter) {
      this.patrolFilter = filter;
   }

   public void pursue(@Nullable UUID id, @Nullable Vec3 believedPosition, Fighter self, Bout bout) {
      this.pursue(id, believedPosition, self, bout, true);
   }

   public void pursue(@Nullable UUID id, @Nullable Vec3 believedPosition, Fighter self, Bout bout, boolean asPrey) {
      this.targetId = id;
      this.estimate = believedPosition;
      this.self = self;
      this.bout = bout;
      this.asPrey = asPrey;
   }

   public void contact(boolean inContact) {
      this.inContact = inContact;
   }

   public boolean huntingForFood() {
      return this.asPrey;
   }

   @Nullable
   public UUID target() {
      return this.targetId;
   }

   @Nullable
   @Override
   public UUID engagedWith() {
      return this.targetId;
   }

   @Override
   public Engage engagement() {
      return this.engage;
   }

   @Override
   public boolean armed() {
      return !this.asPrey;
   }

   public static int patrolInterval(float hunger) {
      float t = Mth.clamp(hunger, 0.0F, 1.0F);
      int floor = 40;
      int ceiling = floor * 4;
      return Math.max(floor, Math.round(Mth.lerp(t, ceiling, floor)));
   }

   @Override
   public void start(AnimalHandle handle, MoveVerbs move, long now) {
      this.engage.reset(handle);
      this.nextPatrolAt = now;
   }

   @Override
   public void tick(AnimalHandle handle, MoveVerbs move, long now) {
      if (this.estimate != null && this.targetId != null) {
         if (!this.asPrey && !this.inContact) {
            this.engage.lose(handle);
            this.searching = true;
            handle.setPose(PoseFlag.ALERT, true);
            handle.applyLook(this.estimate);
            move.goTo(BlockPos.containing(this.estimate), 1.0F);
         } else {
            if (this.searching) {
               this.searching = false;
               handle.setPose(PoseFlag.ALERT, false);
            }

            double contact = Arms.of(handle).strikeRange();
            double halves = handle.clearanceTo(this.bout.girth());
            Engage.Weapon elected = this.engage
               .electWeapon(handle, this.targetId, this.estimate, contact, this.asPrey, !this.asPrey, this.self.bravery(), this.self.nervousness());
            float commit = this.engage
               .commitment(this.self.aggression(), this.self.bravery(), this.self.nervousness(), handle.healthFraction(), this.bout.observed());
            this.engage.tick(handle, this.targetId, this.estimate, contact, elected, this.asPrey, this.self, this.bout, now);
            this.engage.strike(handle, this.targetId, this.estimate, elected, contact, this.self.energy());
            double[] band = this.engage.band(elected, contact, halves, commit);
            if (this.asPrey) {
               move.stalk(this.targetId, band[0], band[1], this.engage.pace(1.0F));
            } else {
               move.engage(this.targetId, band[0], band[1], this.engage.pace(1.0F));
            }
         }
      } else {
         if (this.asPrey) {
            this.patrol(handle, move, now);
         } else {
            move.stop();
         }
      }
   }

   private void patrol(AnimalHandle handle, MoveVerbs move, long now) {
      if (move.idle()) {
         if (now >= this.nextPatrolAt) {
            int interval = patrolInterval(handle.hungerLevel());
            BlockPos destination = Roam.pick(handle, now, 10, 40, null, this.patrolFilter);
            if (destination == null) {
               this.nextPatrolAt = now + 40L;
            } else {
               this.nextPatrolAt = now + interval;
               move.goTo(destination, 1.0F);
            }
         }
      }
   }

   @Override
   public void stop(AnimalHandle handle, MoveVerbs move, long now) {
      this.engage.reset(handle);
      if (this.searching) {
         handle.setPose(PoseFlag.ALERT, false);
      }

      this.searching = false;
      this.inContact = true;
      this.estimate = null;
      this.targetId = null;
      this.nextPatrolAt = Long.MIN_VALUE;
   }
}
