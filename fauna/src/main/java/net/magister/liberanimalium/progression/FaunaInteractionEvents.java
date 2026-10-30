package net.magister.liberanimalium.progression;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;
import net.magister.liberanimalium.chassis.FaunaBase;
import net.minecraft.world.entity.player.Player;
import org.jetbrains.annotations.Nullable;

public final class FaunaInteractionEvents {
   private static final List<Consumer<FaunaInteractionEvents.Event>> LISTENERS = new CopyOnWriteArrayList<>();

   private FaunaInteractionEvents() {
   }

   public static void subscribe(Consumer<FaunaInteractionEvents.Event> listener) {
      LISTENERS.add(listener);
   }

   public static void clearListeners() {
      LISTENERS.clear();
   }

   public static void post(FaunaInteractionEvents.Type type, @Nullable Player player, FaunaBase body, @Nullable Object context) {
      if (body != null && !body.isClientSide()) {
         if (!LISTENERS.isEmpty()) {
            FaunaInteractionEvents.Event event = new FaunaInteractionEvents.Event(type, player, body, context);

            for (Consumer<FaunaInteractionEvents.Event> listener : LISTENERS) {
               listener.accept(event);
            }
         }
      }
   }

   public record Event(FaunaInteractionEvents.Type type, @Nullable Player player, FaunaBase body, @Nullable Object context) {
   }

   public enum Type {
      MOUNTED,
      WITNESSED_ABILITY,
      STRUGGLE_WON,
      ASK_ANSWERED;
   }
}
