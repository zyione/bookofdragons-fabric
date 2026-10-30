package net.magister.liberanimalium.ride.touch;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.magister.liberanimalium.anim.rig.DampedSpring;
import net.magister.liberanimalium.body.touch.TouchState;
import net.magister.liberanimalium.ride.render.RiderClock;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;

public final class TouchEase {
   private static final Map<UUID, TouchEase.Eased> EASED = new HashMap<>();

   private TouchEase() {
   }

   public static float step(Player player, float partialTick) {
      TouchState.Gesture held = TouchState.of(player);
      TouchEase.Eased eased = EASED.get(player.getUUID());
      if (eased == null) {
         if (held == TouchState.Gesture.NONE) {
            return 0.0F;
         }

         eased = new TouchEase.Eased();
         EASED.put(player.getUUID(), eased);
      }

      if (held != TouchState.Gesture.NONE) {
         eased.kind = held;
      }

      eased.clock.frame(player.tickCount + partialTick, System.nanoTime());
      eased.weight.step(held == TouchState.Gesture.NONE ? 0.0 : 1.0, eased.clock.dtTicks() / 20.0, 2.5, 1.0, 0.0);
      eased.now = Mth.clamp((float)eased.weight.value(), 0.0F, 1.0F);
      if (held == TouchState.Gesture.NONE && eased.now < 0.01F) {
         EASED.remove(player.getUUID());
         return 0.0F;
      } else {
         return eased.now;
      }
   }

   public static float weight(UUID player) {
      TouchEase.Eased eased = EASED.get(player);
      return eased == null ? 0.0F : eased.now;
   }

   public static TouchState.Gesture kind(UUID player) {
      TouchEase.Eased eased = EASED.get(player);
      return eased == null ? TouchState.Gesture.NONE : eased.kind;
   }

   public static void clear() {
      EASED.clear();
   }

   private static final class Eased {
      final DampedSpring weight = new DampedSpring();
      final RiderClock clock = new RiderClock();
      TouchState.Gesture kind = TouchState.Gesture.NONE;
      float now;
   }
}
