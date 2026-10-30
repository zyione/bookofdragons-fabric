package net.magister.liberanimalium.render.membrane;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public final class MembraneDefinitions {
   private static final Map<String, MembraneDefinitions.Species> REGISTRY = new ConcurrentHashMap<>();

   private MembraneDefinitions() {
   }

   public static void put(String species, List<MembranePanel> panels) {
      Set<String> captured = new HashSet<>();

      for (MembranePanel p : panels) {
         for (String c : p.corner()) {
            captured.add(c);
         }

         if (p.foldRef() != null) {
            for (String c : p.foldRef()) {
               captured.add(c);
            }
         }

         if (p.dorsalAnchor() != null) {
            captured.add(p.dorsalAnchor());
         }
      }

      REGISTRY.put(species, new MembraneDefinitions.Species(List.copyOf(panels), Set.copyOf(captured)));
   }

   public static MembraneDefinitions.Species get(String species) {
      return REGISTRY.get(species);
   }

   public static void clear() {
      REGISTRY.clear();
   }

   public record Species(List<MembranePanel> panels, Set<String> capturedBones) {
   }
}
