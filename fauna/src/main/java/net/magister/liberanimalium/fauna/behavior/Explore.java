package net.magister.liberanimalium.fauna.behavior;

import java.util.function.Predicate;
import java.util.function.ToDoubleFunction;
import org.jetbrains.annotations.Nullable;
import net.magister.liberanimalium.fauna.api.AnimalHandle;
import net.magister.liberanimalium.fauna.api.MoveMode;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.Vec3;

public final class Explore implements Behavior {
   @Nullable
   private Vec3 biasHeading;
   @Nullable
   private Predicate<BlockPos> filter;
   @Nullable
   private ToDoubleFunction<Vec3> regard;
   private long nextLegAt;
   private boolean wasMoving;
   private boolean aloft;
   private int floor;
   private int ceiling;
   private int legsLeft;
   private float herdMoving;
   private int maxRange = 40;

   public void configure(@Nullable Vec3 bias, @Nullable Predicate<BlockPos> filter, @Nullable ToDoubleFunction<Vec3> regard, int maxRange) {
      this.configure(bias, filter, regard, maxRange, false, 0, 0);
   }

   public void configure(
      @Nullable Vec3 bias, @Nullable Predicate<BlockPos> filter, @Nullable ToDoubleFunction<Vec3> regard, int maxRange, boolean aloft, int floor, int ceiling
   ) {
      this.configure(bias, filter, regard, maxRange, aloft, floor, ceiling, 0.0F);
   }

   public void configure(
      @Nullable Vec3 bias,
      @Nullable Predicate<BlockPos> filter,
      @Nullable ToDoubleFunction<Vec3> regard,
      int maxRange,
      boolean aloft,
      int floor,
      int ceiling,
      float herdMoving
   ) {
      this.herdMoving = Math.max(0.0F, Math.min(1.0F, herdMoving));
      this.biasHeading = bias;
      this.filter = filter;
      this.regard = regard;
      this.maxRange = Math.max(7, maxRange);
      this.aloft = aloft;
      this.floor = floor;
      this.ceiling = Math.max(floor, ceiling);
   }

   @Override
   public void start(AnimalHandle handle, MoveVerbs move, long now) {
      this.nextLegAt = now;
      this.wasMoving = false;
   }

   @Override
   public void tick(AnimalHandle handle, MoveVerbs move, long now) {
      if (!move.idle()) {
         this.wasMoving = true;
      } else {
         boolean flying = handle.currentMode() == MoveMode.AIR;
         if (this.wasMoving && !flying) {
            this.wasMoving = false;
            this.nextLegAt = now + Math.round(60.0F * (1.0F - this.herdMoving * 0.75F));
         } else {
            this.wasMoving = false;
            if (!flying || this.aloft && this.legsLeft > 0) {
               if (now >= this.nextLegAt) {
                  int reach = Math.max(40, this.maxRange);
                  BlockPos destination = this.aloft
                     ? Roam.pickAloft(handle, now, 6, reach, this.biasHeading, this.filter, this.regard, this.floor, this.ceiling)
                     : Roam.pick(handle, now, 6, reach, this.biasHeading, this.filter, this.regard);
                  if (destination == null) {
                     this.nextLegAt = now + 20L;
                  } else {
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
               }
            } else {
               move.alight(1.0F);
               this.wasMoving = true;
            }
         }
      }
   }

   @Override
   public void stop(AnimalHandle handle, MoveVerbs move, long now) {
      this.biasHeading = null;
      this.filter = null;
      this.regard = null;
      this.wasMoving = false;
      this.herdMoving = 0.0F;
   }
}
