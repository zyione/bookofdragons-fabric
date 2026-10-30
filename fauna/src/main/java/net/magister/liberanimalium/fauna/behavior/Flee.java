package net.magister.liberanimalium.fauna.behavior;

import java.util.List;
import java.util.UUID;
import org.jetbrains.annotations.Nullable;
import net.magister.liberanimalium.fauna.api.AnimalHandle;
import net.magister.liberanimalium.fauna.drive.SurvivalTuning;
import net.minecraft.world.phys.Vec3;

public final class Flee implements Behavior {
   private List<Vec3> from = List.of();
   @Nullable
   private UUID lead;
   private float fear;
   @Nullable
   private Vec3 aimedAway;
   private long aimedAt = Long.MIN_VALUE;

   public void avoid(List<Vec3> points) {
      this.avoid(points, null);
   }

   public void avoid(List<Vec3> points, @Nullable UUID lead) {
      this.avoid(points, lead, 0.0F);
   }

   public void avoid(List<Vec3> points, @Nullable UUID lead, float fear) {
      this.from = points;
      this.lead = lead;
      this.fear = fear;
   }

   @Override
   public void tick(AnimalHandle handle, MoveVerbs move, long now) {
      if (!this.from.isEmpty()) {
         if (this.lead != null) {
            move.fleeing(this.lead, this.fear);
         }

         Vec3 at = handle.bodyPosition();
         Vec3 away = awayHeading(at, this.from);
         if (away != null) {
            if (move.idle()) {
               this.aim(move, away, now);
            } else if (this.staleAim(away, now)) {
               this.aim(move, away, now);
            }
         }
      }
   }

   private boolean staleAim(Vec3 away, long now) {
      if (this.aimedAway == null) {
         return true;
      } else {
         return now - this.aimedAt < 20L ? false : this.aimedAway.dot(away) < SurvivalTuning.RETREAT_REAIM_COS;
      }
   }

   private void aim(MoveVerbs move, Vec3 away, long now) {
      this.aimedAway = away;
      this.aimedAt = now;
      move.flee(this.from, 1.0F);
   }

   @Nullable
   private static Vec3 awayHeading(Vec3 at, List<Vec3> from) {
      Vec3 away = at.subtract(mean(from));
      return away.lengthSqr() < 1.0E-6 ? null : away.normalize();
   }

   private static Vec3 mean(List<Vec3> from) {
      Vec3 mean = Vec3.ZERO;

      for (Vec3 point : from) {
         mean = mean.add(point);
      }

      return mean.scale(1.0 / from.size());
   }

   @Override
   public void stop(AnimalHandle handle, MoveVerbs move, long now) {
      this.from = List.of();
      this.lead = null;
      this.fear = 0.0F;
      this.aimedAway = null;
      this.aimedAt = Long.MIN_VALUE;
   }
}
