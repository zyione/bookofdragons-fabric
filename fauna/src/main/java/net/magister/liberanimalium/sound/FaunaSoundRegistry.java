package net.magister.liberanimalium.sound;

import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public final class FaunaSoundRegistry {
   private static final Map<String, FaunaSoundTable> TABLES = new ConcurrentHashMap<>();

   private FaunaSoundRegistry() {
   }

   public static void clear() {
      TABLES.clear();
   }

   public static void register(String species, FaunaSoundTable table) {
      if (species != null && table != null) {
         TABLES.put(species.toLowerCase(Locale.ROOT), table);
      }
   }

   public static FaunaSoundTable of(String species) {
      return species == null ? FaunaSoundTable.EMPTY : TABLES.getOrDefault(species.toLowerCase(Locale.ROOT), FaunaSoundTable.EMPTY);
   }

   public static Set<String> species() {
      return Set.copyOf(TABLES.keySet());
   }
}
