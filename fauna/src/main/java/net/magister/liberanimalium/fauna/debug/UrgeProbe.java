package net.magister.liberanimalium.fauna.debug;

import java.util.UUID;
import java.util.function.Consumer;
import org.jetbrains.annotations.Nullable;
import net.magister.liberanimalium.fauna.api.Relation;

public final class UrgeProbe {
   private static volatile Consumer<UrgeProbe.Urge> sink;

   private UrgeProbe() {
   }

   public static void listen(@Nullable Consumer<UrgeProbe.Urge> listener) {
      sink = listener;
   }

   public static boolean listening() {
      return sink != null;
   }

   public static void urged(UrgeProbe.Urge urge) {
      Consumer<UrgeProbe.Urge> listener = sink;
      if (listener != null) {
         listener.accept(urge);
      }
   }

   public record Urge(UUID animal, UUID owner, UUID target, boolean calm, float share, Relation before, Relation after, long gameTime) {
   }
}
