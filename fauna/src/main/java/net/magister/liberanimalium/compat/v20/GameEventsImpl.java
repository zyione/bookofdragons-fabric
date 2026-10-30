package net.magister.liberanimalium.compat.v20;

import net.magister.liberanimalium.platform.registry.RegEntry;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.gameevent.GameEvent;

public final class GameEventsImpl {
   private GameEventsImpl() {
   }

   public static GameEvent create(String name, int radius) {
      return new GameEvent(name, radius);
   }

   public static void send(Entity entity, RegEntry<GameEvent> event) {
      entity.gameEvent(event.get());
   }
}
