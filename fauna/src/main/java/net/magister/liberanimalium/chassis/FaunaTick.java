package net.magister.liberanimalium.chassis;

import java.util.function.Consumer;
import net.magister.liberanimalium.platform.event.Event;

public final class FaunaTick {
   public static final Event<Consumer<FaunaBase>> LIVING = new Event<>(Consumer.class);

   private FaunaTick() {
   }

   static void fire(FaunaBase body) {
      for (Consumer<FaunaBase> listener : LIVING.all()) {
         listener.accept(body);
      }
   }
}
