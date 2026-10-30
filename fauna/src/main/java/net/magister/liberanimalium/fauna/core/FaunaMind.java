package net.magister.liberanimalium.fauna.core;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Predicate;
import java.util.function.ToDoubleFunction;
import org.jetbrains.annotations.Nullable;
import net.magister.liberanimalium.fauna.api.AnimalHandle;
import net.magister.liberanimalium.fauna.api.Arms;
import net.magister.liberanimalium.fauna.api.DayCycle;
import net.magister.liberanimalium.fauna.api.Delivery;
import net.magister.liberanimalium.fauna.api.ExpressionKey;
import net.magister.liberanimalium.fauna.api.Halter;
import net.magister.liberanimalium.fauna.api.Kind;
import net.magister.liberanimalium.fauna.api.Kindness;
import net.magister.liberanimalium.fauna.api.Ladder;
import net.magister.liberanimalium.fauna.api.MoveIntent;
import net.magister.liberanimalium.fauna.api.MoveMode;
import net.magister.liberanimalium.fauna.api.Order;
import net.magister.liberanimalium.fauna.api.PoseFlag;
import net.magister.liberanimalium.fauna.api.RelationType;
import net.magister.liberanimalium.fauna.api.ShelterProbe;
import net.magister.liberanimalium.fauna.api.Signals;
import net.magister.liberanimalium.fauna.api.SpeciesProfile;
import net.magister.liberanimalium.fauna.api.Stance;
import net.magister.liberanimalium.fauna.api.Terrain;
import net.magister.liberanimalium.fauna.api.Wings;
import net.magister.liberanimalium.fauna.arbiter.Arbiter;
import net.magister.liberanimalium.fauna.behavior.AbilityElection;
import net.magister.liberanimalium.fauna.behavior.Behavior;
import net.magister.liberanimalium.fauna.behavior.Bout;
import net.magister.liberanimalium.fauna.behavior.Engage;
import net.magister.liberanimalium.fauna.behavior.Fighter;
import net.magister.liberanimalium.fauna.behavior.Fighting;
import net.magister.liberanimalium.fauna.behavior.Laddered;
import net.magister.liberanimalium.fauna.behavior.Solicit;
import net.magister.liberanimalium.fauna.behavior.Wander;
import net.magister.liberanimalium.fauna.drive.Drive;
import net.magister.liberanimalium.fauna.drive.DriveContext;
import net.magister.liberanimalium.fauna.drive.EnmityDrive;
import net.magister.liberanimalium.fauna.drive.PlayDrive;
import net.magister.liberanimalium.fauna.drive.SpaceDrive;
import net.magister.liberanimalium.fauna.drive.Stances;
import net.magister.liberanimalium.fauna.drive.ThreatDrive;
import net.magister.liberanimalium.fauna.drive.WarnDrive;
import net.magister.liberanimalium.fauna.identity.Temper;
import net.magister.liberanimalium.fauna.identity.Trait;
import net.magister.liberanimalium.fauna.know.Bedding;
import net.magister.liberanimalium.fauna.know.Company;
import net.magister.liberanimalium.fauna.know.Courtship;
import net.magister.liberanimalium.fauna.know.Crowding;
import net.magister.liberanimalium.fauna.know.Faculty;
import net.magister.liberanimalium.fauna.know.Knowledge;
import net.magister.liberanimalium.fauna.know.Larder;
import net.magister.liberanimalium.fauna.know.Manner;
import net.magister.liberanimalium.fauna.know.Noises;
import net.magister.liberanimalium.fauna.know.Places;
import net.magister.liberanimalium.fauna.know.Representation;
import net.magister.liberanimalium.fauna.know.Territory;
import net.magister.liberanimalium.fauna.know.Ties;
import net.magister.liberanimalium.fauna.maneuver.Brief;
import net.magister.liberanimalium.fauna.maneuver.ManeuverRunner;
import net.magister.liberanimalium.fauna.maneuver.ManeuverTuning;
import net.magister.liberanimalium.fauna.maneuver.Medium;
import net.magister.liberanimalium.fauna.maneuver.MoveStyles;
import net.magister.liberanimalium.fauna.maneuver.Posture;
import net.magister.liberanimalium.fauna.maneuver.Situation;
import net.magister.liberanimalium.fauna.maneuver.Subject;
import net.magister.liberanimalium.fauna.move.MoveTuning;
import net.magister.liberanimalium.fauna.move.RouteFollower;
import net.magister.liberanimalium.fauna.nature.HandoffContext;
import net.magister.liberanimalium.fauna.nature.Nature;
import net.magister.liberanimalium.fauna.nature.Natures;
import net.magister.liberanimalium.fauna.nature.Pursuit;
import net.magister.liberanimalium.fauna.reservoir.Reservoirs;
import net.magister.liberanimalium.fauna.sense.Perception;
import net.magister.liberanimalium.fauna.steer.Approach;
import net.magister.liberanimalium.fauna.steer.ContextRing;
import net.magister.liberanimalium.fauna.steer.Flock;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.phys.Vec3;

public final class FaunaMind {
   private final AnimalHandle handle;
   private final Map<Phase, Integer> phaseRuns = new EnumMap<>(Phase.class);
   private Phase lastPhase;
   private long age;
   private final MindClock clock = new MindClock();
   private MoveIntent intent;
   private boolean farThink;
   @Nullable
   private Temper temper;
   @Nullable
   private UUID temperId;
   @Nullable
   private SpeciesProfile temperSpecies;
   private final Perception perception = new Perception();
   private final Knowledge knowledge;
   private float stuckPressure;
   private final Nature nature;
   private final List<Pursuit> pursuits;
   private final Map<String, Pursuit> pursuitsByDrive;
   private final Arbiter arbiter;
   private final RouteFollower route = new RouteFollower();
   private final ContextRing ring = new ContextRing(16);
   private final Approach approach = new Approach();
   private final Reservoirs reservoirs = new Reservoirs();
   private final Verbs verbs;
   @Nullable
   private BlockPos forcedDestination;
   private long forcedUntil;
   private final ManeuverRunner maneuvers = new ManeuverRunner();
   private final Legs legs;
   @Nullable
   private Brief pendingBeat;
   private boolean routeFailedThisThink;
   @Nullable
   private Pursuit runningPursuit;
   private final DriveContext context = new MindContext(this);
   private boolean wasStartled;
   private final long[] poseForcedUntil = new long[PoseFlag.values().length];
   private String lastDrive;
   private String lastRung;
   private String lastRungDrive;
   private boolean lastRungQuiet;
   private String lastBeat;
   @Nullable
   private MoveMode lastMode;
   private boolean downed;
   private boolean paused;
   @Nullable
   private Medium.Verdict medium;
   private long mediumSince;
   @Nullable
   private Medium.Choice mediumChoice;
   @Nullable
   private Brief lastAct;
   private long lastActAt;
   private boolean glideTold;
   private boolean pendingHover;
   private long pendingHoverUntil;
   private long abilityPressedAt;
   private long companyAt;
   private List<Company.Source> companyField;
   @Nullable
   private Medium.Verdict forcedMedium;
   private float flightFear;
   @Nullable
   private MoveMode lastBeatMode;
   @Nullable
   private Drive holder;
   private final Gaze.Hold gazeHold;
   private final List<String> omitted;
   private final ToDoubleFunction<Vec3> regard;
   private final HandoffContext handoffContext;

   @Nullable
   public Noises noises() {
      return this.knowledge.faculty(Noises.class).orElse(null);
   }

   @Nullable
   private <T extends Behavior> T behavior(String driveId, Class<T> type) {
      Pursuit pursuit = this.pursuitsByDrive.get(driveId);
      return pursuit != null && type.isInstance(pursuit.behavior()) ? type.cast(pursuit.behavior()) : null;
   }

   public void forget(UUID id) {
      this.forget(id, false);
   }

   public void forget(UUID id, boolean mayReturn) {
      this.perception.forget(id);
      this.knowledge.forget(id, mayReturn);

      for (Pursuit pursuit : this.pursuits) {
         pursuit.handoff().forget(id);
      }
   }

   public Approach approach() {
      return this.approach;
   }

   public FaunaMind(AnimalHandle handle) {
      this(handle, Natures.shape(handle.nature(), handle.species().drives(), handle.species().toString()));
   }

   public FaunaMind(AnimalHandle handle, Nature nature) {
      Arrays.fill(this.poseForcedUntil, Long.MIN_VALUE);
      this.lastDrive = "";
      this.lastRung = "";
      this.lastRungDrive = "";
      this.lastBeat = "";
      this.lastActAt = Long.MIN_VALUE;
      this.abilityPressedAt = Long.MIN_VALUE;
      this.companyAt = Long.MIN_VALUE;
      this.companyField = List.of();
      this.gazeHold = new Gaze.Hold();
      this.omitted = new ArrayList<>();
      this.regard = this::companyRegard;
      this.handoffContext = new HandoffContext() {
         @Override
         public AnimalHandle handle() {
            return FaunaMind.this.handle;
         }

         @Override
         public Knowledge knowledge() {
            return FaunaMind.this.knowledge;
         }

         @Override
         public DriveContext drives() {
            return FaunaMind.this.context;
         }

         @Override
         public long age() {
            return FaunaMind.this.age;
         }

         @Override
         public boolean routeFailed() {
            return FaunaMind.this.routeFailedThisThink;
         }

         @Override
         public Fighter fighter() {
            return FaunaMind.this.fighter();
         }

         @Override
         public Bout boutAgainst(@Nullable Representation target) {
            return FaunaMind.this.boutAgainst(target);
         }

         @Nullable
         @Override
         public Subject subjectOf(UUID id) {
            return FaunaMind.this.subjectOf(id);
         }

         @Override
         public ToDoubleFunction<Vec3> regard() {
            return FaunaMind.this.regard;
         }

         @Override
         public int ambientRange() {
            return FaunaMind.this.ambientRange();
         }

         @Override
         public boolean aloft() {
            return FaunaMind.this.aloft();
         }

         @Nullable
         @Override
         public Predicate<BlockPos> withinReach() {
            return FaunaMind.this.withinReach();
         }

         @Nullable
         @Override
         public Vec3 patrolHeading(Vec3 home) {
            return FaunaMind.this.patrolHeading(home);
         }

         @Override
         public Ladder ladderFor() {
            return FaunaMind.this.ladderFor();
         }

         @Override
         public double standoffFor(Representation holder) {
            return FaunaMind.this.standoffFor(holder);
         }
      };
      this.handle = handle;
      this.nature = nature.fitTo(handle, this.omitted::add);
      this.knowledge = new Knowledge(this.nature.faculties());
      this.pursuits = this.nature.pursuits();
      Map<String, Pursuit> byDrive = new LinkedHashMap<>();

      for (Pursuit pursuit : this.pursuits) {
         byDrive.put(pursuit.id(), pursuit);
      }

      this.pursuitsByDrive = Map.copyOf(byDrive);
      this.arbiter = new Arbiter(this.pursuits.stream().map(Pursuit::drive).toList());
      this.verbs = new Verbs(this, handle, this.route);
      this.legs = new Legs(this.verbs, this.route);
      this.knowledge.faculty(Places.class).ifPresent(places -> {
         this.route.rememberDangerVia(node -> places.dangerAt(new Vec3(node.x() + 0.5, node.y(), node.z() + 0.5), this.age) * 8.0F);
         this.route.reportUnreachableTo(at -> places.mark(new Vec3(at.getX() + 0.5, at.getY(), at.getZ() + 0.5), 0.5F, this.age));
         this.route.reportReachTo(reach -> places.fence(reach.centre(), reach.radius(), this.age));
      });

      for (Phase p : Phase.values()) {
         this.phaseRuns.put(p, 0);
      }
   }

   public Reservoirs reservoirs() {
      return this.reservoirs;
   }

   public Temper temper() {
      UUID id = this.handle.faunaId();
      SpeciesProfile species = this.handle.species();
      if (this.temper == null || !id.equals(this.temperId) || species != this.temperSpecies) {
         this.temper = Temper.of(id, species);
         this.temperId = id;
         this.temperSpecies = species;
      }

      return this.temper;
   }

   public List<Vec3> threatPoints() {
      return ThreatDrive.threatPoints(this.context);
   }

   public List<Flock.Mate> flock() {
      List<Flock.Mate> mates = new ArrayList<>();

      for (Representation rep : this.knowledge.all()) {
         if (this.knowledge.caresAbout(rep.id())) {
            mates.add(new Flock.Mate(this.knowledge.estimate(rep, this.age, this.handle.species()), rep.lastVelocity()));
         }
      }

      return mates;
   }

   public List<Flock.Neighbour> neighbours() {
      List<Flock.Neighbour> out = new ArrayList<>();
      Fighting fighting = this.runningFight();
      UUID engaged = fighting == null ? null : fighting.engagedWith();
      double wideEnough = this.handle.bounds().getXsize() * 0.75;

      for (Representation rep : this.knowledge.all()) {
         if (rep.visible() && !rep.isPlayer() && !rep.id().equals(engaged) && !(rep.width() < wideEnough)) {
            out.add(new Flock.Neighbour(rep.id(), this.knowledge.estimate(rep, this.age, this.handle.species()), rep.width(), rep.height()));
         }
      }

      return out;
   }

   public float contagion() {
      int startled = 0;
      int total = 0;

      for (Representation rep : this.knowledge.all()) {
         if (rep.relation().type() == RelationType.PACK) {
            total++;
            if (rep.startledWithin(this.age, 30)) {
               startled++;
            }
         }
      }

      return total == 0 ? 0.0F : (float)startled / total;
   }

   void publishStartle() {
      float worst = this.worstAlarmSpike();
      boolean startled = worst >= 0.5F;
      this.handle.setPose(PoseFlag.STARTLED, startled);
      if (startled && !this.wasStartled) {
         this.reservoirs.startle();
      }

      this.wasStartled = startled;
   }

   void publishPlayful() {
      boolean plays = this.arbiter.drives().stream().anyMatch(d -> "play".equals(d.id()));
      this.handle.setPose(PoseFlag.PLAYFUL, plays && !this.handle.asleep() && PlayDrive.playful(this.context()));
   }

   private float worstAlarmSpike() {
      float worst = 0.0F;

      for (Representation rep : this.knowledge.all()) {
         if (rep.relation().type() == RelationType.AFRAID) {
            worst = Math.max(worst, rep.alarmSpike());
         }
      }

      return worst;
   }

   void publishSignals() {
      Stance stance = this.outwardStance();
      float wary = this.wariness();
      this.handle.applySignals(new Signals(stance, wary, this.activity(), this.temper().readings(), this.asking(), Regard.read(this, stance, wary)));
   }

   private boolean asking() {
      Solicit beg = this.behavior("beg", Solicit.class);
      return beg != null && beg.asking();
   }

   private Stance outwardStance() {
      Stance best = Stance.NONE;
      List<Drive> all = this.arbiter.drives();

      for (int i = 0; i < all.size(); i++) {
         if (!(this.arbiter.scoreOf(i) <= 0.02F)) {
            Stance shown = Stances.shownBy(all.get(i).id());
            if (shown.outranks(best)) {
               best = shown;
            }
         }
      }

      return best;
   }

   private float wariness() {
      float worst = 0.0F;

      for (Representation rep : this.knowledge.all()) {
         worst = Math.max(worst, rep.alarmEnvelope());
      }

      return worst;
   }

   private String activity() {
      return this.holder == null ? "" : this.holder.id();
   }

   void publishStalk() {
      this.setPoseWithOverride(PoseFlag.STALKING, (this.maneuvers.concealing() || Regard.creepingWithOwner(this)) && !Halter.of(this.handle).steered());
   }

   void publishSprint() {
      this.setPoseWithOverride(PoseFlag.SPRINTING, this.intent != null && this.intent.running() && this.intent.moving() && !Halter.of(this.handle).steered());
   }

   public long forcePose(PoseFlag flag, boolean on) {
      long until = on ? this.age + 1536L : Long.MIN_VALUE;
      this.poseForcedUntil[flag.ordinal()] = until;
      this.handle.setPose(flag, on || this.forcedPose(flag));
      return until;
   }

   public boolean forcedPose(PoseFlag flag) {
      return this.age < this.poseForcedUntil[flag.ordinal()];
   }

   private void setPoseWithOverride(PoseFlag flag, boolean on) {
      this.handle.setPose(flag, on || this.forcedPose(flag));
   }

   void publishEdges() {
      String drive = this.holder == null ? "" : this.holder.id();
      if (!drive.equals(this.lastDrive)) {
         if (!this.lastDrive.isEmpty()) {
            this.emit(ExpressionKey.drive(this.lastDrive, ExpressionKey.Edge.END));
         }

         if (!drive.isEmpty()) {
            this.emit(ExpressionKey.drive(drive, ExpressionKey.Edge.BEGIN));
         }

         this.lastDrive = drive;
      }

      String rung = this.rungName().toLowerCase(Locale.ROOT);
      String rungDrive = rung.isEmpty() ? "" : drive;
      if (!rung.equals(this.lastRung) || !rungDrive.equals(this.lastRungDrive)) {
         boolean quiet = this.runningBehavior() instanceof Laddered ladder && ladder.quietRung();
         if (!this.lastRung.isEmpty() && !this.lastRungDrive.isEmpty() && !this.lastRungQuiet) {
            this.emit(ExpressionKey.rung(this.lastRungDrive, this.lastRung, ExpressionKey.Edge.EXIT));
         }

         if (!rung.isEmpty() && !rungDrive.isEmpty() && !quiet) {
            this.emit(ExpressionKey.rung(rungDrive, rung, ExpressionKey.Edge.ENTER));
         }

         this.lastRung = rung;
         this.lastRungDrive = rungDrive;
         this.lastRungQuiet = quiet;
      }

      MoveStyles.Choice choice = this.maneuvers.active() == null ? null : this.maneuvers.choice();
      String beat = choice == null ? "" : choice.primitive().name().toLowerCase(Locale.ROOT);
      if (!beat.equals(this.lastBeat)) {
         if (!this.lastBeat.isEmpty()) {
            this.emit(ExpressionKey.beat(this.lastBeat, ExpressionKey.Edge.END));
         }

         if (!beat.isEmpty()) {
            this.emit(ExpressionKey.beat(beat, ExpressionKey.Edge.BEGIN));
         }

         this.lastBeat = beat;
      }

      MoveMode mode = this.handle.currentMode();
      if (mode != this.lastMode) {
         if (this.lastMode != null && mode != null) {
            this.emit(ExpressionKey.mode(this.lastMode.name(), mode.name()));
         }

         this.lastMode = mode;
      }

      for (ExpressionKey key : this.knowledge.drainMoments()) {
         this.emit(key);
      }
   }

   private void emit(ExpressionKey key) {
      this.handle.express(key, Warnings.subjectOf(this, key));
   }

   public void forceEvent(ExpressionKey key) {
      this.emit(key);
   }

   @Nullable
   Behavior runningBehavior() {
      return this.runningPursuit == null ? null : this.runningPursuit.behavior();
   }

   public float urgency() {
      return this.verbs.urgency();
   }

   public boolean running() {
      return this.verbs.running();
   }

   @Nullable
   public UUID engagedId() {
      Fighting fight = this.runningFight();
      return fight == null ? null : fight.engagedWith();
   }

   @Nullable
   private Fighting runningFight() {
      return this.runningBehavior() instanceof Fighting fight ? fight : null;
   }

   public String rungName() {
      return this.runningBehavior() instanceof Laddered ladder ? ladder.rungName() : "";
   }

   boolean romping() {
      Behavior running = this.runningBehavior();
      return running != null && running.romping();
   }

   void runBehavior() {
      this.issueForcedRoute();
      Pursuit next = this.holder == null ? null : this.pursuitsByDrive.get(this.holder.id());
      if (next != this.runningPursuit) {
         if (this.runningPursuit != null) {
            this.dropBehavior();
         }

         this.runningPursuit = next;
         if (this.runningPursuit != null) {
            this.verbs.gait(false);
            this.runningPursuit.behavior().start(this.handle, this.verbs, this.age);
         }
      }

      if (this.runningPursuit != null) {
         this.brief();
         this.runningPursuit.behavior().tick(this.handle, this.verbs, this.age);
         this.debrief();
      }

      this.electAbility();
      this.electMedium();
      this.keepFlying();
   }

   private void dropBehavior() {
      this.runningPursuit.behavior().stop(this.handle, this.verbs, this.age);
      this.verbs.stop();
      this.collectYield();
      this.runningPursuit = null;
   }

   void collapse() {
      if (this.runningPursuit != null) {
         this.dropBehavior();
      }

      this.maneuvers.clear(this.handle, this.legs);
      this.pendingBeat = null;
      this.holder = null;
      this.intent = null;
      this.verbs.stop();
      this.handle.applyMove(MoveIntent.hold(this.handle.currentMode()));
   }

   public boolean downed() {
      return this.downed;
   }

   void setDowned(boolean value) {
      this.downed = value;
   }

   boolean paused() {
      return this.paused;
   }

   void setPaused(boolean value) {
      this.paused = value;
   }

   private void electMedium() {
      Situation situation = Situation.TRAVEL;
      Subject subject = null;
      float fear = ThreatDrive.fear(this.context);
      if (this.lastAct != null && this.age - this.lastActAt <= 8L) {
         situation = this.lastAct.situation();
         subject = this.lastAct.subject();
         if (situation == Situation.FLEE) {
            fear = Math.max(fear, this.flightFear);
         }
      }

      Medium.Choice choice = MoveStyles.medium(this.handle, situation, this.context::trait, subject, this.medium, this.age - this.mediumSince, fear);
      if (this.forcedDestination != null && this.forcedMedium != null) {
         choice = new Medium.Choice(this.forcedMedium, Medium.biasFor(this.forcedMedium), "forced");
      }

      if (choice.verdict() != this.medium) {
         this.medium = choice.verdict();
         this.mediumSince = this.age;
         this.verbs.mediumChanged(this.medium);
      }

      this.mediumChoice = choice;
      this.verbs.bias(choice.bias());
   }

   private int ambientRange() {
      return this.ambientRange(this.mediumChoice == null ? null : this.mediumChoice.verdict());
   }

   private boolean aloft() {
      return this.mediumChoice != null && this.mediumChoice.verdict() == Medium.Verdict.AIR;
   }

   private int ambientRange(@Nullable Medium.Verdict verdict) {
      int base = 20;
      if (verdict != Medium.Verdict.AIR) {
         return base;
      }

      Float airCost = this.handle.species().movement().cost().get(MoveMode.AIR);
      if (airCost != null && Float.isFinite(airCost)) {
         double crossing = MoveTuning.airCrossingDistance(airCost, 0.5F);
         if (!Double.isFinite(crossing)) {
            return base;
         }

         double wanted = crossing * 1.5;
         return (int)Math.min(this.handle.navRange(), Math.max(base, wanted));
      } else {
         return base;
      }
   }

   @Nullable
   private Vec3 patrolHeading(Vec3 home) {
      Vec3 out = this.handle.bodyPosition().subtract(home);
      Vec3 flat = new Vec3(out.x, 0.0, out.z);
      if (flat.lengthSqr() < 1.0E-4) {
         return null;
      }

      Vec3 unit = flat.normalize();
      int hand = this.ownHand();
      return new Vec3(-unit.z * hand, 0.0, unit.x * hand);
   }

   void publishGlide() {
      double hover = Wings.of(this.handle).stallFraction();
      this.glideTold = this.handle.currentMode() == MoveMode.AIR
         && this.intent != null
         && this.intent.moving()
         && (this.intent.direction().y < -hover || this.glideTold && this.intent.direction().y < -hover * 0.5)
         && !Halter.of(this.handle).steered();
      this.setPoseWithOverride(PoseFlag.GLIDING, this.glideTold);
   }

   public void forceAlight() {
      this.verbs.alight(1.0F);
   }

   public void forceLoiter(double radius) {
      this.route.orbit(this.handle.bodyPosition(), radius, this.ownHand());
   }

   public void forceTakeoff() {
      this.handle.requestMoveMode(MoveMode.AIR);
      this.pendingHover = true;
      this.pendingHoverUntil = this.age + MoveStyles.mediumDwell(this.handle);
   }

   @Nullable
   public BlockPos forcePatrol() {
      Territory territory = this.knowledge().faculty(Territory.class).orElse(null);
      Vec3 home = territory != null && territory.homeCenter() != null && territory.radius() > 0.0F ? territory.homeCenter() : this.handle.bodyPosition();
      Vec3 heading = this.patrolHeading(home);
      if (heading == null) {
         Vec3 look = this.handle.lookDirection();
         Vec3 flat = new Vec3(look.x, 0.0, look.z);
         heading = flat.lengthSqr() < 1.0E-4 ? new Vec3(0.0, 0.0, 1.0) : flat.normalize();
      }

      int range = this.ambientRange(Medium.Verdict.AIR);
      Vec3 aim = this.handle.bodyPosition().add(heading.scale(range));
      BlockPos site = Wings.of(this.handle).landingSiteNear(BlockPos.containing(aim), (int)this.handle.navRange());
      if (site == null) {
         return null;
      }

      BlockPos aloft = this.liftIntoBand(site);
      if (aloft == null) {
         return null;
      }

      this.forceRoute(aloft, Medium.Verdict.AIR);
      return aloft;
   }

   @Nullable
   public BlockPos liftIntoBand(BlockPos site) {
      double width = this.handle.bounds().getXsize();
      int floor = MoveTuning.skyClearance(width);
      int ceiling = MoveTuning.cruiseCeiling(width);
      BlockPos aloft = site.above((floor + ceiling) / 2);
      return this.handle.isBlockedAt(Vec3.atCenterOf(aloft)) ? null : aloft;
   }

   public String mediumLine() {
      return this.mediumChoice == null ? "-" : this.mediumChoice.verdict() + " (" + this.mediumChoice.why() + ")";
   }

   private void keepFlying() {
      if (this.handle.currentMode() == MoveMode.AIR) {
         if (this.handle.bodyOnGround() && !Wings.of(this.handle).takingOff()) {
            this.touchDown();
         } else {
            if (this.pendingHover) {
               this.pendingHover = false;
               if (this.age <= this.pendingHoverUntil) {
                  this.route.hover(this.handle.bodyPosition());
                  return;
               }
            }

            if (this.pendingBeat == null) {
               if (this.route.idle()) {
                  this.verbs.alight(1.0F);
               }
            } else {
               Situation situation = this.pendingBeat.situation();
               boolean grounded = Medium.grounded(situation);
               boolean electedGround = Medium.landsToward(situation) && this.mediumChoice != null && this.mediumChoice.verdict() == Medium.Verdict.GROUND;
               boolean spent = this.maneuvers.failedInARow() >= 4;
               if (grounded || electedGround || spent) {
                  if (this.route.landing(this.handle)) {
                     float pace = this.pendingBeat.urgency();
                     this.maneuvers.clear(this.handle, this.legs);
                     this.pendingBeat = null;
                     this.verbs.pace(pace);
                     return;
                  }

                  Vec3 beside = this.pendingBeat.subject().estimate();
                  float urgency = this.pendingBeat.urgency();
                  BlockPos site = Medium.landsToward(situation) ? this.verbs.landingNear(beside) : this.verbs.landingHere();
                  if (site != null) {
                     this.maneuvers.clear(this.handle, this.legs);
                     this.pendingBeat = null;
                     this.verbs.goTo(site, urgency);
                  }
               }
            }
         }
      }
   }

   private void touchDown() {
      this.route.touchedDown();
      this.handle.requestMoveMode(MoveMode.GROUND);
   }

   private void electAbility() {
      Fighting fight = this.runningFight();
      boolean pressed = Arms.of(this.handle)
         .electAbility(
            AbilityElection.armedAgainst(fight),
            AbilityElection.spent(fight),
            AbilityElection.fleeing(this.runningBehavior()),
            AbilityElection.commitment(fight, this.age)
         );
      if (pressed) {
         this.abilityPressedAt = this.age;
      }
   }

   public long sinceAbilityPress() {
      return this.abilityPressedAt == Long.MIN_VALUE ? -1L : this.age - this.abilityPressedAt;
   }

   public List<Company.Source> company() {
      if (this.companyAt != this.age) {
         this.companyAt = this.age;
         this.companyField = Company.sources(
            this.knowledge.all(),
            this.knowledge.social(),
            this.age,
            Company.pull(this.context.trait(Trait.SYMPATHY)),
            this.handle.senses().sightRadius(),
            this.context::tie
         );
      }

      return this.companyField;
   }

   private double companyRegard(Vec3 point) {
      return Company.score(point, this.company()) - Ties.strayed(point, Halter.of(this.handle).home());
   }

   public boolean forceCompany(UUID other, float like) {
      if (this.knowledge.about(other) == null) {
         return false;
      }

      this.knowledge.social().stage(other, like);
      this.companyAt = Long.MIN_VALUE;
      return true;
   }

   public boolean forceRival(UUID other) {
      if (!this.knowledge.forceRelation(other, RelationType.RIVAL, 1.0F)) {
         return false;
      }

      PairSession.breakTruce(this.handle.faunaId(), other, "territory_contest");
      return true;
   }

   public boolean forceStrike(UUID other) {
      return this.knowledge.forceRelation(other, RelationType.EATS, 1.0F);
   }

   public boolean forceWary(UUID other, float intensity) {
      return this.knowledge.forceRelation(other, RelationType.AFRAID, intensity);
   }

   public float[] warnReading() {
      return new float[]{
         WarnDrive.fearShadow(this.context),
         WarnDrive.warShadow(this.context),
         0.2F,
         WarnDrive.actBid(this.context),
         ThreatDrive.theta(this.context),
         EnmityDrive.theta(this.context),
         0.6F
      };
   }

   public String warnSide() {
      return WarnDrive.leadingSide(this.context);
   }

   public boolean forceEstrange(float cost) {
      UUID owner = Halter.of(this.handle).ownerId();
      if (owner == null) {
         return false;
      }

      this.knowledge.social().betrayed(owner, cost);
      return true;
   }

   public boolean forceEnemy(UUID other) {
      return this.knowledge.forceRelation(other, RelationType.ATTACKS, 1.0F);
   }

   public float forceManner(UUID other, float level) {
      return this.knowledge.forceManner(other, level);
   }

   public boolean forceCrowd(UUID other, float level) {
      return this.knowledge.forceCrowd(other, level, this.age + 1536L);
   }

   @Nullable
   public float[] crowdingReading(UUID other) {
      Representation rep = this.knowledge.about(other);
      return rep == null
         ? null
         : new float[]{
            Crowding.space(this.handle.bounds().getXsize(), this.context.trait(Trait.SYMPATHY), this.context.trait(Trait.NERVOUSNESS)),
            rep.crowding(),
            SpaceDrive.threshold(this.context),
            SpaceDrive.pressure(this.context)
         };
   }

   private double standoffFor(Representation holder) {
      float sympathy = this.context.trait(Trait.SYMPATHY);
      float nervousness = this.context.trait(Trait.NERVOUSNESS);
      return Crowding.solicitStandoff(
         Crowding.space(this.handle.bounds().getXsize(), sympathy, nervousness),
         this.handle.senses().sightRadius(),
         Crowding.guardedness(sympathy, nervousness),
         this.trustToward(holder)
      );
   }

   private float trustToward(Representation holder) {
      return Crowding.trust(this.knowledge.social().of(holder.id()).like(), holder.alarm());
   }

   public boolean beggingAt(UUID candidate) {
      return this.runningBehavior() instanceof Solicit beg && beg.beggingAt(candidate);
   }

   public boolean forceMeal(UUID holder, int count, Delivery delivery) {
      Representation about = this.knowledge.about(holder);
      if (about == null) {
         return false;
      }

      int meals = Math.min(Math.max(count, 1), 16);

      for (int n = 0; n < meals; n++) {
         this.perception
            .deliver(
               this.handle, new Kindness(holder, new Kind(about.typeKey(), Set.of(), about.sameSpecies()), this.handle.bodyPosition(), this.age, delivery)
            );
      }

      return true;
   }

   public boolean forceStage(UUID courter, int rung) {
      if (this.knowledge.about(courter) == null) {
         return false;
      }

      this.knowledge.courtship().forceRung(courter, rung, Halter.of(this.handle).rideable(), this.knowledge.ceremony(this.handle));
      return true;
   }

   public int forceCheck(UUID courter) {
      if (this.knowledge.about(courter) == null) {
         return 0;
      }

      boolean rideable = Halter.of(this.handle).rideable();
      Courtship courtship = this.knowledge.courtship();
      if (courtship.complete(courter, rideable)) {
         return 0;
      }

      courtship.pass(courter, rideable, this.knowledge.ceremony(this.handle));
      return courtship.rung(courter);
   }

   public Ladder ladderFor() {
      return Halter.of(this.handle).stagePool();
   }

   public boolean forceBeg(UUID holder, float craving) {
      if (this.knowledge.about(holder) == null) {
         return false;
      }

      this.knowledge.forceOffer(holder, craving, this.age + 1536L);
      return true;
   }

   @Nullable
   public double[] begReading(UUID holder) {
      Representation rep = this.knowledge.about(holder);
      if (rep == null) {
         return null;
      }

      float space = Crowding.space(this.handle.bounds().getXsize(), this.context.trait(Trait.SYMPATHY), this.context.trait(Trait.NERVOUSNESS));
      return new double[]{
         this.standoffFor(rep),
         this.trustToward(rep),
         this.knowledge.estimate(rep, this.age, this.handle.species()).distanceTo(this.handle.bodyPosition()),
         space
      };
   }

   public int snubbedCount() {
      return this.knowledge.faculty(Larder.class).map(Larder::snubCount).orElse(0);
   }

   @Nullable
   public float[] mannerReading(UUID other) {
      Representation rep = this.knowledge.about(other);
      return rep == null
         ? null
         : new float[]{
            rep.manner(), rep.attribution(), Manner.gainFactor(rep.manner()), Manner.pressureFactor(rep.manner()), rep.armament(), rep.armorFraction()
         };
   }

   public float forceSleep(float level) {
      this.reservoirs.sleepTo(level);
      return this.reservoirs.sleepDebt();
   }

   public List<FaunaMind.BedProbe> forceBed() {
      List<FaunaMind.BedProbe> out = new ArrayList<>();
      Terrain terrain = this.handle.terrain();
      ShelterProbe.Body body = new ShelterProbe.Body(this.handle.bounds().getXsize(), this.handle.eyePosition().y - this.handle.bodyPosition().y);
      double radius = Bedding.searchRadius(this.handle);
      Vec3 centre = this.knowledge.faculty(Territory.class).map(Territory::homeCenter).orElse(null);
      if (centre == null) {
         centre = this.handle.bodyPosition();
      }

      int span = (int)Math.ceil(radius);

      for (int dx = -span; dx <= span; dx += 2) {
         for (int dz = -span; dz <= span; dz += 2) {
            if (!(dx * dx + dz * dz > radius * radius)) {
               BlockPos at = BlockPos.containing(centre).offset(dx, 0, dz);
               if (ShelterProbe.roofed(terrain, at)) {
                  out.add(new FaunaMind.BedProbe(at, ShelterProbe.score(terrain, body, Vec3.atBottomCenterOf(at)), terrain.standable(at)));
               }
            }
         }
      }

      this.knowledge.searchBed(this.handle, this.age);
      return out;
   }

   public float forceAnnoy(float level) {
      this.reservoirs.startleTo(level);
      return this.reservoirs.annoyance();
   }

   public float[] annoyanceBand() {
      float aggression = this.context.trait(Trait.AGGRESSION);
      float nerves = this.context.trait(Trait.NERVOUSNESS);
      return new float[]{this.reservoirs.annoyance(), Engage.breakPatience(aggression, nerves), Engage.resumeAt(aggression, nerves)};
   }

   @Nullable
   public double[] strikeGeometry(UUID other) {
      Subject subject = this.subjectOf(other);
      if (subject == null) {
         return null;
      }

      double gate = Arms.of(this.handle).strikeRange();
      double halves = this.handle.clearanceTo(subject.girth());
      float grip = Posture.of(Situation.HUNT, this.context::trait, this.context.healthFraction(), (float)subject.attentionOn(this.handle.bodyPosition()))
         .grip();
      int dwell = Math.max(7, ManeuverTuning.dwellTicks(this.context.trait(Trait.ENERGY)));
      return new double[]{gate, halves, Engage.meleeReach(gate, halves) + halves, grip, dwell};
   }

   public void forceRoute(@Nullable BlockPos destination) {
      this.forceRoute(destination, null);
   }

   public void forceRoute(@Nullable BlockPos destination, @Nullable Medium.Verdict medium) {
      this.forcedDestination = destination == null ? null : BlockPos.containing(this.handle.keepPlace(Vec3.atBottomCenterOf(destination)));
      this.forcedMedium = destination == null ? null : medium;
      this.forcedUntil = destination == null ? this.age : this.age + 1536L;
   }

   @Nullable
   BlockPos forcedDestination() {
      if (this.forcedDestination == null) {
         return null;
      }

      Vec3 now = this.handle.placeNow(Vec3.atBottomCenterOf(this.forcedDestination));
      return now == null ? null : BlockPos.containing(now);
   }

   private void issueForcedRoute() {
      if (this.forcedDestination != null) {
         BlockPos pin = this.forcedDestination();
         if (this.age >= this.forcedUntil || pin == null) {
            this.forceRoute(null);
         } else if (this.route.arrived() && pin.equals(this.route.debugGoal())) {
            boolean aloft = this.forcedMedium == Medium.Verdict.AIR && this.handle.currentMode() == MoveMode.AIR;
            this.forceRoute(null);
            if (aloft) {
               this.verbs.alight(1.0F);
            }
         } else {
            this.verbs.order(pin, 1.0F);
         }
      }
   }

   private void brief() {
      this.runningPursuit.handoff().brief(this.handoffContext);
   }

   private void debrief() {
      Fighting fighting = this.runningFight();
      this.reservoirs.chafe(fighting == null ? 0.0F : fighting.engagement().takePressure());
      boolean failed = this.verbs.failed();
      boolean proved = this.verbs.lastFailureCertified();
      this.routeFailedThisThink = failed;
      this.stuckPressure = stuckAfter(this.stuckPressure, failed);
      if (proved && fighting != null && fighting.engagedWith() != null) {
         this.knowledge.noteUnreachable(fighting.engagedWith(), this.age);
      }

      this.runningPursuit.handoff().debrief(this.handoffContext);
   }

   private Fighter fighter() {
      return new Fighter(
         this.context.trait(Trait.BRAVERY), this.context.trait(Trait.AGGRESSION), this.context.trait(Trait.NERVOUSNESS), this.context.trait(Trait.ENERGY)
      );
   }

   private Bout boutAgainst(@Nullable Representation target) {
      float observed = 0.0F;
      float girth = 0.0F;
      boolean hurtRecently = target != null && target.hurtWithin(this.age, 45);
      if (target != null) {
         Subject subject = this.subjectOf(target.id());
         if (subject != null) {
            observed = (float)subject.attentionOn(this.handle.bodyPosition());
            girth = (float)subject.girth();
         }
      }

      return new Bout(this.reservoirs.annoyance(), hurtRecently, observed, girth);
   }

   public float forceFrustrate(float level) {
      for (Fighting fight : this.fights()) {
         fight.engagement().frustrateTo(level);
      }

      Engage running = this.engagement();
      return running != null && running.frustration() > 0.0F ? running.frustration() : level;
   }

   public FaunaMind.AbilityReading forceAbility(int ticks) {
      for (Fighting fight : this.fights()) {
         fight.engagement().commitFor(this.age, ticks);
      }

      return this.abilityReading();
   }

   private List<Fighting> fights() {
      List<Fighting> out = new ArrayList<>();

      for (Pursuit pursuit : this.pursuits) {
         if (pursuit.behavior() instanceof Fighting fight) {
            out.add(fight);
         }
      }

      return out;
   }

   public FaunaMind.AbilityReading abilityReading() {
      Fighting fight = this.runningFight();
      return new FaunaMind.AbilityReading(
         AbilityElection.armedAgainst(fight),
         AbilityElection.spent(fight),
         AbilityElection.fleeing(this.runningBehavior()),
         AbilityElection.commitment(fight, this.age),
         this.sinceAbilityPress()
      );
   }

   void stateFlight(UUID from, float fear) {
      Subject subject = this.subjectOf(from);
      if (subject != null) {
         double reach = 24.0;
         this.lastAct = new Brief(subject, Situation.FLEE, reach, reach, 1.0F, this.context.healthFraction(), 0.0F, this.handWith(from));
         this.lastActAt = this.age;
         this.flightFear = fear;
      }
   }

   boolean electedAir() {
      return this.medium == Medium.Verdict.AIR;
   }

   void requestBeat(UUID id, Situation situation, double reach, double ring, float urgency) {
      Subject subject = this.subjectOf(id);
      Engage fight = this.engagement();
      this.pendingBeat = subject == null
         ? null
         : new Brief(
            subject,
            situation,
            reach,
            ring,
            urgency,
            this.context.healthFraction(),
            fight == null ? 0.0F : fight.frustration(),
            this.handWith(id),
            fight == null ? 0 : fight.quietTicks(this.age)
         );
      if (this.pendingBeat != null) {
         this.lastAct = this.pendingBeat;
         this.lastActAt = this.age;
         this.flightFear = 0.0F;
      }
   }

   void runManeuvers() {
      this.legs.noteFailure(this.routeFailedThisThink);
      MoveMode mode = this.handle.currentMode();
      if (mode != this.lastBeatMode) {
         this.maneuvers.resetFailures();
         this.lastBeatMode = mode;
      }

      if (this.pendingBeat == null) {
         this.maneuvers.clear(this.handle, this.legs);
      } else {
         this.maneuvers
            .tick(this.handle, this.legs, this.pendingBeat, this.context::trait, primitive -> this.handle.species().movement().styleAptitude(primitive.id()));
         this.pendingBeat = null;
      }
   }

   @Nullable
   private Subject subjectOf(UUID id) {
      Representation rep = this.knowledge.of(id);
      if (rep == null) {
         return null;
      }

      Vec3 estimate = this.knowledge.estimate(rep, this.age, this.handle.species());
      Vec3 velocity = rep.lastVelocity();
      return new Subject(id, estimate, velocity, this.facingOf(estimate, velocity), rep.apparentSize());
   }

   private Vec3 facingOf(Vec3 estimate, Vec3 velocity) {
      Vec3 drift = new Vec3(velocity.x, 0.0, velocity.z);
      if (drift.lengthSqr() > 1.0E-6) {
         return drift.normalize();
      }

      Vec3 toUs = this.handle.bodyPosition().subtract(estimate);
      Vec3 flat = new Vec3(toUs.x, 0.0, toUs.z);
      return flat.lengthSqr() > 1.0E-6 ? flat.normalize() : new Vec3(0.0, 0.0, 1.0);
   }

   int handWith(UUID other) {
      PairSession session = PairSession.find(this.handle.faunaId(), other);
      if (session != null) {
         return session.hand();
      } else {
         return Seeds.unitPair(this.handle.faunaId(), other, 0L) < 0.5F ? 1 : -1;
      }
   }

   private int ownHand() {
      return Seeds.unit(this.handle.faunaId(), 0L) < 0.5F ? 1 : -1;
   }

   @Nullable
   Vec3 beliefAbout(UUID id) {
      Representation rep = this.knowledge.of(id);
      return rep == null ? null : this.knowledge.estimate(rep, this.age, this.handle.species());
   }

   @Nullable
   public Engage engagement() {
      Fighting fight = this.runningFight();
      return fight == null ? null : fight.engagement();
   }

   public ManeuverRunner maneuvers() {
      return this.maneuvers;
   }

   private void collectYield() {
      this.runningPursuit.handoff().collectYield(this.handoffContext);
   }

   public static float stuckAfter(float current, boolean failed) {
      if (failed) {
         return Math.min(1.0F, current + 0.2F);
      }

      float decayed = current * 0.97F;
      return decayed < 0.001F ? 0.0F : decayed;
   }

   @Nullable
   public Places places() {
      return this.knowledge.faculty(Places.class).orElse(null);
   }

   public float stuckPressure() {
      return this.stuckPressure;
   }

   public boolean cornered() {
      return this.stuckPressure >= 0.5F;
   }

   void integratePlaces() {
      Places places = this.places();
      if (places != null) {
         places.forget(this.age);
         places.leaveFence(this.handle.bodyPosition(), this.age);
      }
   }

   @Nullable
   private Predicate<BlockPos> withinReach() {
      Places places = this.places();
      return places != null && places.fenced(this.age) ? pos -> places.insideFence(Vec3.atBottomCenterOf(pos), this.age) : null;
   }

   public Perception perception() {
      return this.perception;
   }

   public Knowledge knowledge() {
      return this.knowledge;
   }

   public boolean obeys(Order order) {
      return this.knowledge.obedience().obeys(order);
   }

   public float threatFear() {
      return ThreatDrive.fear(this.context);
   }

   public void noteObeyed(Order order, @Nullable Vec3 post) {
      this.knowledge.obedience().obeyed();
      this.knowledge.obedience().ordered(order == Order.STAY ? this.handle.keepPlace(post != null ? post : this.handle.bodyPosition()) : null);
   }

   public boolean commandHolds() {
      return this.holder == null || this.holder.id().equals("command");
   }

   public float forceComply(float level) {
      return this.knowledge.obedience().force(level);
   }

   public Arbiter arbiter() {
      return this.arbiter;
   }

   public RouteFollower route() {
      return this.route;
   }

   public ContextRing ring() {
      return this.ring;
   }

   @Nullable
   public Wander wander() {
      return this.behavior("wander", Wander.class);
   }

   public DriveContext context() {
      return this.context;
   }

   @Nullable
   public Drive holder() {
      return this.holder;
   }

   void setHolder(@Nullable Drive value) {
      this.holder = value;
   }

   public AnimalHandle handle() {
      return this.handle;
   }

   public long age() {
      return this.age;
   }

   Gaze.Hold gazeHold() {
      return this.gazeHold;
   }

   public float sleepPhase() {
      return DayCycle.phase(this.handle.dayTime(), this.handle.species().habit(), DayCycle.offset(this.handle.faunaId()));
   }

   public boolean farThink() {
      return this.farThink;
   }

   void setFarThink(boolean value) {
      this.farThink = value;
   }

   void advanceAge(long now) {
      this.age = this.age + this.clock.step(now);
   }

   void advanceAgeForTest() {
      this.age++;
   }

   void notePhase(Phase phase) {
      this.lastPhase = phase;
      this.phaseRuns.merge(phase, 1, Integer::sum);
   }

   public Phase lastPhase() {
      return this.lastPhase;
   }

   public int runsOf(Phase phase) {
      return this.phaseRuns.getOrDefault(phase, 0);
   }

   public MoveIntent intent() {
      return this.intent;
   }

   public void setIntent(MoveIntent value) {
      this.intent = value;
   }

   public Nature nature() {
      return this.nature;
   }

   public List<String> omitted() {
      return Collections.unmodifiableList(this.omitted);
   }

   public List<Pursuit> pursuits() {
      return this.pursuits;
   }

   public void save(CompoundTag tag) {
      this.clock.save(tag, this.age);
      this.reservoirs.save(tag);

      for (Faculty faculty : this.knowledge.faculties()) {
         faculty.save(tag);
      }

      this.knowledge.standings().save(tag);
      this.knowledge.social().save(tag);
      this.knowledge.obedience().save(tag);
      this.knowledge.courtship().save(tag);
   }

   public void load(CompoundTag tag) {
      this.age = this.clock.load(tag);
      this.reservoirs.load(tag);

      for (Faculty faculty : this.knowledge.faculties()) {
         faculty.load(tag);
      }

      this.knowledge.standings().load(tag);
      this.knowledge.social().load(tag);
      this.knowledge.obedience().load(tag);
      this.knowledge.courtship().load(tag);
   }

   public record AbilityReading(@Nullable UUID target, boolean spent, boolean fleeing, int committed, long sincePress) {
   }

   public record BedProbe(BlockPos at, ShelterProbe.Reading reading, boolean standable) {
   }
}
