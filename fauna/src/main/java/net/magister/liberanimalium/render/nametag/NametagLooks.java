package net.magister.liberanimalium.render.nametag;

import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import org.jetbrains.annotations.Nullable;
import net.magister.liberanimalium.genetics.GeneticsPreset;
import net.magister.liberanimalium.genetics.GeneticsSpawner;
import net.magister.liberanimalium.genetics.config.SpeciesGeneticsConfig;
import net.magister.liberanimalium.genetics.config.SpeciesGeneticsConfigRegistry;
import net.minecraft.util.RandomSource;

public final class NametagLooks {
   private static final Map<String, Optional<GeneticsSpawner.SpawnResult>> LOOKS = new ConcurrentHashMap<>();

   private NametagLooks() {
   }

   @Nullable
   public static GeneticsSpawner.SpawnResult of(String speciesId, String name, GeneticsPreset preset) {
      String key = speciesId + "\u0000" + name.toLowerCase(Locale.ROOT);
      return LOOKS.computeIfAbsent(key, k -> draw(speciesId, k, preset)).orElse(null);
   }

   public static void clear() {
      LOOKS.clear();
   }

   private static Optional<GeneticsSpawner.SpawnResult> draw(String speciesId, String key, GeneticsPreset preset) {
      SpeciesGeneticsConfig config = SpeciesGeneticsConfigRegistry.getConfig(speciesId);
      if (config == null) {
         return Optional.empty();
      }

      GeneticsSpawner spawner = new GeneticsSpawner(RandomSource.create(key.hashCode()));
      return Optional.of(spawner.generateWildGenotype(config, speciesId, null, preset));
   }
}
