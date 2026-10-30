package net.magister.liberanimalium.fauna.api;

import java.util.Optional;
import java.util.UUID;
import org.jetbrains.annotations.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.Vec3;

public interface Halter {
   static Halter of(AnimalHandle handle) {
      return handle.endowed(Halter.class).orElseGet(() -> new Halter.Wild(handle));
   }

   Ladder stagePool();

   default boolean rideable() {
      return true;
   }

   default boolean tameable() {
      return true;
   }

   TamingMatrix tamingMatrix();

   default void tellCourter(UUID courter, int rung, int length) {
   }

   default void tellCourterReset(UUID courter) {
   }

   default void tellAsk(UUID courter, Check check) {
   }

   default void tellAskAnswered(UUID courter) {
   }

   default int touchHold(UUID courter) {
      return 0;
   }

   @Nullable
   UUID ownerId();

   default boolean ownedBy(@Nullable UUID candidate) {
      return candidate != null && candidate.equals(this.ownerId());
   }

   default Order order() {
      return Order.FREE;
   }

   @Nullable
   default Vec3 home() {
      return null;
   }

   default long freeFor() {
      return Long.MAX_VALUE;
   }

   default boolean riddenBy(UUID candidate) {
      return false;
   }

   default boolean steered() {
      return false;
   }

   default boolean carrying() {
      return false;
   }

   @Nullable
   default UUID unwelcomeRider() {
      return null;
   }

   default boolean struggling() {
      return false;
   }

   default void throwRider() {
   }

   default void adopt(UUID courter) {
   }

   @Nullable
   default Vec3 ownerPosition() {
      return null;
   }

   default boolean ownerCrouching() {
      return false;
   }

   default boolean perchOn(UUID owner) {
      return false;
   }

   default boolean perched() {
      return false;
   }

   default void leavePerch() {
   }

   default void wonAgainst(UUID loser) {
   }

   default Optional<Halter.Roost> roost() {
      return Optional.empty();
   }

   record Roost(BlockPos controller, BlockPos min, BlockPos max) {
   }

   record Wild(AnimalHandle handle) implements Halter {
      @Nullable
      @Override
      public UUID ownerId() {
         return null;
      }

      @Override
      public boolean tameable() {
         return false;
      }

      @Override
      public boolean rideable() {
         return false;
      }

      @Override
      public Ladder stagePool() {
         return this.handle.species().taming().pool();
      }

      @Override
      public TamingMatrix tamingMatrix() {
         return this.handle.species().taming().matrix();
      }
   }
}
