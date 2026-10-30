package net.magister.liberanimalium.genetics;

public final class GenotypeRelayout {
   private GenotypeRelayout() {
   }

   public static int pairsPerExpression(Locus locus) {
      return switch (locus.getCategory()) {
         case COLOR, STRUCTURE -> 3;
         case ATTRIBUTE -> 1;
         default -> 0;
      };
   }

   public static int pairsOf(int[] layout) {
      int total = 0;

      for (int i = 0; i < Locus.GENOTYPE_ORDER.length; i++) {
         total += layout[i] * pairsPerExpression(Locus.GENOTYPE_ORDER[i]);
      }

      return total;
   }

   public static Genotype relay(int[] oldLayout, int[] newLayout, Genotype old, GenotypeRelayout.Roller roller) {
      if (oldLayout.length == Locus.GENOTYPE_ORDER.length && newLayout.length == Locus.GENOTYPE_ORDER.length) {
         if (old.getPairCount() < pairsOf(oldLayout)) {
            throw new IllegalArgumentException("saved genotype has " + old.getPairCount() + " pairs but its layout says " + pairsOf(oldLayout));
         }

         Genotype.Builder into = Genotype.builder();
         int oldOffset = 0;

         for (int i = 0; i < Locus.GENOTYPE_ORDER.length; i++) {
            Locus locus = Locus.GENOTYPE_ORDER[i];
            int per = pairsPerExpression(locus);
            int had = oldLayout[i];
            int wants = newLayout[i];
            int kept = Math.min(had, wants);

            for (int e = 0; e < kept; e++) {
               for (int p = 0; p < per; p++) {
                  int pair = oldOffset + e * per + p;
                  into.addPair(old.getAllele1(pair), old.getAllele2(pair));
               }
            }

            for (int e = kept; e < wants; e++) {
               roller.roll(locus, e, into);
            }

            oldOffset += had * per;
         }

         return into.build();
      } else {
         throw new IllegalArgumentException("a layout has one entry per locus of GENOTYPE_ORDER");
      }
   }

   @FunctionalInterface
   public interface Roller {
      void roll(Locus var1, int var2, Genotype.Builder var3);
   }
}
