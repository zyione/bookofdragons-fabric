package net.magister.bookofdragons.discovery;

import net.magister.bookofdragons.entity.base.dragon.DragonBase;
import net.magister.bookofdragons.entity.data.DragonType;
import net.minecraft.world.entity.player.Player;

public class EncounterDetectionSystem {
   private static final double ENCOUNTER_RANGE = 32.0;
   private static final double ENCOUNTER_RANGE_SQ = 1024.0;

   public static boolean shouldMarkEncountered(Player player, DragonBase dragon) {
      double distanceSq = player.distanceToSqr(dragon);
      return distanceSq <= 1024.0;
   }

   public static DragonType getDragonType(DragonBase dragon) {
      return dragon.getDragonType();
   }
}
