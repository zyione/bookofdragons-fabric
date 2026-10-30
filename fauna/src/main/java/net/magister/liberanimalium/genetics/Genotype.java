package net.magister.liberanimalium.genetics;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

public final class Genotype {
   private final List<Allele> alleles;

   private Genotype(List<Allele> alleles) {
      this.alleles = Collections.unmodifiableList(new ArrayList<>(alleles));
   }

   public int getAlleleCount() {
      return this.alleles.size();
   }

   public Allele getAllele(int index) {
      return this.alleles.get(index);
   }

   public Allele getAllele1(int pairIndex) {
      return this.alleles.get(pairIndex * 2);
   }

   public Allele getAllele2(int pairIndex) {
      return this.alleles.get(pairIndex * 2 + 1);
   }

   public Allele getAllele1Safe(int pairIndex, Allele defaultAllele) {
      int idx = pairIndex * 2;
      return idx < this.alleles.size() ? this.alleles.get(idx) : defaultAllele;
   }

   public Allele getAllele2Safe(int pairIndex, Allele defaultAllele) {
      int idx = pairIndex * 2 + 1;
      return idx < this.alleles.size() ? this.alleles.get(idx) : defaultAllele;
   }

   public int getPairCount() {
      return this.alleles.size() / 2;
   }

   public Allele[] getAllelePair(int pairIndex) {
      return new Allele[]{this.getAllele1(pairIndex), this.getAllele2(pairIndex)};
   }

   public boolean isHomozygous(int pairIndex) {
      return this.getAllele1(pairIndex).equals(this.getAllele2(pairIndex));
   }

   public boolean isHomozygous(int pairIndex, String alleleId) {
      Allele a1 = this.getAllele1(pairIndex);
      Allele a2 = this.getAllele2(pairIndex);
      return a1.getId().equals(alleleId) && a2.getId().equals(alleleId);
   }

   @Override
   public String toString() {
      StringBuilder sb = new StringBuilder();

      for (int i = 0; i < this.alleles.size(); i += 2) {
         if (i > 0) {
            sb.append("-");
         }

         sb.append(this.alleles.get(i).getId());
         sb.append(this.alleles.get(i + 1).getId());
      }

      return sb.toString();
   }

   public String toCompactString() {
      StringBuilder sb = new StringBuilder();
      int attributeStart = this.getPairCount() - 6;

      for (int i = 0; i < attributeStart; i++) {
         Allele c1 = this.getAllele1(i);
         Allele c2 = this.getAllele2(i);
         String id1 = c1.getId();
         String prefix = id1.replaceAll("[0-9]", "");
         if (!prefix.equals("m")) {
            if (sb.length() > 0) {
               sb.append(" ");
            }

            sb.append(prefix).append(":").append(id1.replaceAll("[^0-9]", "")).append(c2.getId().replaceAll("[^0-9]", ""));
            if (i + 2 < attributeStart) {
               Allele m1A = this.getAllele1(i + 1);
               Allele m1B = this.getAllele2(i + 1);
               Allele m2A = this.getAllele1(i + 2);
               Allele m2B = this.getAllele2(i + 2);
               if (m1A.getId().startsWith("m") && m2A.getId().startsWith("m")) {
                  sb.append("[")
                     .append(m1A.getId().substring(1))
                     .append(m1B.getId().substring(1))
                     .append(",")
                     .append(m2A.getId().substring(1))
                     .append(m2B.getId().substring(1))
                     .append("]");
                  i += 2;
               }
            }
         }
      }

      sb.append(" | ");

      for (int i = attributeStart; i < this.getPairCount(); i++) {
         if (i > attributeStart) {
            sb.append(",");
         }

         Allele a1 = this.getAllele1(i);
         Allele a2 = this.getAllele2(i);
         String label = a1.getId().replaceAll("[0-9]", "");
         sb.append(label).append(":").append(a1.getId().replaceAll("[^0-9]", "")).append("/").append(a2.getId().replaceAll("[^0-9]", ""));
      }

      return sb.toString();
   }

   @Override
   public boolean equals(Object o) {
      if (this == o) {
         return true;
      } else {
         return !(o instanceof Genotype genotype) ? false : this.alleles.equals(genotype.alleles);
      }
   }

   @Override
   public int hashCode() {
      return this.alleles.hashCode();
   }

   public static Genotype.Builder builder() {
      return new Genotype.Builder();
   }

   public static class Builder {
      private final List<Allele> alleles = new ArrayList<>();

      public Genotype.Builder addPair(Allele allele1, Allele allele2) {
         Objects.requireNonNull(allele1, "Allele 1 cannot be null");
         Objects.requireNonNull(allele2, "Allele 2 cannot be null");
         this.alleles.add(allele1);
         this.alleles.add(allele2);
         return this;
      }

      public Genotype.Builder addHomozygousPair(Allele allele) {
         return this.addPair(allele, allele);
      }

      public Genotype.Builder addPair(String allele1Id, String allele2Id) {
         return this.addPair(Allele.of(allele1Id), Allele.of(allele2Id));
      }

      public Genotype build() {
         if (this.alleles.isEmpty()) {
            throw new IllegalStateException("Genotype must have at least one allele pair");
         } else if (this.alleles.size() % 2 != 0) {
            throw new IllegalStateException("Genotype must have an even number of alleles (diploid pairs)");
         } else {
            return new Genotype(this.alleles);
         }
      }
   }
}
