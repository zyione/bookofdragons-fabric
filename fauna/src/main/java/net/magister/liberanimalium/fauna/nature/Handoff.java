package net.magister.liberanimalium.fauna.nature;

import java.util.UUID;
import org.jetbrains.annotations.Nullable;

public interface Handoff {
   void brief(HandoffContext var1);

   default void debrief(HandoffContext ctx) {
   }

   default void collectYield(HandoffContext ctx) {
   }

   @Nullable
   default UUID subject() {
      return null;
   }

   default void forget(UUID id) {
   }
}
