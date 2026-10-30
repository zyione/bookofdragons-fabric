package net.magister.liberanimalium.body.touch;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.minecraft.world.entity.player.Player;

public final class TouchState {
   private static final Map<UUID, TouchState.Gesture> SERVER = new HashMap<>();
   private static final Map<UUID, TouchState.Gesture> CLIENT = new HashMap<>();

   private TouchState() {
   }

   private static Map<UUID, TouchState.Gesture> side(boolean client) {
      return client ? CLIENT : SERVER;
   }

   public static TouchState.Gesture of(Player player) {
      return side(player.level().isClientSide()).getOrDefault(player.getUUID(), TouchState.Gesture.NONE);
   }

   public static TouchState.Gesture of(UUID player, boolean client) {
      return side(client).getOrDefault(player, TouchState.Gesture.NONE);
   }

   public static void set(UUID player, boolean client, TouchState.Gesture gesture) {
      if (gesture == TouchState.Gesture.NONE) {
         side(client).remove(player);
      } else {
         side(client).put(player, gesture);
      }
   }

   public static void clear(boolean client) {
      side(client).clear();
   }

   public enum Gesture {
      NONE,
      REACH,
      BRUSH;
   }
}
