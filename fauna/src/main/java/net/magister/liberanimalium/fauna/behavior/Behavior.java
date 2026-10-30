package net.magister.liberanimalium.fauna.behavior;

import net.magister.liberanimalium.fauna.api.AnimalHandle;

public interface Behavior {
   default void start(AnimalHandle handle, MoveVerbs move, long now) {
   }

   void tick(AnimalHandle var1, MoveVerbs var2, long var3);

   default void stop(AnimalHandle handle, MoveVerbs move, long now) {
   }

   default boolean romping() {
      return false;
   }
}
