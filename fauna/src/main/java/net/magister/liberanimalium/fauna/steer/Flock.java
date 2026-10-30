package net.magister.liberanimalium.fauna.steer;

import java.util.List;
import java.util.UUID;
import net.magister.liberanimalium.fauna.api.AnimalHandle;
import net.magister.liberanimalium.fauna.api.MoveMode;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

public final class Flock {
   private Flock() {
   }

   public static double separationRadius(AnimalHandle handle) {
      return Math.max(1.0, handle.bounds().getXsize()) * 2.5;
   }

   public static double cohesionRadius(AnimalHandle handle) {
      return separationRadius(handle) * 2.0 * 2.0;
   }

   public static void write(AnimalHandle handle, ContextRing ring, List<Flock.Mate> mates) {
      if (!mates.isEmpty()) {
         double separation = separationRadius(handle);
         double alignment = separation * 2.0;
         double cohesion = cohesionRadius(handle);
         Vec3 at = handle.bodyPosition();
         Vec3 facing = handle.lookDirection();
         Vec3 headingSum = Vec3.ZERO;
         int headingCount = 0;
         Vec3 centreSum = Vec3.ZERO;
         int centreCount = 0;

         for (Flock.Mate mate : mates) {
            Vec3 toMate = new Vec3(mate.position().x - at.x, 0.0, mate.position().z - at.z);
            double distance = toMate.length();
            if (!(distance < 0.001) && !(distance > cohesion)) {
               float attention = attention(facing, toMate.scale(1.0 / distance));
               if (distance < separation) {
                  float crowding = (float)(1.0 - distance / separation);
                  push(ring, toMate.scale(1.0 / distance), crowding * attention);
               }

               if (distance < alignment && mate.velocity().lengthSqr() > 1.0E-6) {
                  headingSum = headingSum.add(mate.velocity().scale(attention));
                  headingCount++;
               }

               centreSum = centreSum.add(mate.position().scale(attention));
               centreCount++;
            }
         }

         if (headingCount > 0) {
            Vec3 heading = new Vec3(headingSum.x, 0.0, headingSum.z);
            if (heading.lengthSqr() > 1.0E-6) {
               ring.addInterest(heading.normalize(), 0.45F, ring.slots() / 4);
            }
         }

         if (centreCount > 0) {
            Vec3 centre = centreSum.scale(1.0 / centreCount);
            Vec3 toCentre = new Vec3(centre.x - at.x, 0.0, centre.z - at.z);
            if (toCentre.lengthSqr() > 1.0E-6) {
               ring.addInterest(toCentre.normalize(), 0.3F, ring.slots() / 4);
            }
         }
      }
   }

   public static void separate(AnimalHandle handle, ContextRing ring, List<Flock.Neighbour> neighbours) {
      if (!neighbours.isEmpty()) {
         double separation = separationRadius(handle);
         double height = handle.bounds().getYsize();
         boolean layered = handle.currentMode() != MoveMode.GROUND;
         Vec3 at = handle.bodyPosition();
         Vec3 facing = handle.lookDirection();
         double centre = at.y + height * 0.5;

         for (Flock.Neighbour body : neighbours) {
            Vec3 toIt = new Vec3(body.position().x - at.x, 0.0, body.position().z - at.z);
            double distance = toIt.length();
            if (!(distance < 0.001) && !(distance >= separation)) {
               double clear = height + body.height();
               double dy = body.position().y + body.height() * 0.5 - centre;
               double overlap = clear <= 0.0 ? 1.0 : Math.max(0.0, 1.0 - Math.abs(dy) / clear);
               if (!(overlap <= 0.0)) {
                  float attention = attention(facing, toIt.scale(1.0 / distance));
                  float crowding = (float)((1.0 - distance / separation) * overlap);
                  push(ring, toIt.scale(1.0 / distance), crowding * attention);
                  if (layered) {
                     boolean above = dy > 0.0 || dy == 0.0 && body.id().compareTo(handle.faunaId()) > 0;
                     ring.addVerticalDanger(above, crowding * 0.9F);
                  }
               }
            }
         }
      }
   }

   private static void push(ContextRing ring, Vec3 toward, float crowding) {
      ring.addDanger(toward, crowding * 0.9F, ring.slots() / 4);
   }

   private static float attention(Vec3 facing, Vec3 toMate) {
      Vec3 flat = new Vec3(facing.x, 0.0, facing.z);
      if (flat.lengthSqr() < 1.0E-6) {
         return 1.0F;
      }

      double dot = flat.normalize().dot(toMate);
      return (float)Mth.lerp((dot + 1.0) * 0.5, 0.35, 1.0);
   }

   public record Mate(Vec3 position, Vec3 velocity) {
   }

   public record Neighbour(UUID id, Vec3 position, float width, float height) {
   }
}
