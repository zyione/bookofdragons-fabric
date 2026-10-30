package net.magister.liberanimalium.fauna.api;

import java.util.UUID;
import org.jetbrains.annotations.Nullable;

public interface Gullet {
   boolean eatItem(UUID var1);

   default boolean snatch(UUID holder) {
      return false;
   }

   @Nullable
   default UUID loot() {
      return null;
   }

   default boolean eatLoot() {
      return false;
   }

   default void dropLoot() {
   }

   default boolean takeItem(UUID itemId) {
      return false;
   }

   default void stash() {
   }
}
