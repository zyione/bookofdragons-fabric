package net.magister.liberanimalium.body.nav.noise;

import java.util.Map;
import org.jetbrains.annotations.Nullable;
import net.magister.liberanimalium.fauna.api.SoundKind;
import net.minecraft.world.level.gameevent.GameEvent;

public final class NoiseKind {
   private static final Map<GameEvent, SoundKind> EVENTS = Map.ofEntries(
      Map.entry(GameEvent.ENTITY_DAMAGE, SoundKind.COMBAT),
      Map.entry(GameEvent.ENTITY_DIE, SoundKind.COMBAT),
      Map.entry(GameEvent.PROJECTILE_SHOOT, SoundKind.COMBAT),
      Map.entry(GameEvent.PROJECTILE_LAND, SoundKind.COMBAT),
      Map.entry(GameEvent.EXPLODE, SoundKind.COMBAT),
      Map.entry(GameEvent.BLOCK_PLACE, SoundKind.CONSTRUCTION),
      Map.entry(GameEvent.BLOCK_CHANGE, SoundKind.CONSTRUCTION),
      Map.entry(GameEvent.BLOCK_DESTROY, SoundKind.CONSTRUCTION),
      Map.entry(GameEvent.BLOCK_ACTIVATE, SoundKind.CONSTRUCTION),
      Map.entry(GameEvent.CONTAINER_OPEN, SoundKind.CONSTRUCTION),
      Map.entry(GameEvent.STEP, SoundKind.LOCOMOTION),
      Map.entry(GameEvent.SWIM, SoundKind.LOCOMOTION),
      Map.entry(GameEvent.ELYTRA_GLIDE, SoundKind.LOCOMOTION),
      Map.entry(GameEvent.ENTITY_MOUNT, SoundKind.LOCOMOTION),
      Map.entry(GameEvent.EAT, SoundKind.FORAGING),
      Map.entry(GameEvent.DRINK, SoundKind.FORAGING),
      Map.entry(GameEvent.ENTITY_INTERACT, SoundKind.FORAGING),
      Map.entry(GameEvent.ITEM_INTERACT_FINISH, SoundKind.FORAGING)
   );

   private NoiseKind() {
   }

   @Nullable
   public static SoundKind classify(GameEvent event) {
      return event == FaunaGameEvents.CALL.get() ? SoundKind.VOCAL : EVENTS.get(event);
   }

   public static boolean isRelevant(GameEvent event) {
      return event == FaunaGameEvents.CALL.get() || EVENTS.containsKey(event);
   }
}
