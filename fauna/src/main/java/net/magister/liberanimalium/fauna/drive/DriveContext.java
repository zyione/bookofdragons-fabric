package net.magister.liberanimalium.fauna.drive;

import java.util.List;
import java.util.UUID;
import org.jetbrains.annotations.Nullable;
import net.magister.liberanimalium.fauna.api.Order;
import net.magister.liberanimalium.fauna.api.SpeciesProfile;
import net.magister.liberanimalium.fauna.identity.Trait;
import net.magister.liberanimalium.fauna.know.Chests;
import net.magister.liberanimalium.fauna.know.Larder;
import net.magister.liberanimalium.fauna.know.Representation;
import net.magister.liberanimalium.fauna.know.SocialBook;
import net.magister.liberanimalium.fauna.know.Ties;
import net.minecraft.world.phys.Vec3;

public interface DriveContext {
   long age();

   float speciesWeight(String var1);

   @Nullable
   SpeciesProfile.Smoothing smoothing(String var1);

   float reservoir(String var1);

   float trait(Trait var1);

   List<Representation> known();

   List<Larder.Morsel> morsels();

   List<Larder.Offer> offers();

   Vec3 estimate(Representation var1);

   @Nullable
   default Vec3 whereIs(UUID id) {
      for (Representation rep : this.known()) {
         if (rep.id().equals(id)) {
            return this.estimate(rep);
         }
      }

      return null;
   }

   Vec3 position();

   float draw(long var1);

   SocialBook.Opinion opinion(UUID var1);

   float mysteryCount();

   default float mysteryMeals() {
      return 0.0F;
   }

   @Nullable
   Vec3 mysteryPoint();

   float freshGround();

   float sleepPhase();

   default boolean asleep() {
      return false;
   }

   float contagion();

   float courage();

   double sightRange();

   double flockRadius();

   float healthFraction();

   boolean settledWith(UUID var1);

   boolean disputeOpenWith(UUID var1);

   boolean playedWith(UUID var1);

   boolean playingWith(UUID var1);

   default boolean sparringWith(UUID other) {
      return false;
   }

   default boolean sparredWith(UUID other) {
      return false;
   }

   float territoryBase();

   float stuckPressure();

   default Order order() {
      return Order.FREE;
   }

   @Nullable
   default Vec3 home() {
      return null;
   }

   default float tie(Representation rep) {
      return Ties.pull(rep, this.age(), this.order(), this.home(), this.estimate(rep));
   }

   default float complianceShare() {
      return 0.0F;
   }

   default float fightClaim() {
      return 0.0F;
   }

   default boolean owned() {
      return false;
   }

   @Nullable
   default UUID unwelcomeRider() {
      return null;
   }

   default boolean carrying() {
      return false;
   }

   @Nullable
   default UUID loot() {
      return null;
   }

   @Nullable
   default UUID perchInvitation() {
      return null;
   }

   default boolean perched() {
      return false;
   }

   default List<Chests.Chest> chests() {
      return List.of();
   }
}
