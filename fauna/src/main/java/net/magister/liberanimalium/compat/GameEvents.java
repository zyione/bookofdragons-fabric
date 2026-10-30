package net.magister.liberanimalium.compat;

import net.magister.liberanimalium.compat.v20.GameEventsImpl;
import net.magister.liberanimalium.platform.registry.RegEntry;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.gameevent.GameEvent;

public final class GameEvents {
   private GameEvents() {
   }

   public static GameEvent create(String name, int radius) {
      return GameEventsImpl.create(name, radius);
   }

   public static void send(Entity entity, RegEntry<GameEvent> event) {
      GameEventsImpl.send(entity, event);
   }
}
