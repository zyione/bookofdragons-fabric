package net.magister.bookofdragons.client;

import net.magister.bookofdragons.effect.Paralysis;
import net.minecraft.client.player.Input;
import net.minecraft.world.entity.player.Player;

public final class ParalysisInput {
   private ParalysisInput() {
   }

   public static void still(Player player, Input input) {
      if (Paralysis.is(player)) {
         input.forwardImpulse = 0.0F;
         input.leftImpulse = 0.0F;
         input.up = false;
         input.down = false;
         input.left = false;
         input.right = false;
         input.jumping = false;
      }
   }
}
