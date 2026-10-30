package net.magister.bookofdragons.discovery;

import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;
import net.magister.bookofdragons.datagen.data.DragonDiscoveryData;
import net.minecraft.world.entity.player.Player;

public final class DiscoveryStore {
   private static volatile Function<Player, Optional<DragonDiscoveryData>> lookup;

   private DiscoveryStore() {
   }

   public static Optional<DragonDiscoveryData> get(Player player) {
      Function<Player, Optional<DragonDiscoveryData>> installed = lookup;
      if (installed == null) {
         throw new IllegalStateException("no discovery store installed: the mod's entry installs one while the mod is built");
      } else {
         return installed.apply(player);
      }
   }

   public static void install(Function<Player, Optional<DragonDiscoveryData>> lookup) {
      DiscoveryStore.lookup = Objects.requireNonNull(lookup, "lookup");
   }
}
