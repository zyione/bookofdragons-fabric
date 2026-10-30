package net.magister.liberanimalium.fauna.behavior;

import java.util.UUID;
import org.jetbrains.annotations.Nullable;
import net.magister.liberanimalium.fauna.api.AnimalHandle;
import net.magister.liberanimalium.fauna.api.Gullet;
import net.magister.liberanimalium.fauna.api.PoseFlag;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.Vec3;

public final class Forage implements Behavior {
   @Nullable
   private UUID lead;
   @Nullable
   private Vec3 itemAt;
   @Nullable
   private Vec3 pileAt;
   private boolean edible;
   private int dwell;
   @Nullable
   private UUID refused;
   private boolean routeFailed;
   private boolean wanted;

   public void fetch(@Nullable UUID id, @Nullable Vec3 at, @Nullable Vec3 pile, boolean isFood) {
      this.fetch(id, at, pile, isFood, false);
   }

   public void fetch(@Nullable UUID id, @Nullable Vec3 at, @Nullable Vec3 pile, boolean isFood, boolean wanted) {
      if (id == null || !id.equals(this.lead)) {
         this.dwell = 0;
      }

      this.lead = id;
      this.itemAt = at;
      this.pileAt = pile;
      this.edible = isFood;
      this.wanted = wanted;
   }

   public void noteFailure(boolean failed) {
      this.routeFailed = failed;
   }

   @Nullable
   public UUID takeRefused() {
      UUID was = this.refused;
      this.refused = null;
      return was;
   }

   @Override
   public void tick(AnimalHandle handle, MoveVerbs move, long now) {
      if (this.lead != null && this.itemAt != null && this.pileAt != null) {
         double reach = handle.bounds().getXsize() * 2.0;
         if (!(this.itemAt.distanceTo(handle.bodyPosition()) > reach)) {
            handle.applyLook(this.itemAt);
            if (this.wanted) {
               boolean taken = handle.endowed(Gullet.class).map(gullet -> gullet.takeItem(this.lead)).orElse(false);
               move.stop();
               if (!taken) {
                  this.refused = this.lead;
               }
            } else if (this.edible) {
               handle.endowed(Gullet.class).ifPresent(gullet -> gullet.eatItem(this.lead));
               move.stop();
            } else {
               handle.setPose(PoseFlag.ALERT, true);
               move.stop();
               if (++this.dwell >= 150) {
                  this.refused = this.lead;
               }
            }
         } else if (this.routeFailed) {
            this.refused = this.lead;
         } else {
            move.goTo(BlockPos.containing(this.pileAt), 1.0F);
         }
      }
   }

   @Override
   public void stop(AnimalHandle handle, MoveVerbs move, long now) {
      handle.setPose(PoseFlag.ALERT, false);
      handle.applyLook(null);
      this.lead = null;
      this.itemAt = null;
      this.pileAt = null;
      this.dwell = 0;
   }
}
