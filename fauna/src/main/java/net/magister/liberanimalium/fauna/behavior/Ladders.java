package net.magister.liberanimalium.fauna.behavior;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public final class Ladders {
   private static final Map<String, List<String>> RUNGS = new ConcurrentHashMap<>(build());

   private Ladders() {
   }

   public static void register(String driveId, Enum<?>[] rungs) {
      if (RUNGS.putIfAbsent(driveId, names(rungs)) != null) {
         throw new IllegalArgumentException("drive '" + driveId + "' already has a ladder");
      }
   }

   private static Map<String, List<String>> build() {
      Map<String, List<String>> out = new LinkedHashMap<>();
      out.put("rival", names(Contest.Rung.values()));
      out.put("space", names(Shoo.Rung.values()));
      out.put("buck", names(Buck.Rung.values()));
      out.put("pilfer", names(Pilfer.Rung.values()));
      return Map.copyOf(out);
   }

   private static List<String> names(Enum<?>[] values) {
      List<String> out = new ArrayList<>(values.length);

      for (Enum<?> value : values) {
         out.add(value.name().toLowerCase(Locale.ROOT));
      }

      return List.copyOf(out);
   }

   public static Set<String> ladderedDrives() {
      return RUNGS.keySet();
   }

   public static List<String> rungsOf(String driveId) {
      return RUNGS.getOrDefault(driveId, List.of());
   }
}
