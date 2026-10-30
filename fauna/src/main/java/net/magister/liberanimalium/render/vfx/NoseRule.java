package net.magister.liberanimalium.render.vfx;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public final class NoseRule {
   private static final Map<String, Set<String>> KNOWN = new ConcurrentHashMap<>();

   private NoseRule() {
   }

   public static Set<String> noseBones(Collection<String> boneNames) {
      Set<String> nostrils = new LinkedHashSet<>();
      Set<String> nose = new LinkedHashSet<>();
      Set<String> snout = new LinkedHashSet<>();

      for (String name : boneNames) {
         String lower = name.toLowerCase(Locale.ROOT);
         if (lower.contains("nostril")) {
            nostrils.add(name);
         } else if (lower.equals("nose")) {
            nose.add(name);
         } else if (lower.equals("snout")) {
            snout.add(name);
         }
      }

      if (!nostrils.isEmpty()) {
         return nostrils;
      } else {
         return !nose.isEmpty() ? nose : snout;
      }
   }

   public static Set<String> of(String species, Collection<String> boneNames, Collection<String> named) {
      return KNOWN.computeIfAbsent(
         species, id -> named.isEmpty() ? Set.copyOf(noseBones(boneNames)) : Set.copyOf(named.stream().filter(boneNames::contains).toList())
      );
   }

   public static Set<String> known(String species) {
      return KNOWN.getOrDefault(species, Set.of());
   }
}
