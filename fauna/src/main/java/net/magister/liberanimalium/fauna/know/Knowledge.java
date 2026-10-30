package net.magister.liberanimalium.fauna.know;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.Map.Entry;
import org.jetbrains.annotations.Nullable;
import net.magister.liberanimalium.config.WorldRules;
import net.magister.liberanimalium.fauna.api.AnimalHandle;
import net.magister.liberanimalium.fauna.api.Buck;
import net.magister.liberanimalium.fauna.api.Check;
import net.magister.liberanimalium.fauna.api.Delivery;
import net.magister.liberanimalium.fauna.api.ExpressionKey;
import net.magister.liberanimalium.fauna.api.Halter;
import net.magister.liberanimalium.fauna.api.HeardSound;
import net.magister.liberanimalium.fauna.api.Hurt;
import net.magister.liberanimalium.fauna.api.Kind;
import net.magister.liberanimalium.fauna.api.Kindness;
import net.magister.liberanimalium.fauna.api.Ladder;
import net.magister.liberanimalium.fauna.api.Relation;
import net.magister.liberanimalium.fauna.api.RelationType;
import net.magister.liberanimalium.fauna.api.SpeciesProfile;
import net.magister.liberanimalium.fauna.api.TamingMatrix;
import net.magister.liberanimalium.fauna.api.Touch;
import net.magister.liberanimalium.fauna.api.Valence;
import net.magister.liberanimalium.fauna.api.Witness;
import net.magister.liberanimalium.fauna.core.PairSession;
import net.magister.liberanimalium.fauna.identity.Temper;
import net.magister.liberanimalium.fauna.identity.Trait;
import net.magister.liberanimalium.fauna.sense.Perception;
import net.magister.liberanimalium.fauna.sense.TouchSense;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

public final class Knowledge {
   final Map<UUID, Representation> known = new HashMap<>();
   private final RelationMemory standings = new RelationMemory();
   private final List<Faculty> faculties;
   private final Map<Class<?>, Faculty> facultiesByType;
   private final Obedience obedience = new Obedience();
   private final Courtship courtship = new Courtship();
   private Wants wants = Wants.EAGER;
   @Nullable
   private UUID warnedLast;
   @Nullable
   private UUID settled;
   private final List<ExpressionKey> moments = new ArrayList<>();
   private boolean estranged;
   private float estrangement;
   private long lastForgave = Long.MIN_VALUE;
   @Nullable
   private Temper lastTemper;
   private long lastAlarmTick = Long.MIN_VALUE;
   private long lastHabituatedAt = Long.MIN_VALUE;
   private final PositionEstimator estimator = DriftEstimator.INSTANCE;
   private final Set<UUID> rebuffed = new HashSet<>();
   @Nullable
   private UUID crowdPin;
   private float crowdPinLevel;
   private long crowdPinUntil;
   @Nullable
   private AnimalHandle lastHandle;
   private final SocialBook social = new SocialBook();
   @Nullable
   private SpeciesProfile lastSpecies;
   private int edits;

   public Knowledge(List<Faculty> faculties) {
      this.faculties = List.copyOf(faculties);
      Map<Class<?>, Faculty> byType = new HashMap<>();

      for (Faculty faculty : this.faculties) {
         if (byType.put(faculty.getClass(), faculty) != null) {
            throw new IllegalArgumentException("two faculties of type " + faculty.getClass().getSimpleName());
         }
      }

      this.facultiesByType = Map.copyOf(byType);
   }

   public List<Faculty> faculties() {
      return this.faculties;
   }

   public <T extends Faculty> Optional<T> faculty(Class<T> type) {
      return Optional.ofNullable(type.cast(this.facultiesByType.get(type)));
   }

   public Knowledge() {
      this(List.of());
   }

   public Obedience obedience() {
      return this.obedience;
   }

   public Courtship courtship() {
      return this.courtship;
   }

   public boolean courting(AnimalHandle handle, @Nullable Representation rep) {
      return rep != null && rep.isPlayer() && Halter.of(handle).tameable() && Halter.of(handle).ownerId() == null;
   }

   private static Ladder acts(AnimalHandle handle) {
      return Halter.of(handle).stagePool();
   }

   public boolean sparring(AnimalHandle handle, UUID courter) {
      if (!this.courting(handle, this.known.get(courter))) {
         return false;
      }

      if (!this.courtship.playing(courter)) {
         return false;
      }

      if (!acts(handle).recognises(Check.Engagement.SPAR)) {
         return false;
      }

      PairSession bout = PairSession.find(handle.faunaId(), courter);
      return bout != null && "spar".equals(bout.kind());
   }

   public Wants wants() {
      return this.wants;
   }

   private Courtship.Pay court(AnimalHandle handle, Temper temper, Representation rep, Courtship.Act act, float units, float want, boolean asked) {
      this.introduce(rep);
      Courtship.Pay pay = this.courtship.court(this.social, rep.id(), act, units, temper, this.wants, want, asked);
      pay.shown(Halter.of(handle), rep.id(), this::moment);
      return pay;
   }

   private void introduce(Representation rep) {
      Relation species = rep.staticRelation();
      this.social.introduce(rep.id(), species.type() == RelationType.AFRAID ? species.intensity() : 0.0F);
   }

   private void defended(AnimalHandle handle, Temper temper, Witness seen) {
      Representation courter = this.known.get(seen.attackerId());
      Representation victim = this.known.get(seen.victimId());
      if (this.courting(handle, courter) && victim != null) {
         float units = Mth.clamp(seen.severity(), 0.0F, 1.0F);
         if (Bend.isNegative(victim.relation().type())) {
            if (acts(handle).recognises(Check.Engagement.DEFEND)) {
               this.court(handle, temper, courter, Courtship.Act.DEFEND, units, 1.0F, true);
            }
         } else if (victim.id().equals(this.wants.hunting()) && acts(handle).recognises(Check.Engagement.HUNT_TOGETHER)) {
            this.court(handle, temper, courter, Courtship.Act.HUNT_TOGETHER, units, 0.0F, false);
         }
      }
   }

   private boolean calm(Representation rep) {
      if (Bend.isFighting(rep.relation().type()) && rep.offering() <= 0.0F) {
         return false;
      }

      if (rep.id().equals(this.wants.warnedAt())) {
         return false;
      }

      if (rep.manner() > 0.45000002F) {
         return false;
      }

      PairSession bout = this.lastHandle == null ? null : PairSession.find(this.lastHandle.faunaId(), rep.id());
      return bout == null || !"territory_contest".equals(bout.kind());
   }

   private void moment(ExpressionKey key) {
      this.moments.add(key);
   }

   public List<ExpressionKey> drainMoments() {
      if (this.moments.isEmpty()) {
         return List.of();
      }

      List<ExpressionKey> out = List.copyOf(this.moments);
      this.moments.clear();
      return out;
   }

   public Courtship.Rite ceremony(final AnimalHandle handle) {
      return new Courtship.Rite() {
         @Override
         public void performed(UUID courter, int rung, int length) {
            Knowledge.this.moment(ExpressionKey.body(ExpressionKey.Body.COURTSHIP_RUNG));
            Halter.of(handle).tellCourter(courter, rung, length);
         }

         @Override
         public void undone(UUID courter) {
            Knowledge.this.moment(ExpressionKey.body(ExpressionKey.Body.COURTSHIP_RESET));
            Halter.of(handle).tellCourterReset(courter);
         }

         @Override
         public void completed(UUID courter) {
            Knowledge.this.moment(ExpressionKey.body(ExpressionKey.Body.COURTSHIP_TAMED));
            Halter.of(handle).adopt(courter);
         }

         @Override
         public void asks(UUID courter, Check check) {
            Halter.of(handle).tellAsk(courter, check);
         }
      };
   }

   public void noteSnubbed(UUID id, long now) {
      this.faculty(Larder.class).ifPresent(larder -> larder.snub(id, now));
   }

   public void forceOffer(UUID holder, float craving, long until) {
      this.faculty(Larder.class).ifPresent(larder -> larder.forceOffer(holder, craving, until));
   }

   public boolean estranged() {
      return this.estranged;
   }

   public float estrangement() {
      return this.estrangement;
   }

   private void forgiveOwner(AnimalHandle handle, long now) {
      UUID owner = Halter.of(handle).ownerId();
      if (owner == null) {
         this.lastForgave = Long.MIN_VALUE;
      } else {
         long world = handle.gameTime();
         if (this.lastForgave == Long.MIN_VALUE) {
            this.lastForgave = world;
         } else {
            long elapsed = world - this.lastForgave;
            if (elapsed > 0L) {
               this.lastForgave = world;
               Relation remembered = this.standings.of(owner);
               Representation rep = this.known.get(owner);
               Relation underlying = remembered != null ? remembered : (rep != null ? rep.staticRelation() : null);
               if (underlying != null) {
                  this.social.forgive(owner, Bend.packLine(underlying.intensity()), elapsed);
               }
            }
         }
      }
   }

   @Nullable
   public Bed bed() {
      return this.faculty(Bedding.class).map(Bedding::bed).orElse(null);
   }

   @Nullable
   public Bed searchBed(AnimalHandle handle, long now) {
      return this.faculty(Bedding.class).map(b -> b.searchBed(this.offTick(handle, now))).orElse(null);
   }

   public boolean confirmBed(AnimalHandle handle, long now) {
      return this.faculty(Bedding.class).map(b -> b.confirmBed(this.offTick(handle, now))).orElse(false);
   }

   private FacultyContext offTick(AnimalHandle handle, long now) {
      return new KnowledgePass(
         this,
         handle,
         this.lastTemper,
         now,
         handle.species(),
         List.of(),
         handle.bounds().getXsize() * 2.0,
         this.lastTemper == null
            ? 0.0F
            : Crowding.space(handle.bounds().getXsize(), this.lastTemper.get(Trait.SYMPATHY), this.lastTemper.get(Trait.NERVOUSNESS))
      );
   }

   public void integrate(AnimalHandle handle, Perception perception, long now, Temper temper, boolean cornered) {
      this.integrate(handle, perception, now, temper, cornered, Wants.EAGER);
   }

   public void integrate(AnimalHandle handle, Perception perception, long now, Temper temper, boolean cornered, Wants wants) {
      this.wants = wants;
      this.edits++;
      this.settled = this.warnedLast != null && !this.warnedLast.equals(wants.warnedAt()) ? this.warnedLast : null;
      this.warnedLast = wants.warnedAt();
      SpeciesProfile species = handle.species();
      this.lastSpecies = species;
      this.lastHandle = handle;
      this.lastTemper = temper;
      this.courtship.age(now);
      double reach = handle.bounds().getXsize() * 2.0;
      float space = Crowding.space(handle.bounds().getXsize(), temper.get(Trait.SYMPATHY), temper.get(Trait.NERVOUSNESS));
      KnowledgePass pass = new KnowledgePass(this, handle, temper, now, species, perception.items(), reach, space);
      Set<UUID> seenNow = new HashSet<>();

      for (Perception.Seen seen : perception.visible()) {
         Representation rep = this.known
            .computeIfAbsent(
               seen.body().id(),
               id -> this.meet(id, seen.body().typeKey(), seen.body().kind().sameSpecies(), species.relationTo(seen.body().kind()), seen.body().position(), now)
            );
         rep.sighted(
            seen.body().position(),
            now,
            seen.score(),
            seen.body().visualSize(),
            seen.body().startled(),
            perception.scanSerial(),
            seen.body().heldMenace(),
            seen.body().armorFraction(),
            seen.body().sneaking(),
            seen.body().owned(),
            seen.body().leashHolder(),
            seen.body().tethered(),
            seen.body().providedBy(),
            seen.body().inWater(),
            seen.body().width(),
            seen.body().height(),
            seen.body().playful(),
            seen.body().leads(),
            seen.body().answers(),
            seen.body().pressing()
         );
         seenNow.add(rep.id());
      }

      for (Representation rep : this.known.values()) {
         if (!seenNow.contains(rep.id())) {
            rep.lostSight();
         }
      }

      for (HeardSound sound : perception.drainSounds()) {
         pass.stimuli.add(sound);
         Representation rep = sound.sourceId() == null ? null : this.known.get(sound.sourceId());
         if (rep == null && sound.announces()) {
            rep = this.known
               .computeIfAbsent(
                  sound.sourceId(),
                  id -> this.meet(id, sound.source().typeKey(), sound.sameSpecies(), species.relationTo(sound.source()), sound.position(), sound.tick())
               );
         }

         if (rep != null) {
            rep.heard(sound.position(), sound.tick(), perception.strengthOf(handle, sound));
            if (handle.faunaId().equals(sound.about())) {
               rep.roaredAt(sound.tick(), perception.strengthOf(handle, sound));
            }
         } else {
            pass.mysteries.add(sound);
         }
      }

      pass.stimuli.addAll(perception.drainPosted());

      for (Faculty faculty : this.faculties) {
         faculty.integrate(pass);
      }

      for (Hurt hurt : perception.drainHurts()) {
         if (!hurt.hasAttacker()) {
            this.faculty(Places.class).ifPresent(places -> places.mark(hurt.position(), TouchSense.severity(hurt, handle.maxHealth()), now));
         } else if (!hurt.attackerId().equals(handle.faunaId())) {
            Representation rep = this.known
               .computeIfAbsent(
                  hurt.attackerId(),
                  id -> this.meet(id, hurt.kind().typeKey(), hurt.kind().sameSpecies(), species.relationTo(hurt.kind()), hurt.position(), hurt.tick())
               );
            if (this.sparring(handle, hurt.attackerId())) {
               boolean won = this.courtship.sparred(rep.id(), Courtship.sparTarget(temper));
               if (won) {
                  this.court(handle, temper, rep, Courtship.Act.SPAR, 1.0F, wants.boredom(), Courtship.asksToPlay(wants.boredom(), temper));
                  PairSession.close(handle.faunaId(), rep.id());
               }

               this.moment(ExpressionKey.body(won ? ExpressionKey.Body.PLAY_WON : ExpressionKey.Body.PLAY_CAUGHT));
            } else {
               float severity = TouchSense.severity(hurt, handle.maxHealth());
               rep.hurtBy(hurt.position(), hurt.tick(), severity);
               PairSession.breakTruce(handle.faunaId(), hurt.attackerId(), "territory_contest", handle.gameTime() - (now - hurt.tick()));
               this.noteReachable(hurt.attackerId());
               this.bend(
                  rep,
                  Bend.afterHurt(
                     this.grudgeBase(rep),
                     severity,
                     Courage.of(temper, this.all(), handle.bodyPosition()),
                     temper.get(Trait.AGGRESSION),
                     cornered,
                     hurt.kind().sameSpecies() && Halter.of(handle).ownerId() == null
                  )
               );
               if (Halter.of(handle).ownedBy(hurt.attackerId())) {
                  this.obedience.struckByOwner();
               }

               this.social.betrayed(hurt.attackerId(), SocialBookTuning.betrayalCost(hurt.amount(), handle.maxHealth()));
            }
         }
      }

      for (Kindness kindness : perception.drainKindnesses()) {
         Representation rep = this.known
            .computeIfAbsent(
               kindness.sourceId(),
               id -> this.meet(
                  id, kindness.kind().typeKey(), kindness.kind().sameSpecies(), species.relationTo(kindness.kind()), kindness.position(), kindness.tick()
               )
            );
         float share = Math.max(0.0F, kindness.share());
         boolean courted = this.courting(handle, rep);
         boolean gift = kindness.delivery() == Delivery.GIFT;
         Ladder.StageDef given = gift ? acts(handle).stageOf(Check.Engagement.GIFT) : null;
         Check act = !courted ? null : (gift ? (given == null ? null : given.check()) : acts(handle).fedBy(kindness.delivery()));
         if (courted && act == null) {
            rep.fedBy(kindness.tick());
         } else {
            this.bend(rep, Bend.kindness(rep.relation(), share));
            if (courted) {
               boolean prey = kindness.delivery() == Delivery.PREY;
               float want = Courtship.beg(prey ? Courtship.preyCraving(Halter.of(handle).tamingMatrix()) : kindness.craving(), handle.hungerLevel());
               this.court(
                  handle,
                  temper,
                  rep,
                  gift ? Courtship.Act.GIFT : (prey ? Courtship.Act.PREY : Courtship.Act.MEAL),
                  share * (!gift && !prey ? Math.max(0.0F, kindness.relish()) : 1.0F),
                  want,
                  Courtship.asked(want)
               );
            } else {
               this.social.fed(kindness.sourceId(), 0.08F * share, 0.5F);
            }

            rep.fedBy(kindness.tick());
            if (Halter.of(handle).ownedBy(kindness.sourceId())) {
               this.obedience.fed(share);
               this.warmToTamesInSight(handle, temper, rep, share);
            }
         }
      }

      for (Touch touch : perception.drainTouches()) {
         Representation rep = this.known
            .computeIfAbsent(
               touch.sourceId(),
               id -> this.meet(id, touch.kind().typeKey(), touch.kind().sameSpecies(), species.relationTo(touch.kind()), touch.position(), touch.tick())
            );
         TamingMatrix matrix = Halter.of(handle).tamingMatrix();
         boolean courted = this.courting(handle, rep);
         PairSession bout = PairSession.find(handle.faunaId(), touch.sourceId());
         boolean playing = bout != null && "play".equals(bout.kind());
         Ladder.StageDef hand = courted && this.courtship.playing(touch.sourceId()) ? acts(handle).stageOf(Check.Engagement.TOUCH) : null;
         switch (Courtship.faceOf(matrix.act("groom"), hand, touch.fits(), touch.heldTicks(), playing)) {
            case INTRUSION:
               float pressure = Manner.spiked(rep.attribution(), 0.33333334F);
               rep.updateManner(pressure, Manner.of(pressure, rep.armament(), rep.armorFraction(), rep.sneakingSeen()));
               this.moment(ExpressionKey.body(ExpressionKey.Body.TOUCH_REBUFF));
               continue;
            case DECLINE:
               this.moment(ExpressionKey.body(ExpressionKey.Body.TOUCH_DECLINE));
            case HOLDING:
            default:
               continue;
            case CHECK:
               this.court(handle, temper, rep, Courtship.Act.TOUCH, 1.0F, wants.company(), Courtship.asked(wants.company()));
               continue;
            case CATCH:
         }

         boolean tag = courted && this.courtship.playing(touch.sourceId()) && acts(handle).recognises(Check.Engagement.TAG);
         boolean won = tag && this.courtship.caught(touch.sourceId(), Courtship.catchTarget(temper));
         if (won) {
            this.court(handle, temper, rep, Courtship.Act.TAG, 1.0F, wants.boredom(), Courtship.asksToPlay(wants.boredom(), temper));
         }

         this.moment(ExpressionKey.body(won ? ExpressionKey.Body.PLAY_WON : ExpressionKey.Body.PLAY_CAUGHT));
      }

      UUID rider = Halter.of(handle).unwelcomeRider();
      this.courtship.ridingOnly(rider);
      boolean rideable = Halter.of(handle).rideable();

      for (Buck buck : perception.drainBucks()) {
         if (this.courtship.atMount(buck.riderId(), rideable)) {
            if (buck.survived()) {
               this.courtship.bucked(buck.riderId(), Courtship.buckTarget(temper), rideable, this.ceremony(handle));
            } else {
               this.moment(ExpressionKey.body(ExpressionKey.Body.MOUNT_THROWN));
               Halter.of(handle).throwRider();
            }
         }
      }

      for (Witness seen : perception.drainWitnesses()) {
         if (seen.hasAttacker() && !seen.attackerId().equals(handle.faunaId()) && !seen.attackerId().equals(seen.victimId())) {
            this.defended(handle, temper, seen);
            Representation victim = this.known.get(seen.victimId());
            Representation striker = this.known.get(seen.attackerId());
            if (striker != null && victim != null && Bend.isNegative(victim.relation().type())) {
               striker.sawDefend();
            }

            PairSession fight = PairSession.find(seen.victimId(), seen.attackerId());
            if (this.caresAbout(seen.victimId()) && (fight == null || !"spar".equals(fight.kind()))) {
               Representation rep = this.known
                  .computeIfAbsent(
                     seen.attackerId(),
                     id -> this.meet(
                        id,
                        seen.attackerKind().typeKey(),
                        seen.attackerKind().sameSpecies(),
                        species.relationTo(seen.attackerKind()),
                        seen.position(),
                        seen.tick()
                     )
                  );
               this.bend(rep, Bend.defendingFriend(rep.relation(), seen.severity(), seen.attackerKind().sameSpecies()));
            }
         }
      }

      Urges.integrate(this, handle, perception);
      Vec3 position = handle.bodyPosition();

      for (Representation rep : this.known.values()) {
         if (rep.visible()) {
            if (rep.lastKnownPos().distanceToSqr(position) <= reach * reach) {
               rep.canReach();
            }
         } else if (rep.age(now) >= 80L && this.estimate(rep, now, species).distanceToSqr(position) <= reach * reach) {
            rep.searchFailed(now);
         }
      }

      long elapsed = this.lastAlarmTick == Long.MIN_VALUE ? 1L : Math.max(0L, now - this.lastAlarmTick);
      this.lastAlarmTick = now;
      this.fadeUnseen(elapsed);
      boolean habituateDue = this.lastHabituatedAt == Long.MIN_VALUE || now - this.lastHabituatedAt >= 1200L;
      if (habituateDue) {
         this.lastHabituatedAt = now;
      }

      float sightRadius = handle.senses().sightRadius();
      double retention = KnowTuning.alarmRetentionPerTick(temper.get(Trait.NERVOUSNESS));
      double fearRetention = Math.pow(retention, Courage.calm(Courage.lift(temper.get(Trait.SYMPATHY), Courage.kinNear(this.all(), handle.bodyPosition()))));
      double pressRetention = MannerTuning.pressRetentionPerTick(temper.get(Trait.NERVOUSNESS));
      boolean quarry = this.known
         .values()
         .stream()
         .anyMatch(r -> r.visible() && r.relation().type() == RelationType.EATS && r.leashHolder() == null && !r.tethered());

      for (Representation rep : this.known.values()) {
         Vec3 believed = this.estimate(rep, now, species);
         double gap = believed.distanceTo(position);
         boolean led = rep.leashHolder() != null || rep.tethered();
         float approach = rep.visible() && !led ? Manner.approachFraction(Manner.closingRate(believed, rep.lastVelocity(), position)) : 0.0F;
         float armed = rep.armament() * (1.0F - rep.defending());
         float carriage = Manner.carriage(armed, rep.armorFraction());
         float pressure = Manner.advance(
            rep.attribution(), approach, Crowding.within(gap, space), Manner.hold(Manner.occupancy(gap, reach), carriage), pressRetention, elapsed
         );
         float rising = pressure - rep.attribution();
         rep.updateManner(pressure, Manner.of(pressure, armed, rep.armorFraction(), rep.sneakingSeen()));
         if (this.courting(handle, rep) && (rising > 0.0F || approach > 0.0F) && rep.id().equals(wants.warnedAt())) {
            this.social.courted(rep.id(), -Math.max(0.0F, rising) * 0.5F, 0.0F);
            this.warnedLast = null;
         } else if (this.courting(handle, rep)
            && rep.id().equals(this.settled)
            && rising <= 0.0F
            && approach <= 0.0F
            && rep.visible()
            && wants.want() != Wants.Want.THREATENED
            && acts(handle).recognises(Check.Engagement.APPROACH)) {
            this.bend(rep, Bend.kindness(rep.relation(), 1.0F));
            this.court(handle, temper, rep, Courtship.Act.HEED, 1.0F, 1.0F, true);
         }

         rep.updateCrowding(Math.max(Crowding.press(gap, space, rep.manner(), rep.find(now)), this.stagedCrowd(rep.id(), now)));
         rep.updateOffering(this.offering(rep, Halter.of(handle).tamingMatrix(), species, rep.id().equals(wants.warnedAt())));
         Relation held = this.standings.of(rep.id());
         if (!Halter.of(handle).ownedBy(rep.id()) && !household(handle, rep)) {
            this.wear(
               rep,
               Bend.pressed(
                  rep.relation(),
                  held == null ? rep.staticRelation() : held,
                  Manner.pressureFactor(rep.manner()),
                  this.social.of(rep.id()).like(),
                  Courage.of(temper, this.all(), handle.bodyPosition()),
                  temper.get(Trait.AGGRESSION),
                  cornered,
                  rep.sameSpecies() || rep.challengedWithin(now),
                  elapsed
               )
            );
         }

         if (this.rebuffed.contains(rep.id()) && rep.sameSpecies() && !Halter.of(handle).ownedBy(rep.id())) {
            this.wear(
               rep,
               Bend.pressed(
                  rep.relation(),
                  held == null ? rep.staticRelation() : held,
                  rep.crowding(),
                  this.social.of(rep.id()).like(),
                  Courage.of(temper, this.all(), handle.bodyPosition()),
                  temper.get(Trait.AGGRESSION),
                  cornered,
                  true,
                  elapsed
               )
            );
         }

         Relation remembered = this.standings.of(rep.id());
         Relation underlying = remembered != null ? remembered : rep.staticRelation();
         boolean wasPack = rep.relation().type() == RelationType.PACK;
         Relation worn = Bend.toyedWith(
            underlying,
            Bend.projection(underlying, rep.relation(), this.social.of(rep.id()).like(), household(handle, rep) ? Courtship.comfort(temper) : 0.0F),
            wants,
            handle.hungerLevel()
         );
         this.wear(rep, rep.isPlayer() && handle.peacefulWorld() ? Bend.peaceful(worn) : worn);
         if (remembered == null) {
            rep.quiet(Bend.unanswered(rep.relation(), rep.answers(), rep.pressing(), WorldRules.strangerShare()));
         }

         if (this.courting(handle, rep)
            && this.courtship.tolerated(rep.id(), Bend.tolerates(underlying, this.social.of(rep.id()).like()))
            && (rep.relation().type() == RelationType.UNCOMFORTABLE || rep.relation().type() == RelationType.IGNORES)) {
            this.moment(ExpressionKey.body(ExpressionKey.Body.COURTSHIP_RUNG));
            Halter.of(handle).tellCourter(rep.id(), 0, Courtship.length(Halter.of(handle).rideable()));
         }

         if (!wasPack && rep.relation().type() == RelationType.PACK) {
            PairSession.noteSettled(handle.faunaId(), rep.id(), "play", handle.gameTime());
         }

         boolean wooed = this.courting(handle, rep);
         if (wooed) {
            this.courtship
               .observe(
                  rep.id(),
                  rep.relation().type() == RelationType.PACK,
                  this.social.of(rep.id()).familiarity(),
                  Courtship.familiarityLine(temper),
                  Halter.of(handle).rideable(),
                  this.ceremony(handle)
               );
         }

         rep.ask(
            wooed
               ? this.courtship
                  .guide(
                     rep.id(),
                     acts(handle),
                     wants,
                     handle.hungerLevel(),
                     Bend.isNegative(underlying.type()) ? underlying.intensity() : 0.0F,
                     temper,
                     Halter.of(handle).rideable(),
                     rep.visible() && gap <= 16.0,
                     this.ceremony(handle),
                     quarry
                  )
               : null
         );
         float vouch = rep.isPlayer()
            ? Vouch.forPerson(
               this.estimate(rep, now, handle.species()), this.known.values(), r -> this.estimate(r, now, handle.species()), temper.get(Trait.SYMPATHY)
            )
            : 0.0F;
         rep.notePersonVouchedFor(vouch);
         float spike = Alarm.spike(
            rep.relation().type(), rep.relation().intensity(), rep.find(now), gap, sightRadius, rep.manner(), Vouch.shade(rep.offering(), vouch)
         );
         rep.updateAlarm(spike, elapsed, fearRetention);
         rep.coolDefending(elapsed, retention);
         rep.updatePresence(rep.find(now), elapsed, retention);
         boolean company = this.courting(handle, rep) && this.calm(rep) && (rep.visible() || gap <= 16.0);
         if (company) {
            this.introduce(rep);
            SocialBook.Opinion opinion = this.social.of(rep.id());
            Courtship.Pay pay = Courtship.company(
               (float)elapsed / 1200.0F, wants.want() == Wants.Want.RESTLESS && gap <= 16.0, temper, opinion.fear(), opinion, Bend.packLine(underlying)
            );
            this.social.courted(rep.id(), pay.like(), pay.familiarity());
            if (habituateDue && pay.familiarity() > 0.0F) {
               this.moment(ExpressionKey.body(ExpressionKey.Body.TOUCH_ACCEPT));
            }
         }

         Courtship.Act together = !this.courting(handle, rep)
            ? null
            : this.courtship
               .together(
                  rep.id(),
                  handle.asleep(),
                  this.calm(rep) && rep.sneakingSeen() && gap <= 8.0,
                  wants.want() == Wants.Want.RESTLESS,
                  this.calm(rep) && gap <= 16.0,
                  elapsed,
                  (float)(handle.velocity().horizontalDistance() * elapsed)
               );
         if (together != null && acts(handle).recognises(together == Courtship.Act.REST ? Check.Engagement.REST : Check.Engagement.WALK)) {
            this.court(handle, temper, rep, together, 1.0F, 0.0F, false);
         }

         if (rep.visible()) {
            float nearness = Alarm.nearness(gap, sightRadius);
            if (PairSession.find(handle.faunaId(), rep.id()) == null) {
               this.wear(
                  rep,
                  Bend.habituate(
                     rep.relation(), held == null ? rep.staticRelation() : held, nearness, this.courtship.reliefFor(rep.id(), Halter.of(handle).rideable())
                  )
               );
            }

            if (habituateDue && !company) {
               this.social.habituate(rep.id());
            }

            boolean peaceful = habituateDue && PairSession.find(handle.faunaId(), rep.id()) == null;
            if (peaceful && household(handle, rep)) {
               this.social.warm(rep.id(), 0.05F * (1.0F - 0.5F * temper.get(Trait.AGGRESSION)), Bend.packLine(underlying, Courtship.comfort(temper)));
            } else if (peaceful && this.companyKeeps(handle, rep, underlying)) {
               this.social.warm(rep.id(), 0.025F, Bend.packLine(underlying.intensity()));
            }

            if (habituateDue && !rep.isPlayer() && rep.answers() && (playingWith(handle, rep) || this.sharesToy(handle, rep))) {
               this.social.warm(rep.id(), 0.1F, Bend.packLine(underlying, household(handle, rep) ? Courtship.comfort(temper) : 0.0F));
               this.social.habituate(rep.id());
            }
         }
      }

      for (Faculty faculty : this.faculties) {
         faculty.settle(pass);
      }

      this.provisions(handle, space, now);
      this.rebuffed.clear();
      this.social.age(now, Halter.of(handle).ownerId());
      this.forgiveOwner(handle, now);
      UUID ownerId = Halter.of(handle).ownerId();
      Representation ownerRep = ownerId == null ? null : this.known.get(ownerId);
      float worstAlarm = 0.0F;
      float fightClaim = 0.0F;

      for (Representation rep : this.known.values()) {
         worstAlarm = Math.max(worstAlarm, rep.alarm());
         RelationType type = rep.relation().type();
         if (type == RelationType.ATTACKS || type == RelationType.AFRAID) {
            fightClaim = Math.max(fightClaim, rep.relation().intensity() * rep.find(now));
         }
      }

      Relation ownerRelation = ownerRep == null ? null : ownerRep.relation();
      this.estranged = ownerRelation != null && Bend.isNegative(ownerRelation.type());
      this.estrangement = this.estranged ? ownerRelation.intensity() : 0.0F;
      this.obedience
         .integrate(
            now,
            Compliance.bondNorm(ownerId == null ? 0.0F : this.social.of(ownerId).like()),
            1.0F - temper.get(Trait.NERVOUSNESS),
            temper.get(Trait.SYMPATHY),
            temper.get(Trait.BRAVERY),
            ownerRep == null ? 0.0F : ownerRep.find(now),
            worstAlarm,
            ownerRep == null ? 0.0F : this.faculty(Places.class).map(p -> p.dangerAt(ownerRep.lastKnownPos(), now)).orElse(0.0F),
            fightClaim,
            Math.max(Math.max(handle.hungerLevel(), handle.fatigueLevel()), wants.playWant()),
            this.estranged,
            this.estrangement
         );
      this.forget(now, species.social().forgetTicks());
      this.evict(now, species.social().trackerCapacity());
   }

   Representation meet(UUID id, String typeKey, boolean sameSpecies, Relation staticRelation, Vec3 position, long now) {
      Relation remembered = this.standings.of(id);
      return new Representation(id, typeKey, sameSpecies, remembered == null ? staticRelation : remembered, staticRelation, position, now);
   }

   private void fadeUnseen(long elapsed) {
      Map<UUID, RelationMemory.Faded> faded = this.standings
         .fade(elapsed, id -> this.known.get(id) != null && (this.known.get(id).visible() || !Bend.isNegative(this.known.get(id).relation().type())));

      for (Entry<UUID, RelationMemory.Faded> entry : faded.entrySet()) {
         Representation rep = this.known.get(entry.getKey());
         if (rep != null && rep.relation().type() == entry.getValue().was().type()) {
            Relation now = entry.getValue().now();
            if (now == null) {
               rep.bendTo(rep.staticRelation());
            } else if (rep.relation().intensity() > now.intensity()) {
               rep.bendTo(now);
            }
         }
      }
   }

   private Relation grudgeBase(Representation rep) {
      Relation live = rep.relation();
      if (Bend.isPressable(live.type())) {
         return live;
      }

      Relation remembered = this.standings.of(rep.id());
      return remembered != null && Bend.isPressable(remembered.type()) ? remembered : new Relation(live.type(), 0.0F);
   }

   private void warmToTamesInSight(AnimalHandle handle, Temper temper, Representation feeder, float share) {
      for (Representation other : this.known.values()) {
         if (other != feeder && other.visible() && household(handle, other)) {
            Relation held = this.standings.of(other.id());
            Relation base = held != null ? held : other.staticRelation();
            if (Bend.isNegative(base.type()) && other.relation().type() != RelationType.PACK) {
               this.social.warm(other.id(), 0.08F * share, Bend.packLine(base, Courtship.comfort(temper)));
            }
         }
      }
   }

   public static boolean household(AnimalHandle handle, Representation rep) {
      return rep.owned() && Halter.of(handle).ownerId() != null;
   }

   private static boolean playingWith(AnimalHandle handle, Representation rep) {
      PairSession bout = PairSession.find(handle.faunaId(), rep.id());
      return bout != null && "play".equals(bout.kind());
   }

   private boolean sharesToy(AnimalHandle handle, Representation rep) {
      for (Representation toy : this.known.values()) {
         if (!toy.answers() && !toy.isPlayer() && playingWith(handle, toy) && playingWith(rep.id(), toy.id())) {
            return true;
         }
      }

      return false;
   }

   private static boolean playingWith(UUID animal, UUID toy) {
      PairSession bout = PairSession.find(animal, toy);
      return bout != null && "play".equals(bout.kind());
   }

   public void callOff(AnimalHandle handle, Representation rep, float word) {
      PairSession.noteSettled(handle.faunaId(), rep.id(), "territory_contest", handle.gameTime());
      PairSession.close(handle.faunaId(), rep.id());
      this.bend(rep, Bend.calledOff(rep.relation(), word));
   }

   public void standDown(AnimalHandle handle) {
      for (Representation rep : List.copyOf(this.known.values())) {
         if (household(handle, rep)) {
            PairSession bout = PairSession.find(handle.faunaId(), rep.id());
            boolean disputing = bout != null && "territory_contest".equals(bout.kind());
            if (disputing || Bend.isFighting(rep.relation().type())) {
               this.callOff(handle, rep, this.obedience.share());
            }
         }
      }
   }

   private boolean companyKeeps(AnimalHandle handle, Representation rep, Relation underlying) {
      if (!rep.sameSpecies() || rep.isPlayer()) {
         return false;
      }

      if (Bend.isPressable(underlying.type())) {
         return false;
      }

      float line = 0.5F;
      if (this.social.of(rep.id()).like() >= line) {
         return true;
      }

      int cap = handle.species().social().bonds();
      return cap <= 0 || this.kithHeld(line) < cap;
   }

   private int kithHeld(float line) {
      int held = 0;

      for (Representation rep : this.known.values()) {
         if (this.social.of(rep.id()).like() >= line) {
            held++;
         }
      }

      return held;
   }

   void bend(Representation rep, Relation bent) {
      if (!bent.equals(rep.relation())) {
         rep.bendTo(bent);
         if (rep.bent()) {
            this.standings.remember(rep.id(), bent, rep.staticRelation());
         } else {
            this.standings.forget(rep.id());
         }
      }
   }

   private void wear(Representation rep, Relation worn) {
      if (!worn.equals(rep.relation())) {
         rep.bendTo(worn);
      }
   }

   public void noteLostFight(UUID id, float fear) {
      Representation rep = this.known.get(id);
      if (rep != null) {
         Relation over = this.lastHandle != null && household(this.lastHandle, rep) ? new Relation(rep.relation().type(), 0.0F) : rep.relation();
         this.bend(rep, Bend.afterLostFight(over, fear));
         this.social.note(id, -fear * 0.5F, fear);
      }
   }

   public void noteSettledWith(UUID other) {
      Representation rep = this.known.get(other);
      if (rep != null && !rep.isPlayer() && rep.sameSpecies()) {
         Relation underlying = rep.staticRelation();
         float line = 0.5F;
         if (this.social.of(other).like() < line) {
            int cap = this.lastHandle == null ? 0 : this.lastHandle.species().social().bonds();
            if (cap > 0 && this.kithHeld(line) >= cap) {
               return;
            }
         }

         this.social.warm(other, 0.026666665F, Bend.packLine(underlying.intensity()));
      }
   }

   public void notePlayedWith(UUID id) {
      if (this.known.get(id) != null) {
         this.social.note(id, 0.04F, 0.0F);
      }
   }

   public void noteKilled(UUID victimId, UUID killerId, Kind killerKind, Vec3 where, long now) {
      if (!killerId.equals(victimId)) {
         if (this.caresAbout(victimId)) {
            this.edits++;
            Representation rep = this.known
               .computeIfAbsent(killerId, id -> this.meet(id, killerKind.typeKey(), killerKind.sameSpecies(), this.species(killerKind), where, now));
            this.bend(rep, Bend.defendingFriend(rep.relation(), 1.0F, killerKind.sameSpecies()));
         }
      }
   }

   private Relation species(Kind kind) {
      return this.lastSpecies == null ? Relation.NONE : this.lastSpecies.relationTo(kind);
   }

   public boolean caresAbout(UUID id) {
      Representation rep = this.known.get(id);
      return rep != null && rep.relation().type() == RelationType.PACK ? true : this.social.of(id).like() > 0.5F;
   }

   public SocialBook social() {
      return this.social;
   }

   public RelationMemory standings() {
      return this.standings;
   }

   public Vec3 estimate(Representation rep, long now, SpeciesProfile species) {
      return this.estimator.estimate(rep, now, species.senses().sightRadius());
   }

   @Nullable
   public Representation of(UUID id) {
      return this.known.get(id);
   }

   public void forget(UUID id) {
      this.forget(id, false);
   }

   public void forget(UUID id, boolean mayReturn) {
      this.edits++;
      this.known.remove(id);
      if (!mayReturn) {
         this.social.forget(id);
         this.standings.forget(id);
      }
   }

   public void noteUnreachable(UUID id, long now) {
      Representation rep = this.known.get(id);
      if (rep != null) {
         rep.couldNotReach(now);
      }
   }

   public void noteReachable(UUID id) {
      Representation rep = this.known.get(id);
      if (rep != null) {
         rep.canReach();
      }
   }

   public boolean forceRelation(UUID id, RelationType type, float intensity) {
      Representation rep = this.known.get(id);
      if (rep == null) {
         return false;
      }

      this.bend(rep, new Relation(type, intensity));
      return true;
   }

   public float forceManner(UUID id, float attribution) {
      Representation rep = this.known.get(id);
      if (rep == null) {
         return -1.0F;
      }

      float pressure = Mth.clamp(attribution, 0.0F, 1.0F);
      rep.updateManner(pressure, Manner.of(pressure, rep.armament(), rep.armorFraction(), rep.sneakingSeen()));
      return rep.manner();
   }

   public void noteRebuffed(UUID id) {
      this.rebuffed.add(id);
   }

   public boolean forceCrowd(UUID id, float level, long until) {
      if (!this.known.containsKey(id)) {
         return false;
      }

      this.crowdPin = id;
      this.crowdPinLevel = Mth.clamp(level, 0.0F, 1.0F);
      this.crowdPinUntil = until;
      return true;
   }

   private float stagedCrowd(UUID id, long now) {
      if (this.crowdPin == null) {
         return 0.0F;
      } else if (now >= this.crowdPinUntil) {
         this.crowdPin = null;
         return 0.0F;
      } else {
         return this.crowdPin.equals(id) ? this.crowdPinLevel : 0.0F;
      }
   }

   private float offering(Representation courter, TamingMatrix matrix, SpeciesProfile species, boolean warned) {
      if (!courter.isPlayer()) {
         return 0.0F;
      }

      float worth = this.offerWorth(courter, matrix, species);
      if (worth <= 0.0F) {
         return 0.0F;
      }

      float pressure = Math.max(Manner.pressureFactor(courter.manner()), warned ? courter.attribution() : 0.0F);
      return Math.min(Mth.clamp(worth * (1.0F - pressure * pressure), 0.0F, 1.0F), 0.9F);
   }

   private float offerWorth(Representation courter, TamingMatrix matrix, SpeciesProfile species) {
      float best = 0.0F;
      float liveprey = Courtship.preyCraving(matrix);
      if (liveprey > 0.0F && this.offersPrey(courter, species)) {
         best = liveprey;
      }

      for (Larder.Offer offer : this.faculty(Larder.class).map(Larder::offers).orElse(List.of())) {
         if (courter.id().equals(offer.holder())) {
            float craving = Mth.clamp(offer.craving(), 0.0F, 1.0F);
            if (craving > best) {
               best = craving;
            }
         }
      }

      return best;
   }

   private boolean offersPrey(Representation courter, SpeciesProfile species) {
      for (Kind kind : courter.leads()) {
         if (species.relationTo(kind).type() == RelationType.EATS) {
            return true;
         }
      }

      UUID id = courter.id();

      for (Representation rep : this.known.values()) {
         if (rep.relation().type() == RelationType.EATS) {
            if (id.equals(rep.leashHolder())) {
               return true;
            }

            if (id.equals(rep.providedBy())) {
               return true;
            }
         }
      }

      return false;
   }

   private void provisions(AnimalHandle handle, float space, long now) {
      float liveprey = Valence.weightOf(Halter.of(handle).tamingMatrix().act("live_prey"));

      for (Representation rep : this.known.values()) {
         boolean held = rep.tethered() || rep.leashHolder() != null;
         if (held && rep.relation().type() == RelationType.EATS && !(liveprey <= 0.0F)) {
            UUID lead = rep.leashHolder();
            float loom = Larder.loom(
               this.estimate(rep, now, handle.species()),
               space,
               lead == null ? this.known.values() : this.known.values().stream().filter(r -> !r.id().equals(lead)).toList(),
               rep.id()
            );
            Representation holder = lead == null ? null : this.known.get(lead);
            if (holder != null) {
               loom = Math.max(loom, Mth.clamp(holder.attribution(), 0.0F, 1.0F));
            }

            float before = rep.provided();
            rep.updateProvision(Mth.clamp(liveprey * (1.0F - loom), 0.0F, 1.0F), loom);
            if (before <= 0.0F && rep.provided() > 0.0F) {
               this.moment(ExpressionKey.body(ExpressionKey.Body.HUNT_NOTICE));
            }
         } else {
            rep.updateProvision(0.0F, 0.0F);
         }
      }
   }

   @Nullable
   public Representation about(UUID id) {
      return this.known.get(id);
   }

   public Collection<Representation> all() {
      return this.known.values();
   }

   public int edits() {
      return this.edits;
   }

   public List<Representation> byFind(long now) {
      List<Representation> out = new ArrayList<>(this.known.values());
      out.sort((a, b) -> Float.compare(b.find(now), a.find(now)));
      return out;
   }

   public int size() {
      return this.known.size();
   }

   private void forget(long now, int forgetTicks) {
      this.known.values().removeIf(rep -> !rep.visible() && rep.age(now) > forgetTicks);
   }

   private void evict(long now, int capacity) {
      if (this.known.size() > capacity) {
         List<Representation> candidates = new ArrayList<>(this.known.values());
         candidates.removeIf(Representation::visible);
         candidates.sort((a, b) -> Float.compare(a.evictionScore(now), b.evictionScore(now)));
         int excess = this.known.size() - capacity;

         for (int i = 0; i < excess && i < candidates.size(); i++) {
            this.known.remove(candidates.get(i).id());
         }
      }
   }
}
