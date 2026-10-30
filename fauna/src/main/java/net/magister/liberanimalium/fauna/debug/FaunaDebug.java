package net.magister.liberanimalium.fauna.debug;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.magister.liberanimalium.fauna.api.AnimalHandle;
import net.magister.liberanimalium.fauna.api.Halter;
import net.magister.liberanimalium.fauna.api.Order;
import net.magister.liberanimalium.fauna.api.Relation;
import net.magister.liberanimalium.fauna.api.SensedBody;
import net.magister.liberanimalium.fauna.api.SpeciesProfile;
import net.magister.liberanimalium.fauna.arbiter.Arbiter;
import net.magister.liberanimalium.fauna.behavior.Engage;
import net.magister.liberanimalium.fauna.core.FaunaMind;
import net.magister.liberanimalium.fauna.core.Phase;
import net.magister.liberanimalium.fauna.drive.BidShape;
import net.magister.liberanimalium.fauna.identity.Trait;
import net.magister.liberanimalium.fauna.know.Bend;
import net.magister.liberanimalium.fauna.know.Courtship;
import net.magister.liberanimalium.fauna.know.Representation;
import net.magister.liberanimalium.fauna.know.Territory;
import net.magister.liberanimalium.fauna.maneuver.MoveStyles;
import net.magister.liberanimalium.fauna.maneuver.Posture;
import net.magister.liberanimalium.fauna.maneuver.pattern.TargetedManeuver;
import net.magister.liberanimalium.fauna.move.NavPath;
import net.magister.liberanimalium.fauna.sense.Perception;
import net.magister.liberanimalium.fauna.steer.ContextRing;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

public final class FaunaDebug {
   private static final double RANGE = 96.0;
   private static final Set<UUID> ENABLED = ConcurrentHashMap.newKeySet();
   private static volatile boolean paused;

   private FaunaDebug() {
   }

   public static boolean toggle(UUID playerId) {
      if (!ENABLED.remove(playerId)) {
         ENABLED.add(playerId);
         return true;
      } else {
         forgetIfUnwatched();
         return false;
      }
   }

   public static void setEnabled(UUID playerId, boolean on) {
      if (on) {
         ENABLED.add(playerId);
      } else {
         ENABLED.remove(playerId);
         forgetIfUnwatched();
      }
   }

   public static boolean paused() {
      return paused;
   }

   public static void pause(boolean on) {
      paused = on;
   }

   private static void forgetIfUnwatched() {
      if (!anyoneWatching()) {
         StimulusLog.clearAll();
      }
   }

   public static boolean anyoneWatching() {
      return !ENABLED.isEmpty();
   }

   public static boolean isWatching(UUID playerId) {
      return ENABLED.contains(playerId);
   }

   public static double range() {
      return 96.0;
   }

   public static FaunaSnapshot snapshot(AnimalHandle handle, FaunaMind mind) {
      List<NavPath.Step> steps = mind.route().debugSteps();
      List<BlockPos> nodes = new ArrayList<>(steps.size());
      List<Byte> modes = new ArrayList<>(steps.size());

      for (NavPath.Step step : steps) {
         nodes.add(step.node().pos());
         modes.add((byte)step.node().mode().ordinal());
      }

      Arbiter arbiter = mind.arbiter();
      List<String> names = new ArrayList<>(arbiter.drives().size());
      List<Float> scores = new ArrayList<>(arbiter.drives().size());
      int holder = -1;

      for (int i = 0; i < arbiter.drives().size(); i++) {
         names.add(arbiter.drives().get(i).id());
         scores.add(arbiter.scoreOf(i));
         if (arbiter.drives().get(i) == arbiter.holder()) {
            holder = i;
         }
      }

      List<FaunaSnapshot.Known> known = new ArrayList<>();

      for (Representation rep : mind.knowledge().byFind(mind.age())) {
         SensedBody scanned = null;

         for (Perception.Seen seen : mind.perception().visible()) {
            if (seen.body().id().equals(rep.id())) {
               scanned = seen.body();
               break;
            }
         }

         known.add(
            new FaunaSnapshot.Known(
               rep.id(),
               rep.typeKey(),
               standing(mind, handle, rep),
               rep.visible(),
               rep.find(mind.age()),
               rep.lastSightScore(),
               rep.age(mind.age()),
               BlockPos.containing(rep.lastKnownPos()),
               BlockPos.containing(mind.knowledge().estimate(rep, mind.age(), handle.species())),
               scanned == null ? -1 : scanned.lightLevel(),
               scanned != null && scanned.sneaking(),
               scanned != null && scanned.inWater(),
               rep.manner(),
               rep.attribution(),
               rep.crowding()
            )
         );
      }

      Territory terr = mind.knowledge().faculty(Territory.class).orElse(null);
      ContextRing ring = mind.ring();
      float[] interest = new float[ring.slots()];
      float[] danger = new float[ring.slots()];

      for (int i = 0; i < ring.slots(); i++) {
         interest[i] = ring.interestAt(i);
         danger[i] = ring.dangerAt(i);
      }

      float[] intent = null;
      if (mind.intent() != null) {
         Vec3 dir = mind.intent().direction();
         intent = new float[]{(float)dir.x, (float)dir.y, (float)dir.z, mind.intent().speed()};
      }

      SpeciesProfile.Senses senses = handle.species().senses();
      Vec3 facing = handle.sightDirection();
      Vec3 eyeOffset = handle.eyePosition().subtract(handle.bodyPosition());
      return new FaunaSnapshot(
         handle.faunaId(),
         handle.entity().getType().getDescription().getString(),
         mind.age(),
         mind.farThink(),
         handle.currentMode().name(),
         handle.health(),
         handle.maxHealth(),
         mind.route().debugGoal(),
         nodes,
         modes,
         mind.route().debugIndex(),
         routeState(mind),
         mind.route().lastFailure(),
         names,
         scores,
         holder,
         mind.reservoirs().level("hunger"),
         mind.arbiter().releaseLevel(),
         mind.reservoirs().level("energy"),
         mind.reservoirs().level("annoyance"),
         mind.knowledge().obedience().compliance(),
         mind.knowledge().obedience().equilibrium(),
         Halter.of(handle).order().name(),
         mind.knowledge().obedience().fightClaim(),
         mind.knowledge().obedience().demand(Order.STAY),
         mind.knowledge().obedience().demand(Order.FOLLOW),
         beatOf(mind),
         mind.rungName(),
         beatTicksOf(mind),
         runnerUpOf(mind),
         marginOf(mind),
         postureOf(mind),
         weaponOf(mind),
         firingOf(mind),
         outrunOf(mind),
         tankSpentOf(mind),
         breakingOffOf(mind),
         mind.noises() == null ? 0.0F : mind.noises().count(),
         mind.context().freshGround(),
         terr != null && terr.homeCenter() != null ? BlockPos.containing(terr.homeCenter()) : null,
         terr == null ? 0.0F : terr.radius(),
         terr == null ? null : terr.roostAnchor(),
         terr != null && terr.avoidedCenter() != null ? BlockPos.containing(terr.avoidedCenter()) : null,
         terr == null ? 0.0F : terr.avoidedRadius(),
         terr == null ? 0L : terr.exileRemaining(mind.age()),
         known,
         mind.company(),
         StimulusLog.pings(handle.faunaId(), mind.age()),
         interest,
         danger,
         intent,
         new float[]{(float)facing.x, (float)facing.y, (float)facing.z},
         new float[]{(float)eyeOffset.x, (float)eyeOffset.y, (float)eyeOffset.z},
         (float)handle.velocity().length(),
         senses.sightRadius(),
         senses.perfectVisionDeg(),
         senses.peripheralDeg(),
         senses.blindSpotDeg(),
         senses.nightVision(),
         senses.hearingSkill(),
         mind.places() == null ? 0 : mind.places().markCount(),
         mind.stuckPressure(),
         mind.temper().readings(),
         mind.runsOf(Phase.SENSE),
         mind.runsOf(Phase.DRIVE),
         mind.runsOf(Phase.STEER),
         mind.reservoirs().sleepDebt(),
         mind.sleepPhase(),
         sleepThetaOf(mind),
         mind.handle().asleep(),
         mind.knowledge().bed() == null ? null : mind.knowledge().bed().at(),
         mind.knowledge().bed() == null ? 0.0F : mind.knowledge().bed().score(),
         mind.knowledge().bed() == null ? "" : mind.knowledge().bed().source().name()
      );
   }

   private static float sleepThetaOf(FaunaMind mind) {
      float languor = 1.0F - mind.temper().get(Trait.ENERGY);
      float active = BidShape.threshold(languor, 0.9F, 1.0F);
      float inactive = BidShape.threshold(languor, 0.2F, 0.35F);
      return Mth.lerp(mind.sleepPhase(), active, inactive);
   }

   private static String beatOf(FaunaMind mind) {
      TargetedManeuver beat = mind.maneuvers().active();
      if (beat == null) {
         return "";
      } else {
         return beat.phase().isEmpty() ? beat.name() : beat.name() + "/" + beat.phase();
      }
   }

   private static int beatTicksOf(FaunaMind mind) {
      TargetedManeuver beat = mind.maneuvers().active();
      return beat == null ? 0 : beat.ticksRun();
   }

   private static String runnerUpOf(FaunaMind mind) {
      MoveStyles.Choice choice = mind.maneuvers().choice();
      return choice != null && choice.runnerUp() != null ? choice.runnerUp().name() : "";
   }

   private static float marginOf(FaunaMind mind) {
      MoveStyles.Choice choice = mind.maneuvers().choice();
      return choice == null ? 0.0F : choice.margin();
   }

   private static float[] postureOf(FaunaMind mind) {
      Posture posture = mind.maneuvers().posture();
      return new float[]{posture.radial(), posture.tangential(), posture.covert(), posture.grip()};
   }

   private static String weaponOf(FaunaMind mind) {
      Engage fight = mind.engagement();
      if (fight == null) {
         return "";
      }

      int quiet = fight.quietTicks(mind.age());
      return quiet > 0 ? fight.weapon().name() + " quiet " + quiet + "t" : fight.weapon().name();
   }

   private static boolean firingOf(FaunaMind mind) {
      Engage fight = mind.engagement();
      return fight != null && fight.firing();
   }

   private static boolean outrunOf(FaunaMind mind) {
      Engage fight = mind.engagement();
      return fight != null && fight.beingOutrun();
   }

   private static boolean tankSpentOf(FaunaMind mind) {
      Engage fight = mind.engagement();
      return fight != null && fight.tankSpent();
   }

   private static boolean breakingOffOf(FaunaMind mind) {
      Engage fight = mind.engagement();
      return fight != null && fight.breakingOff();
   }

   private static String routeState(FaunaMind mind) {
      if (!mind.route().idle()) {
         return mind.route().progress();
      }

      long wait = mind.wander().waitRemaining(mind.age());
      return wait > 0L ? "next leg " + wait + "t" : "ready";
   }

   private static String standing(FaunaMind mind, AnimalHandle handle, Representation rep) {
      Relation live = rep.relation();
      Relation remembered = mind.knowledge().standings().of(rep.id());
      Relation underlying = remembered != null ? remembered : rep.staticRelation();
      float like = mind.knowledge().social().of(rep.id()).like();
      if (!live.matters() && !Bend.isNegative(underlying.type()) && like == 0.0F) {
         return "-";
      }

      boolean household = rep.owned() && Halter.of(handle).ownerId() != null;
      float line = Bend.packLine(underlying, household ? Courtship.comfort(mind.temper()) : 0.0F);
      return String.format("%s %.2f  bond %.2f/%.2f", live.type(), live.intensity(), like, line)
         + (live.type() != underlying.type() ? String.format("  over %s %.2f", underlying.type(), underlying.intensity()) : "")
         + (rep.vouched() > 0.0F ? String.format("  vouched %.2f", rep.vouched()) : "");
   }
}
