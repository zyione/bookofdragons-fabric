package net.magister.liberanimalium.genetics;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.Map.Entry;
import java.util.function.Predicate;
import net.magister.liberanimalium.genetics.config.SpeciesGeneticsConfig;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;

public final class PaletteDraws {
   private static final String NBT_KEY = "PaletteDraws";
   private static final int MAX_PACKABLE_ORDINAL = 31;
   private final Map<String, ResourceLocation> draws;
   private final Set<String> dormantKeys;
   private final Map<String, Integer> unresolvedDraws;

   private static boolean isDrawable(Locus locus) {
      return locus.getCategory() == Locus.LocusCategory.COLOR || locus.getCategory() == Locus.LocusCategory.STRUCTURE;
   }

   public PaletteDraws() {
      this.draws = new HashMap<>();
      this.dormantKeys = new HashSet<>();
      this.unresolvedDraws = new HashMap<>();
   }

   private PaletteDraws(Map<String, ResourceLocation> draws, Set<String> dormantKeys, Map<String, Integer> unresolvedDraws) {
      this.draws = new HashMap<>(draws);
      this.dormantKeys = new HashSet<>(dormantKeys);
      this.unresolvedDraws = new HashMap<>(unresolvedDraws);
   }

   public void setDraw(Locus locus, int exprIndex, ResourceLocation c1, ResourceLocation c2, String dormantSlot) {
      String prefix = locus.name() + "_" + exprIndex;
      String c1Key = prefix + "_c1";
      String c2Key = prefix + "_c2";
      if (c1 != null) {
         this.draws.put(c1Key, c1);
      }

      if (c2 != null) {
         this.draws.put(c2Key, c2);
      }

      this.unresolvedDraws.remove(c1Key);
      this.unresolvedDraws.remove(c2Key);
      this.dormantKeys.remove(c1Key);
      this.dormantKeys.remove(c2Key);
      if ("c1".equals(dormantSlot)) {
         this.dormantKeys.add(c1Key);
      } else if ("c2".equals(dormantSlot)) {
         this.dormantKeys.add(c2Key);
      }
   }

   public void setDraw(Locus locus, int exprIndex, ResourceLocation c1, ResourceLocation c2) {
      this.setDraw(locus, exprIndex, c1, c2, (String)null);
   }

   public void setColorDraw(Locus locus, int exprIndex, ResourceLocation c1, ResourceLocation c2, String dormantSlot) {
      this.setDraw(locus, exprIndex, c1, c2, dormantSlot);
   }

   public void setColorDraw(Locus locus, int exprIndex, ResourceLocation c1, ResourceLocation c2) {
      this.setDraw(locus, exprIndex, c1, c2, (String)null);
   }

   public ResourceLocation getColorDraw(Locus locus, int exprIndex, String slot) {
      String key = locus.name() + "_" + exprIndex + "_" + slot;
      ResourceLocation rl = this.draws.get(key);
      if (rl == null && this.unresolvedDraws.containsKey(key)) {
         int hash = this.unresolvedDraws.get(key);
         rl = PalettePoolRegistry.getByHash(hash);
         if (rl != null) {
            this.draws.put(key, rl);
            this.unresolvedDraws.remove(key);
         }
      }

      return rl;
   }

   public ResourceLocation getC1(Locus locus, int exprIndex) {
      return this.getColorDraw(locus, exprIndex, "c1");
   }

   public ResourceLocation getC2(Locus locus, int exprIndex) {
      return this.getColorDraw(locus, exprIndex, "c2");
   }

   public void setStructureDraw(Locus locus, int exprIndex, ResourceLocation primary, ResourceLocation secondary, String dormantSlot) {
      this.setDraw(locus, exprIndex, primary, secondary, dormantSlot);
   }

   public boolean isDormant(Locus locus, int exprIndex, String slot) {
      return this.dormantKeys.contains(locus.name() + "_" + exprIndex + "_" + slot);
   }

   public boolean isDormant(String key) {
      return this.dormantKeys.contains(key);
   }

   public int size() {
      return this.draws.size() + this.unresolvedDraws.size();
   }

   public boolean isEmpty() {
      return this.draws.isEmpty() && this.unresolvedDraws.isEmpty();
   }

   public void retainExpressed(SpeciesGeneticsConfig config) {
      Predicate<String> gone = key -> !expressed(key, config);
      this.draws.keySet().removeIf(gone);
      this.unresolvedDraws.keySet().removeIf(gone);
      this.dormantKeys.removeIf(gone);
   }

   public boolean hasLostArt() {
      this.unresolvedDraws.entrySet().removeIf(entry -> {
         ResourceLocation rl = PalettePoolRegistry.getByHash(entry.getValue());
         if (rl != null) {
            this.draws.put(entry.getKey(), rl);
         }

         return rl != null;
      });
      return !this.unresolvedDraws.isEmpty();
   }

   private static boolean expressed(String key, SpeciesGeneticsConfig config) {
      int lastUnderscore = key.lastIndexOf(95);
      int prevUnderscore = key.lastIndexOf(95, lastUnderscore - 1);
      if (prevUnderscore <= 0) {
         return false;
      }

      try {
         Locus locus = Locus.valueOf(key.substring(0, prevUnderscore));
         return Integer.parseInt(key.substring(prevUnderscore + 1, lastUnderscore)) < config.getExpressionCount(locus);
      } catch (IllegalArgumentException e) {
         return false;
      }
   }

   public CompoundTag toNBT() {
      CompoundTag tag = new CompoundTag();
      long[] packedArray = new long[this.draws.size() + this.unresolvedDraws.size()];
      int i = 0;

      for (Entry<String, ResourceLocation> entry : this.draws.entrySet()) {
         String key = entry.getKey();
         ResourceLocation rl = entry.getValue();
         int textureHash = rl.toString().hashCode();
         boolean isC2 = key.endsWith("c2");
         boolean isDormant = this.dormantKeys.contains(key);
         int lastUnderscore = key.lastIndexOf(95);
         int prevUnderscore = key.lastIndexOf(95, lastUnderscore - 1);
         if (prevUnderscore != -1 && lastUnderscore != -1) {
            String locusName = key.substring(0, prevUnderscore);

            int exprIndex;
            try {
               exprIndex = Integer.parseInt(key.substring(prevUnderscore + 1, lastUnderscore));
            } catch (NumberFormatException e) {
               continue;
            }

            Locus locus;
            try {
               locus = Locus.valueOf(locusName);
            } catch (IllegalArgumentException e) {
               continue;
            }

            int locusOrdinal = locus.ordinal();
            if (locusOrdinal > 31) {
               throw new IllegalStateException(
                  "Locus "
                     + locus
                     + " has ordinal "
                     + locusOrdinal
                     + ", past the five-bit palette field (max 31). Declare non-drawing loci after the drawing ones, or widen the field."
               );
            }

            long packed = (locusOrdinal & 31L) << 59
               | (exprIndex & 15L) << 55
               | (isC2 ? 1L : 0L) << 54
               | (isDormant ? 1L : 0L) << 53
               | textureHash & 4294967295L;
            packedArray[i++] = packed;
         }
      }

      for (Entry<String, Integer> entry : this.unresolvedDraws.entrySet()) {
         String key = entry.getKey();
         int textureHash = entry.getValue();
         boolean isC2 = key.endsWith("c2");
         boolean isDormant = this.dormantKeys.contains(key);
         int lastUnderscore = key.lastIndexOf(95);
         int prevUnderscore = key.lastIndexOf(95, lastUnderscore - 1);
         if (prevUnderscore != -1 && lastUnderscore != -1) {
            String locusName = key.substring(0, prevUnderscore);

            int exprIndex;
            try {
               exprIndex = Integer.parseInt(key.substring(prevUnderscore + 1, lastUnderscore));
            } catch (NumberFormatException e) {
               continue;
            }

            Locus locus;
            try {
               locus = Locus.valueOf(locusName);
            } catch (IllegalArgumentException e) {
               continue;
            }

            int locusOrdinal = locus.ordinal();
            if (locusOrdinal > 31) {
               throw new IllegalStateException("Locus " + locus + " has ordinal " + locusOrdinal + ", past the five-bit palette field (max 31)");
            }

            long packed = (locusOrdinal & 31L) << 59
               | (exprIndex & 15L) << 55
               | (isC2 ? 1L : 0L) << 54
               | (isDormant ? 1L : 0L) << 53
               | textureHash & 4294967295L;
            packedArray[i++] = packed;
         }
      }

      if (i < packedArray.length) {
         packedArray = Arrays.copyOf(packedArray, i);
      }

      tag.putLongArray("Draws", packedArray);
      return tag;
   }

   public static PaletteDraws fromNBT(CompoundTag tag) {
      Map<String, ResourceLocation> draws = new HashMap<>();
      Set<String> dormant = new HashSet<>();
      Map<String, Integer> unresolved = new HashMap<>();
      if (tag.getTagType("Draws") != 12) {
         return new PaletteDraws(draws, dormant, unresolved);
      }

      Locus[] locusValues = Locus.values();
      long[] packedArray = tag.getLongArray("Draws");

      for (long packed : packedArray) {
         int locusOrdinal = (int)(packed >> 59 & 31L);
         int exprIndex = (int)(packed >> 55 & 15L);
         boolean isC2 = (packed >> 54 & 1L) == 1L;
         boolean isDormant = (packed >> 53 & 1L) == 1L;
         int textureHash = (int)(packed & 4294967295L);
         if (locusOrdinal < locusValues.length && isDrawable(locusValues[locusOrdinal])) {
            Locus locus = locusValues[locusOrdinal];
            String slot = isC2 ? "c2" : "c1";
            String key = locus.name() + "_" + exprIndex + "_" + slot;
            ResourceLocation rl = PalettePoolRegistry.getByHash(textureHash);
            if (rl != null) {
               draws.put(key, rl);
            } else {
               unresolved.put(key, textureHash);
            }

            if (isDormant) {
               dormant.add(key);
            }
         }
      }

      return new PaletteDraws(draws, dormant, unresolved);
   }

   public void saveTo(CompoundTag parent) {
      parent.put("PaletteDraws", this.toNBT());
   }

   public String toCacheString() {
      StringBuilder sb = new StringBuilder();
      this.draws.entrySet().stream().sorted(Entry.comparingByKey()).forEach(e -> {
         if (sb.length() > 0) {
            sb.append("|");
         }

         sb.append(e.getKey()).append("=").append(e.getValue());
         if (this.dormantKeys.contains(e.getKey())) {
            sb.append("[D]");
         }
      });
      return sb.toString();
   }

   @Override
   public boolean equals(Object o) {
      if (this == o) {
         return true;
      } else {
         return !(o instanceof PaletteDraws that) ? false : this.draws.equals(that.draws) && this.dormantKeys.equals(that.dormantKeys);
      }
   }

   @Override
   public int hashCode() {
      return Objects.hash(this.draws, this.dormantKeys);
   }

   @Override
   public String toString() {
      return "PaletteDraws{" + this.draws.size() + " entries, " + this.dormantKeys.size() + " dormant}";
   }
}
