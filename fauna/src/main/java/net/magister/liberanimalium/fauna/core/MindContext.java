package net.magister.liberanimalium.fauna.core;

import java.util.List;
import java.util.UUID;
import org.jetbrains.annotations.Nullable;
import net.magister.liberanimalium.fauna.api.Gullet;
import net.magister.liberanimalium.fauna.api.Halter;
import net.magister.liberanimalium.fauna.api.Order;
import net.magister.liberanimalium.fauna.api.SpeciesProfile;
import net.magister.liberanimalium.fauna.drive.DriveContext;
import net.magister.liberanimalium.fauna.drive.PlayDrive;
import net.magister.liberanimalium.fauna.drive.SocialTuning;
import net.magister.liberanimalium.fauna.identity.Trait;
import net.magister.liberanimalium.fauna.know.Beckons;
import net.magister.liberanimalium.fauna.know.Chests;
import net.magister.liberanimalium.fauna.know.Courage;
import net.magister.liberanimalium.fauna.know.Larder;
import net.magister.liberanimalium.fauna.know.Noises;
import net.magister.liberanimalium.fauna.know.Representation;
import net.magister.liberanimalium.fauna.know.SocialBook;
import net.magister.liberanimalium.fauna.know.Territory;
import net.magister.liberanimalium.fauna.steer.Flock;
import net.minecraft.world.phys.Vec3;

final class MindContext implements DriveContext {
   private final FaunaMind mind;
   private List<Representation> knownSnapshot;
   private int knownEdits = Integer.MIN_VALUE;

   MindContext(FaunaMind mind) {
      this.mind = mind;
   }

   @Override
   public long age() {
      return this.mind.age();
   }

   @Override
   public float speciesWeight(String driveId) {
      return this.mind.handle().species().driveWeight(driveId);
   }

   @Override
   public SpeciesProfile.Smoothing smoothing(String driveId) {
      return this.mind.handle().species().smoothingFor(driveId);
   }

   @Override
   public float reservoir(String name) {
      return this.mind.reservoirs().level(name);
   }

   @Override
   public float trait(Trait trait) {
      return this.mind.temper().get(trait);
   }

   @Override
   public List<Representation> known() {
      int edits = this.mind.knowledge().edits();
      if (this.knownSnapshot == null || this.knownEdits != edits) {
         this.knownSnapshot = List.copyOf(this.mind.knowledge().all());
         this.knownEdits = edits;
      }

      return this.knownSnapshot;
   }

   @Override
   public List<Larder.Morsel> morsels() {
      return this.mind.knowledge().faculty(Larder.class).map(Larder::morsels).orElse(List.of());
   }

   @Override
   public List<Larder.Offer> offers() {
      return this.mind.knowledge().faculty(Larder.class).map(Larder::offers).orElse(List.of());
   }

   @Override
   public Vec3 estimate(Representation rep) {
      return this.mind.knowledge().estimate(rep, this.mind.age(), this.mind.handle().species());
   }

   @Override
   public Vec3 position() {
      return this.mind.handle().bodyPosition();
   }

   @Override
   public float draw(long salt) {
      return Seeds.unit(this.mind.handle().faunaId(), salt);
   }

   @Override
   public float healthFraction() {
      return this.mind.handle().healthFraction();
   }

   @Override
   public SocialBook.Opinion opinion(UUID id) {
      return this.mind.knowledge().social().of(id);
   }

   @Override
   public Order order() {
      return Halter.of(this.mind.handle()).order();
   }

   @Override
   public Vec3 home() {
      return Halter.of(this.mind.handle()).home();
   }

   @Override
   public float complianceShare() {
      return this.mind.knowledge().obedience().share();
   }

   @Override
   public float fightClaim() {
      return this.mind.knowledge().obedience().fightClaim();
   }

   @Override
   public boolean owned() {
      return Halter.of(this.mind.handle()).ownerId() != null;
   }

   @Override
   public UUID unwelcomeRider() {
      return Halter.of(this.mind.handle()).unwelcomeRider();
   }

   @Override
   public boolean carrying() {
      return Halter.of(this.mind.handle()).carrying();
   }

   @Override
   public float mysteryCount() {
      return this.mind.knowledge().faculty(Noises.class).map(Noises::count).orElse(0.0F);
   }

   @Override
   public float mysteryMeals() {
      return this.mind.knowledge().faculty(Noises.class).map(Noises::meals).orElse(0.0F);
   }

   @Nullable
   @Override
   public Vec3 mysteryPoint() {
      return this.mind.knowledge().faculty(Noises.class).map(Noises::point).orElse(null);
   }

   @Override
   public float freshGround() {
      return !this.mind.handle().species().social().territorial()
         ? 1.0F
         : this.mind
            .knowledge()
            .faculty(Territory.class)
            .map(terr -> Math.max(0.2F, 1.0F - terr.homeAdvantage(this.mind.handle().bodyPosition())))
            .orElse(1.0F);
   }

   @Override
   public float sleepPhase() {
      return this.mind.sleepPhase();
   }

   @Override
   public boolean asleep() {
      return this.mind.handle().asleep();
   }

   @Override
   public float contagion() {
      return this.mind.contagion();
   }

   @Override
   public float courage() {
      return Courage.of(this.mind.temper(), this.mind.knowledge().all(), this.mind.handle().bodyPosition());
   }

   @Override
   public double sightRange() {
      return this.mind.handle().senses().sightRadius();
   }

   @Override
   public double flockRadius() {
      return Flock.cohesionRadius(this.mind.handle());
   }

   @Override
   public boolean settledWith(UUID other) {
      return PairSession.settledRecently(this.mind.handle().faunaId(), other, "territory_contest", this.mind.handle().gameTime(), 1200L);
   }

   @Override
   public boolean playedWith(UUID other) {
      return PairSession.settledRecently(
         this.mind.handle().faunaId(), other, "play", this.mind.handle().gameTime(), SocialTuning.playedCooldownTicks(PlayDrive.appetite(this))
      );
   }

   @Override
   public boolean playingWith(UUID other) {
      PairSession open = PairSession.find(this.mind.handle().faunaId(), other);
      return open != null && "play".equals(open.kind());
   }

   @Override
   public boolean sparringWith(UUID other) {
      PairSession open = PairSession.find(this.mind.handle().faunaId(), other);
      return open != null && "spar".equals(open.kind());
   }

   @Override
   public boolean sparredWith(UUID other) {
      return PairSession.settledRecently(this.mind.handle().faunaId(), other, "spar", this.mind.handle().gameTime(), 600L);
   }

   @Override
   public boolean disputeOpenWith(UUID other) {
      PairSession session = PairSession.find(this.mind.handle().faunaId(), other);
      return session != null && "territory_contest".equals(session.kind());
   }

   @Override
   public float territoryBase() {
      return this.mind.handle().species().social().territoryRadius();
   }

   @Override
   public float stuckPressure() {
      return this.mind.stuckPressure();
   }

   @Nullable
   @Override
   public UUID loot() {
      return this.mind.handle().endowed(Gullet.class).map(Gullet::loot).orElse(null);
   }

   @Nullable
   @Override
   public UUID perchInvitation() {
      return this.mind.knowledge().faculty(Beckons.class).map(Beckons::invitation).orElse(null);
   }

   @Override
   public boolean perched() {
      return Halter.of(this.mind.handle()).perched();
   }

   @Override
   public List<Chests.Chest> chests() {
      return this.mind.knowledge().faculty(Chests.class).map(Chests::all).orElse(List.of());
   }
}
