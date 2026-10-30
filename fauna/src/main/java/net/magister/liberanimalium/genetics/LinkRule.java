package net.magister.liberanimalium.genetics;

import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;
import java.util.Map.Entry;
import net.minecraft.util.RandomSource;

public final class LinkRule {
   private final Map<LinkRule.Mode, Integer> modeWeights;
   private final Map<Integer, Integer> tierWeights;
   private final int totalModeWeight;

   public LinkRule(Map<LinkRule.Mode, Integer> modeWeights, Map<Integer, Integer> tierWeights) {
      this.modeWeights = Collections.unmodifiableMap(new EnumMap<>(Objects.requireNonNull(modeWeights)));
      this.tierWeights = Collections.unmodifiableMap(new TreeMap<>(Objects.requireNonNull(tierWeights)));
      int total = 0;

      for (int w : this.modeWeights.values()) {
         if (w < 0) {
            throw new IllegalArgumentException("Mode weight cannot be negative");
         }

         total += w;
      }

      if (total <= 0) {
         throw new IllegalArgumentException("Total mode weight must be positive");
      }

      this.totalModeWeight = total;
   }

   public LinkRule.Mode rollMode(RandomSource random) {
      int roll = random.nextInt(this.totalModeWeight);

      for (Entry<LinkRule.Mode, Integer> entry : this.modeWeights.entrySet()) {
         roll -= entry.getValue();
         if (roll < 0) {
            return entry.getKey();
         }
      }

      return LinkRule.Mode.NONE;
   }

   public int totalTierWeight() {
      int total = 0;

      for (int w : this.tierWeights.values()) {
         total += w;
      }

      return total;
   }

   public int rollTier(RandomSource random) {
      int total = this.totalTierWeight();
      if (total <= 0) {
         return 0;
      }

      int roll = random.nextInt(total);

      for (Entry<Integer, Integer> entry : this.tierWeights.entrySet()) {
         roll -= entry.getValue();
         if (roll < 0) {
            return entry.getKey();
         }
      }

      return 0;
   }

   @Override
   public String toString() {
      return "LinkRule{modes=" + this.modeWeights + ", tiers=" + this.tierWeights + "}";
   }

   public enum Mode {
      SAME,
      SIMILAR,
      NONE;
   }
}
