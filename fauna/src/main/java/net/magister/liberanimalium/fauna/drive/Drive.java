package net.magister.liberanimalium.fauna.drive;

import org.jetbrains.annotations.Nullable;
import net.magister.liberanimalium.fauna.know.Representation;
import net.magister.liberanimalium.fauna.know.Wants;

public interface Drive {
   String id();

   float score(DriveContext var1);

   default void onStart(DriveContext context) {
   }

   default void onTick(DriveContext context) {
   }

   default void onStop(DriveContext context) {
   }

   default boolean needsASubject() {
      return false;
   }

   default float settles(DriveContext context) {
      return 1.0F;
   }

   default boolean occupies(DriveContext context) {
      return false;
   }

   default String store() {
      return null;
   }

   default boolean satiates() {
      return this.store() != null;
   }

   default Wants.Want shows() {
      return Wants.Want.NONE;
   }

   @Nullable
   default Representation warns(DriveContext context) {
      return null;
   }
}
