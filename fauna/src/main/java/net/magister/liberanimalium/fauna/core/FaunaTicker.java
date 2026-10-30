package net.magister.liberanimalium.fauna.core;

import java.util.List;
import java.util.Locale;
import net.magister.liberanimalium.fauna.api.AnimalHandle;
import net.magister.liberanimalium.fauna.debug.DriveAutopsy;
import net.magister.liberanimalium.fauna.debug.FaunaDebug;
import net.magister.liberanimalium.fauna.drive.Drive;
import net.magister.liberanimalium.fauna.drive.PlayDrive;
import net.magister.liberanimalium.fauna.drive.SocialTuning;
import net.magister.liberanimalium.fauna.know.Representation;
import net.magister.liberanimalium.fauna.know.Wants;
import net.magister.liberanimalium.fauna.reservoir.Reservoirs;
import net.magister.liberanimalium.fauna.steer.Flock;
import net.magister.liberanimalium.fauna.steer.Steering;
import net.magister.liberanimalium.util.PerfClock;
import net.minecraft.world.phys.Vec3;

public final class FaunaTicker {
   public static final List<Phase> ORDER = List.of(Phase.values());
   private static final PerfClock.Section[] PHASE_CLOCK = phaseClocks();

   static boolean inCompany(FaunaMind mind) {
      AnimalHandle handle = mind.handle();
      double ring = Flock.cohesionRadius(handle);
      Vec3 at = handle.bodyPosition();

      for (Representation rep : mind.knowledge().all()) {
         if (mind.knowledge().caresAbout(rep.id()) && mind.knowledge().estimate(rep, mind.age(), handle.species()).distanceTo(at) <= ring) {
            return true;
         }
      }

      return false;
   }

   static Reservoirs.Pastime pastime(FaunaMind mind) {
      if (mind.romping()) {
         return Reservoirs.Pastime.ROMPING;
      }

      Drive holder = mind.holder();
      return !mind.context().carrying() && (holder == null || !holder.occupies(mind.context())) ? Reservoirs.Pastime.IDLE : Reservoirs.Pastime.OCCUPIED;
   }

   static float playWant(FaunaMind mind) {
      boolean plays = mind.arbiter().drives().stream().anyMatch(d -> "play".equals(d.id()));
      return plays ? PlayDrive.want(mind.context()) : 0.0F;
   }

   private FaunaTicker() {
   }

   public static void tick(FaunaMind mind) {
      AnimalHandle handle = mind.handle();
      mind.advanceAge(handle.gameTime());
      int stagger = Math.floorMod(handle.faunaId().hashCode(), 20);
      long clock = mind.age() + stagger;
      double farLine = Math.max(48.0, handle.simulationRange() * 0.5);
      double playerDistance = handle.nearestPlayerDistance(farLine + 1.0);
      boolean far = playerDistance > farLine;
      mind.setFarThink(far);
      if (FaunaDebug.paused()) {
         if (!mind.paused()) {
            mind.collapse();
         }

         mind.setPaused(true);
      } else {
         mind.setPaused(false);
         boolean wasDown = mind.downed();
         boolean isDown = handle.incapacitated();
         mind.setDowned(isDown);
         if (isDown) {
            if (!wasDown) {
               mind.collapse();
            }
         } else {
            int thinkPeriod = far ? 8 : 2;
            boolean senseNow = clock % 20L == 0L;
            boolean reservoirNow = clock % 20L == 0L;
            boolean thinkNow = clock % thinkPeriod == 0L;

            for (Phase phase : ORDER) {
               boolean run = switch (phase) {
                  case IDENTITY -> true;
                  case SENSE -> senseNow;
                  case RESERVOIR -> reservoirNow;
                  case KNOW, DRIVE, ARBITER, BEHAVIOR, MANEUVER, MOVE -> thinkNow;
                  case STEER, ACTUATE -> true;
               };
               if (run) {
                  mind.notePhase(phase);
                  long phaseStart = PHASE_CLOCK[phase.ordinal()].start();

                  try {
                     runPhase(phase, mind);
                  } finally {
                     PHASE_CLOCK[phase.ordinal()].end(phaseStart);
                  }
               }
            }
         }
      }
   }

   private static PerfClock.Section[] phaseClocks() {
      Phase[] phases = Phase.values();
      PerfClock.Section[] clocks = new PerfClock.Section[phases.length];

      for (Phase phase : phases) {
         clocks[phase.ordinal()] = PerfClock.server("fauna." + phase.name().toLowerCase(Locale.ROOT));
      }

      return clocks;
   }

   private static void runPhase(Phase phase, FaunaMind mind) {
      switch (phase) {
         case IDENTITY:
            mind.temper();
            break;
         case SENSE:
            mind.perception().scan(mind.handle());
            break;
         case RESERVOIR:
            mind.reservoirs().refresh(mind.handle(), inCompany(mind), pastime(mind), PlayDrive.appetite(mind.context()));
            mind.publishPlayful();
            break;
         case KNOW:
            know(mind);
            break;
         case DRIVE:
            mind.arbiter().score(mind.context());
            DriveAutopsy.watch(mind.handle(), mind);
            break;
         case ARBITER:
            mind.setHolder(mind.arbiter().pick(mind.context()));
            mind.publishSignals();
            break;
         case BEHAVIOR:
            mind.runBehavior();
            Gaze.tick(mind);
            break;
         case MANEUVER:
            mind.runManeuvers();
            mind.publishStalk();
            break;
         case MOVE:
            mind.route().tick(mind.handle(), mind.age());
            break;
         case STEER:
            mind.setIntent(
               Steering.steer(
                  mind.handle(),
                  mind.ring(),
                  mind.approach(),
                  mind.route().effectiveTarget(mind.handle()),
                  mind.holder() == null ? 0.0F : mind.urgency(),
                  mind.running(),
                  mind.route().distanceToEnd(mind.handle().bodyPosition()),
                  mind.threatPoints(),
                  mind.flock(),
                  mind.neighbours(),
                  mind.route().planned(),
                  mind.temper()
               )
            );
            break;
         case ACTUATE:
            mind.handle().applyMove(mind.intent());
            mind.publishSprint();
            mind.publishGlide();
            mind.publishEdges();
      }
   }

   private static void know(FaunaMind mind) {
      PairSession.closeStale(mind.handle().gameTime(), 12000L, SocialTuning.SETTLED_KEEP_TICKS);
      Reservoirs stores = mind.reservoirs();
      Drive holder = mind.holder();
      Representation warned = holder == null ? null : holder.warns(mind.context());
      mind.knowledge()
         .integrate(
            mind.handle(),
            mind.perception(),
            mind.age(),
            mind.temper(),
            mind.cornered(),
            new Wants(
               stores.level("company"),
               stores.level("boredom"),
               stores.level("annoyance"),
               holder == null ? Wants.Want.NONE : holder.shows(),
               warned == null ? null : warned.id(),
               stores.level("sleep"),
               holder != null && holder.shows() == Wants.Want.HUNGRY ? mind.engagedId() : null,
               playWant(mind),
               PlayDrive.playsRough(mind.context())
            )
         );
      mind.integratePlaces();
      mind.publishStartle();
   }
}
