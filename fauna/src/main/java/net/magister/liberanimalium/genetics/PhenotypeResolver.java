package net.magister.liberanimalium.genetics;

public class PhenotypeResolver {
   public String resolve(Genotype genotype, Locus locus, int pairIndex) {
      Allele allele1 = genotype.getAllele1(pairIndex);
      Allele allele2 = genotype.getAllele2(pairIndex);
      return this.resolve(locus, allele1, allele2);
   }

   public String resolve(Locus locus, Allele allele1, Allele allele2) {
      if (allele1.equals(allele2)) {
         return allele1.getId();
      } else {
         return allele1.getDominance() <= allele2.getDominance() ? allele1.getId() : allele2.getId();
      }
   }

   public PhenotypeResolver.ColorBlendResult resolveColorBlend(String tier1, String tier2, boolean isHomozygous) {
      if (isHomozygous) {
         return new PhenotypeResolver.ColorBlendResult("", "", 100, 0);
      }

      int r1 = this.getTierRank(tier1);
      int r2 = this.getTierRank(tier2);
      int dist = Math.abs(r1 - r2);

      int secondaryPercent = switch (dist) {
         case 0 -> 55;
         case 1 -> 35;
         case 2 -> 15;
         case 3 -> 5;
         default -> 50;
      };
      int primaryPercent = 100 - secondaryPercent;
      return r2 < r1
         ? new PhenotypeResolver.ColorBlendResult("", "", secondaryPercent, primaryPercent)
         : new PhenotypeResolver.ColorBlendResult("", "", primaryPercent, secondaryPercent);
   }

   private int getTierRank(String tier) {
      if (tier == null) {
         return 1;
      }

      switch (tier.toLowerCase()) {
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

   public static class ColorBlendResult {
      public final String allele1Id;
      public final String allele2Id;
      public final int allele1Percent;
      public final int allele2Percent;
      public final boolean isSinglePalette;

      public ColorBlendResult(String a1, String a2, int p1, int p2) {
         this.allele1Id = a1;
         this.allele2Id = a2;
         this.allele1Percent = p1;
         this.allele2Percent = p2;
         this.isSinglePalette = p1 == 100 || p2 == 100;
      }
   }
}
