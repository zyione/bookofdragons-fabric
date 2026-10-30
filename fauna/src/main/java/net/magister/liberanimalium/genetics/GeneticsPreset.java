package net.magister.liberanimalium.genetics;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;
import org.jetbrains.annotations.Nullable;
import net.minecraft.resources.ResourceLocation;

public record GeneticsPreset(Map<String, Set<String>> pools, Map<String, String> locks, Map<String, String> tiers, Map<String, Integer> stats) {
   public static final GeneticsPreset NONE = new GeneticsPreset(Map.of(), Map.of(), Map.of(), Map.of());
   private static final List<String> TIERS = List.of("common", "uncommon", "rare", "very_rare");

   public GeneticsPreset(Map<String, Set<String>> pools, Map<String, String> locks, Map<String, String> tiers) {
      this(pools, locks, tiers, Map.of());
   }

   public boolean isNone() {
      return this.pools.isEmpty() && this.locks.isEmpty() && this.tiers.isEmpty() && this.stats.isEmpty();
   }

   static int dominanceOf(int score) {
      return Math.max(1, Math.min(10, Math.round(10.0F - score / 100.0F * 9.0F)));
   }

   @Nullable
   public Allele stat(Locus locus) {
      Integer dominance = this.stats.get(locus.name());
      return dominance == null ? null : Allele.of(locus.name() + dominance, dominance);
   }

   public static GeneticsPreset parse(JsonObject json) {
      Map<String, Set<String>> pools = new LinkedHashMap<>();
      Map<String, String> locks = new LinkedHashMap<>();
      Map<String, String> tiers = new LinkedHashMap<>();
      Map<String, Integer> stats = new LinkedHashMap<>();
      if (json.has("stats")) {
         for (Entry<String, JsonElement> e : json.getAsJsonObject("stats").entrySet()) {
            if (!e.getKey().startsWith("__")) {
               Locus locus;
               try {
                  locus = Locus.valueOf(e.getKey());
               } catch (IllegalArgumentException bad) {
                  throw new IllegalArgumentException("stats." + e.getKey() + ": no such attribute (CON, ATH, AGI, PRW, POT, VIG)");
               }

               if (locus.getCategory() != Locus.LocusCategory.ATTRIBUTE) {
                  throw new IllegalArgumentException("stats." + e.getKey() + ": not an attribute locus");
               }

               int score = e.getValue().getAsInt();
               if (score < 0 || score > 100) {
                  throw new IllegalArgumentException("stats." + e.getKey() + ": a score of 0..100");
               }

               stats.put(locus.name(), dominanceOf(score));
            }
         }
      }

      if (json.has("pools")) {
         for (Entry<String, JsonElement> e : json.getAsJsonObject("pools").entrySet()) {
            Locus locus = locus(e.getKey());
            List<String> names = new ArrayList<>();

            for (JsonElement name : e.getValue().getAsJsonArray()) {
               names.add(name.getAsString());
            }

            if (names.isEmpty()) {
               throw new IllegalArgumentException("pools." + e.getKey() + ": an empty list");
            }

            pools.put(locus.name(), Set.copyOf(names));
         }
      }

      if (json.has("lock")) {
         for (Entry<String, JsonElement> e : json.getAsJsonObject("lock").entrySet()) {
            Locus locus = locus(e.getKey());
            String allele = e.getValue().getAsString();
            boolean known = false;

            for (Allele a : locus.getDefaultAlleles()) {
               known |= a.getId().equalsIgnoreCase(allele);
            }

            if (!known) {
               throw new IllegalArgumentException("lock." + e.getKey() + ": no slot allele " + allele);
            }

            locks.put(locus.name(), allele);
         }
      }

      if (json.has("tiers")) {
         for (Entry<String, JsonElement> e : json.getAsJsonObject("tiers").entrySet()) {
            Locus locus = locus(e.getKey());
            String tier = e.getValue().getAsString().toLowerCase(Locale.ROOT);
            if (!TIERS.contains(tier)) {
               throw new IllegalArgumentException("tiers." + e.getKey() + ": one of " + TIERS);
            }

            tiers.put(locus.name(), tier);
         }
      }

      return new GeneticsPreset(Map.copyOf(pools), Map.copyOf(locks), Map.copyOf(tiers), Map.copyOf(stats));
   }

   private static Locus locus(String name) {
      try {
         Locus locus = Locus.valueOf(name);
         if (locus.getCategory() != Locus.LocusCategory.COLOR && locus.getCategory() != Locus.LocusCategory.STRUCTURE) {
            throw new IllegalArgumentException("locus " + name + " is not a colour or structure locus");
         } else {
            return locus;
         }
      } catch (IllegalArgumentException e) {
         if (e.getMessage() != null && e.getMessage().startsWith("locus ")) {
            throw e;
         } else {
            throw new IllegalArgumentException("no locus " + name);
         }
      }
   }

   public Map<String, List<ResourceLocation>> narrow(String locus, Map<String, List<ResourceLocation>> pool) {
      Set<String> names = this.pools.get(locus);
      if (names != null && !pool.isEmpty()) {
         List<ResourceLocation> allowed = new ArrayList<>();

         for (List<ResourceLocation> tier : pool.values()) {
            for (ResourceLocation entry : tier) {
               if (names.contains(fileName(entry)) && !allowed.contains(entry)) {
                  allowed.add(entry);
               }
            }
         }

         if (allowed.isEmpty()) {
            return pool;
         }

         Map<String, List<ResourceLocation>> out = new HashMap<>();

         for (String tier : pool.keySet()) {
            out.put(tier, List.copyOf(allowed));
         }

         return out;
      } else {
         return pool;
      }
   }

   @Nullable
   public String lock(String locus) {
      return this.locks.get(locus);
   }

   @Nullable
   public String tier(String locus) {
      return this.tiers.get(locus);
   }

   static Allele modifierOf(String tier) {
      return Allele.of("m" + (TIERS.indexOf(tier) + 1));
   }

   private static String fileName(ResourceLocation entry) {
      String path = entry.getPath();
      String file = path.substring(path.lastIndexOf(47) + 1);
      return file.endsWith(".png") ? file.substring(0, file.length() - 4) : file;
   }
}
