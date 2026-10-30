package net.magister.liberanimalium.genetics;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import net.minecraft.resources.ResourceLocation;

public final class PaletteTaxonomyRegistry {
   private static final Map<ResourceLocation, PaletteCoord> REGISTRY = new HashMap<>();

   private PaletteTaxonomyRegistry() {
   }

   public static void register(ResourceLocation paletteId, PaletteCoord coord) {
      REGISTRY.put(paletteId, coord);
   }

   public static PaletteCoord get(ResourceLocation paletteId) {
      return REGISTRY.get(paletteId);
   }

   public static List<ResourceLocation> nearestByFineDistance(List<ResourceLocation> bucket, ResourceLocation anchorId) {
      PaletteCoord anchor = REGISTRY.get(anchorId);
      if (anchor != null && bucket.size() >= 2) {
         int best = Integer.MAX_VALUE;
         List<ResourceLocation> nearest = new ArrayList<>();

         for (ResourceLocation candidate : bucket) {
            PaletteCoord coord = REGISTRY.get(candidate);
            if (coord == null) {
               return bucket;
            }

            int distance = anchor.fineDistance(coord);
            if (distance < best) {
               best = distance;
               nearest.clear();
            }

            if (distance == best) {
               nearest.add(candidate);
            }
         }

         return nearest;
      } else {
         return bucket;
      }
   }

   public static TreeMap<Integer, List<ResourceLocation>> findCandidatesByTier(List<ResourceLocation> candidates, ResourceLocation anchorId, int maxTier) {
      PaletteCoord anchor = REGISTRY.get(anchorId);
      TreeMap<Integer, List<ResourceLocation>> buckets = new TreeMap<>();

      for (ResourceLocation candidate : candidates) {
         PaletteCoord coord = REGISTRY.get(candidate);
         int tier;
         if (anchor != null && coord != null) {
            tier = anchor.tierDistance(coord);
         } else {
            tier = Integer.MAX_VALUE;
         }

         if (maxTier < 0 || tier <= maxTier || tier == Integer.MAX_VALUE) {
            buckets.computeIfAbsent(tier, k -> new ArrayList<>()).add(candidate);
         }
      }

      return buckets;
   }

   public static void clear() {
      REGISTRY.clear();
   }

   public static int size() {
      return REGISTRY.size();
   }
}
