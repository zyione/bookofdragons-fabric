package net.magister.liberanimalium.fauna.steer;

import java.util.List;
import org.jetbrains.annotations.Nullable;
import net.magister.liberanimalium.fauna.api.AnimalHandle;
import net.magister.liberanimalium.fauna.api.MoveIntent;
import net.magister.liberanimalium.fauna.api.MoveMode;
import net.magister.liberanimalium.fauna.api.Wings;
import net.magister.liberanimalium.fauna.identity.Temper;
import net.magister.liberanimalium.fauna.identity.Trait;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

public final class Steering {
   private Steering() {
   }

   @Nullable
   public static MoveIntent steer(AnimalHandle handle, ContextRing ring, @Nullable Vec3 goal, float urgency, double distanceToRouteEnd, Temper temper) {
      return steer(handle, ring, goal, urgency, distanceToRouteEnd, List.of(), temper);
   }

   public static MoveIntent steer(
      AnimalHandle handle, ContextRing ring, @Nullable Vec3 goal, float urgency, double distanceToRouteEnd, List<Vec3> threats, Temper temper
   ) {
      return steer(handle, ring, new Approach(), goal, urgency, false, distanceToRouteEnd, threats, List.of(), temper);
   }

   public static MoveIntent steer(
      AnimalHandle handle,
      ContextRing ring,
      Approach approach,
      @Nullable Vec3 goal,
      float urgency,
      boolean running,
      double distanceToRouteEnd,
      List<Vec3> threats,
      List<Flock.Mate> flock,
      Temper temper
   ) {
      return steer(handle, ring, approach, goal, urgency, running, distanceToRouteEnd, threats, flock, List.of(), temper);
   }

   public static MoveIntent steer(
      AnimalHandle handle,
      ContextRing ring,
      Approach approach,
      @Nullable Vec3 goal,
      float urgency,
      boolean running,
      double distanceToRouteEnd,
      List<Vec3> threats,
      List<Flock.Mate> flock,
      List<Flock.Neighbour> neighbours,
      Temper temper
   ) {
      return steer(handle, ring, approach, goal, urgency, running, distanceToRouteEnd, threats, flock, neighbours, false, temper);
   }

   public static MoveIntent steer(
      AnimalHandle handle,
      ContextRing ring,
      Approach approach,
      @Nullable Vec3 goal,
      float urgency,
      boolean running,
      double distanceToRouteEnd,
      List<Vec3> threats,
      List<Flock.Mate> flock,
      List<Flock.Neighbour> neighbours,
      boolean planned,
      Temper temper
   ) {
      ring.begin();
      writeThreats(handle, ring, threats, temper.get(Trait.NERVOUSNESS));
      Flock.write(handle, ring, flock);
      Flock.separate(handle, ring, neighbours);
      if (goal == null) {
         return MoveIntent.hold(handle.currentMode());
      }

      Vec3 at = handle.bodyPosition();
      Vec3 toGoal = goal.subtract(at);
      double flatDistance = Math.sqrt(toGoal.x * toGoal.x + toGoal.z * toGoal.z);
      if (flatDistance < 0.001) {
         return handle.currentMode() != MoveMode.AIR
            ? MoveIntent.hold(handle.currentMode())
            : new MoveIntent(new Vec3(0.0, holdVertical(handle, goal.y - at.y), 0.0), 0.0F, handle.currentMode(), false);
      }

      Vec3 desired = new Vec3(toGoal.x / flatDistance, 0.0, toGoal.z / flatDistance);
      ring.addInterest(desired, 1.0F, ring.slots() / 4);
      probeTerrain(handle, ring);
      double chosenSlot = ring.decideSlot();
      if (Double.isNaN(chosenSlot)) {
         return MoveIntent.hold(handle.currentMode());
      }

      Vec3 chosen = deflect(desired, ring.deflectionFrom(chosenSlot, ring.slotOf(desired)) * ring.slotAngle());
      float paced = SteerTuning.pacedByEnergy(urgency, temper.get(Trait.ENERGY));
      float speed = Math.min(1.0F, paced * ring.confidence());
      float cut = 1.0F;
      if (breakingGround(handle)) {
         cut = Math.min(cut, 0.3F);
      }

      cut = Math.min(cut, turnSlowdown(handle, chosen));
      float arrival = 1.0F;
      if (handle.currentMode() != MoveMode.GROUND) {
         arrival = arrivalSlowdown(handle, approach, distanceToRouteEnd);
         cut = Math.min(cut, arrival);
      } else {
         approach.reset();
      }

      if (handle.currentMode() == MoveMode.AIR) {
         cut *= cut;
      }

      speed *= cut;
      double vertical = verticalDemand(handle, ring, goal, at, flatDistance, planned);
      return new MoveIntent(new Vec3(chosen.x, vertical, chosen.z), speed, handle.currentMode(), running);
   }

   private static double floorLift(AnimalHandle handle, Vec3 goal, Vec3 at) {
      double under = Wings.of(handle).airUnder(at, 8.0);
      double deficit = 1.0 - under / 8.0;
      if (deficit <= 0.0) {
         return 0.0;
      }

      double descent = Math.min(1.0, Math.max(0.0, (at.y - goal.y) / 4.0));
      return 1.0 * deficit * (1.0 - descent);
   }

   private static Vec3 deflect(Vec3 heading, double radians) {
      double cos = Math.cos(radians);
      double sin = Math.sin(radians);
      return new Vec3(heading.x * cos + heading.z * sin, 0.0, heading.z * cos - heading.x * sin);
   }

   private static void writeThreats(AnimalHandle handle, ContextRing ring, List<Vec3> threats, float nervousness) {
      if (!threats.isEmpty()) {
         int skirt = SteerTuning.threatSkirt(ring.slots(), nervousness);
         Vec3 at = handle.bodyPosition();

         for (Vec3 point : threats) {
            Vec3 toward = point.subtract(at);
            double distance = toward.length();
            if (!(distance < 0.001)) {
               float weight = (float)Mth.clamp(1.0 - distance / 12.0, 0.0, 1.0);
               if (!(weight <= 0.0F)) {
                  ring.addDanger(toward.normalize(), weight * 0.48000002F, skirt);
               }
            }
         }
      }
   }

   private static float turnSlowdown(AnimalHandle handle, Vec3 chosen) {
      Vec3 facing = handle.lookDirection();
      double facingFlat = Math.sqrt(facing.x * facing.x + facing.z * facing.z);
      if (facingFlat < 0.001) {
         return 1.0F;
      } else {
         double dot = (facing.x * chosen.x + facing.z * chosen.z) / facingFlat;
         double errorDegrees = Math.toDegrees(Math.acos(Math.max(-1.0, Math.min(1.0, dot))));
         boolean ground = handle.currentMode() == MoveMode.GROUND;
         float from = ground ? 25.0F : 0.0F;
         float t = (float)Math.max(0.0, Math.min(1.0, (errorDegrees - from) / (90.0F - from)));
         float floor = ground ? 0.5F : 0.35F;
         if (ground && errorDegrees > 90.0) {
            float k = (float)Math.min(1.0, (errorDegrees - 90.0) / 45.0);
            return floor - k * (floor - 0.05F);
         } else {
            return 1.0F - t * (1.0F - floor);
         }
      }
   }

   private static float arrivalSlowdown(AnimalHandle handle, Approach approach, double distanceToRouteEnd) {
      double distance = distanceToRouteEnd;
      double perTick = handle.velocity().length();
      double live = Math.max(handle.bounds().getXsize(), perTick * 20.0 * 1.0);
      double runway = approach.runwayFor(distance, live);
      return distance >= runway ? 1.0F : (float)Math.max(0.3F, distance / runway);
   }

   private static void verticalRelief(AnimalHandle handle, ContextRing ring, Vec3 ahead, float grade) {
      double step = SteerTuning.verticalRelief(handle.bounds().getYsize());
      int up = clearanceSteps(handle, ahead, step, 1);
      int down = clearanceSteps(handle, ahead, step, -1);
      if (up != down) {
         float margin = Math.abs(up - down) / 4.0F;
         ring.addVerticalDanger(down < up, 1.0F * grade * margin);
      }
   }

   private static int clearanceSteps(AnimalHandle handle, Vec3 ahead, double step, int sign) {
      for (int out = 1; out <= 4; out++) {
         if (!handle.isBlockedAt(ahead.add(0.0, sign * step * out, 0.0))) {
            return out;
         }
      }

      return 5;
   }

   private static double verticalDemand(AnimalHandle handle, ContextRing ring, Vec3 goal, Vec3 at, double flatDistance, boolean planned) {
      if (handle.currentMode() == MoveMode.GROUND) {
         return 0.0;
      } else if (breakingGround(handle)) {
         return 1.0;
      } else {
         double relief = ring.verticalBias() * 1.0;
         if (handle.currentMode() == MoveMode.AIR) {
            double angle = Math.atan2(goal.y - at.y, flatDistance);
            double slope = Math.tan(Math.max(-SteerTuning.MAX_FLIGHT_ANGLE, Math.min(SteerTuning.MAX_FLIGHT_ANGLE, angle)));
            Vec3 velocity = handle.velocity();
            double carried = Math.sqrt(velocity.x * velocity.x + velocity.z * velocity.z);
            double top = Math.max(0.001, handle.topSpeed(MoveMode.AIR));
            double rate = Math.max(carried / top, hoverFraction(handle));
            return slope * rate + relief + (planned ? 0.0 : floorLift(handle, goal, at));
         } else {
            double difference = goal.y - at.y;
            double wanted = Math.abs(difference) < 0.5 ? 0.0 : Math.max(-1.0, Math.min(1.0, difference / 4.0));
            return Math.max(-1.0, Math.min(1.0, wanted + relief));
         }
      }
   }

   private static boolean breakingGround(AnimalHandle handle) {
      return handle.currentMode() != MoveMode.GROUND && handle.bodyOnGround() && Wings.of(handle).takingOff();
   }

   private static Vec3 travelDirection(AnimalHandle handle) {
      Vec3 velocity = handle.velocity();
      double flat = Math.sqrt(velocity.x * velocity.x + velocity.z * velocity.z);
      if (flat > 0.001) {
         return new Vec3(velocity.x / flat, 0.0, velocity.z / flat);
      }

      Vec3 facing = handle.lookDirection();
      double facingFlat = Math.sqrt(facing.x * facing.x + facing.z * facing.z);
      return facingFlat > 0.001 ? new Vec3(facing.x / facingFlat, 0.0, facing.z / facingFlat) : new Vec3(0.0, 0.0, 1.0);
   }

   private static double holdVertical(AnimalHandle handle, double rise) {
      if (breakingGround(handle)) {
         return 1.0;
      }

      double wanted = Math.max(-1.0, Math.min(1.0, rise / 4.0));
      return wanted * hoverFraction(handle);
   }

   private static double hoverFraction(AnimalHandle handle) {
      return Math.max(0.0, Wings.of(handle).stallFraction());
   }

   private static void probeTerrain(AnimalHandle handle, ContextRing ring) {
      Vec3 at = handle.bodyPosition();
      double half = handle.bounds().getXsize() * 0.5;
      boolean grounded = handle.currentMode() == MoveMode.GROUND;
      double slotAngle = (Math.PI * 2) / ring.slots();
      double sideReach = half * 0.75;
      double frontReach = Math.max(sideReach * 2.0, Math.min(2.5, 1.5 + 1.0 * Math.min(1.0, handle.velocity().horizontalDistance())));
      Vec3 heading = travelDirection(handle);
      double cliffDrop = handle.stepHeight() * 3.0;
      Vec3 reliefAt = null;
      float reliefGrade = 0.0F;
      double reliefAlignment = -1.0;

      for (int slot = 0; slot < ring.slots(); slot++) {
         Vec3 dir = ring.direction(slot);
         double alignment = Math.max(0.0, heading.x * dir.x + heading.z * dir.z);
         double reach = sideReach + (frontReach - sideReach) * alignment * alignment;
         double groundY = at.y;
         double lastDistance = 0.0;
         double span = half + reach;

         for (int sample = 1; sample <= 3; sample++) {
            double distance = span * sample / 3.0;
            Vec3 ahead = at.add(dir.scale(distance));
            float grade = 1.0F - 0.65F * (sample - 1) / Math.max(1, 2);
            int skirt = (int)Math.round(Math.atan2(half, distance) / slotAngle);
            if (!grounded) {
               if (handle.isBlockedAt(ahead)) {
                  ring.addDangerAt(slot, 1.0F * grade, skirt);
                  if (alignment > reliefAlignment) {
                     reliefAlignment = alignment;
                     reliefAt = ahead;
                     reliefGrade = grade;
                  }
                  break;
               }
            } else {
               double travelled = distance - lastDistance;
               double maxRise = Math.max(handle.stepHeight() * (1.0 + travelled), handle.hopHeight());
               double next = handle.groundHeightNear(ahead, groundY, maxRise);
               if (Double.isNaN(next)) {
                  ring.addDangerAt(slot, 1.0F * grade, skirt);
                  break;
               }

               if (groundY - next > cliffDrop) {
                  ring.addDangerAt(slot, 0.8F * grade, skirt);
                  break;
               }

               groundY = next;
               lastDistance = distance;
            }
         }
      }

      if (reliefAt != null) {
         verticalRelief(handle, ring, reliefAt, reliefGrade);
      }
   }
}
