package net.magister.liberanimalium.fauna.core;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import org.jetbrains.annotations.Nullable;
import net.magister.liberanimalium.fauna.api.AnimalHandle;
import net.magister.liberanimalium.fauna.api.Arms;
import net.magister.liberanimalium.fauna.api.MoveMode;
import net.magister.liberanimalium.fauna.know.Representation;
import net.minecraft.world.phys.Vec3;

final class Gaze {
   private Gaze() {
   }

   static void tick(FaunaMind mind) {
      AnimalHandle handle = mind.handle();
      Gaze.Hold hold = mind.gazeHold();
      if (!handle.asleep() && !handle.incapacitated()) {
         long now = window(handle.faunaId(), mind.age());
         if (!hold.holds(now)) {
            choose(mind, handle, hold, now);
         }

         Vec3 at = hold.point();
         if (hold.creature() != null) {
            Representation rep = mind.knowledge().about(hold.creature());
            at = rep != null && rep.visible() ? mind.knowledge().estimate(rep, mind.age(), handle.species()) : null;
         } else if (at != null && passed(handle.eyePosition(), at)) {
            at = null;
         }

         boolean turns = mind.route().idle() && handle.currentMode() == MoveMode.GROUND;
         if (at != null && (turns || Arms.of(handle).canAimAt(at))) {
            handle.gazeAt(at, hold.creature());
         } else {
            hold.rest();
            handle.gazeAt(null);
         }
      } else {
         hold.rest();
         handle.gazeAt(null);
      }
   }

   static boolean passed(Vec3 eye, Vec3 point) {
      double dx = point.x - eye.x;
      double dz = point.z - eye.z;
      return dx * dx + dz * dz < 4.0;
   }

   private static void choose(FaunaMind mind, AnimalHandle handle, Gaze.Hold hold, long now) {
      long window = window(handle.faunaId(), mind.age());
      List<Representation> seen = new ArrayList<>();

      for (Representation rep : mind.knowledge().all()) {
         if (rep.visible()) {
            seen.add(rep);
         }
      }

      seen.sort(Comparator.comparingDouble(rep -> interest(rep.id(), window)));
      List<Vec3> creatures = new ArrayList<>(seen.size());

      for (Representation rep : seen) {
         creatures.add(mind.knowledge().estimate(rep, mind.age(), handle.species()));
      }

      Vec3 going = mind.route().idle() ? null : mind.route().pursuitTarget(handle);
      Vec3 target = pick(handle.faunaId(), mind.age(), handle.eyePosition(), handle.lookDirection(), creatures, going);
      int which = target == null ? -1 : creatures.indexOf(target);
      hold.take(now, target, which >= 0 && creatures.get(which) == target ? seen.get(which).id() : null);
   }

   private static long window(UUID id, long now) {
      long jitter = Math.floorMod(Seeds.mix(id, 3L), 100);
      return Math.floorDiv(now + jitter, 100);
   }

   private static double interest(UUID creature, long window) {
      return Seeds.unit(creature, window + 7L);
   }

   @Nullable
   static Vec3 pick(UUID id, long now, Vec3 eye, Vec3 facing, List<Vec3> creatures, @Nullable Vec3 going) {
      long window = window(id, now);
      if (!creatures.isEmpty() && Seeds.unit(id, window) < 0.7F) {
         return creatures.get(0);
      } else if (Seeds.unit(id, window + 11L) < 0.5F) {
         double ahead = Math.atan2(facing.z, facing.x);
         double reach = going != null ? 25.0 : 120.0;
         double aside = Math.toRadians((Seeds.unit(id, window + 7L) * 2.0 - 1.0) * reach);
         return level(eye, ahead + aside);
      } else if (going != null) {
         double dx = going.x - eye.x;
         double dz = going.z - eye.z;
         return dx * dx + dz * dz < 1.0 ? null : level(eye, Math.atan2(dz, dx));
      } else {
         return null;
      }
   }

   private static Vec3 level(Vec3 eye, double yaw) {
      return eye.add(Math.cos(yaw) * 8.0, 0.0, Math.sin(yaw) * 8.0);
   }

   static final class Hold {
      private long window = Long.MIN_VALUE;
      @Nullable
      private Vec3 point;
      @Nullable
      private UUID creature;

      boolean holds(long window) {
         return this.window == window;
      }

      void take(long window, @Nullable Vec3 point, @Nullable UUID creature) {
         this.window = window;
         this.point = point;
         this.creature = creature;
      }

      void rest() {
         this.point = null;
         this.creature = null;
      }

      @Nullable
      Vec3 point() {
         return this.point;
      }

      @Nullable
      UUID creature() {
         return this.creature;
      }
   }
}
