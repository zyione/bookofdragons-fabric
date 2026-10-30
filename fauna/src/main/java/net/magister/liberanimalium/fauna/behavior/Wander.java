package net.magister.liberanimalium.fauna.behavior;

import java.util.function.Predicate;
import java.util.function.ToDoubleFunction;
import org.jetbrains.annotations.Nullable;
import net.magister.liberanimalium.fauna.api.AnimalHandle;
import net.magister.liberanimalium.fauna.api.MoveMode;
import net.magister.liberanimalium.fauna.core.Seeds;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.Vec3;

public final class Wander implements Behavior {
   private long nextDecisionAt;
   private boolean wasMoving;
   @Nullable
   private ToDoubleFunction<Vec3> regard;
   private boolean aloft;
   private int floor;
   private int ceiling;
   @Nullable
   private Predicate<BlockPos> filter;
   @Nullable
   private Vec3 heading;
   private float herdMoving;
   private int legsLeft;
   private int maxRange = 20;
   private static final float WANDER_URGENCY = 1.0F;

   public void configure(@Nullable ToDoubleFunction<Vec3> regard, int maxRange) {
      this.configure(regard, maxRange, false, 0, 0);
   }

   public void configure(@Nullable ToDoubleFunction<Vec3> regard, int maxRange, boolean aloft, int floor, int ceiling) {
      this.configure(regard, maxRange, aloft, floor, ceiling, null);
   }

   public void configure(@Nullable ToDoubleFunction<Vec3> regard, int maxRange, boolean aloft, int floor, int ceiling, @Nullable Predicate<BlockPos> filter) {
      this.configure(regard, maxRange, aloft, floor, ceiling, filter, null, 0.0F);
   }

   public void configure(
      @Nullable ToDoubleFunction<Vec3> regard,
      int maxRange,
      boolean aloft,
      int floor,
      int ceiling,
      @Nullable Predicate<BlockPos> filter,
      @Nullable Vec3 heading,
      float herdMoving
   ) {
      this.heading = heading;
      this.herdMoving = Math.max(0.0F, Math.min(1.0F, herdMoving));
      this.regard = regard;
      this.maxRange = Math.max(7, maxRange);
      this.aloft = aloft;
      this.floor = floor;
      this.ceiling = Math.max(floor, ceiling);
      this.filter = filter;
   }

   @Override
   public void start(AnimalHandle handle, MoveVerbs move, long now) {
      this.nextDecisionAt = now;
      this.wasMoving = false;
      if (handle.currentMode() == MoveMode.AIR && this.legsLeft <= 0) {
         this.legsLeft = 3;
      }
   }

   @Override
   public void tick(AnimalHandle handle, MoveVerbs move, long now) {
      if (!move.idle()) {
         this.wasMoving = true;
      } else {
         boolean flying = handle.currentMode() == MoveMode.AIR;
         if (this.wasMoving && !flying) {
            this.wasMoving = false;
            int spread = Math.floorMod(Seeds.mix(handle.faunaId(), now), 120);
            this.nextDecisionAt = now + 40L + Math.round(spread * (1.0F - this.herdMoving * 0.75F));
         } else {
            this.wasMoving = false;
            if (!flying || this.aloft && this.legsLeft > 0) {
               BlockPos destination = this.chooseDestination(handle, now);
               if (destination != null) {
                  if (this.aloft) {
                     if (!flying) {
                        this.legsLeft = 3;
                     }

                     move.cruise(destination, 1.0F);
                     this.legsLeft--;
                  } else {
                     move.goTo(destination, 1.0F);
                  }

                  this.wasMoving = true;
               }
            } else {
               move.alight(1.0F);
               this.wasMoving = true;
            }
         }
      }
   }

   public long waitRemaining(long now) {
      return Math.max(0L, this.nextDecisionAt - now);
   }

   @Nullable
   public BlockPos chooseDestination(AnimalHandle handle, long now) {
      if (now < this.nextDecisionAt) {
         return null;
      } else {
         BlockPos destination = this.aloft
            ? Roam.pickAloft(handle, now, 6, this.maxRange, this.heading, this.filter, this.regard, this.floor, this.ceiling)
            : Roam.pick(handle, now, 6, this.maxRange, this.heading, this.filter, this.regard);
         if (destination == null) {
            this.nextDecisionAt = now + 20L;
            return null;
         } else {
            return destination;
         }
      }
   }

   @Override
   public void stop(AnimalHandle handle, MoveVerbs move, long now) {
      this.wasMoving = false;
      this.regard = null;
      this.heading = null;
      this.herdMoving = 0.0F;
   }
}
