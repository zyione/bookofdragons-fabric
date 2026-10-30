package net.magister.liberanimalium.genetics;

import java.util.Map;
import net.minecraft.util.RandomSource;

public final class ModifierConfig {
   public static final String TIER_COMMON = "common";
   public static final String TIER_UNCOMMON = "uncommon";
   public static final String TIER_RARE = "rare";
   public static final String TIER_VERY_RARE = "very_rare";
   private static final String[] TIERS = new String[]{null, "common", "uncommon", "rare", "very_rare"};
   private final int weightM1;
   private final int weightM2;
   private final int weightM3;
   private final int weightM4;
   private final int totalWeight;

   public ModifierConfig(int weightM1, int weightM2, int weightM3, int weightM4) {
      if (weightM1 >= 0 && weightM2 >= 0 && weightM3 >= 0 && weightM4 >= 0) {
         this.weightM1 = weightM1;
         this.weightM2 = weightM2;
         this.weightM3 = weightM3;
         this.weightM4 = weightM4;
         this.totalWeight = weightM1 + weightM2 + weightM3 + weightM4;
         if (this.totalWeight <= 0) {
            throw new IllegalArgumentException("Total modifier weight must be positive");
         }
      } else {
         throw new IllegalArgumentException("Modifier weights must be non-negative");
      }
   }

   public static ModifierConfig fromMap(Map<String, Integer> weights) {
      return new ModifierConfig(weights.getOrDefault("m1", 40), weights.getOrDefault("m2", 30), weights.getOrDefault("m3", 20), weights.getOrDefault("m4", 10));
   }

   public static ModifierConfig defaultConfig() {
      return new ModifierConfig(40, 30, 20, 10);
   }

   public Allele rollAllele(RandomSource random) {
      int roll = random.nextInt(this.totalWeight);
      if (roll < this.weightM1) {
         return Allele.of("m1", 1);
      }

      roll -= this.weightM1;
      if (roll < this.weightM2) {
         return Allele.of("m2", 2);
      }

      roll -= this.weightM2;
      return roll < this.weightM3 ? Allele.of("m3", 3) : Allele.of("m4", 4);
   }

   public static String tierFor(String alleleId) {
      if (alleleId == null) {
         return "common";
      }

      if (alleleId.length() == 2 && alleleId.charAt(0) == 'm') {
         int num = alleleId.charAt(1) - '0';
         if (num >= 1 && num <= 4) {
            return TIERS[num];
         }
      }

      return "common";
   }

   public static String tierFor(Allele allele) {
      return tierFor(allele.getId());
   }

   public static Allele resolveDominant(Allele allele1, Allele allele2) {
      return allele1.getDominance() <= allele2.getDominance() ? allele1 : allele2;
   }

   public static String resolveTier(Allele allele1, Allele allele2) {
      return tierFor(resolveDominant(allele1, allele2));
   }

   public static int tierDominanceRank(String tier) {
      if (tier == null) {
         return 1;
      }

      switch (tier) {
         case "common":
            return 1;
         case "uncommon":
            return 2;
         case "rare":
            return 3;
         case "very_rare":
            return 4;
         default:
            return 1;
      }
   }

   @Override
   public String toString() {
      return "ModifierConfig{m1=" + this.weightM1 + ", m2=" + this.weightM2 + ", m3=" + this.weightM3 + ", m4=" + this.weightM4 + "}";
   }
}
