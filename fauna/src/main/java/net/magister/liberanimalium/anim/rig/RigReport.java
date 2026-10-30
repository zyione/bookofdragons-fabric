package net.magister.liberanimalium.anim.rig;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;

public final class RigReport {
   private static final Map<String, Set<String>> GHOSTS = new ConcurrentHashMap<>();
   private static final Set<String> CHECKED = ConcurrentHashMap.newKeySet();

   private RigReport() {
   }

   public static void check(String species, RigSpec spec, BoneSink sink) {
      check(species, spec, List.of(), sink);
   }

   public static boolean checked(String species) {
      return CHECKED.contains(species);
   }

   public static void check(String species, RigSpec spec, List<String> partBones, BoneSink sink) {
      if (CHECKED.add(species)) {
         Set<String> ghosts = new LinkedHashSet<>();

         for (String bone : partBones) {
            if (!sink.has(bone)) {
               ghosts.add("parts." + bone);
            }
         }

         for (Entry<String, List<String>> chain : spec.chains().entrySet()) {
            for (String bone : chain.getValue()) {
               boolean found = false;

               for (String prefix : spec.prefixes()) {
                  if (sink.has(prefix + bone)) {
                     found = true;
                     break;
                  }
               }

               if (!found) {
                  ghosts.add(chain.getKey() + "." + bone);
               }
            }
         }

         if (!ghosts.isEmpty()) {
            GHOSTS.put(species, Set.copyOf(ghosts));
         }
      }
   }

   public static Map<String, Set<String>> ghosts() {
      return Map.copyOf(GHOSTS);
   }

   public static int ghostCount() {
      int n = 0;

      for (Collection<String> names : GHOSTS.values()) {
         n += names.size();
      }

      return n;
   }

   public static void invalidate() {
      GHOSTS.clear();
      CHECKED.clear();
   }
}
